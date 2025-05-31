package com.groovy.lsp.workspace.internal.index;

import com.groovy.lsp.workspace.api.dto.SymbolInfo;
import com.groovy.lsp.workspace.api.dto.SymbolKind;
import org.lmdbjava.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.Objects;

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
    private @Nullable Env<ByteBuffer> env;
    private @Nullable Dbi<ByteBuffer> symbolsDb;
    private @Nullable Dbi<ByteBuffer> filesDb;
    private @Nullable Dbi<ByteBuffer> dependenciesDb;
    private boolean initialized = false;
    
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
            this.initialized = true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize symbol index", e);
        }
    }
    
    /**
     * Check if the index has been initialized.
     */
    private void checkInitialized() {
        if (!initialized || env == null || symbolsDb == null || filesDb == null || dependenciesDb == null) {
            throw new IllegalStateException("Symbol index is not initialized. Call initialize() first.");
        }
    }
    
    /**
     * Get the environment, ensuring it's initialized.
     */
    private Env<ByteBuffer> getEnv() {
        checkInitialized();
        return Objects.requireNonNull(env, "env should not be null after checkInitialized()");
    }
    
    /**
     * Get the symbols database, ensuring it's initialized.
     */
    private Dbi<ByteBuffer> getSymbolsDb() {
        checkInitialized();
        return Objects.requireNonNull(symbolsDb, "symbolsDb should not be null after checkInitialized()");
    }
    
    /**
     * Get the files database, ensuring it's initialized.
     */
    private Dbi<ByteBuffer> getFilesDb() {
        checkInitialized();
        return Objects.requireNonNull(filesDb, "filesDb should not be null after checkInitialized()");
    }
    
    /**
     * Get the dependencies database, ensuring it's initialized.
     */
    private Dbi<ByteBuffer> getDependenciesDb() {
        checkInitialized();
        return Objects.requireNonNull(dependenciesDb, "dependenciesDb should not be null after checkInitialized()");
    }
    
    /**
     * Add a file to the index.
     */
    public void addFile(Path file) {
        try (Txn<ByteBuffer> txn = getEnv().txnWrite()) {
            ByteBuffer key = toBuffer(file.toString());
            ByteBuffer value = toBuffer(Long.toString(System.currentTimeMillis()));
            
            getFilesDb().put(txn, key, value);
            txn.commit();
            
            // Clear cache for this file
            fileSymbols.remove(file);
        }
    }
    
    /**
     * Remove a file from the index.
     */
    public void removeFile(Path file) {
        checkInitialized();
        try (Txn<ByteBuffer> txn = getEnv().txnWrite()) {
            ByteBuffer key = toBuffer(file.toString());
            getFilesDb().delete(txn, key);
            
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
        checkInitialized();
        try (Txn<ByteBuffer> txn = getEnv().txnWrite()) {
            ByteBuffer key = toBuffer(dependency.toString());
            ByteBuffer value = toBuffer(Long.toString(System.currentTimeMillis()));
            
            getDependenciesDb().put(txn, key, value);
            txn.commit();
        }
    }
    
    /**
     * Add a symbol to the index.
     */
    public void addSymbol(SymbolInfo symbol) {
        checkInitialized();
        try (Txn<ByteBuffer> txn = getEnv().txnWrite()) {
            String symbolKey = createSymbolKey(symbol);
            ByteBuffer key = toBuffer(symbolKey);
            ByteBuffer value = serializeSymbol(symbol);
            
            getSymbolsDb().put(txn, key, value);
            
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
        checkInitialized();
        // Check cache first
        List<SymbolInfo> cached = symbolCache.get(query);
        if (cached != null) {
            return cached.stream();
        }
        
        List<SymbolInfo> results = new ArrayList<>();
        
        try (Txn<ByteBuffer> txn = getEnv().txnRead()) {
            try (Cursor<ByteBuffer> cursor = getSymbolsDb().openCursor(txn)) {
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
        checkInitialized();
        List<SymbolInfo> symbols = new ArrayList<>();
        
        try (Txn<ByteBuffer> txn = getEnv().txnRead()) {
            try (Cursor<ByteBuffer> cursor = getSymbolsDb().openCursor(txn)) {
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
        try (Cursor<ByteBuffer> cursor = getSymbolsDb().openCursor(txn)) {
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
    private @Nullable SymbolInfo deserializeSymbol(ByteBuffer buffer) {
        try {
            String data = toString(buffer);
            List<String> parts = Arrays.asList(data.split("\\|"));
            if (parts.size() == 5) {
                return new SymbolInfo(
                    parts.get(0),
                    SymbolKind.valueOf(parts.get(1)),
                    Path.of(parts.get(2)),
                    Integer.parseInt(parts.get(3)),
                    Integer.parseInt(parts.get(4))
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
            // Clear caches
            fileSymbols.clear();
            symbolCache.clear();
            
            // Force sync before closing
            env.sync(true);
            
            // Close environment
            env.close();
            env = null;
            symbolsDb = null;
            filesDb = null;
            dependenciesDb = null;
            initialized = false;
        }
    }
}