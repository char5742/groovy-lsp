package com.groovy.lsp.protocol.internal.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.groovy.core.api.TypeInferenceService;
import com.groovy.lsp.protocol.api.IServiceRouter;
import com.groovy.lsp.protocol.internal.document.DocumentManager;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefinitionHandlerTest {

    @Mock private IServiceRouter serviceRouter;

    @Mock private DocumentManager documentManager;

    @Mock private ASTService astService;

    @Mock private TypeInferenceService typeInferenceService;

    // TODO: Enable when circular dependency is resolved
    // @Mock
    // private WorkspaceIndexService workspaceIndexService;

    @Mock private ModuleNode moduleNode;

    private DefinitionHandler handler;

    @BeforeEach
    void setUp() {
        when(serviceRouter.getAstService()).thenReturn(astService);
        // TypeInferenceService is not needed for DefinitionHandler
        // when(serviceRouter.getTypeInferenceService()).thenReturn(typeInferenceService);
        // TODO: Enable when circular dependency is resolved
        // when(serviceRouter.getWorkspaceIndexService()).thenReturn(workspaceIndexService);

        handler = new DefinitionHandler(serviceRouter, documentManager);
    }

    @Test
    void testHandleDefinition_DocumentNotFound() {
        // Arrange
        String uri = "file:///test.groovy";
        DefinitionParams params =
                new DefinitionParams(new TextDocumentIdentifier(uri), new Position(0, 0));

        when(documentManager.getDocumentContent(uri)).thenReturn(null);

        // Act
        CompletableFuture<
                        Either<
                                List<? extends Location>,
                                List<? extends org.eclipse.lsp4j.LocationLink>>>
                result = handler.handleDefinition(params);

        // Assert
        Either<List<? extends Location>, List<? extends org.eclipse.lsp4j.LocationLink>> either =
                result.join();
        assertTrue(either.isLeft());
        assertTrue(either.getLeft().isEmpty());
    }

    @Test
    void testHandleDefinition_ParseError() {
        // Arrange
        String uri = "file:///test.groovy";
        String content = "class Test { def test() {} }";
        DefinitionParams params =
                new DefinitionParams(new TextDocumentIdentifier(uri), new Position(0, 6));

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(null);

        // Act
        CompletableFuture<
                        Either<
                                List<? extends Location>,
                                List<? extends org.eclipse.lsp4j.LocationLink>>>
                result = handler.handleDefinition(params);

        // Assert
        Either<List<? extends Location>, List<? extends org.eclipse.lsp4j.LocationLink>> either =
                result.join();
        assertTrue(either.isLeft());
        assertTrue(either.getLeft().isEmpty());
    }

    @Test
    void testHandleDefinition_NoNodeAtPosition() {
        // Arrange
        String uri = "file:///test.groovy";
        String content = "class Test { def test() {} }";
        DefinitionParams params =
                new DefinitionParams(new TextDocumentIdentifier(uri), new Position(0, 6));

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 7)).thenReturn(null);

        // Act
        CompletableFuture<
                        Either<
                                List<? extends Location>,
                                List<? extends org.eclipse.lsp4j.LocationLink>>>
                result = handler.handleDefinition(params);

        // Assert
        Either<List<? extends Location>, List<? extends org.eclipse.lsp4j.LocationLink>> either =
                result.join();
        assertTrue(either.isLeft());
        assertTrue(either.getLeft().isEmpty());
    }

    @Test
    void testHandleDefinition_EmptyFile() {
        // Arrange
        String uri = "file:///empty.groovy";
        String content = "";
        DefinitionParams params =
                new DefinitionParams(new TextDocumentIdentifier(uri), new Position(0, 0));

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);

        // Act
        CompletableFuture<
                        Either<
                                List<? extends Location>,
                                List<? extends org.eclipse.lsp4j.LocationLink>>>
                result = handler.handleDefinition(params);

        // Assert
        Either<List<? extends Location>, List<? extends org.eclipse.lsp4j.LocationLink>> either =
                result.join();
        assertTrue(either.isLeft());
        assertTrue(either.getLeft().isEmpty());
    }

    @Test
    void testHandleDefinition_SyntaxError() {
        // Arrange
        String uri = "file:///syntax-error.groovy";
        String content = "class Test { def foo( } }"; // Missing closing parenthesis
        DefinitionParams params =
                new DefinitionParams(new TextDocumentIdentifier(uri), new Position(0, 18));

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        // Parse might succeed but AST might be incomplete
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 19)).thenReturn(null);

        // Act
        CompletableFuture<
                        Either<
                                List<? extends Location>,
                                List<? extends org.eclipse.lsp4j.LocationLink>>>
                result = handler.handleDefinition(params);

        // Assert
        Either<List<? extends Location>, List<? extends org.eclipse.lsp4j.LocationLink>> either =
                result.join();
        assertTrue(either.isLeft());
        assertTrue(either.getLeft().isEmpty());
    }

    @Test
    void testHandleDefinition_LargePositionNumbers() {
        // Arrange - Test with very large line/column numbers
        String uri = "file:///test.groovy";
        String content = "class Test {}";
        Position position = new Position(999999, 999999); // Very large position
        DefinitionParams params = new DefinitionParams(new TextDocumentIdentifier(uri), position);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1000000, 1000000)).thenReturn(null);

        // Act
        CompletableFuture<
                        Either<
                                List<? extends Location>,
                                List<? extends org.eclipse.lsp4j.LocationLink>>>
                result = handler.handleDefinition(params);

        // Assert
        Either<List<? extends Location>, List<? extends org.eclipse.lsp4j.LocationLink>> either =
                result.join();
        assertTrue(either.isLeft());
        assertTrue(either.getLeft().isEmpty());
    }

    @Test
    void testHandleDefinition_MethodCall_LocalMethod() {
        // Arrange
        String uri = "file:///test.groovy";
        String content = "class Test { def foo() {} def bar() { foo() } }";
        Position position = new Position(0, 40); // position on 'foo()'
        DefinitionParams params = new DefinitionParams(new TextDocumentIdentifier(uri), position);

        // Mock AST structure
        MethodCallExpression methodCall = mock(MethodCallExpression.class);
        when(methodCall.getMethodAsString()).thenReturn("foo");

        ClassNode classNode = mock(ClassNode.class);
        MethodNode methodNode = mock(MethodNode.class);
        when(methodNode.getName()).thenReturn("foo");
        when(methodNode.getLineNumber()).thenReturn(1);
        when(methodNode.getColumnNumber()).thenReturn(14);
        when(methodNode.getLastLineNumber()).thenReturn(1);
        when(methodNode.getLastColumnNumber()).thenReturn(25);

        when(classNode.getMethods()).thenReturn(List.of(methodNode));
        when(moduleNode.getClasses()).thenReturn(List.of(classNode));

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 41)).thenReturn(methodCall);

        // Act
        CompletableFuture<
                        Either<
                                List<? extends Location>,
                                List<? extends org.eclipse.lsp4j.LocationLink>>>
                result = handler.handleDefinition(params);

        // Assert
        Either<List<? extends Location>, List<? extends org.eclipse.lsp4j.LocationLink>> either =
                result.join();
        assertTrue(either.isLeft());
        List<? extends Location> locations = either.getLeft();
        assertEquals(1, locations.size());

        Location location = locations.get(0);
        assertEquals(uri, location.getUri());
        assertEquals(0, location.getRange().getStart().getLine());
        assertEquals(13, location.getRange().getStart().getCharacter());
    }

    // TODO: Enable when circular dependency is resolved
    // @Test
    // void testHandleDefinition_MethodCall_WorkspaceSearch() {
    //     // Arrange
    //     String uri = "file:///test.groovy";
    //     String content = "class Test { def bar() { someMethod() } }";
    //     Position position = new Position(0, 30); // position on 'someMethod()'
    //     DefinitionParams params = new DefinitionParams(
    //             new TextDocumentIdentifier(uri),
    //             position
    //     );
    //
    //     // Mock AST structure
    //     MethodCallExpression methodCall = mock(MethodCallExpression.class);
    //     when(methodCall.getMethodAsString()).thenReturn("someMethod");
    //
    //     ClassNode classNode = mock(ClassNode.class);
    //     when(classNode.getMethods()).thenReturn(List.of()); // No local methods
    //     when(moduleNode.getClasses()).thenReturn(List.of(classNode));
    //
    //     // Mock workspace index search
    //     Path symbolPath = Paths.get("/another/file.groovy");
    //     SymbolInfo symbolInfo = new SymbolInfo("someMethod", SymbolKind.METHOD, symbolPath, 10,
    // 5);
    //     when(workspaceIndexService.searchSymbols("someMethod"))
    //             .thenReturn(CompletableFuture.completedFuture(Stream.of(symbolInfo)));
    //
    //     when(documentManager.getDocumentContent(uri)).thenReturn(content);
    //     when(astService.parseSource(content, uri)).thenReturn(moduleNode);
    //     when(astService.findNodeAtPosition(moduleNode, 1, 31)).thenReturn(methodCall);
    //
    //     // Act
    //     CompletableFuture<Either<List<? extends Location>, List<? extends
    // org.eclipse.lsp4j.LocationLink>>>
    //             result = handler.handleDefinition(params);
    //
    //     // Assert
    //     Either<List<? extends Location>, List<? extends org.eclipse.lsp4j.LocationLink>>
    //             either = result.join();
    //     assertTrue(either.isLeft());
    //     List<? extends Location> locations = either.getLeft();
    //     assertEquals(1, locations.size());
    //
    //     Location location = locations.get(0);
    //     assertEquals(symbolPath.toUri().toString(), location.getUri());
    //     assertEquals(9, location.getRange().getStart().getLine()); // 10 - 1 (0-based)
    //     assertEquals(4, location.getRange().getStart().getCharacter()); // 5 - 1 (0-based)
    // }
}
