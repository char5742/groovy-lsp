package com.groovy.lsp.launcher

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.TextDocumentService
import java.util.concurrent.CompletableFuture

class GroovyTextDocumentService implements TextDocumentService {
    
    @Override
    void didOpen(DidOpenTextDocumentParams params) {
        // TODO: Implement file open handling
    }
    
    @Override
    void didChange(DidChangeTextDocumentParams params) {
        // TODO: Implement file change handling
    }
    
    @Override
    void didClose(DidCloseTextDocumentParams params) {
        // TODO: Implement file close handling
    }
    
    @Override
    void didSave(DidSaveTextDocumentParams params) {
        // TODO: Implement file save handling
    }
    
    @Override
    CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
        // TODO: Implement code completion
        return CompletableFuture.completedFuture(Either.forLeft([]))
    }
    
    @Override
    CompletableFuture<Hover> hover(HoverParams params) {
        // TODO: Implement hover
        return CompletableFuture.completedFuture(null)
    }
    
    @Override
    CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(DefinitionParams params) {
        // TODO: Implement go to definition
        return CompletableFuture.completedFuture(Either.forLeft([]))
    }
    
    @Override
    CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
        // TODO: Implement find references
        return CompletableFuture.completedFuture([])
    }
    
    @Override
    CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
        // TODO: Implement rename
        return CompletableFuture.completedFuture(null)
    }
}