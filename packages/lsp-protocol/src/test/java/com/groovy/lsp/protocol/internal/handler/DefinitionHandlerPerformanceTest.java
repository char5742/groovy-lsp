package com.groovy.lsp.protocol.internal.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.protocol.api.IServiceRouter;
import com.groovy.lsp.protocol.internal.document.DocumentManager;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Variable;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Performance tests for DefinitionHandler.
 * These tests verify that definition resolution completes within 50ms.
 */
@ExtendWith(MockitoExtension.class)
@EnabledIfEnvironmentVariable(named = "RUN_PERFORMANCE_TESTS", matches = "true")
class DefinitionHandlerPerformanceTest {

    @Mock private IServiceRouter serviceRouter;
    @Mock private DocumentManager documentManager;
    @Mock private ASTService astService;
    @Mock private ModuleNode moduleNode;

    private DefinitionHandler handler;

    @BeforeEach
    void setUp() {
        when(serviceRouter.getAstService()).thenReturn(astService);
        handler = new DefinitionHandler(serviceRouter, documentManager);
    }

    @Test
    @DisplayName("Definition resolution should complete within 50ms for simple method call")
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    void testPerformance_SimpleMethodCall() {
        // Arrange
        String uri = "file:///test.groovy";
        String content = "class Test { def foo() {} def bar() { foo() } }";
        Position position = new Position(0, 40);
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

        // Act & Assert - measure time
        long startTime = System.currentTimeMillis();
        CompletableFuture<
                        Either<
                                List<? extends Location>,
                                List<? extends org.eclipse.lsp4j.LocationLink>>>
                result = handler.handleDefinition(params);

        Either<List<? extends Location>, List<? extends org.eclipse.lsp4j.LocationLink>> either =
                result.join();
        long endTime = System.currentTimeMillis();

        // Verify result
        assertTrue(either.isLeft());
        assertEquals(1, either.getLeft().size());

        // Verify performance
        long duration = endTime - startTime;
        assertTrue(
                duration < 50,
                "Definition resolution took " + duration + "ms, should be less than 50ms");
    }

    @Test
    @DisplayName("Definition resolution should complete within 50ms for large class")
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    void testPerformance_LargeClass() {
        // Arrange
        String uri = "file:///large.groovy";
        StringBuilder contentBuilder = new StringBuilder("class LargeClass {\n");

        // Generate a large class with many methods
        for (int i = 0; i < 100; i++) {
            contentBuilder.append("    def method").append(i).append("() { }\n");
        }
        contentBuilder.append("    def testMethod() { method50() }\n");
        contentBuilder.append("}");

        String content = contentBuilder.toString();
        Position position = new Position(101, 25); // position on method50()
        DefinitionParams params = new DefinitionParams(new TextDocumentIdentifier(uri), position);

        // Mock AST structure
        MethodCallExpression methodCall = mock(MethodCallExpression.class);
        when(methodCall.getMethodAsString()).thenReturn("method50");

        ClassNode classNode = mock(ClassNode.class);

        // Create 100 method nodes
        List<MethodNode> methods = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            MethodNode method = mock(MethodNode.class);
            when(method.getName()).thenReturn("method" + i);
            when(method.getLineNumber()).thenReturn(i + 2);
            when(method.getColumnNumber()).thenReturn(9);
            when(method.getLastLineNumber()).thenReturn(i + 2);
            when(method.getLastColumnNumber()).thenReturn(30);
            methods.add(method);
        }

        when(classNode.getMethods()).thenReturn(methods);
        when(moduleNode.getClasses()).thenReturn(List.of(classNode));

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 102, 26)).thenReturn(methodCall);

        // Act & Assert - measure time
        long startTime = System.currentTimeMillis();
        CompletableFuture<
                        Either<
                                List<? extends Location>,
                                List<? extends org.eclipse.lsp4j.LocationLink>>>
                result = handler.handleDefinition(params);

        Either<List<? extends Location>, List<? extends org.eclipse.lsp4j.LocationLink>> either =
                result.join();
        long endTime = System.currentTimeMillis();

        // Verify result
        assertTrue(either.isLeft());
        assertEquals(1, either.getLeft().size());

        // Verify performance
        long duration = endTime - startTime;
        assertTrue(
                duration < 50,
                "Definition resolution for large class took "
                        + duration
                        + "ms, should be less than 50ms");
    }

    @Test
    @DisplayName("Definition resolution should complete within 50ms for variable reference")
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    void testPerformance_VariableReference() {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                def calculateComplexValue() {
                    def result = 0
                    for (int i = 0; i < 100; i++) {
                        result += i
                    }
                    return result
                }
                """;
        Position position = new Position(3, 8); // position on 'result' in line 4
        DefinitionParams params = new DefinitionParams(new TextDocumentIdentifier(uri), position);

        // Mock AST structure
        VariableExpression varExpr = mock(VariableExpression.class);
        when(varExpr.getName()).thenReturn("result");

        Variable variable = mock(Variable.class);
        when(variable.getName()).thenReturn("result");
        when(varExpr.getAccessedVariable()).thenReturn(variable);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 4, 9)).thenReturn(varExpr);
        when(moduleNode.getClasses()).thenReturn(List.of());
        when(moduleNode.getStatementBlock()).thenReturn(null);

        // Act & Assert - measure time
        long startTime = System.currentTimeMillis();
        CompletableFuture<
                        Either<
                                List<? extends Location>,
                                List<? extends org.eclipse.lsp4j.LocationLink>>>
                result = handler.handleDefinition(params);

        Either<List<? extends Location>, List<? extends org.eclipse.lsp4j.LocationLink>> either =
                result.join();
        long endTime = System.currentTimeMillis();

        // Verify result
        assertTrue(either.isLeft());

        // Verify performance
        long duration = endTime - startTime;
        assertTrue(
                duration < 50,
                "Variable definition resolution took " + duration + "ms, should be less than 50ms");
    }
}
