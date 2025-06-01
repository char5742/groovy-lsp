package com.groovy.lsp.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import com.groovy.lsp.protocol.api.GroovyLanguageServer;
import com.groovy.lsp.protocol.internal.impl.GroovyTextDocumentService;
import com.groovy.lsp.protocol.internal.impl.GroovyWorkspaceService;
import com.groovy.lsp.protocol.test.AbstractProtocolTest;
import com.groovy.lsp.protocol.test.LSPTestHarness;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.junit.jupiter.api.Test;

/**
 * Protocol-level tests for the Groovy Language Server.
 * Tests the LSP protocol implementation and message handling.
 */
class GroovyLanguageServerProtocolTest extends AbstractProtocolTest {

    @Override
    protected LSPTestHarness createHarness() throws Exception {
        // Create a test implementation of GroovyLanguageServer
        LanguageServer server = new TestGroovyLanguageServer();
        return LSPTestHarness.builder().server(server).build();
    }

    @Test
    void testInitialize() throws Exception {
        // Server is already initialized in setUp()
        InitializeResult result = server.initialize(createDefaultInitializeParams()).get();

        assertThat(result).isNotNull();
        assertThat(result.getCapabilities()).isNotNull();

        ServerCapabilities capabilities = result.getCapabilities();
        assertThat(capabilities.getTextDocumentSync()).isNotNull();
    }

    @Test
    void testDidOpenTextDocument() {
        // Open a Groovy document
        openGroovyDocument(
                "Test.groovy",
                """
                class Test {
                    String name

                    void sayHello() {
                        println "Hello, $name!"
                    }
                }
                """);

        // Wait for diagnostics
        waitForDiagnostics("file://" + workspaceRoot + "/Test.groovy");

        // Verify diagnostics were published
        assertThat(client.getDiagnostics()).isNotEmpty();
    }

    @Test
    void testCompletion() throws Exception {
        String uri = "file://" + workspaceRoot + "/Test.groovy";
        openGroovyDocument(
                "Test.groovy",
                """
                class Test {
                    String name

                    void test() {
                        na
                    }
                }
                """);

        // Request completion at position of "na"
        CompletionParams params = new CompletionParams();
        params.setTextDocument(textDocument(uri));
        params.setPosition(new Position(4, 10)); // After "na"

        Either<List<CompletionItem>, CompletionList> completion =
                server.getTextDocumentService().completion(params).get();

        assertThat(completion).isNotNull();
        if (completion.isLeft()) {
            assertThat(completion.getLeft()).isNotEmpty();
        } else {
            assertThat(completion.getRight().getItems()).isNotEmpty();
        }
    }

    @Test
    void testHover() throws Exception {
        String uri = "file://" + workspaceRoot + "/Test.groovy";
        openGroovyDocument(
                "Test.groovy",
                """
                class Test {
                    String name

                    void sayHello() {
                        println name
                    }
                }
                """);

        // Request hover on "name" variable
        HoverParams params = new HoverParams();
        params.setTextDocument(textDocument(uri));
        params.setPosition(new Position(4, 20)); // On "name"

        Hover hover = server.getTextDocumentService().hover(params).get();

        assertThat(hover).isNotNull();
        assertThat(hover.getContents()).isNotNull();
    }

    /**
     * Test implementation of GroovyLanguageServer for protocol testing.
     */
    private static class TestGroovyLanguageServer implements GroovyLanguageServer {
        private final TextDocumentService textDocumentService = new GroovyTextDocumentService();
        private final WorkspaceService workspaceService = new GroovyWorkspaceService();

        @Override
        public CompletableFuture<InitializeResult> initialize(
                org.eclipse.lsp4j.InitializeParams params) {
            InitializeResult result = new InitializeResult();
            ServerCapabilities capabilities = new ServerCapabilities();
            capabilities.setTextDocumentSync(org.eclipse.lsp4j.TextDocumentSyncKind.Full);
            capabilities.setCompletionProvider(new org.eclipse.lsp4j.CompletionOptions());
            capabilities.setHoverProvider(true);
            result.setCapabilities(capabilities);
            return CompletableFuture.completedFuture(result);
        }

        @Override
        public CompletableFuture<Object> shutdown() {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void exit() {
            // No-op for testing
        }

        @Override
        public TextDocumentService getTextDocumentService() {
            return textDocumentService;
        }

        @Override
        public WorkspaceService getWorkspaceService() {
            return workspaceService;
        }
    }
}
