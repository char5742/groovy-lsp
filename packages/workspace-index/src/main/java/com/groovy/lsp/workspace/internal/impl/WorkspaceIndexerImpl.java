package com.groovy.lsp.workspace.internal.impl;

import com.groovy.lsp.shared.event.EventBus;
import com.groovy.lsp.shared.event.EventBusFactory;
import com.groovy.lsp.shared.workspace.api.WorkspaceIndexService;
import com.groovy.lsp.shared.workspace.api.dto.SymbolInfo;
import com.groovy.lsp.workspace.api.events.FileIndexedEvent;
import com.groovy.lsp.workspace.api.events.WorkspaceIndexedEvent;
import com.groovy.lsp.workspace.internal.dependency.DependencyResolver;
import com.groovy.lsp.workspace.internal.index.SymbolIndex;
import com.groovy.lsp.workspace.internal.jar.JarFileIndexer;
import com.groovy.lsp.workspace.internal.parser.GroovyFileParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal implementation of WorkspaceIndexService.
 * Coordinates between dependency resolution and symbol indexing.
 */
public class WorkspaceIndexerImpl implements WorkspaceIndexService, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(WorkspaceIndexerImpl.class);

    private final Path workspaceRoot;
    private final SymbolIndex symbolIndex;
    private final DependencyResolver dependencyResolver;
    private final ExecutorService executorService;
    private final EventBus eventBus;
    private final GroovyFileParser groovyFileParser;
    private final JarFileIndexer jarFileIndexer;

    public WorkspaceIndexerImpl(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
        this.symbolIndex = new SymbolIndex(workspaceRoot.resolve(".groovy-lsp/index"));
        this.dependencyResolver = new DependencyResolver(workspaceRoot);
        this.executorService = Executors.newWorkStealingPool();
        this.eventBus = EventBusFactory.getInstance();
        this.groovyFileParser = new GroovyFileParser();
        this.jarFileIndexer = new JarFileIndexer();
    }

    /**
     * Initialize the workspace index.
     * This includes setting up the symbol database and scanning for build files.
     */
    @Override
    public CompletableFuture<Void> initialize() {
        logger.info("Initializing workspace indexer for: {}", workspaceRoot);

        return CompletableFuture.runAsync(
                () -> {
                    long startTime = System.currentTimeMillis();
                    int totalFiles = 0;
                    int totalSymbols = 0;

                    try {
                        // Initialize symbol index
                        symbolIndex.initialize();

                        // Detect and resolve dependencies
                        dependencyResolver.detectBuildSystem();
                        var dependencies = dependencyResolver.resolveDependencies();

                        // Index workspace files
                        var stats = indexWorkspaceFilesWithStats();
                        totalFiles += stats.files;
                        totalSymbols += stats.symbols;

                        // Index dependency files
                        for (Path dependency : dependencies) {
                            var depStats = indexDependency(dependency);
                            totalFiles += depStats.files;
                            totalSymbols += depStats.symbols;
                        }

                        long duration = System.currentTimeMillis() - startTime;
                        logger.info(
                                "Workspace indexing completed in {}ms - Files: {}, Symbols: {}",
                                duration,
                                totalFiles,
                                totalSymbols);

                        // Publish workspace indexed event
                        eventBus.publish(
                                new WorkspaceIndexedEvent(
                                        workspaceRoot, totalFiles, totalSymbols, duration));

                    } catch (Exception e) {
                        logger.error("Failed to initialize workspace indexer", e);
                        throw new RuntimeException(e);
                    }
                },
                executorService);
    }

    /**
     * Index workspace files and return statistics.
     */
    private IndexStats indexWorkspaceFilesWithStats() {
        IndexStats stats = new IndexStats();
        try (Stream<Path> paths = Files.walk(workspaceRoot)) {
            paths.filter(this::shouldIndexFile)
                    .forEach(
                            file -> {
                                var symbols = indexFileWithResult(file);
                                if (symbols != null) {
                                    stats.files++;
                                    stats.symbols += symbols.size();
                                }
                            });
        } catch (Exception e) {
            logger.error("Error indexing workspace files", e);
        }
        return stats;
    }

    /**
     * Simple class to hold indexing statistics.
     */
    private static class IndexStats {
        int files = 0;
        int symbols = 0;
    }

    /**
     * Index a single file.
     */
    private void indexFile(Path file) {
        var symbols = indexFileWithResult(file);
        if (symbols != null) {
            eventBus.publish(new FileIndexedEvent(file, symbols));
        }
    }

    /**
     * Index a single file and return the symbols found.
     */
    private List<SymbolInfo> indexFileWithResult(Path file) {
        try {
            logger.debug("Indexing file: {}", file);

            List<SymbolInfo> symbols = List.of();

            // Check if it's a Groovy file
            String fileName = file.getFileName().toString();
            if (fileName.endsWith(".groovy") || fileName.endsWith(".gradle")) {
                // Parse the file and extract symbols
                symbols = groovyFileParser.parseFile(file);

                // Add symbols to the index
                for (SymbolInfo symbol : symbols) {
                    symbolIndex.addSymbol(symbol);
                }
            } else if (fileName.endsWith(".java")) {
                // TODO: Implement Java file parsing
                logger.debug("Java file parsing not yet implemented: {}", file);
            } else if (fileName.endsWith(".gradle.kts")) {
                // TODO: Implement Kotlin script parsing
                logger.debug("Kotlin script parsing not yet implemented: {}", file);
            }

            // Register the file in the index
            symbolIndex.addFile(file);

            logger.debug("Indexed {} symbols from file: {}", symbols.size(), file);
            return symbols;
        } catch (Exception e) {
            logger.error("Error indexing file: {}", file, e);
            String errorMessage =
                    Objects.requireNonNullElse(
                            e.getMessage(), "Unknown error occurred while indexing file");
            eventBus.publish(new FileIndexedEvent(file, errorMessage));
            return List.of(); // Return empty list instead of null
        }
    }

    /**
     * Index symbols from a dependency.
     * Returns statistics about the indexed dependency.
     */
    private IndexStats indexDependency(Path dependency) {
        IndexStats stats = new IndexStats();
        try {
            logger.debug("Indexing dependency: {}", dependency);

            if (Files.isDirectory(dependency)) {
                // Index directory as a source directory
                try (Stream<Path> paths = Files.walk(dependency)) {
                    paths.filter(this::shouldIndexFile)
                            .forEach(
                                    file -> {
                                        var symbols = indexFileWithResult(file);
                                        if (symbols != null && !symbols.isEmpty()) {
                                            stats.files++;
                                            stats.symbols += symbols.size();
                                        }
                                    });
                }
            } else if (dependency.toString().endsWith(".jar")) {
                // Index JAR file
                List<SymbolInfo> jarSymbols = jarFileIndexer.indexJar(dependency);
                if (!jarSymbols.isEmpty()) {
                    stats.files = 1; // Count the JAR as one file
                    stats.symbols = jarSymbols.size();

                    // Add symbols to the index
                    for (SymbolInfo symbol : jarSymbols) {
                        symbolIndex.addSymbol(symbol);
                    }
                }
            }

            // Register the dependency in the index
            symbolIndex.addDependency(dependency);
        } catch (Exception e) {
            logger.error("Error indexing dependency: {}", dependency, e);
        }
        return stats;
    }

    /**
     * Check if a file should be indexed.
     */
    private boolean shouldIndexFile(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }

        String fileName = path.getFileName().toString();
        return fileName.endsWith(".groovy")
                || fileName.endsWith(".java")
                || fileName.endsWith(".gradle")
                || fileName.endsWith(".gradle.kts");
    }

    /**
     * Update index for a single file change.
     */
    @Override
    public CompletableFuture<Void> updateFile(Path file) {
        return CompletableFuture.runAsync(
                () -> {
                    if (Files.exists(file)) {
                        indexFile(file);
                    } else {
                        symbolIndex.removeFile(file);
                    }
                },
                executorService);
    }

    /**
     * Search for symbols matching the given query.
     */
    @Override
    public CompletableFuture<Stream<SymbolInfo>> searchSymbols(String query) {
        return CompletableFuture.supplyAsync(() -> symbolIndex.search(query), executorService);
    }

    /**
     * Get the symbol index for direct access.
     */
    public SymbolIndex getSymbolIndex() {
        return symbolIndex;
    }

    @Override
    public void shutdown() {
        close();
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
}
