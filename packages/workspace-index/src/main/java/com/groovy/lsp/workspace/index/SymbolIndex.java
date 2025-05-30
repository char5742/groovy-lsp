package com.groovy.lsp.workspace.index;

import com.groovy.lsp.workspace.WorkspaceIndexer.SymbolInfo;
import com.groovy.lsp.workspace.WorkspaceIndexer.SymbolKind;
import org.lmdbjava.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Manages the symbol database using LMDB for high-performance lookups.
 * Provides fast symbol search and navigation capabilities.
 */
public class SymbolIndex implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(SymbolIndex.class);
    
    private static final String DB_SYMBOLS = "symbols";
    private static final String DB_FILES = "files";
    private static final String DB_DEPENDENCIES = "dependencies";
    
    private final Path indexPath;
    private Env<ByteBuffer> env;
    private Dbi<ByteBuffer> symbolsDb;
    private Dbi<ByteBuffer> filesDb;
    private Dbi<ByteBuffer> dependenciesDb;
    
    // In-memory caches for frequently accessed data
    private final Map<Path, Set<String>> fileSymbols = new ConcurrentHashMap<>();
    private final Map<String, List<SymbolInfo>> symbolCache = new ConcurrentHashMap<>();
    
    public SymbolIndex(Path indexPath) {
        this.indexPath = indexPath;
    }
    
    /**
     * Initialize the LMDB environment and databases.
     */
    public void initialize() {
        try {
            // Create index directory if it doesn't exist
            Files.createDirectories(indexPath);
            
            // Configure LMDB environment
            env = Env.create()
                .setMaxDbs(3)
                .setMapSize(1024L * 1024L * 1024L) // 1GB initial size
                .open(indexPath.toFile());
            
            // Open databases
            symbolsDb = env.openDbi(DB_SYMBOLS, DbiFlags.MDB_CREATE);
            filesDb = env.openDbi(DB_FILES, DbiFlags.MDB_CREATE);
            dependenciesDb = env.openDbi(DB_DEPENDENCIES, DbiFlags.MDB_CREATE);
            
            logger.info("Symbol index initialized at: {}", indexPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize symbol index", e);
        }
    }
    
    /**
     * Add a file to the index.
     */
    public void addFile(Path file) {
        try (Txn<ByteBuffer> txn = env.txnWrite()) {
            ByteBuffer key = toBuffer(file.toString());
            ByteBuffer value = toBuffer(Long.toString(System.currentTimeMillis()));
            
            filesDb.put(txn, key, value);
            txn.commit();
            
            // Clear cache for this file
            fileSymbols.remove(file);
        }
    }
    
    /**
     * Remove a file from the index.
     */
    public void removeFile(Path file) {
        try (Txn<ByteBuffer> txn = env.txnWrite()) {
            ByteBuffer key = toBuffer(file.toString());
            filesDb.delete(txn, key);
            
            // Remove all symbols from this file
            Set<String> symbols = fileSymbols.remove(file);
            if (symbols != null) {
                for (String symbol : symbols) {
                    removeSymbolEntry(txn, symbol, file);
                }
            }
            
            txn.commit();
        }
    }
    
    /**
     * Add a dependency to the index.
     */
    public void addDependency(Path dependency) {
        try (Txn<ByteBuffer> txn = env.txnWrite()) {
            ByteBuffer key = toBuffer(dependency.toString());
            ByteBuffer value = toBuffer(Long.toString(System.currentTimeMillis()));
            
            dependenciesDb.put(txn, key, value);
            txn.commit();
        }
    }
    
    /**
     * Add a symbol to the index.
     */
    public void addSymbol(SymbolInfo symbol) {
        try (Txn<ByteBuffer> txn = env.txnWrite()) {
            String symbolKey = createSymbolKey(symbol);
            ByteBuffer key = toBuffer(symbolKey);
            ByteBuffer value = serializeSymbol(symbol);
            
            symbolsDb.put(txn, key, value);
            
            // Update file symbols cache
            fileSymbols.computeIfAbsent(symbol.location(), k -> ConcurrentHashMap.newKeySet())
                      .add(symbol.name());
            
            // Invalidate symbol cache
            symbolCache.remove(symbol.name());
            
            txn.commit();
        }
    }
    
    /**
     * Search for symbols matching the query.
     */
    public Stream<SymbolInfo> search(String query) {
        // Check cache first
        List<SymbolInfo> cached = symbolCache.get(query);
        if (cached != null) {
            return cached.stream();
        }
        
        List<SymbolInfo> results = new ArrayList<>();
        
        try (Txn<ByteBuffer> txn = env.txnRead()) {
            try (Cursor<ByteBuffer> cursor = symbolsDb.openCursor(txn)) {
                ByteBuffer queryBuffer = toBuffer(query);
                
                // Prefix search
                if (cursor.get(queryBuffer, GetOp.MDB_SET_RANGE)) {
                    do {
                        String key = toString(cursor.key());
                        if (!key.startsWith(query)) {
                            break;
                        }
                        
                        SymbolInfo symbol = deserializeSymbol(cursor.val());
                        if (symbol != null) {
                            results.add(symbol);
                        }
                    } while (cursor.next());
                }
            }
        }
        
        // Cache results
        symbolCache.put(query, results);
        
        return results.stream();
    }
    
    /**
     * Get all symbols in a file.
     */
    public Stream<SymbolInfo> getFileSymbols(Path file) {
        List<SymbolInfo> symbols = new ArrayList<>();
        
        try (Txn<ByteBuffer> txn = env.txnRead()) {
            try (Cursor<ByteBuffer> cursor = symbolsDb.openCursor(txn)) {
                if (cursor.first()) {
                    do {
                        SymbolInfo symbol = deserializeSymbol(cursor.val());
                        if (symbol != null && symbol.location().equals(file)) {
                            symbols.add(symbol);
                        }
                    } while (cursor.next());
                }
            }
        }
        
        return symbols.stream();
    }
    
    /**
     * Create a unique key for a symbol.
     */
    private String createSymbolKey(SymbolInfo symbol) {
        return String.format("%s:%s:%s:%d:%d",
            symbol.name(),
            symbol.kind(),
            symbol.location(),
            symbol.line(),
            symbol.column());
    }
    
    /**
     * Remove a symbol entry for a specific file.
     */
    private void removeSymbolEntry(Txn<ByteBuffer> txn, String symbolName, Path file) {
        try (Cursor<ByteBuffer> cursor = symbolsDb.openCursor(txn)) {
            ByteBuffer key = toBuffer(symbolName);
            if (cursor.get(key, GetOp.MDB_SET_RANGE)) {
                do {
                    String keyStr = toString(cursor.key());
                    if (!keyStr.startsWith(symbolName)) {
                        break;
                    }
                    
                    SymbolInfo symbol = deserializeSymbol(cursor.val());
                    if (symbol != null && symbol.location().equals(file)) {
                        cursor.delete();
                    }
                } while (cursor.next());
            }
        }
    }
    
    /**
     * Serialize a symbol to ByteBuffer.
     */
    private ByteBuffer serializeSymbol(SymbolInfo symbol) {
        String data = String.format("%s|%s|%s|%d|%d",
            symbol.name(),
            symbol.kind(),
            symbol.location(),
            symbol.line(),
            symbol.column());
        return toBuffer(data);
    }
    
    /**
     * Deserialize a symbol from ByteBuffer.
     */
    private SymbolInfo deserializeSymbol(ByteBuffer buffer) {
        try {
            String data = toString(buffer);
            String[] parts = data.split("\\|");
            if (parts.length == 5) {
                return new SymbolInfo(
                    parts[0],
                    SymbolKind.valueOf(parts[1]),
                    Path.of(parts[2]),
                    Integer.parseInt(parts[3]),
                    Integer.parseInt(parts[4])
                );
            }
        } catch (Exception e) {
            logger.error("Failed to deserialize symbol", e);
        }
        return null;
    }
    
    /**
     * Convert string to ByteBuffer.
     */
    private ByteBuffer toBuffer(String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
        buffer.put(bytes).flip();
        return buffer;
    }
    
    /**
     * Convert ByteBuffer to string.
     */
    private String toString(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    @Override
    public void close() throws Exception {
        if (env != null) {
            env.close();
        }
    }
}