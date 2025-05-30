package com.groovy.lsp.workspace;

import com.groovy.lsp.workspace.dependency.DependencyResolver;
import com.groovy.lsp.workspace.index.SymbolIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * Main service for indexing workspace files and dependencies.
 * Coordinates between dependency resolution and symbol indexing.
 */
public class WorkspaceIndexer implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(WorkspaceIndexer.class);
    
    private final Path workspaceRoot;
    private final SymbolIndex symbolIndex;
    private final DependencyResolver dependencyResolver;
    private final ExecutorService executorService;
    
    public WorkspaceIndexer(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
        this.symbolIndex = new SymbolIndex(workspaceRoot.resolve(".groovy-lsp/index"));
        this.dependencyResolver = new DependencyResolver(workspaceRoot);
        this.executorService = Executors.newWorkStealingPool();
    }
    
    /**
     * Initialize the workspace index.
     * This includes setting up the symbol database and scanning for build files.
     */
    public CompletableFuture<Void> initialize() {
        logger.info("Initializing workspace indexer for: {}", workspaceRoot);
        
        return CompletableFuture.runAsync(() -> {
            try {
                // Initialize symbol index
                symbolIndex.initialize();
                
                // Detect and resolve dependencies
                dependencyResolver.detectBuildSystem();
                var dependencies = dependencyResolver.resolveDependencies();
                
                // Index workspace files
                indexWorkspaceFiles();
                
                // Index dependency files
                dependencies.forEach(this::indexDependency);
                
                logger.info("Workspace indexing completed");
            } catch (Exception e) {
                logger.error("Failed to initialize workspace indexer", e);
                throw new RuntimeException(e);
            }
        }, executorService);
    }
    
    /**
     * Index all Groovy and Java files in the workspace.
     */
    private void indexWorkspaceFiles() {
        try (Stream<Path> paths = Files.walk(workspaceRoot)) {
            paths.filter(this::shouldIndexFile)
                 .forEach(this::indexFile);
        } catch (Exception e) {
            logger.error("Error indexing workspace files", e);
        }
    }
    
    /**
     * Index a single file.
     */
    private void indexFile(Path file) {
        try {
            logger.debug("Indexing file: {}", file);
            // TODO: Parse file and extract symbols
            // For now, just register the file
            symbolIndex.addFile(file);
        } catch (Exception e) {
            logger.error("Error indexing file: {}", file, e);
        }
    }
    
    /**
     * Index symbols from a dependency.
     */
    private void indexDependency(Path dependency) {
        try {
            logger.debug("Indexing dependency: {}", dependency);
            // TODO: Extract and index symbols from JAR/directory
            symbolIndex.addDependency(dependency);
        } catch (Exception e) {
            logger.error("Error indexing dependency: {}", dependency, e);
        }
    }
    
    /**
     * Check if a file should be indexed.
     */
    private boolean shouldIndexFile(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        
        String fileName = path.getFileName().toString();
        return fileName.endsWith(".groovy") || 
               fileName.endsWith(".java") ||
               fileName.endsWith(".gradle") ||
               fileName.endsWith(".gradle.kts");
    }
    
    /**
     * Update index for a single file change.
     */
    public CompletableFuture<Void> updateFile(Path file) {
        return CompletableFuture.runAsync(() -> {
            if (Files.exists(file)) {
                indexFile(file);
            } else {
                symbolIndex.removeFile(file);
            }
        }, executorService);
    }
    
    /**
     * Search for symbols matching the given query.
     */
    public CompletableFuture<Stream<SymbolInfo>> searchSymbols(String query) {
        return CompletableFuture.supplyAsync(() -> 
            symbolIndex.search(query), executorService);
    }
    
    /**
     * Get the symbol index for direct access.
     */
    public SymbolIndex getSymbolIndex() {
        return symbolIndex;
    }
    
    @Override
    public void close() {
        executorService.shutdown();
        try {
            symbolIndex.close();
        } catch (Exception e) {
            logger.error("Error closing symbol index", e);
        }
    }
    
    /**
     * Information about a symbol in the workspace.
     */
    public record SymbolInfo(
        String name,
        SymbolKind kind,
        Path location,
        int line,
        int column
    ) {}
    
    /**
     * Types of symbols that can be indexed.
     */
    public enum SymbolKind {
        CLASS,
        INTERFACE,
        TRAIT,
        METHOD,
        FIELD,
        PROPERTY,
        CONSTRUCTOR,
        ENUM,
        ENUM_CONSTANT
    }
}