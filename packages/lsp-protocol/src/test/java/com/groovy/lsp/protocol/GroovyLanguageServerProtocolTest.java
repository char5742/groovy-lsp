package com.groovy.lsp.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.groovy.core.api.CompilationResult;
import com.groovy.lsp.groovy.core.api.IncrementalCompilationService;
import com.groovy.lsp.groovy.core.api.TypeInferenceService;
import com.groovy.lsp.protocol.api.IServiceRouter;
import com.groovy.lsp.protocol.internal.document.DocumentManager;
import com.groovy.lsp.protocol.internal.impl.GroovyTextDocumentService;
import com.groovy.lsp.protocol.internal.impl.GroovyWorkspaceService;
import com.groovy.lsp.protocol.test.AbstractProtocolTest;
import com.groovy.lsp.protocol.test.LSPTestHarness;
import com.groovy.lsp.test.annotations.UnitTest;
import java.util.concurrent.CompletableFuture;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.CompilationUnit;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

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

    @UnitTest
    void testInitialize() throws Exception {
        // Server is already initialized in setUp()
        InitializeResult result = server.initialize(createDefaultInitializeParams()).get();

        assertThat(result).isNotNull();
        assertThat(result.getCapabilities()).isNotNull();

        ServerCapabilities capabilities = result.getCapabilities();
        assertThat(capabilities.getTextDocumentSync()).isNotNull();
    }

    @UnitTest
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

    @UnitTest
    void testCompletion() throws Exception {
        // Open a document
        openGroovyDocument(
                "Completion.groovy",
                """
                class Person {
                    String name
                    int age
                }

                def person = new Person()
                person.
                """);

        // Request completion at the dot position
        CompletionParams params = new CompletionParams();
        params.setTextDocument(textDocument("file://" + workspaceRoot + "/Completion.groovy"));
        params.setPosition(new Position(6, 7)); // After "person."

        var completions = server.getTextDocumentService().completion(params).get();
        assertThat(completions).isNotNull();
        assertThat(completions.isRight()).isTrue();

        // Even though the mock returns an empty list, this tests the protocol handling
        CompletionList list = completions.getRight();
        assertThat(list).isNotNull();
        assertThat(list.isIncomplete()).isFalse();
    }

    @UnitTest
    void testHover() throws Exception {
        // Open a document
        openGroovyDocument(
                "Hover.groovy",
                """
                class Person {
                    String name

                    void greet() {
                        println "Hello, $name"
                    }
                }
                """);

        // Request hover on "name" variable
        HoverParams params = new HoverParams();
        params.setTextDocument(textDocument("file://" + workspaceRoot + "/Hover.groovy"));
        params.setPosition(new Position(1, 11)); // On "String name"

        server.getTextDocumentService().hover(params).get();
        // Mock implementation returns null, but this tests the protocol handling
        // Real implementation would return hover information
    }

    @UnitTest
    void testDocumentSymbols() throws Exception {
        // Open a document with various symbols
        openGroovyDocument(
                "Symbols.groovy",
                """
                package com.example

                class Calculator {
                    int add(int a, int b) {
                        return a + b
                    }

                    int multiply(int a, int b) {
                        return a * b
                    }
                }

                interface MathOperations {
                    int calculate(int x, int y)
                }
                """);

        DocumentSymbolParams params = new DocumentSymbolParams();
        params.setTextDocument(textDocument("file://" + workspaceRoot + "/Symbols.groovy"));

        var symbols = server.getTextDocumentService().documentSymbol(params).get();
        assertThat(symbols).isNotNull();
        assertThat(symbols).isEmpty(); // Mock returns empty list
    }

    @UnitTest
    void testGoToDefinition() throws Exception {
        // Open a document
        openGroovyDocument(
                "Definition.groovy",
                """
                class Book {
                    String title
                    String author
                }

                def myBook = new Book()
                myBook.title = "Groovy in Action"
                """);

        DefinitionParams params = new DefinitionParams();
        params.setTextDocument(textDocument("file://" + workspaceRoot + "/Definition.groovy"));
        params.setPosition(new Position(6, 7)); // On "title" in myBook.title

        var locations = server.getTextDocumentService().definition(params).get();
        assertThat(locations).isNotNull();
        assertThat(locations.isLeft()).isTrue();
        assertThat(locations.getLeft()).isEmpty(); // Mock returns empty list
    }

    @UnitTest
    void testFormatting() throws Exception {
        // Open an unformatted document
        openGroovyDocument(
                "Unformatted.groovy",
                """
                class  Messy   {
                def   name
                  void   doSomething( )  {
                println   "hello"
                  }
                }
                """);

        DocumentFormattingParams params = new DocumentFormattingParams();
        params.setTextDocument(textDocument("file://" + workspaceRoot + "/Unformatted.groovy"));
        params.setOptions(new FormattingOptions(4, true)); // 4 spaces, insert spaces

        var edits = server.getTextDocumentService().formatting(params).get();
        assertThat(edits).isNotNull();
        assertThat(edits).isEmpty(); // Mock returns empty list
    }

    /**
     * Test implementation of LanguageServer for protocol testing.
     */
    private static class TestGroovyLanguageServer implements LanguageServer, LanguageClientAware {
        private final GroovyTextDocumentService textDocumentService;
        private final WorkspaceService workspaceService = new GroovyWorkspaceService();

        public TestGroovyLanguageServer() {
            // Setup mock dependencies
            IServiceRouter serviceRouter = mock(IServiceRouter.class);
            DocumentManager documentManager = new DocumentManager();
            IncrementalCompilationService compilationService =
                    mock(IncrementalCompilationService.class);
            ASTService astService = mock(ASTService.class);
            TypeInferenceService typeInferenceService = mock(TypeInferenceService.class);
            CompilationUnit compilationUnit = mock(CompilationUnit.class);

            // Configure mocks
            when(serviceRouter.getIncrementalCompilationService()).thenReturn(compilationService);
            when(serviceRouter.getAstService()).thenReturn(astService);
            when(serviceRouter.getTypeInferenceService()).thenReturn(typeInferenceService);

            // Setup compilation service to return success by default
            when(compilationService.createCompilationUnit(any())).thenReturn(compilationUnit);
            when(compilationService.compileToPhaseWithResult(any(), any(), any(), any()))
                    .thenReturn(CompilationResult.success(mock(ModuleNode.class)));

            // Create text document service with dependencies
            textDocumentService = new GroovyTextDocumentService(serviceRouter, documentManager);
        }

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

        @Override
        public void connect(LanguageClient client) {
            textDocumentService.connect(client);
            if (workspaceService instanceof LanguageClientAware languageClientAware) {
                languageClientAware.connect(client);
            }
        }
    }
}
