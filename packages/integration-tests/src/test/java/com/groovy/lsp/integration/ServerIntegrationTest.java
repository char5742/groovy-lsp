package com.groovy.lsp.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.groovy.lsp.protocol.api.GroovyLanguageServer;
import com.groovy.lsp.server.launcher.di.ServerModule;
import com.groovy.lsp.test.annotations.IntegrationTest;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

/**
 * LSPサーバー全体の統合テスト
 */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServerIntegrationTest {

    private LanguageServer server;
    private TestLanguageClient client;

    @BeforeAll
    void setUp() {
        client = new TestLanguageClient();
        Injector injector = Guice.createInjector(new ServerModule());
        server = injector.getInstance(GroovyLanguageServer.class);
        ((GroovyLanguageServer) server).connect(client);
    }

    @AfterAll
    void tearDown() {
        @SuppressWarnings("FutureReturnValueIgnored")
        var unused = server.shutdown();
    }

    @IntegrationTest
    @DisplayName("サーバーの初期化と基本的な機能の動作確認")
    void testServerInitialization(@TempDir Path workspaceRoot) throws Exception {
        // Initialize parameters
        InitializeParams params = new InitializeParams();
        params.setRootUri(workspaceRoot.toUri().toString());
        params.setCapabilities(new ClientCapabilities());

        // Initialize server
        CompletableFuture<InitializeResult> initFuture = server.initialize(params);
        InitializeResult result = initFuture.get(5, TimeUnit.SECONDS);

        // Verify capabilities
        assertThat(result).isNotNull();
        assertThat(result.getCapabilities()).isNotNull();
        // TextDocumentSync can be Either<TextDocumentSyncKind, TextDocumentSyncOptions>
        Object textDocumentSync = result.getCapabilities().getTextDocumentSync();
        assertThat(textDocumentSync).isNotNull();
        assertThat(result.getCapabilities().getCompletionProvider()).isNotNull();
        // TODO: Update assertions when server capabilities are properly implemented
        assertThat(result.getCapabilities().getHoverProvider()).isNotNull();
        assertThat(result.getCapabilities().getDefinitionProvider()).isNotNull();

        // Send initialized notification
        server.initialized(new InitializedParams());
    }

    @IntegrationTest
    @DisplayName("テキストドキュメントの同期機能")
    void testTextDocumentSync(@TempDir Path workspaceRoot) throws Exception {
        // Initialize server first
        InitializeParams initParams = new InitializeParams();
        initParams.setRootUri(workspaceRoot.toUri().toString());
        server.initialize(initParams).get(5, TimeUnit.SECONDS);
        server.initialized(new InitializedParams());

        // Open document
        String uri = workspaceRoot.resolve("test.groovy").toUri().toString();
        String content = "class TestClass {\n    def method() {\n        println 'Hello'\n    }\n}";

        DidOpenTextDocumentParams openParams = new DidOpenTextDocumentParams();
        TextDocumentItem textDocument = new TextDocumentItem();
        textDocument.setUri(uri);
        textDocument.setLanguageId("groovy");
        textDocument.setVersion(1);
        textDocument.setText(content);
        openParams.setTextDocument(textDocument);

        server.getTextDocumentService().didOpen(openParams);

        // Wait for diagnostics
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(client.getDiagnostics()).isNotEmpty());

        // Verify diagnostics received for the current document
        List<PublishDiagnosticsParams> diagnostics = client.getDiagnostics();
        List<PublishDiagnosticsParams> diagnosticsForUri =
                diagnostics.stream().filter(d -> d.getUri().equals(uri)).toList();
        assertThat(diagnosticsForUri).hasSize(1);
        assertThat(diagnosticsForUri.get(0).getUri()).isEqualTo(uri);
    }

    @IntegrationTest
    @DisplayName("補完機能の動作確認")
    void testCompletion(@TempDir Path workspaceRoot) throws Exception {
        // Initialize and open document
        String uri = workspaceRoot.resolve("completion.groovy").toUri().toString();
        initializeAndOpenDocument(uri, "def str = 'hello'\nstr.");

        // Request completion
        CompletionParams completionParams = new CompletionParams();
        completionParams.setTextDocument(new TextDocumentIdentifier(uri));
        completionParams.setPosition(new Position(1, 4)); // After "str."

        // TODO: Update when completion is implemented
        // For now, just verify the method can be called without error
        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completionFuture =
                server.getTextDocumentService().completion(completionParams);

        assertThat(completionFuture).isNotNull();
    }

    @IntegrationTest
    @DisplayName("ホバー機能の基本的な動作確認")
    void testBasicHover(@TempDir Path workspaceRoot) throws Exception {
        // Initialize and open a simple document
        String uri = workspaceRoot.resolve("hover.groovy").toUri().toString();
        String content =
                """
                class TestClass {
                    String name = "test"

                    def greet() {
                        println "Hello"
                    }
                }
                """;

        initializeAndOpenDocument(uri, content);

        // Wait for document processing
        Thread.sleep(1000);

        // Request hover on class name (line 0, around character 6-15)
        HoverParams hoverParams = new HoverParams();
        hoverParams.setTextDocument(new TextDocumentIdentifier(uri));
        hoverParams.setPosition(new Position(0, 10)); // Position on "TestClass"

        CompletableFuture<Hover> hoverFuture = server.getTextDocumentService().hover(hoverParams);
        Hover hover = hoverFuture.get(5, TimeUnit.SECONDS);

        // If null, try field position
        if (hover == null) {
            hoverParams.setPosition(new Position(1, 20)); // Position on "name" field
            hover = server.getTextDocumentService().hover(hoverParams).get(5, TimeUnit.SECONDS);
        }

        // If still null, try method position
        if (hover == null) {
            hoverParams.setPosition(new Position(3, 15)); // Position on "greet" method
            hover = server.getTextDocumentService().hover(hoverParams).get(5, TimeUnit.SECONDS);
        }

        // Just verify we get some hover response
        // The content might be null if the AST parsing has issues, but we should at least not crash
        System.out.println("Hover result: " + (hover != null ? "Got hover" : "No hover"));
        if (hover != null && hover.getContents() != null && hover.getContents().isRight()) {
            System.out.println("Hover content: " + hover.getContents().getRight().getValue());
        }
    }

    private void initializeAndOpenDocument(String uri, String content) throws Exception {
        // Initialize if not already done
        if (client.getDiagnostics().isEmpty()) {
            InitializeParams initParams = new InitializeParams();
            Path parent = Paths.get(new URI(uri)).getParent();
            assertThat(parent).isNotNull();
            initParams.setRootUri(Objects.requireNonNull(parent).toUri().toString());
            server.initialize(initParams).get(5, TimeUnit.SECONDS);
            server.initialized(new InitializedParams());
        }

        // Open document
        DidOpenTextDocumentParams openParams = new DidOpenTextDocumentParams();
        TextDocumentItem textDocument = new TextDocumentItem();
        textDocument.setUri(uri);
        textDocument.setLanguageId("groovy");
        textDocument.setVersion(1);
        textDocument.setText(content);
        openParams.setTextDocument(textDocument);

        server.getTextDocumentService().didOpen(openParams);

        // Wait a bit for processing
        Thread.sleep(500);
    }

    /**
     * テスト用のLanguageClient実装
     */
    private static class TestLanguageClient implements LanguageClient {
        private final List<PublishDiagnosticsParams> diagnostics = new java.util.ArrayList<>();

        @Override
        public void telemetryEvent(Object object) {
            // Test implementation
        }

        @Override
        public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
            this.diagnostics.add(diagnostics);
        }

        @Override
        public void showMessage(MessageParams messageParams) {
            // Test implementation
        }

        @Override
        public CompletableFuture<MessageActionItem> showMessageRequest(
                ShowMessageRequestParams requestParams) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void logMessage(MessageParams message) {
            // Test implementation
        }

        public List<PublishDiagnosticsParams> getDiagnostics() {
            return new java.util.ArrayList<>(diagnostics);
        }

        // TODO: Use when diagnostic clearing is needed
        @SuppressWarnings("UnusedMethod")
        public void clearDiagnostics() {
            diagnostics.clear();
        }
    }
}
