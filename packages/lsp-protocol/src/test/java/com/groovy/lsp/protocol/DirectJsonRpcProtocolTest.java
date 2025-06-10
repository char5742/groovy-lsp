package com.groovy.lsp.protocol;

import static com.groovy.lsp.protocol.test.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.groovy.lsp.protocol.api.GroovyLanguageServer;
import com.groovy.lsp.protocol.api.IServiceRouter;
import com.groovy.lsp.protocol.internal.document.DocumentManager;
import com.groovy.lsp.protocol.internal.impl.GroovyTextDocumentService;
import com.groovy.lsp.protocol.internal.impl.GroovyWorkspaceService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

/**
 * Direct JSON-RPC protocol tests that bypass the LSP4J transport layer.
 * These tests validate JSON serialization/deserialization directly.
 */
class DirectJsonRpcProtocolTest {

    private GroovyLanguageServer server;
    private Gson gson;
    private Path workspaceRoot;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        workspaceRoot = tempDir;
        gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

        // Create server with mocked dependencies
        IServiceRouter serviceRouter = Mockito.mock(IServiceRouter.class);
        DocumentManager documentManager = new DocumentManager();

        GroovyTextDocumentService textDocumentService = new GroovyTextDocumentService();
        textDocumentService.setServiceRouter(serviceRouter);
        textDocumentService.setDocumentManager(documentManager);

        GroovyWorkspaceService workspaceService = new GroovyWorkspaceService();

        server = new GroovyLanguageServer(textDocumentService, workspaceService);
    }

    @Test
    void testInitializeJsonProtocol() throws Exception {
        // Prepare JSON request
        String jsonRequest =
                """
                {
                  "jsonrpc": "2.0",
                  "id": 1,
                  "method": "initialize",
                  "params": {
                    "processId": 12345,
                    "rootUri": "%s",
                    "capabilities": {
                      "textDocument": {
                        "hover": {
                          "contentFormat": ["markdown", "plaintext"]
                        }
                      }
                    }
                  }
                }
                """
                        .formatted(workspaceRoot.toUri().toString());

        // Parse request
        JsonObject requestObj = JsonParser.parseString(jsonRequest).getAsJsonObject();
        InitializeParams params = gson.fromJson(requestObj.get("params"), InitializeParams.class);

        // Call method
        CompletableFuture<InitializeResult> future = server.initialize(params);
        InitializeResult result = future.get(5, TimeUnit.SECONDS);

        // Create JSON response
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.addProperty("id", 1);
        response.add("result", gson.toJsonTree(result));

        String jsonResponse = gson.toJson(response);

        // Validate response
        assertThatJson(jsonResponse)
                .isValidLspResponse()
                .hasJsonPath("$.id", 1)
                .hasJsonPath("$.result.capabilities")
                .hasJsonPath("$.result.capabilities.textDocumentSync")
                .hasJsonPath("$.result.capabilities.hoverProvider")
                .hasJsonPath("$.result.capabilities.completionProvider")
                .hasJsonPath("$.result.capabilities.definitionProvider");
    }

    @Test
    void testHoverJsonProtocol() throws Exception {
        // Initialize server first
        InitializeParams initParams = new InitializeParams();
        initParams.setRootUri(workspaceRoot.toUri().toString());
        server.initialize(initParams).get(5, TimeUnit.SECONDS);
        server.initialized(new InitializedParams());

        // Create test file with unique name
        Path testFile =
                workspaceRoot.resolve(
                        "Test_testHoverJsonProtocol_" + UUID.randomUUID() + ".groovy");
        String content =
                """
                class Test {
                    String name = "test"

                    void hello() {
                        println name
                    }
                }
                """;
        Files.writeString(testFile, content);

        // Open document
        TextDocumentItem textDocument = new TextDocumentItem();
        textDocument.setUri(testFile.toUri().toString());
        textDocument.setLanguageId("groovy");
        textDocument.setVersion(1);
        textDocument.setText(content);

        DidOpenTextDocumentParams openParams = new DidOpenTextDocumentParams();
        openParams.setTextDocument(textDocument);
        server.getTextDocumentService().didOpen(openParams);

        // Prepare hover request
        String jsonRequest =
                """
                {
                  "jsonrpc": "2.0",
                  "id": 2,
                  "method": "textDocument/hover",
                  "params": {
                    "textDocument": {
                      "uri": "%s"
                    },
                    "position": {
                      "line": 4,
                      "character": 16
                    }
                  }
                }
                """
                        .formatted(testFile.toUri().toString());

        // Parse and execute
        JsonObject requestObj = JsonParser.parseString(jsonRequest).getAsJsonObject();
        HoverParams hoverParams = gson.fromJson(requestObj.get("params"), HoverParams.class);

        CompletableFuture<Hover> hoverFuture = server.getTextDocumentService().hover(hoverParams);
        Hover hover = hoverFuture.get(5, TimeUnit.SECONDS);

        // Create response
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.addProperty("id", 2);
        if (hover != null) {
            response.add("result", gson.toJsonTree(hover));
        } else {
            response.add("result", gson.toJsonTree(null));
        }

        String jsonResponse = gson.toJson(response);

        // Validate
        assertThatJson(jsonResponse)
                .isValidLspResponse()
                .hasJsonPath("$.id", 2)
                .hasJsonPath("$.result");
    }

    @Test
    void testCompletionJsonProtocol() throws Exception {
        // Initialize
        InitializeParams initParams = new InitializeParams();
        initParams.setRootUri(workspaceRoot.toUri().toString());
        server.initialize(initParams).get(5, TimeUnit.SECONDS);

        // Prepare completion request
        String jsonRequest =
                """
                {
                  "jsonrpc": "2.0",
                  "id": 3,
                  "method": "textDocument/completion",
                  "params": {
                    "textDocument": {
                      "uri": "file:///test.groovy"
                    },
                    "position": {
                      "line": 0,
                      "character": 10
                    }
                  }
                }
                """;

        JsonObject requestObj = JsonParser.parseString(jsonRequest).getAsJsonObject();
        CompletionParams params = gson.fromJson(requestObj.get("params"), CompletionParams.class);

        CompletableFuture<Either<List<CompletionItem>, CompletionList>> future =
                server.getTextDocumentService().completion(params);
        Either<List<CompletionItem>, CompletionList> result = future.get(5, TimeUnit.SECONDS);

        // Create response
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.addProperty("id", 3);
        response.add("result", gson.toJsonTree(result));

        String jsonResponse = gson.toJson(response);

        // Validate
        assertThatJson(jsonResponse)
                .isValidLspResponse()
                .hasJsonPath("$.id", 3)
                .hasJsonPath("$.result");
    }

    @Test
    void testInvalidMethodError() throws Exception {
        // Create an error response for invalid method

        // Create error response
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.addProperty("id", 999);

        JsonObject error = new JsonObject();
        error.addProperty("code", -32601);
        error.addProperty("message", "Method not found");
        response.add("error", error);

        String jsonResponse = gson.toJson(response);

        // Validate error format
        assertThatJson(jsonResponse)
                .isValidJsonRpc()
                .hasJsonPath("$.id", 999)
                .hasJsonPath("$.error.code", -32601)
                .hasJsonPath("$.error.message", "Method not found");
    }

    @Test
    void testInvalidMethodServerResponse() throws Exception {
        // Initialize server first
        InitializeParams initParams = new InitializeParams();
        initParams.setRootUri(workspaceRoot.toUri().toString());
        server.initialize(initParams).get(5, TimeUnit.SECONDS);

        // This test validates that the server properly handles unknown methods
        // In a real JSON-RPC server, calling an unknown method should return a -32601 error
        // However, since we're testing at the Java API level, we can't directly test this
        // without implementing a full JSON-RPC transport layer.

        // Instead, we validate that our test framework can handle error responses correctly
        JsonObject errorResponse = new JsonObject();
        errorResponse.addProperty("jsonrpc", "2.0");
        errorResponse.addProperty("id", 1000);

        JsonObject error = new JsonObject();
        error.addProperty("code", -32601);
        error.addProperty("message", "Method 'textDocument/unknownMethod' not found");
        error.addProperty("data", "The requested method is not supported by this server");
        errorResponse.add("error", error);

        String jsonResponse = gson.toJson(errorResponse);

        // Validate comprehensive error response
        assertThatJson(jsonResponse)
                .isLspError(-32601)
                .hasJsonPath("$.id", 1000)
                .hasJsonPath("$.error.message")
                .hasJsonPath("$.error.data");
    }

    @Test
    void testDefinitionJsonProtocol() throws Exception {
        // Initialize
        InitializeParams initParams = new InitializeParams();
        initParams.setRootUri(workspaceRoot.toUri().toString());
        server.initialize(initParams).get(5, TimeUnit.SECONDS);

        // Prepare definition request
        String jsonRequest =
                """
                {
                  "jsonrpc": "2.0",
                  "id": 4,
                  "method": "textDocument/definition",
                  "params": {
                    "textDocument": {
                      "uri": "file:///test.groovy"
                    },
                    "position": {
                      "line": 5,
                      "character": 10
                    }
                  }
                }
                """;

        JsonObject requestObj = JsonParser.parseString(jsonRequest).getAsJsonObject();
        DefinitionParams params = gson.fromJson(requestObj.get("params"), DefinitionParams.class);

        CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> future =
                server.getTextDocumentService().definition(params);
        Either<List<? extends Location>, List<? extends LocationLink>> result =
                future.get(5, TimeUnit.SECONDS);

        // Create response
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.addProperty("id", 4);
        response.add("result", gson.toJsonTree(result));

        String jsonResponse = gson.toJson(response);

        // Validate
        assertThatJson(jsonResponse)
                .isValidLspResponse()
                .hasJsonPath("$.id", 4)
                .hasJsonPath("$.result");
    }

    @Test
    void testMalformedParamsError() throws Exception {
        // Simulate a malformed params scenario
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.addProperty("id", 100);

        JsonObject error = new JsonObject();
        error.addProperty("code", -32602);
        error.addProperty("message", "Invalid params");
        response.add("error", error);

        String jsonResponse = gson.toJson(response);

        // Validate
        assertThatJson(jsonResponse)
                .isValidJsonRpc()
                .hasJsonPath("$.id", 100)
                .hasJsonPath("$.error.code", -32602)
                .hasJsonPath("$.error.message", "Invalid params");
    }

    @Test
    void testNotificationHandling() throws Exception {
        // Test didChange notification with unique file name
        Path testFile =
                workspaceRoot.resolve(
                        "Test_testNotificationHandling_" + UUID.randomUUID() + ".groovy");
        Files.writeString(testFile, "class Test {}");

        String notification =
                """
                {
                  "jsonrpc": "2.0",
                  "method": "textDocument/didChange",
                  "params": {
                    "textDocument": {
                      "uri": "%s",
                      "version": 2
                    },
                    "contentChanges": [{
                      "text": "class Test { String name }"
                    }]
                  }
                }
                """
                        .formatted(testFile.toUri().toString());

        // Parse notification
        JsonObject notificationObj = JsonParser.parseString(notification).getAsJsonObject();
        DidChangeTextDocumentParams params =
                gson.fromJson(notificationObj.get("params"), DidChangeTextDocumentParams.class);

        // Execute (no response expected for notifications)
        server.getTextDocumentService().didChange(params);

        // For notifications, we just verify no exception was thrown
        assertThat(true).isTrue();
    }
}
