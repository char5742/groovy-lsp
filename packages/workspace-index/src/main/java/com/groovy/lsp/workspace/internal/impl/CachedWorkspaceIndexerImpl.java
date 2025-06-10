package com.groovy.lsp.workspace.internal.impl;

import com.groovy.lsp.shared.event.EventBus;
import com.groovy.lsp.shared.event.EventBusFactory;
import com.groovy.lsp.shared.workspace.api.WorkspaceIndexService;
import com.groovy.lsp.shared.workspace.api.dto.SymbolInfo;
import com.groovy.lsp.shared.workspace.api.events.FileIndexedEvent;
import com.groovy.lsp.shared.workspace.api.events.WorkspaceIndexedEvent;
import com.groovy.lsp.workspace.dependency.MavenAndGradleDependencyResolver;
import com.groovy.lsp.workspace.dependency.cache.CachedDependencyResolver;
import com.groovy.lsp.workspace.dependency.cache.DependencyCache;
import com.groovy.lsp.workspace.dependency.cache.DependencyCacheFactory;
import com.groovy.lsp.workspace.internal.index.SymbolIndex;
import com.groovy.lsp.workspace.internal.jar.JarFileIndexer;
import com.groovy.lsp.workspace.internal.parser.GroovyFileParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhanced WorkspaceIndexer implementation with dependency caching support.
 * Extends the base implementation to add caching capabilities for improved performance.
 */
public class CachedWorkspaceIndexerImpl implements WorkspaceIndexService, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(CachedWorkspaceIndexerImpl.class);

    private final Path workspaceRoot;
    private final SymbolIndex symbolIndex;
    private final CachedDependencyResolver cachedDependencyResolver;
    private final ExecutorService executorService;
    private final EventBus eventBus;
    private final GroovyFileParser groovyFileParser;
    private final JarFileIndexer jarFileIndexer;
    private final DependencyCache dependencyCache;

    public CachedWorkspaceIndexerImpl(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
        this.symbolIndex = new SymbolIndex(workspaceRoot.resolve(".groovy-lsp/index"));

        // Get shared cache instance
        this.dependencyCache = DependencyCacheFactory.getInstance();

        // Create base resolver and wrap with cache
        var baseResolver = new MavenAndGradleDependencyResolver(workspaceRoot);
        this.cachedDependencyResolver =
                new CachedDependencyResolver(baseResolver, dependencyCache, workspaceRoot);

        this.executorService = Executors.newWorkStealingPool();
        this.eventBus = EventBusFactory.getInstance();
        this.groovyFileParser = new GroovyFileParser();
        this.jarFileIndexer = new JarFileIndexer();
    }

    @Override
    public CompletableFuture<Void> initialize() {
        logger.info("Initializing cached workspace indexer for: {}", workspaceRoot);

        return CompletableFuture.runAsync(
                () -> {
                    long startTime = System.currentTimeMillis();
                    int totalFiles = 0;
                    int totalSymbols = 0;

                    try {
                        // Initialize symbol index
                        symbolIndex.initialize();

                        // Resolve dependencies with caching
                        var dependencies = cachedDependencyResolver.resolveDependencies();

                        // Log cache statistics
                        var stats = cachedDependencyResolver.getCacheStatistics();
                        logger.info(
                                "Cache statistics - Hits: {}, Misses: {}, Memory: {}MB",
                                stats.getHitCount(),
                                stats.getMissCount(),
                                stats.getTotalMemoryUsageMB());

                        // Index workspace files
                        var workspaceStats = indexWorkspaceFilesWithStats();
                        totalFiles += workspaceStats.files;
                        totalSymbols += workspaceStats.symbols;

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

    @Override
    public CompletableFuture<Void> updateFile(Path file) {
        return CompletableFuture.runAsync(
                () -> {
                    try {
                        if (isBuildFile(file)) {
                            logger.info(
                                    "Build file changed, invalidating dependency cache: {}", file);
                            cachedDependencyResolver.invalidateCache();
                            // Re-initialize to update dependencies
                            initialize().join();
                        } else if (isGroovyFile(file)) {
                            // Update single file in index
                            var symbols = groovyFileParser.parseFile(file);
                            // Remove old symbols from this file first
                            symbolIndex.removeFile(file);
                            // Add the file to index
                            symbolIndex.addFile(file);
                            // Add each symbol individually
                            for (SymbolInfo symbol : symbols) {
                                symbolIndex.addSymbol(symbol);
                            }
                            eventBus.publish(new FileIndexedEvent(file, symbols));
                        }
                    } catch (Exception e) {
                        logger.error("Failed to update file: {}", file, e);
                        throw new RuntimeException(e);
                    }
                },
                executorService);
    }

    @Override
    public CompletableFuture<Stream<SymbolInfo>> searchSymbols(String query) {
        return CompletableFuture.supplyAsync(() -> symbolIndex.search(query), executorService);
    }

    @Override
    public void shutdown() {
        try {
            executorService.shutdown();
            symbolIndex.close();
            // Log final cache statistics
            var stats = dependencyCache.getStatistics();
            logger.info(
                    "Final cache statistics - Hits: {}, Misses: {}, Evictions: {}",
                    stats.getHitCount(),
                    stats.getMissCount(),
                    stats.getEvictionCount());
        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        }
    }

    @Override
    public void close() throws Exception {
        shutdown();
    }

    private IndexStats indexWorkspaceFilesWithStats() {
        int files = 0;
        int symbols = 0;

        try (var paths = Files.walk(workspaceRoot)) {
            var groovyFiles =
                    paths.filter(Files::isRegularFile).filter(this::isGroovyFile).toList();

            for (Path file : groovyFiles) {
                try {
                    var fileSymbols = groovyFileParser.parseFile(file);
                    // Add the file to index
                    symbolIndex.addFile(file);
                    // Add each symbol individually
                    for (SymbolInfo symbol : fileSymbols) {
                        symbolIndex.addSymbol(symbol);
                    }
                    files++;
                    symbols += fileSymbols.size();
                    eventBus.publish(new FileIndexedEvent(file, fileSymbols));
                } catch (Exception e) {
                    logger.warn("Failed to parse file: {}", file, e);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to walk workspace", e);
        }

        return new IndexStats(files, symbols);
    }

    private IndexStats indexDependency(Path dependency) {
        if (!Files.exists(dependency)) {
            return new IndexStats(0, 0);
        }

        if (dependency.toString().endsWith(".jar")) {
            try {
                var symbols = jarFileIndexer.indexJar(dependency);
                // Add dependency to index
                symbolIndex.addDependency(dependency);
                // Add each symbol individually
                for (SymbolInfo symbol : symbols) {
                    symbolIndex.addSymbol(symbol);
                }
                return new IndexStats(1, symbols.size());
            } catch (Exception e) {
                logger.warn("Failed to index JAR: {}", dependency, e);
                return new IndexStats(0, 0);
            }
        }

        return new IndexStats(0, 0);
    }

    private boolean isGroovyFile(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".groovy") || name.endsWith(".gradle");
    }

    private boolean isBuildFile(Path path) {
        String name = path.getFileName().toString();
        return name.equals("build.gradle")
                || name.equals("build.gradle.kts")
                || name.equals("pom.xml")
                || name.equals("settings.gradle")
                || name.equals("settings.gradle.kts");
    }

    private record IndexStats(int files, int symbols) {}
}
