package com.groovy.lsp.workspace.api;

import com.groovy.lsp.workspace.api.dto.SymbolInfo;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.jmolecules.ddd.annotation.Service;

/**
 * Service interface for workspace indexing operations.
 * Provides methods to index and search symbols in a Groovy workspace.
 *
 * This is a domain service that orchestrates the indexing and searching
 * of symbols across the workspace and its dependencies.
 */
@Service
public interface WorkspaceIndexService {

    /**
     * Initializes the workspace index.
     * This method scans the workspace, resolves dependencies, and builds the initial index.
     *
     * @return a CompletableFuture that completes when initialization is done
     */
    CompletableFuture<Void> initialize();

    /**
     * Updates the index for a specific file.
     * This method should be called when a file is created, modified, or deleted.
     *
     * @param file the file to update in the index
     * @return a CompletableFuture that completes when the update is done
     */
    CompletableFuture<Void> updateFile(Path file);

    /**
     * Searches for symbols matching the given query.
     *
     * @param query the search query (supports prefix matching)
     * @return a CompletableFuture containing a stream of matching symbols
     */
    CompletableFuture<Stream<SymbolInfo>> searchSymbols(String query);

    /**
     * Shuts down the indexing service and releases resources.
     */
    void shutdown();
}
