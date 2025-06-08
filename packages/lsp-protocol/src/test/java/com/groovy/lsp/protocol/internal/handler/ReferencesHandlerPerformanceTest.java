package com.groovy.lsp.protocol.internal.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.protocol.api.IServiceRouter;
import com.groovy.lsp.protocol.internal.document.DocumentManager;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Performance tests for ReferencesHandler.
 * These tests verify that reference searching completes within 50ms.
 */
@ExtendWith(MockitoExtension.class)
@EnabledIfEnvironmentVariable(named = "RUN_PERFORMANCE_TESTS", matches = "true")
class ReferencesHandlerPerformanceTest {

    @Mock private IServiceRouter serviceRouter;
    @Mock private DocumentManager documentManager;
    @Mock private ASTService astService;
    @Mock private ModuleNode moduleNode;

    private ReferencesHandler handler;

    @BeforeEach
    void setUp() {
        when(serviceRouter.getAstService()).thenReturn(astService);
        handler = new ReferencesHandler(serviceRouter, documentManager);
    }

    @Test
    @DisplayName("References search should complete within 50ms for variable")
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    void testPerformance_VariableReferences() {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                def x = 10
                def y = x + 5
                println x
                if (x > 0) {
                    return x * 2
                }
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

        // Act & Assert - measure time
        long startTime = System.currentTimeMillis();
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);

        List<? extends Location> locations = result.join();
        long endTime = System.currentTimeMillis();

        // Verify result
        assertNotNull(locations);

        // Verify performance
        long duration = endTime - startTime;
        assertTrue(
                duration < 50,
                "Variable references search took " + duration + "ms, should be less than 50ms");
    }

    @Test
    @DisplayName("References search should complete within 50ms for method in large class")
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    void testPerformance_MethodReferences_LargeClass() {
        // Arrange
        String uri = "file:///large.groovy";
        StringBuilder contentBuilder = new StringBuilder("class LargeClass {\n");

        // Generate a large class with many methods that call targetMethod
        contentBuilder.append("    def targetMethod() { }\n");
        for (int i = 0; i < 100; i++) {
            contentBuilder.append("    def method").append(i).append("() {\n");
            contentBuilder.append("        targetMethod()\n");
            contentBuilder.append("        this.targetMethod()\n");
            contentBuilder.append("    }\n");
        }
        contentBuilder.append("}");

        String content = contentBuilder.toString();
        Position position = new Position(1, 8); // position on 'targetMethod' declaration
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(uri),
                        position,
                        new ReferenceContext(true) // include declaration
                        );

        // Mock AST structure
        MethodNode methodNode = mock(MethodNode.class);
        when(methodNode.getName()).thenReturn("targetMethod");

        ClassNode classNode = new ClassNode("LargeClass", 0, null);
        when(moduleNode.getClasses()).thenReturn(List.of(classNode));

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 2, 9)).thenReturn(methodNode);

        // Act & Assert - measure time
        long startTime = System.currentTimeMillis();
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);

        List<? extends Location> locations = result.join();
        long endTime = System.currentTimeMillis();

        // Verify result
        assertNotNull(locations);

        // Verify performance
        long duration = endTime - startTime;
        assertTrue(
                duration < 50,
                "Method references search in large class took "
                        + duration
                        + "ms, should be less than 50ms");
    }

    @Test
    @DisplayName("References search should complete within 50ms for class references")
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    void testPerformance_ClassReferences() {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                class MyClass {
                    String name
                }

                def obj1 = new MyClass()
                def obj2 = new MyClass()
                MyClass obj3 = new MyClass()

                def createObject() {
                    return new MyClass()
                }

                class SubClass extends MyClass {
                }
                """;
        Position position = new Position(0, 6); // position on 'MyClass' declaration
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(uri), position, new ReferenceContext(true));

        // Mock AST structure
        ClassNode classNode = mock(ClassNode.class);
        when(classNode.getName()).thenReturn("MyClass");

        ClassNode scriptClass = new ClassNode("Script", 0, null);
        when(moduleNode.getClasses()).thenReturn(List.of(scriptClass));

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 7)).thenReturn(classNode);

        // Act & Assert - measure time
        long startTime = System.currentTimeMillis();
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);

        List<? extends Location> locations = result.join();
        long endTime = System.currentTimeMillis();

        // Verify result
        assertNotNull(locations);

        // Verify performance
        long duration = endTime - startTime;
        assertTrue(
                duration < 50,
                "Class references search took " + duration + "ms, should be less than 50ms");
    }

    @Test
    @DisplayName("References search should complete within 50ms for property references")
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    void testPerformance_PropertyReferences() {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                class Person {
                    String name
                    int age

                    def printInfo() {
                        println "Name: ${name}, Age: ${age}"
                    }

                    def updateName(String newName) {
                        this.name = newName
                    }

                    def getName() {
                        return name
                    }
                }

                def person = new Person()
                person.name = "John"
                println person.name
                """;
        Position position = new Position(1, 11); // position on 'name' property declaration
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(uri), position, new ReferenceContext(false));

        // Mock AST structure
        PropertyNode propertyNode = mock(PropertyNode.class);
        when(propertyNode.getName()).thenReturn("name");

        ClassNode classNode = new ClassNode("Person", 0, null);
        when(moduleNode.getClasses()).thenReturn(List.of(classNode));

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 2, 12)).thenReturn(propertyNode);

        // Act & Assert - measure time
        long startTime = System.currentTimeMillis();
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);

        List<? extends Location> locations = result.join();
        long endTime = System.currentTimeMillis();

        // Verify result
        assertNotNull(locations);

        // Verify performance
        long duration = endTime - startTime;
        assertTrue(
                duration < 50,
                "Property references search took " + duration + "ms, should be less than 50ms");
    }
}
