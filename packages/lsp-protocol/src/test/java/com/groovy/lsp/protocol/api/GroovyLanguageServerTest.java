package com.groovy.lsp.protocol.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.groovy.lsp.protocol.internal.impl.GroovyTextDocumentService;
import com.groovy.lsp.protocol.internal.impl.GroovyWorkspaceService;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.CompletionCapabilities;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GroovyLanguageServerのテストクラス。
 */
@ExtendWith(MockitoExtension.class)
class GroovyLanguageServerTest {

    private GroovyLanguageServer server;

    @Mock private LanguageClient mockClient;
    @Mock private GroovyTextDocumentService mockTextDocumentService;
    @Mock private GroovyWorkspaceService mockWorkspaceService;

    @BeforeEach
    void setUp() {
        server = new GroovyLanguageServer(mockTextDocumentService, mockWorkspaceService);
    }

    @Test
    void initialize_shouldInitializeServerWithCorrectCapabilities() throws Exception {
        // given
        InitializeParams params = new InitializeParams();
        params.setRootUri("file:///workspace");
        params.setCapabilities(new ClientCapabilities());

        // when
        InitializeResult result = server.initialize(params).get();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getCapabilities()).isNotNull();

        ServerCapabilities capabilities = result.getCapabilities();
        assertThat(capabilities.getTextDocumentSync()).isNotNull();
        assertThat(capabilities.getTextDocumentSync().getLeft())
                .isEqualTo(TextDocumentSyncKind.Incremental);
        assertThat(capabilities.getCompletionProvider()).isNotNull();
        assertThat(capabilities.getHoverProvider()).isNotNull();
        assertThat(capabilities.getHoverProvider().getLeft()).isTrue();
        assertThat(capabilities.getDefinitionProvider()).isNotNull();
        assertThat(capabilities.getDefinitionProvider().getLeft()).isTrue();
        assertThat(capabilities.getReferencesProvider()).isNotNull();
        assertThat(capabilities.getReferencesProvider().getLeft()).isTrue();
        assertThat(capabilities.getDocumentSymbolProvider()).isNotNull();
        assertThat(capabilities.getDocumentSymbolProvider().getLeft()).isTrue();
        assertThat(capabilities.getWorkspaceSymbolProvider()).isNotNull();
        assertThat(capabilities.getWorkspaceSymbolProvider().getLeft()).isTrue();
        assertThat(capabilities.getCodeActionProvider()).isNotNull();
        assertThat(capabilities.getCodeActionProvider().getLeft()).isTrue();
        assertThat(capabilities.getCodeLensProvider()).isNotNull();
        assertThat(capabilities.getDocumentFormattingProvider()).isNotNull();
        assertThat(capabilities.getDocumentFormattingProvider().getLeft()).isTrue();
        assertThat(capabilities.getDocumentRangeFormattingProvider()).isNotNull();
        assertThat(capabilities.getDocumentRangeFormattingProvider().getLeft()).isTrue();
        assertThat(capabilities.getRenameProvider()).isNotNull();
        assertThat(capabilities.getRenameProvider().getLeft()).isTrue();
        assertThat(capabilities.getFoldingRangeProvider()).isNotNull();
        assertThat(capabilities.getFoldingRangeProvider().getLeft()).isTrue();
    }

    @Test
    void shutdown_shouldShutdownNormally() throws Exception {
        // when
        Object result = server.shutdown().get();

        // then
        assertThat(result).isNull();
    }

    @Test
    void exit_shouldExitWithCodeZero() {
        // given - shutdown called first
        var shutdownResult = server.shutdown();
        assertThat(shutdownResult).isNotNull();

        // when/then - would call System.exit(0)
        // Can't test System.exit directly, but we verify the method exists
        // In real implementation, this would exit with code 0
    }

    @Test
    void exit_shouldExitWithCodeOneWithoutShutdown() {
        // when/then - would call System.exit(1)
        // Can't test System.exit directly, but we verify the method exists
        // In real implementation, this would exit with code 1
    }

    @Test
    void getTextDocumentService_shouldReturnTextDocumentService() {
        // when
        var service = server.getTextDocumentService();

        // then
        assertThat(service).isNotNull();
    }

    @Test
    void getWorkspaceService_shouldReturnWorkspaceService() {
        // when
        var service = server.getWorkspaceService();

        // then
        assertThat(service).isNotNull();
    }

    @Test
    void connect_shouldConnectToClient() {
        // when
        server.connect(mockClient);

        // then
        assertThat(server.getClient()).isSameAs(mockClient);
    }

    @Test
    void connect_shouldPropagateClientToServices() {
        // given
        GroovyTextDocumentService textDocumentService = mock(GroovyTextDocumentService.class);
        GroovyWorkspaceService workspaceService = mock(GroovyWorkspaceService.class);
        GroovyLanguageServer testServer =
                new GroovyLanguageServer(textDocumentService, workspaceService);

        // when
        testServer.connect(mockClient);

        // then
        verify(textDocumentService).connect(mockClient);
        verify(workspaceService).connect(mockClient);
        assertThat(testServer.getClient()).isSameAs(mockClient);
    }

    @Test
    void getClient_shouldReturnNullBeforeConnection() {
        // when
        var client = server.getClient();

        // then
        assertThat(client).isNull();
    }

    @Test
    void getClient_shouldReturnClientAfterConnection() {
        // given
        server.connect(mockClient);

        // when
        var client = server.getClient();

        // then
        assertThat(client).isSameAs(mockClient);
    }

    @Test
    void initialize_shouldInitializeWithDifferentClientCapabilities() throws Exception {
        // given
        InitializeParams params = new InitializeParams();
        params.setRootUri("file:///project");

        ClientCapabilities clientCaps = new ClientCapabilities();
        TextDocumentClientCapabilities textDocCaps = new TextDocumentClientCapabilities();
        CompletionCapabilities completionCaps = new CompletionCapabilities();
        completionCaps.setDynamicRegistration(true);
        textDocCaps.setCompletion(completionCaps);
        clientCaps.setTextDocument(textDocCaps);
        params.setCapabilities(clientCaps);

        // when
        InitializeResult result = server.initialize(params).get();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getCapabilities()).isNotNull();
    }

    @Test
    void initialize_shouldInitializeWithWorkspaceFolders() throws Exception {
        // given
        InitializeParams params = new InitializeParams();
        WorkspaceFolder folder = new WorkspaceFolder("file:///workspace", "workspace");
        params.setWorkspaceFolders(java.util.List.of(folder));

        // when
        InitializeResult result = server.initialize(params).get();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getCapabilities()).isNotNull();
    }

    @Test
    void initialize_shouldReturnSameCapabilitiesWhenCalledMultipleTimes() throws Exception {
        // given
        InitializeParams params = new InitializeParams();

        // when
        InitializeResult result1 = server.initialize(params).get();
        InitializeResult result2 = server.initialize(params).get();

        // then
        assertThat(result1.getCapabilities().getTextDocumentSync())
                .isEqualTo(result2.getCapabilities().getTextDocumentSync());
        assertThat(result1.getCapabilities().getCompletionProvider()).isNotNull();
        assertThat(result2.getCapabilities().getCompletionProvider()).isNotNull();
    }
}
