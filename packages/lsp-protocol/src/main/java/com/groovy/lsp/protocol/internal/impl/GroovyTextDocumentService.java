package com.groovy.lsp.protocol.internal.impl;

import com.google.inject.Inject;
import com.groovy.lsp.protocol.api.IServiceRouter;
import com.groovy.lsp.protocol.internal.document.DocumentManager;
import com.groovy.lsp.protocol.internal.handler.DiagnosticsHandler;
import com.groovy.lsp.protocol.internal.handler.HoverHandler;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightParams;
import org.eclipse.lsp4j.DocumentOnTypeFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureHelpParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Text Document Service implementation for Groovy.
 *
 * This service handles all text document related operations like
 * completion, hover, diagnostics, formatting, etc.
 */
public class GroovyTextDocumentService implements TextDocumentService, LanguageClientAware {

    private static final Logger logger = LoggerFactory.getLogger(GroovyTextDocumentService.class);

    private @Nullable LanguageClient client;
    private @Nullable IServiceRouter serviceRouter;
    private @Nullable DocumentManager documentManager;
    private @Nullable DiagnosticsHandler diagnosticsHandler;

    @Inject
    public void setServiceRouter(IServiceRouter serviceRouter) {
        this.serviceRouter = serviceRouter;
        // Initialize diagnostics handler when both dependencies are available
        if (this.serviceRouter != null && this.documentManager != null) {
            this.diagnosticsHandler =
                    new DiagnosticsHandler(this.serviceRouter, this.documentManager);
        }
    }

    @Inject
    public void setDocumentManager(DocumentManager documentManager) {
        this.documentManager = documentManager;
        // Initialize diagnostics handler when both dependencies are available
        if (this.serviceRouter != null && this.documentManager != null) {
            this.diagnosticsHandler =
                    new DiagnosticsHandler(this.serviceRouter, this.documentManager);
        }
    }

    @Override
    public void connect(LanguageClient client) {
        this.client = client;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        logger.debug("Document opened: {}", params.getTextDocument().getUri());

        // Store document in document manager
        if (documentManager != null) {
            documentManager.openDocument(params.getTextDocument());
        }

        // Trigger diagnostics immediately on open
        if (client != null && diagnosticsHandler != null) {
            logger.debug("Triggering diagnostics for opened document");
            diagnosticsHandler
                    .handleDiagnosticsImmediate(params.getTextDocument().getUri(), client)
                    .exceptionally(
                            ex -> {
                                logger.error(
                                        "Failed to handle diagnostics for opened document", ex);
                                return null;
                            });
        }
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        logger.debug("Document changed: {}", params.getTextDocument().getUri());

        // Update document content
        if (documentManager != null && !params.getContentChanges().isEmpty()) {
            // For simplicity, assuming full document sync
            String newContent = params.getContentChanges().get(0).getText();
            documentManager.updateDocument(
                    params.getTextDocument().getUri(),
                    newContent,
                    params.getTextDocument().getVersion());

            // Trigger diagnostics with debouncing on change
            if (client != null && diagnosticsHandler != null) {
                logger.debug("Triggering debounced diagnostics for changed document");
                diagnosticsHandler
                        .handleDiagnosticsDebounced(params.getTextDocument().getUri(), client)
                        .exceptionally(
                                ex -> {
                                    logger.error(
                                            "Failed to handle diagnostics for changed document",
                                            ex);
                                    return null;
                                });
            }
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        logger.debug("Document closed: {}", params.getTextDocument().getUri());

        // Remove document from manager
        if (documentManager != null) {
            documentManager.closeDocument(params.getTextDocument().getUri());
        }
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        logger.debug("Document saved: {}", params.getTextDocument().getUri());

        // Trigger diagnostics immediately on save
        if (client != null && diagnosticsHandler != null) {
            logger.debug("Triggering diagnostics for saved document");
            diagnosticsHandler
                    .handleDiagnosticsImmediate(params.getTextDocument().getUri(), client)
                    .exceptionally(
                            ex -> {
                                logger.error("Failed to handle diagnostics for saved document", ex);
                                return null;
                            });
        }
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
            CompletionParams params) {
        logger.debug("Completion requested at: {}", params.getPosition());
        // TODO: Implement code completion
        return CompletableFuture.completedFuture(
                Either.forRight(new CompletionList(false, Collections.emptyList())));
    }

    @Override
    public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem item) {
        logger.debug("Resolving completion item: {}", item.getLabel());
        // TODO: Implement completion item resolution
        return CompletableFuture.completedFuture(item);
    }

    @Override
    public CompletableFuture<Hover> hover(HoverParams params) {
        logger.debug("Hover requested at: {}", params.getPosition());

        if (serviceRouter == null) {
            logger.error("ServiceRouter is not initialized");
            return CompletableFuture.completedFuture(null);
        }

        if (documentManager == null) {
            logger.error("DocumentManager is not initialized");
            return CompletableFuture.completedFuture(null);
        }

        HoverHandler handler = new HoverHandler(serviceRouter, documentManager);
        return handler.handleHover(params);
    }

    @Override
    public CompletableFuture<SignatureHelp> signatureHelp(SignatureHelpParams params) {
        logger.debug("Signature help requested at: {}", params.getPosition());
        // TODO: Implement signature help
        return CompletableFuture.completedFuture(
                new SignatureHelp(Collections.emptyList(), null, null));
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>>
            definition(DefinitionParams params) {
        logger.debug("Definition requested at: {}", params.getPosition());
        // TODO: Implement go to definition
        return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
    }

    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
        logger.debug("References requested at: {}", params.getPosition());
        // TODO: Implement find references
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(
            DocumentHighlightParams params) {
        logger.debug("Document highlight requested at: {}", params.getPosition());
        // TODO: Implement document highlight
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(
            DocumentSymbolParams params) {
        logger.debug("Document symbols requested for: {}", params.getTextDocument().getUri());
        // TODO: Implement document symbols
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(
            CodeActionParams params) {
        logger.debug("Code actions requested for range: {}", params.getRange());
        // TODO: Implement code actions
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
        logger.debug("Code lens requested for: {}", params.getTextDocument().getUri());
        // TODO: Implement code lens
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    public CompletableFuture<CodeLens> resolveCodeLens(CodeLens codeLens) {
        logger.debug("Resolving code lens");
        // TODO: Implement code lens resolution
        return CompletableFuture.completedFuture(codeLens);
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
        logger.debug("Formatting requested for: {}", params.getTextDocument().getUri());
        // TODO: Implement document formatting
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> rangeFormatting(
            DocumentRangeFormattingParams params) {
        logger.debug("Range formatting requested for: {}", params.getRange());
        // TODO: Implement range formatting
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> onTypeFormatting(
            DocumentOnTypeFormattingParams params) {
        logger.debug("On-type formatting requested at: {}", params.getPosition());
        // TODO: Implement on-type formatting
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
        logger.debug("Rename requested at: {} to '{}'", params.getPosition(), params.getNewName());
        // TODO: Implement rename
        return CompletableFuture.completedFuture(new WorkspaceEdit());
    }

    @Override
    public CompletableFuture<List<FoldingRange>> foldingRange(FoldingRangeRequestParams params) {
        logger.debug("Folding ranges requested for: {}", params.getTextDocument().getUri());
        // TODO: Implement folding ranges
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
}
