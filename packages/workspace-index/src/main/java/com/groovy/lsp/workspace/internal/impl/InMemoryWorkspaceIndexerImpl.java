package com.groovy.lsp.workspace.internal.impl;

import com.groovy.lsp.shared.workspace.api.WorkspaceIndexService;
import com.groovy.lsp.shared.workspace.api.dto.SymbolInfo;
import com.groovy.lsp.workspace.internal.parser.GroovyFileParser;
import com.groovy.lsp.workspace.internal.parser.JavaFileParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple in-memory implementation of WorkspaceIndexService.
 * This implementation stores all symbols in memory without persistence.
 * Used as a temporary workaround for LMDB native library issues.
 */
public class InMemoryWorkspaceIndexerImpl implements WorkspaceIndexService {
    private static final Logger logger =
            LoggerFactory.getLogger(InMemoryWorkspaceIndexerImpl.class);

    private final Path workspaceRoot;
    private final Map<String, List<SymbolInfo>> symbolsByFile = new ConcurrentHashMap<>();
    private final Map<String, List<SymbolInfo>> symbolsByName = new ConcurrentHashMap<>();
    private final GroovyFileParser groovyFileParser;
    private final JavaFileParser javaFileParser;

    public InMemoryWorkspaceIndexerImpl(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
        this.groovyFileParser = new GroovyFileParser();
        this.javaFileParser = new JavaFileParser();
        logger.info("Created InMemoryWorkspaceIndexerImpl for: {}", workspaceRoot);
    }

    @Override
    public CompletableFuture<Void> initialize() {
        logger.info("Initializing in-memory workspace indexer for: {}", workspaceRoot);

        return CompletableFuture.runAsync(
                () -> {
                    try {
                        indexWorkspaceFiles();
                        logger.info("In-memory workspace indexing completed");
                    } catch (Exception e) {
                        logger.error("Failed to initialize in-memory workspace indexer", e);
                        throw new RuntimeException(e);
                    }
                });
    }

    private void indexWorkspaceFiles() {
        try (var paths = Files.walk(workspaceRoot)) {
            var sourceFiles =
                    paths.filter(Files::isRegularFile)
                            .filter(path -> isGroovyFile(path) || isJavaFile(path))
                            .toList();

            logger.info("Found {} source files to index", sourceFiles.size());

            for (Path file : sourceFiles) {
                try {
                    List<SymbolInfo> symbols;
                    if (isGroovyFile(file)) {
                        symbols = groovyFileParser.parseFile(file);
                    } else if (isJavaFile(file)) {
                        symbols = javaFileParser.parseFile(file);
                    } else {
                        continue;
                    }

                    // Store symbols by file
                    symbolsByFile.put(file.toString(), symbols);

                    // Store symbols by name
                    for (SymbolInfo symbol : symbols) {
                        symbolsByName
                                .computeIfAbsent(symbol.name(), k -> new ArrayList<>())
                                .add(symbol);
                    }

                    logger.debug("Indexed {} symbols from {}", symbols.size(), file);
                } catch (Exception e) {
                    logger.error("Failed to index file: {}", file, e);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to walk workspace directory", e);
        }
    }

    @Override
    public CompletableFuture<Void> updateFile(Path file) {
        return CompletableFuture.runAsync(
                () -> {
                    try {
                        // Remove old symbols for this file
                        List<SymbolInfo> oldSymbols = symbolsByFile.remove(file.toString());
                        if (oldSymbols != null) {
                            for (SymbolInfo symbol : oldSymbols) {
                                List<SymbolInfo> byName = symbolsByName.get(symbol.name());
                                if (byName != null) {
                                    byName.removeIf(s -> s.location().equals(symbol.location()));
                                }
                            }
                        }

                        // Parse and add new symbols
                        if (isGroovyFile(file) || isJavaFile(file)) {
                            List<SymbolInfo> symbols;
                            if (isGroovyFile(file)) {
                                symbols = groovyFileParser.parseFile(file);
                            } else {
                                symbols = javaFileParser.parseFile(file);
                            }

                            symbolsByFile.put(file.toString(), symbols);
                            for (SymbolInfo symbol : symbols) {
                                symbolsByName
                                        .computeIfAbsent(symbol.name(), k -> new ArrayList<>())
                                        .add(symbol);
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Failed to update file: {}", file, e);
                    }
                });
    }

    @Override
    public CompletableFuture<Stream<SymbolInfo>> searchSymbols(String query) {
        return CompletableFuture.supplyAsync(
                () -> {
                    String lowerQuery = query.toLowerCase(Locale.ROOT);
                    return symbolsByName.entrySet().stream()
                            .filter(
                                    entry ->
                                            entry.getKey()
                                                    .toLowerCase(Locale.ROOT)
                                                    .contains(lowerQuery))
                            .flatMap(entry -> entry.getValue().stream());
                });
    }

    @Override
    public void shutdown() {
        symbolsByFile.clear();
        symbolsByName.clear();
    }

    // Helper methods (not part of interface)
    public List<SymbolInfo> findSymbolsByName(String name) {
        return symbolsByName.getOrDefault(name, Collections.emptyList());
    }

    public List<SymbolInfo> findSymbolsInFile(Path file) {
        return symbolsByFile.getOrDefault(file.toString(), Collections.emptyList());
    }

    private boolean isGroovyFile(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".groovy") || name.endsWith(".gradle");
    }

    private boolean isJavaFile(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".java");
    }
}
