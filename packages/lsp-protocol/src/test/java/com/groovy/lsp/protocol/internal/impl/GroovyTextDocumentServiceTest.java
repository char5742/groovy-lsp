package com.groovy.lsp.protocol.internal.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.groovy.core.api.CompilationResult;
import com.groovy.lsp.groovy.core.api.IncrementalCompilationService;
import com.groovy.lsp.groovy.core.api.TypeInferenceService;
import com.groovy.lsp.protocol.api.IServiceRouter;
import com.groovy.lsp.protocol.internal.document.DocumentManager;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.CompilationUnit;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
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
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureHelpParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * GroovyTextDocumentServiceのテストクラス。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GroovyTextDocumentServiceTest {

    private GroovyTextDocumentService service;

    @Mock private LanguageClient mockClient;
    @Mock private IServiceRouter serviceRouter;
    @Mock private DocumentManager documentManager;
    @Mock private IncrementalCompilationService compilationService;
    @Mock private ASTService astService;
    @Mock private TypeInferenceService typeInferenceService;
    @Mock private CompilationUnit compilationUnit;

    @BeforeEach
    void setUp() {
        service = new GroovyTextDocumentService();

        // Setup service router mocks
        when(serviceRouter.getIncrementalCompilationService()).thenReturn(compilationService);
        when(serviceRouter.getAstService()).thenReturn(astService);
        when(serviceRouter.getTypeInferenceService()).thenReturn(typeInferenceService);

        // Inject dependencies
        service.setServiceRouter(serviceRouter);
        service.setDocumentManager(documentManager);
    }

    @Test
    void connect_shouldSetClient() throws Exception {
        // when
        service.connect(mockClient);

        // given
        String uri = "file:///test.groovy";
        when(documentManager.getDocumentContent(uri)).thenReturn("test");
        when(compilationService.createCompilationUnit(any())).thenReturn(compilationUnit);
        when(compilationService.compileToPhaseWithResult(any(), any(), any(), any()))
                .thenReturn(CompilationResult.success(mock(ModuleNode.class)));

        // then - verify by using the client
        service.didOpen(
                new DidOpenTextDocumentParams(new TextDocumentItem(uri, "groovy", 1, "test")));

        // Wait a bit for async processing
        Thread.sleep(100);
        verify(mockClient).publishDiagnostics(any());
    }

    @Test
    void didOpen_shouldWorkWithoutClient() {
        // given
        TextDocumentItem textDocument =
                new TextDocumentItem("file:///test.groovy", "groovy", 1, "class Test {}");
        DidOpenTextDocumentParams params = new DidOpenTextDocumentParams(textDocument);

        // when/then - should not throw
        service.didOpen(params);
    }

    @Test
    void didOpen_shouldPublishDiagnostics() throws Exception {
        // given
        service.connect(mockClient);
        String uri = "file:///workspace/Test.groovy";
        String content = "class Test { }";
        TextDocumentItem textDocument = new TextDocumentItem(uri, "groovy", 1, content);
        DidOpenTextDocumentParams params = new DidOpenTextDocumentParams(textDocument);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(compilationService.createCompilationUnit(any())).thenReturn(compilationUnit);
        when(compilationService.compileToPhaseWithResult(any(), any(), any(), any()))
                .thenReturn(CompilationResult.success(mock(ModuleNode.class)));

        // when
        service.didOpen(params);

        // Wait a bit for async processing
        Thread.sleep(100);

        // then
        ArgumentCaptor<PublishDiagnosticsParams> captor =
                ArgumentCaptor.forClass(PublishDiagnosticsParams.class);
        verify(mockClient).publishDiagnostics(captor.capture());

        PublishDiagnosticsParams diagnostics = captor.getValue();
        assertThat(diagnostics.getUri()).isEqualTo(uri);
        assertThat(diagnostics.getDiagnostics()).isEmpty();
    }

    @Test
    void didChange_shouldHandleDocumentChanges() {
        // given
        VersionedTextDocumentIdentifier textDocument =
                new VersionedTextDocumentIdentifier("file:///test.groovy", 2);
        TextDocumentContentChangeEvent change =
                new TextDocumentContentChangeEvent("updated content");
        DidChangeTextDocumentParams params =
                new DidChangeTextDocumentParams(textDocument, Arrays.asList(change));

        // when/then - should not throw
        service.didChange(params);
    }

    @Test
    void didClose_shouldHandleDocumentClose() {
        // given
        service.connect(mockClient);
        String uri = "file:///test.groovy";
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
        DidCloseTextDocumentParams params = new DidCloseTextDocumentParams(textDocument);

        // when
        service.didClose(params);

        // then
        // Wait a bit for async processing
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify diagnostics are cleared
        verify(mockClient).publishDiagnostics(any());
    }

    @Test
    void didSave_shouldHandleDocumentSave() {
        // given
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///test.groovy");
        DidSaveTextDocumentParams params = new DidSaveTextDocumentParams(textDocument);
        params.setText("saved content");

        // when/then - should not throw
        service.didSave(params);
    }

    @Test
    void completion_shouldReturnEmptyCompletionList() throws Exception {
        // given
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///test.groovy");
        Position position = new Position(0, 10);
        CompletionParams params = new CompletionParams(textDocument, position);

        // when
        Either<List<CompletionItem>, CompletionList> result = service.completion(params).get();

        // then
        assertThat(result.isRight()).isTrue();
        assertThat(result.getRight().isIncomplete()).isFalse();
        assertThat(result.getRight().getItems()).isEmpty();
    }

    @Test
    void resolveCompletionItem_shouldReturnSameItem() throws Exception {
        // given
        CompletionItem item = new CompletionItem("test");
        item.setKind(CompletionItemKind.Method);

        // when
        CompletionItem result = service.resolveCompletionItem(item).get();

        // then
        assertThat(result).isSameAs(item);
    }

    @Test
    void hover_shouldReturnNull() throws Exception {
        // given
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///test.groovy");
        Position position = new Position(1, 5);
        HoverParams params = new HoverParams(textDocument, position);

        // when
        Hover result = service.hover(params).get();

        // then
        assertThat(result).isNull();
    }

    @Test
    void signatureHelp_shouldReturnEmptySignatureHelp() throws Exception {
        // given
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///test.groovy");
        Position position = new Position(2, 15);
        SignatureHelpParams params = new SignatureHelpParams(textDocument, position);

        // when
        SignatureHelp result = service.signatureHelp(params).get();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSignatures()).isEmpty();
    }

    @Test
    void definition_shouldReturnEmptyLocationList() throws Exception {
        // given
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///test.groovy");
        Position position = new Position(3, 10);
        DefinitionParams params = new DefinitionParams(textDocument, position);

        // when
        Either<List<? extends Location>, List<? extends LocationLink>> result =
                service.definition(params).get();

        // then
        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isEmpty();
    }

    @Test
    void references_shouldReturnEmptyLocationList() throws Exception {
        // given
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///test.groovy");
        Position position = new Position(4, 20);
        ReferenceContext context = new ReferenceContext(true);
        ReferenceParams params = new ReferenceParams(textDocument, position, context);

        // when
        List<? extends Location> result = service.references(params).get();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void documentHighlight_shouldReturnEmptyHighlightList() throws Exception {
        // given
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///test.groovy");
        Position position = new Position(5, 8);
        DocumentHighlightParams params = new DocumentHighlightParams(textDocument, position);

        // when
        List<? extends DocumentHighlight> result = service.documentHighlight(params).get();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void documentSymbol_shouldReturnEmptySymbolList() throws Exception {
        // given
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///test.groovy");
        DocumentSymbolParams params = new DocumentSymbolParams(textDocument);

        // when
        List<Either<SymbolInformation, DocumentSymbol>> result =
                service.documentSymbol(params).get();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void codeAction_shouldReturnEmptyCodeActionList() throws Exception {
        // given
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///test.groovy");
        Range range = new Range(new Position(0, 0), new Position(10, 0));
        CodeActionContext context = new CodeActionContext(Arrays.asList());
        CodeActionParams params = new CodeActionParams(textDocument, range, context);

        // when
        List<Either<Command, CodeAction>> result = service.codeAction(params).get();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void codeLens_shouldReturnEmptyCodeLensList() throws Exception {
        // given
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///test.groovy");
        CodeLensParams params = new CodeLensParams(textDocument);

        // when
        List<? extends CodeLens> result = service.codeLens(params).get();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void resolveCodeLens_shouldReturnSameCodeLens() throws Exception {
        // given
        Range range = new Range(new Position(1, 0), new Position(1, 10));
        CodeLens codeLens = new CodeLens(range);
        codeLens.setCommand(new Command("Test", "test.command"));

        // when
        CodeLens result = service.resolveCodeLens(codeLens).get();

        // then
        assertThat(result).isSameAs(codeLens);
    }

    @Test
    void formatting_shouldReturnEmptyEditList() throws Exception {
        // given
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///test.groovy");
        FormattingOptions options = new FormattingOptions(4, true);
        DocumentFormattingParams params = new DocumentFormattingParams(textDocument, options);

        // when
        List<? extends TextEdit> result = service.formatting(params).get();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void rangeFormatting_shouldReturnEmptyEditList() throws Exception {
        // given
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///test.groovy");
        Range range = new Range(new Position(0, 0), new Position(5, 0));
        FormattingOptions options = new FormattingOptions(2, false);
        DocumentRangeFormattingParams params = new DocumentRangeFormattingParams();
        params.setTextDocument(textDocument);
        params.setRange(range);
        params.setOptions(options);

        // when
        List<? extends TextEdit> result = service.rangeFormatting(params).get();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void onTypeFormatting_shouldReturnEmptyEditList() throws Exception {
        // given
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///test.groovy");
        Position position = new Position(3, 5);
        String ch = ";";
        FormattingOptions options = new FormattingOptions(4, true);
        DocumentOnTypeFormattingParams params = new DocumentOnTypeFormattingParams();
        params.setTextDocument(textDocument);
        params.setPosition(position);
        params.setCh(ch);
        params.setOptions(options);

        // when
        List<? extends TextEdit> result = service.onTypeFormatting(params).get();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void rename_shouldReturnEmptyWorkspaceEdit() throws Exception {
        // given
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///test.groovy");
        Position position = new Position(2, 10);
        String newName = "newVariable";
        RenameParams params = new RenameParams(textDocument, position, newName);

        // when
        WorkspaceEdit result = service.rename(params).get();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getChanges()).isNullOrEmpty();
    }

    @Test
    void foldingRange_shouldReturnEmptyFoldingRangeList() throws Exception {
        // given
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///test.groovy");
        FoldingRangeRequestParams params = new FoldingRangeRequestParams(textDocument);

        // when
        List<FoldingRange> result = service.foldingRange(params).get();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shutdown_shouldCleanupResources() {
        // given - service with initialized diagnostics handler
        service.connect(mockClient);

        // when
        service.shutdown();

        // then - should not throw
        // In a real test, we would verify that the diagnostics handler's shutdown was called
    }

    @Test
    void isDiagnosticsReady_shouldCheckAllDependencies() {
        // given - service without client
        service.setServiceRouter(serviceRouter);
        service.setDocumentManager(documentManager);

        // when - open document without client
        TextDocumentItem textDocument =
                new TextDocumentItem("file:///test.groovy", "groovy", 1, "class Test {}");
        DidOpenTextDocumentParams params = new DidOpenTextDocumentParams(textDocument);
        service.didOpen(params);

        // then - diagnostics should not be triggered
        verify(mockClient, never()).publishDiagnostics(any());
    }

    @Test
    void didChange_shouldTriggerDebouncedDiagnostics() throws Exception {
        // given
        service.connect(mockClient);
        String uri = "file:///test.groovy";
        when(documentManager.getDocumentContent(uri)).thenReturn("updated content");
        when(compilationService.createCompilationUnit(any())).thenReturn(compilationUnit);
        when(compilationService.compileToPhaseWithResult(any(), any(), any(), any()))
                .thenReturn(CompilationResult.success(mock(ModuleNode.class)));

        VersionedTextDocumentIdentifier textDocument = new VersionedTextDocumentIdentifier(uri, 2);
        TextDocumentContentChangeEvent change =
                new TextDocumentContentChangeEvent("updated content");
        DidChangeTextDocumentParams params =
                new DidChangeTextDocumentParams(textDocument, Arrays.asList(change));

        // when
        service.didChange(params);

        // Wait for debounced diagnostics
        Thread.sleep(300);

        // then
        verify(mockClient).publishDiagnostics(any());
    }

    @Test
    void didSave_shouldTriggerImmediateDiagnostics() throws Exception {
        // given
        service.connect(mockClient);
        String uri = "file:///test.groovy";
        when(documentManager.getDocumentContent(uri)).thenReturn("saved content");
        when(compilationService.createCompilationUnit(any())).thenReturn(compilationUnit);
        when(compilationService.compileToPhaseWithResult(any(), any(), any(), any()))
                .thenReturn(CompilationResult.success(mock(ModuleNode.class)));

        TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
        DidSaveTextDocumentParams params = new DidSaveTextDocumentParams(textDocument);

        // when
        service.didSave(params);

        // Wait a bit for async processing
        Thread.sleep(100);

        // then
        verify(mockClient).publishDiagnostics(any());
    }

    @Test
    void hover_shouldHandleNullServiceRouter() throws Exception {
        // given
        service = new GroovyTextDocumentService();
        service.setDocumentManager(documentManager);
        // serviceRouter is null

        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///test.groovy");
        Position position = new Position(1, 5);
        HoverParams params = new HoverParams(textDocument, position);

        // when
        Hover result = service.hover(params).get();

        // then
        assertThat(result).isNull();
    }

    @Test
    void hover_shouldHandleNullDocumentManager() throws Exception {
        // given
        service = new GroovyTextDocumentService();
        service.setServiceRouter(serviceRouter);
        // documentManager is null

        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///test.groovy");
        Position position = new Position(1, 5);
        HoverParams params = new HoverParams(textDocument, position);

        // when
        Hover result = service.hover(params).get();

        // then
        assertThat(result).isNull();
    }

    @Test
    void didOpen_shouldHandleNullDocumentManager() {
        // given
        service = new GroovyTextDocumentService();
        service.setServiceRouter(serviceRouter);
        // documentManager is null

        TextDocumentItem textDocument =
                new TextDocumentItem("file:///test.groovy", "groovy", 1, "class Test {}");
        DidOpenTextDocumentParams params = new DidOpenTextDocumentParams(textDocument);

        // when/then - should not throw
        service.didOpen(params);
    }

    @Test
    void didChange_shouldHandleEmptyContentChanges() {
        // given
        VersionedTextDocumentIdentifier textDocument =
                new VersionedTextDocumentIdentifier("file:///test.groovy", 2);
        DidChangeTextDocumentParams params =
                new DidChangeTextDocumentParams(textDocument, Collections.emptyList());

        // when/then - should not throw
        service.didChange(params);
    }

    @Test
    void didClose_shouldHandleNullDocumentManager() {
        // given
        service = new GroovyTextDocumentService();
        service.connect(mockClient);
        service.setServiceRouter(serviceRouter);
        // documentManager is null

        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///test.groovy");
        DidCloseTextDocumentParams params = new DidCloseTextDocumentParams(textDocument);

        // when/then - should not throw
        service.didClose(params);
    }

    @Test
    void didOpen_shouldHandleDiagnosticsError() throws Exception {
        // given
        service.connect(mockClient);
        String uri = "file:///test.groovy";
        String content = "class Test { }";

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(compilationService.createCompilationUnit(any())).thenReturn(compilationUnit);
        when(compilationService.compileToPhaseWithResult(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Compilation failed"));

        TextDocumentItem textDocument = new TextDocumentItem(uri, "groovy", 1, content);
        DidOpenTextDocumentParams params = new DidOpenTextDocumentParams(textDocument);

        // when
        service.didOpen(params);

        // Wait a bit for async processing
        Thread.sleep(100);

        // then - should not publish diagnostics due to error
        verify(mockClient, never()).publishDiagnostics(any());
    }
}
