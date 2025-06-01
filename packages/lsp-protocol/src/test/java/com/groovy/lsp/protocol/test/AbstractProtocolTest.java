package com.groovy.lsp.protocol.test;

import static org.awaitility.Awaitility.await;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

/**
 * Base class for LSP protocol tests.
 * Provides common setup and utility methods for testing language server implementations.
 */
public abstract class AbstractProtocolTest {
    protected LSPTestHarness harness;
    protected LanguageServer server;
    protected TestLanguageClient client;
    protected String workspaceRoot;

    @BeforeEach
    void setUp(TestInfo testInfo) throws Exception {
        workspaceRoot = createTestWorkspace(testInfo);
        harness = createHarness();
        server = harness.getServer();
        client = harness.getTestClient();

        // Initialize the server
        InitializeResult result = initialize().get(10, TimeUnit.SECONDS);
        assertNotNull(result);

        // Send initialized notification
        server.initialized(new InitializedParams());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (harness != null) {
            harness.close();
        }
        cleanupTestWorkspace();
    }

    /**
     * Create the test harness with the language server implementation.
     */
    protected abstract LSPTestHarness createHarness() throws Exception;

    /**
     * Create a temporary workspace for the test.
     */
    protected String createTestWorkspace(TestInfo testInfo) {
        // Default implementation returns a temp directory
        return System.getProperty("java.io.tmpdir")
                + "/lsp-test-"
                + testInfo.getTestMethod().map(m -> m.getName()).orElse("unknown");
    }

    /**
     * Clean up the test workspace.
     */
    protected void cleanupTestWorkspace() {
        // Override if needed
    }

    /**
     * Initialize the language server with default parameters.
     */
    protected CompletableFuture<InitializeResult> initialize() {
        return initialize(null);
    }

    /**
     * Initialize the language server with custom parameters.
     */
    protected CompletableFuture<InitializeResult> initialize(
            @Nullable InitializeParams customParams) {
        InitializeParams params =
                customParams != null ? customParams : createDefaultInitializeParams();
        return server.initialize(params);
    }

    /**
     * Create default initialization parameters.
     */
    protected InitializeParams createDefaultInitializeParams() {
        InitializeParams params = new InitializeParams();
        params.setRootUri(URI.create("file://" + workspaceRoot).toString());

        WorkspaceFolder folder = new WorkspaceFolder();
        folder.setUri(params.getRootUri());
        folder.setName("test-workspace");
        params.setWorkspaceFolders(List.of(folder));

        client.addWorkspaceFolder(folder);

        return params;
    }

    /**
     * Open a text document in the language server.
     */
    protected void openDocument(
            @NonNull String uri, @NonNull String languageId, @NonNull String content) {
        TextDocumentItem document = new TextDocumentItem();
        document.setUri(uri);
        document.setLanguageId(languageId);
        document.setVersion(1);
        document.setText(content);

        DidOpenTextDocumentParams params = new DidOpenTextDocumentParams();
        params.setTextDocument(document);

        server.getTextDocumentService().didOpen(params);
    }

    /**
     * Open a Groovy document with the given content.
     */
    protected void openGroovyDocument(@NonNull String filename, @NonNull String content) {
        String uri = URI.create("file://" + workspaceRoot + "/" + filename).toString();
        openDocument(uri, "groovy", content);
    }

    /**
     * Wait for diagnostics to be published for the given URI.
     */
    protected void waitForDiagnostics(@NonNull String uri) {
        await().atMost(Duration.ofSeconds(5))
                .until(
                        () ->
                                client.getDiagnostics().stream()
                                        .anyMatch(d -> d.getUri().equals(uri)));
    }

    /**
     * Get a text document identifier for the given URI.
     */
    protected TextDocumentIdentifier textDocument(@NonNull String uri) {
        TextDocumentIdentifier id = new TextDocumentIdentifier();
        id.setUri(uri);
        return id;
    }

    /**
     * Assert that a value is not null (helper for tests without static imports).
     */
    protected void assertNotNull(@Nullable Object value) {
        if (value == null) {
            throw new AssertionError("Expected non-null value");
        }
    }
}
