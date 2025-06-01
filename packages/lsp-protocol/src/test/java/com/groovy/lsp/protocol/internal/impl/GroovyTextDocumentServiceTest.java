package com.groovy.lsp.protocol.internal.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GroovyTextDocumentServiceのテストクラス。
 */
@ExtendWith(MockitoExtension.class)
class GroovyTextDocumentServiceTest {

    private GroovyTextDocumentService service;

    @Mock private LanguageClient mockClient;

    @BeforeEach
    void setUp() {
        service = new GroovyTextDocumentService();
    }

    @Test
    void connect_shouldSetClient() {
        // when
        service.connect(mockClient);

        // then - verify by using the client
        service.didOpen(
                new DidOpenTextDocumentParams(
                        new TextDocumentItem("file:///test.groovy", "groovy", 1, "test")));
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
    void didOpen_shouldPublishDiagnostics() {
        // given
        service.connect(mockClient);
        TextDocumentItem textDocument =
                new TextDocumentItem(
                        "file:///workspace/Test.groovy", "groovy", 1, "class Test { }");
        DidOpenTextDocumentParams params = new DidOpenTextDocumentParams(textDocument);

        // when
        service.didOpen(params);

        // then
        ArgumentCaptor<PublishDiagnosticsParams> captor =
                ArgumentCaptor.forClass(PublishDiagnosticsParams.class);
        verify(mockClient).publishDiagnostics(captor.capture());

        PublishDiagnosticsParams diagnostics = captor.getValue();
        assertThat(diagnostics.getUri()).isEqualTo("file:///workspace/Test.groovy");
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
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///test.groovy");
        DidCloseTextDocumentParams params = new DidCloseTextDocumentParams(textDocument);

        // when/then - should not throw
        service.didClose(params);
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
}
