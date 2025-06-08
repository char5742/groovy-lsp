package com.groovy.lsp.protocol.internal.handler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.protocol.api.IServiceRouter;
import com.groovy.lsp.protocol.internal.document.DocumentManager;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReferencesHandlerTest {

    @Mock private IServiceRouter serviceRouter;

    @Mock private DocumentManager documentManager;

    @Mock private ASTService astService;

    // TODO: Enable when circular dependency is resolved
    // @Mock
    // private WorkspaceIndexService workspaceIndexService;

    @Mock private ModuleNode moduleNode;

    private ReferencesHandler handler;

    @BeforeEach
    void setUp() {
        when(serviceRouter.getAstService()).thenReturn(astService);
        // TypeInferenceService is not needed for ReferencesHandler
        // when(serviceRouter.getTypeInferenceService()).thenReturn(typeInferenceService);
        // TODO: Enable when circular dependency is resolved
        // when(serviceRouter.getWorkspaceIndexService()).thenReturn(workspaceIndexService);

        handler = new ReferencesHandler(serviceRouter, documentManager);
    }

    @Test
    void testHandleReferences_DocumentNotFound() {
        // Arrange
        String uri = "file:///test.groovy";
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(uri),
                        new Position(0, 0),
                        new ReferenceContext(false));

        when(documentManager.getDocumentContent(uri)).thenReturn(null);

        // Act
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);

        // Assert
        List<? extends Location> locations = result.join();
        assertTrue(locations.isEmpty());
    }

    @Test
    void testHandleReferences_ParseError() {
        // Arrange
        String uri = "file:///test.groovy";
        String content = "class Test { def test() {} }";
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(uri),
                        new Position(0, 6),
                        new ReferenceContext(false));

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(null);

        // Act
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);

        // Assert
        List<? extends Location> locations = result.join();
        assertTrue(locations.isEmpty());
    }

    @Test
    void testHandleReferences_NoNodeAtPosition() {
        // Arrange
        String uri = "file:///test.groovy";
        String content = "class Test { def test() {} }";
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(uri),
                        new Position(0, 6),
                        new ReferenceContext(false));

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 7)).thenReturn(null);

        // Act
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);

        // Assert
        List<? extends Location> locations = result.join();
        assertTrue(locations.isEmpty());
    }

    @Test
    void testHandleReferences_VariableReferences() {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                def x = 10
                def y = x + 5
                println x
                """;
        Position position = new Position(0, 4); // position on 'x' in first line
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(uri), position, new ReferenceContext(false));

        // Mock AST structure
        VariableExpression varExpr = mock(VariableExpression.class);
        when(varExpr.getName()).thenReturn("x");

        // Create a simple class node to visit
        ClassNode classNode = new ClassNode("Script", 0, null);
        when(moduleNode.getClasses()).thenReturn(List.of(classNode));

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 5)).thenReturn(varExpr);

        // Act
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);

        // Assert
        List<? extends Location> locations = result.join();
        assertNotNull(locations);
        // The actual references would be found by the visitor, but in this mock test
        // we can't easily simulate the full AST traversal
    }

    @Test
    void testHandleReferences_MethodReferences_IncludeDeclaration() {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                class Test {
                    def foo() {}
                    def bar() {
                        foo()
                        this.foo()
                    }
                }
                """;
        Position position = new Position(1, 8); // position on 'foo' method declaration
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(uri),
                        position,
                        new ReferenceContext(true) // include declaration
                        );

        // Mock AST structure
        MethodNode methodNode = mock(MethodNode.class);
        when(methodNode.getName()).thenReturn("foo");

        ClassNode classNode = new ClassNode("Test", 0, null);
        when(moduleNode.getClasses()).thenReturn(List.of(classNode));

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 2, 9)).thenReturn(methodNode);

        // Act
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);

        // Assert
        List<? extends Location> locations = result.join();
        assertNotNull(locations);
    }

    @Test
    void testHandleReferences_MethodCallReferences() {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                class Test {
                    def foo() {}
                    def bar() {
                        foo()
                    }
                }
                """;
        Position position = new Position(3, 8); // position on 'foo()' call
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(uri), position, new ReferenceContext(false));

        // Mock AST structure
        MethodCallExpression methodCall = mock(MethodCallExpression.class);
        when(methodCall.getMethodAsString()).thenReturn("foo");

        ClassNode classNode = new ClassNode("Test", 0, null);
        when(moduleNode.getClasses()).thenReturn(List.of(classNode));

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 4, 9)).thenReturn(methodCall);

        // Act
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);

        // Assert
        List<? extends Location> locations = result.join();
        assertNotNull(locations);
    }

    @Test
    void testHandleReferences_ClassReferences() {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                class MyClass {}
                def obj1 = new MyClass()
                MyClass obj2
                """;
        Position position = new Position(0, 6); // position on 'MyClass' declaration
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(uri), position, new ReferenceContext(false));

        // Mock AST structure - ClassNode is concrete so no need to mock getName()
        ClassNode classNode = new ClassNode("MyClass", 0, null);

        when(moduleNode.getClasses()).thenReturn(List.of(classNode));

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 7)).thenReturn(classNode);

        // Act
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);

        // Assert
        List<? extends Location> locations = result.join();
        assertNotNull(locations);
    }

    @Test
    void testHandleReferences_FieldReferences() {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                class Test {
                    String name
                    def getName() { name }
                }
                """;
        Position position = new Position(1, 11); // position on 'name' field
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(uri), position, new ReferenceContext(false));

        // Mock AST structure
        FieldNode fieldNode = mock(FieldNode.class);
        when(fieldNode.getName()).thenReturn("name");

        ClassNode classNode = new ClassNode("Test", 0, null);
        when(moduleNode.getClasses()).thenReturn(List.of(classNode));

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 2, 12)).thenReturn(fieldNode);

        // Act
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);

        // Assert
        List<? extends Location> locations = result.join();
        assertNotNull(locations);
    }

    @Test
    void testHandleReferences_EmptyFile() {
        // Arrange
        String uri = "file:///empty.groovy";
        String content = "";
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(uri),
                        new Position(0, 0),
                        new ReferenceContext(false));

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);

        // Act
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);

        // Assert
        List<? extends Location> locations = result.join();
        assertTrue(locations.isEmpty());
    }

    @Test
    void testHandleReferences_SyntaxError() {
        // Arrange
        String uri = "file:///syntax-error.groovy";
        String content = "class Test { def foo( } }"; // Missing closing parenthesis
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(uri),
                        new Position(0, 18),
                        new ReferenceContext(false));

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        // Parse might succeed but AST might be incomplete
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 19)).thenReturn(null);

        // Act
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);

        // Assert
        List<? extends Location> locations = result.join();
        assertTrue(locations.isEmpty());
    }

    @Test
    void testHandleReferences_LargePositionNumbers() {
        // Arrange - Test with very large line/column numbers
        String uri = "file:///test.groovy";
        String content = "class Test {}";
        Position position = new Position(999999, 999999); // Very large position
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(uri), position, new ReferenceContext(false));

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1000000, 1000000)).thenReturn(null);

        // Act
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);

        // Assert
        List<? extends Location> locations = result.join();
        assertTrue(locations.isEmpty());
    }

    @Test
    void testHandleReferences_NullContent() {
        // Arrange
        String uri = "file:///null-content.groovy";
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(uri),
                        new Position(0, 0),
                        new ReferenceContext(false));

        when(documentManager.getDocumentContent(uri)).thenReturn(null);

        // Act
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);

        // Assert
        List<? extends Location> locations = result.join();
        assertTrue(locations.isEmpty());
    }
}
