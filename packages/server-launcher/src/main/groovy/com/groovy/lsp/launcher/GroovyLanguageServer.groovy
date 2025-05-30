package com.groovy.lsp.launcher

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.services.*
import java.util.concurrent.CompletableFuture

class GroovyLanguageServer implements LanguageServer, LanguageClientAware {
    private LanguageClient client
    private WorkspaceService workspaceService
    private TextDocumentService textDocumentService
    
    GroovyLanguageServer() {
        this.workspaceService = new GroovyWorkspaceService()
        this.textDocumentService = new GroovyTextDocumentService()
    }
    
    @Override
    CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        InitializeResult result = new InitializeResult()
        ServerCapabilities capabilities = new ServerCapabilities()
        
        // Set server capabilities
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full)
        capabilities.setCompletionProvider(new CompletionOptions(false, ['.', '?', '&', '*'] as List))
        capabilities.setHoverProvider(true)
        capabilities.setDefinitionProvider(true)
        capabilities.setReferencesProvider(true)
        capabilities.setRenameProvider(true)
        
        result.setCapabilities(capabilities)
        return CompletableFuture.completedFuture(result)
    }
    
    @Override
    CompletableFuture<Object> shutdown() {
        return CompletableFuture.completedFuture(null)
    }
    
    @Override
    void exit() {
        System.exit(0)
    }
    
    @Override
    TextDocumentService getTextDocumentService() {
        return textDocumentService
    }
    
    @Override
    WorkspaceService getWorkspaceService() {
        return workspaceService
    }
    
    @Override
    void connect(LanguageClient client) {
        this.client = client
    }
}