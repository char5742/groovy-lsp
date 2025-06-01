package com.groovy.lsp.protocol.api;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.groovy.lsp.protocol.internal.impl.GroovyTextDocumentService;
import com.groovy.lsp.protocol.internal.impl.GroovyWorkspaceService;
import com.groovy.lsp.server.launcher.di.ServiceRouter;
import com.groovy.lsp.server.launcher.di.ServerExecutor;
import com.groovy.lsp.server.launcher.di.ScheduledServerExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.google.inject.Inject;
import org.jspecify.annotations.Nullable;

/**
 * Main Language Server implementation for Groovy.
 * 
 * This class implements the LSP LanguageServer interface and coordinates
 * between the various service implementations. It receives requests from 
 * LSP clients and delegates to domain services.
 */
public class GroovyLanguageServer implements LanguageServer, LanguageClientAware {
    
    private static final Logger logger = LoggerFactory.getLogger(GroovyLanguageServer.class);
    
    private final GroovyTextDocumentService textDocumentService;
    private final GroovyWorkspaceService workspaceService;
    private final ServiceRouter serviceRouter;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;
    private @Nullable LanguageClient client;
    private int errorCode = 1;
    
    @Inject
    public GroovyLanguageServer(
            ServiceRouter serviceRouter,
            @ServerExecutor ExecutorService executorService,
            @ScheduledServerExecutor ScheduledExecutorService scheduledExecutorService) {
        this.serviceRouter = serviceRouter;
        this.executorService = executorService;
        this.scheduledExecutorService = scheduledExecutorService;
        this.textDocumentService = new GroovyTextDocumentService();
        this.workspaceService = new GroovyWorkspaceService();
    }
    
    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        logger.info("Initializing Groovy Language Server");
        
        InitializeResult result = new InitializeResult(new ServerCapabilities());
        
        // Set server capabilities
        ServerCapabilities capabilities = result.getCapabilities();
        
        // Text document sync
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Incremental);
        
        // Completion support
        capabilities.setCompletionProvider(new CompletionOptions());
        
        // Hover support
        capabilities.setHoverProvider(true);
        
        // Definition support
        capabilities.setDefinitionProvider(true);
        
        // References support
        capabilities.setReferencesProvider(true);
        
        // Document symbols
        capabilities.setDocumentSymbolProvider(true);
        
        // Workspace symbols
        capabilities.setWorkspaceSymbolProvider(true);
        
        // Code actions
        capabilities.setCodeActionProvider(true);
        
        // Code lens
        capabilities.setCodeLensProvider(new CodeLensOptions());
        
        // Document formatting
        capabilities.setDocumentFormattingProvider(true);
        capabilities.setDocumentRangeFormattingProvider(true);
        
        // Rename
        capabilities.setRenameProvider(true);
        
        // Folding range
        capabilities.setFoldingRangeProvider(true);
        
        logger.info("Server capabilities configured");
        
        return CompletableFuture.completedFuture(result);
    }
    
    @Override
    public CompletableFuture<Object> shutdown() {
        logger.info("Shutting down Groovy Language Server");
        
        // Shutdown executor services
        shutdownExecutors();
        
        errorCode = 0;
        return CompletableFuture.completedFuture(null);
    }
    
    private void shutdownExecutors() {
        logger.info("Shutting down executor services");
        
        // Shutdown main executor
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Main executor did not terminate in time, forcing shutdown");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted while waiting for main executor termination", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Shutdown scheduled executor
        scheduledExecutorService.shutdown();
        try {
            if (!scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Scheduled executor did not terminate in time, forcing shutdown");
                scheduledExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted while waiting for scheduled executor termination", e);
            scheduledExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("Executor services shut down");
    }
    
    @Override
    public void exit() {
        logger.info("Exiting Groovy Language Server");
        System.exit(errorCode);
    }
    
    @Override
    public TextDocumentService getTextDocumentService() {
        return textDocumentService;
    }
    
    @Override
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }
    
    @Override
    public void connect(LanguageClient client) {
        logger.info("Connected to language client");
        this.client = client;
        
        // Pass client to services that need it
        if (textDocumentService != null) {
            ((LanguageClientAware) textDocumentService).connect(client);
        }
        if (workspaceService != null) {
            ((LanguageClientAware) workspaceService).connect(client);
        }
    }
    
    /**
     * Get the connected language client.
     * 
     * @return the language client
     */
    public @Nullable LanguageClient getClient() {
        return client;
    }
}