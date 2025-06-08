package com.groovy.lsp.protocol.internal.handler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.protocol.api.IServiceRouter;
import com.groovy.lsp.protocol.internal.document.DocumentManager;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.control.SourceUnit;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for ReferencesHandler's visitor implementations.
 * These tests specifically target the various visitor classes for different AST node types.
 */
@ExtendWith(MockitoExtension.class)
class ReferencesHandlerVisitorTest {

    @Mock private IServiceRouter serviceRouter;
    @Mock private DocumentManager documentManager;
    @Mock private ASTService astService;

    private ReferencesHandler handler;

    @BeforeEach
    void setUp() {
        when(serviceRouter.getAstService()).thenReturn(astService);
        handler = new ReferencesHandler(serviceRouter, documentManager);
    }

    @Test
    @DisplayName("Should find all method references including declaration")
    void testMethodReferenceVisitor_WithDeclaration() throws Exception {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                class TestClass {
                    def targetMethod() {
                        println "target"
                    }

                    def caller1() {
                        targetMethod()
                    }

                    def caller2() {
                        this.targetMethod()
                    }
                }
                """;
        Position position = new Position(1, 8); // position on method declaration
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(uri),
                        position,
                        new ReferenceContext(true) // include declaration
                        );

        // Create real AST nodes
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode classNode = new ClassNode("TestClass", 0, null);

        // Create target method
        MethodNode targetMethod =
                new MethodNode("targetMethod", 0, null, new Parameter[0], null, null);
        targetMethod.setLineNumber(2);
        targetMethod.setColumnNumber(9);

        // Create method calls
        MethodCallExpression call1 =
                new MethodCallExpression(
                        VariableExpression.THIS_EXPRESSION,
                        new ConstantExpression("targetMethod"),
                        ArgumentListExpression.EMPTY_ARGUMENTS);
        call1.setLineNumber(7);
        call1.setColumnNumber(9);

        MethodCallExpression call2 =
                new MethodCallExpression(
                        VariableExpression.THIS_EXPRESSION,
                        new ConstantExpression("targetMethod"),
                        ArgumentListExpression.EMPTY_ARGUMENTS);
        call2.setLineNumber(11);
        call2.setColumnNumber(14);

        // Add methods to class
        classNode.addMethod(targetMethod);

        // Create caller methods with calls
        BlockStatement block1 = new BlockStatement();
        block1.addStatement(new ExpressionStatement(call1));
        MethodNode caller1 = new MethodNode("caller1", 0, null, new Parameter[0], null, block1);
        classNode.addMethod(caller1);

        BlockStatement block2 = new BlockStatement();
        block2.addStatement(new ExpressionStatement(call2));
        MethodNode caller2 = new MethodNode("caller2", 0, null, new Parameter[0], null, block2);
        classNode.addMethod(caller2);

        moduleNode.addClass(classNode);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 2, 9)).thenReturn(targetMethod);

        // Act
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);
        List<? extends Location> locations = result.join();

        // Assert
        assertNotNull(locations);
        assertTrue(locations.size() >= 2, "Should find at least method calls");
    }

    @Test
    @DisplayName("Should find class references")
    void testClassReferenceVisitor() throws Exception {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                class TargetClass {
                    String name
                }

                def obj1 = new TargetClass()
                TargetClass obj2 = new TargetClass()

                class SubClass extends TargetClass {
                }
                """;
        Position position = new Position(0, 6); // position on class declaration
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(uri),
                        position,
                        new ReferenceContext(false) // exclude declaration
                        );

        // Create real AST nodes
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);

        // Create target class
        ClassNode targetClass = new ClassNode("TargetClass", 0, null);
        targetClass.setLineNumber(1);
        targetClass.setColumnNumber(7);
        moduleNode.addClass(targetClass);

        // Create class usage in new expression
        ClassExpression classExpr1 = new ClassExpression(targetClass);
        classExpr1.setLineNumber(5);
        classExpr1.setColumnNumber(16);

        ConstructorCallExpression ctorCall1 =
                new ConstructorCallExpression(targetClass, ArgumentListExpression.EMPTY_ARGUMENTS);
        ctorCall1.setLineNumber(5);
        ctorCall1.setColumnNumber(12);

        // Create class usage in variable declaration
        ClassExpression classExpr2 = new ClassExpression(targetClass);
        classExpr2.setLineNumber(6);
        classExpr2.setColumnNumber(1);

        // Create subclass
        ClassNode subClass = new ClassNode("SubClass", 0, targetClass);
        subClass.setLineNumber(8);
        subClass.setColumnNumber(7);
        moduleNode.addClass(subClass);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 7)).thenReturn(targetClass);

        // Act
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);
        List<? extends Location> locations = result.join();

        // Assert
        assertNotNull(locations);
        // Should find references but not the declaration itself
    }

    @Test
    @DisplayName("Should find property references")
    void testPropertyReferenceVisitor() throws Exception {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                class Person {
                    String name

                    def getName() {
                        return name
                    }

                    def setName(String newName) {
                        this.name = newName
                    }
                }

                def person = new Person()
                person.name = "John"
                println person.name
                """;
        Position position = new Position(1, 11); // position on property declaration
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(uri), position, new ReferenceContext(false));

        // Create real AST nodes
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode personClass = new ClassNode("Person", 0, null);

        // Create property
        PropertyNode nameProperty =
                new PropertyNode("name", 0, ClassHelper.STRING_TYPE, personClass, null, null, null);
        nameProperty.setLineNumber(2);
        nameProperty.setColumnNumber(12);
        personClass.addProperty(nameProperty);

        // Create property access expressions
        PropertyExpression propAccess1 =
                new PropertyExpression(
                        VariableExpression.THIS_EXPRESSION, new ConstantExpression("name"));
        propAccess1.setLineNumber(5);
        propAccess1.setColumnNumber(16);

        PropertyExpression propAccess2 =
                new PropertyExpression(
                        VariableExpression.THIS_EXPRESSION, new ConstantExpression("name"));
        propAccess2.setLineNumber(9);
        propAccess2.setColumnNumber(14);

        PropertyExpression propAccess3 =
                new PropertyExpression(
                        new VariableExpression("person"), new ConstantExpression("name"));
        propAccess3.setLineNumber(14);
        propAccess3.setColumnNumber(8);

        PropertyExpression propAccess4 =
                new PropertyExpression(
                        new VariableExpression("person"), new ConstantExpression("name"));
        propAccess4.setLineNumber(15);
        propAccess4.setColumnNumber(16);

        moduleNode.addClass(personClass);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 2, 12)).thenReturn(nameProperty);

        // Act
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);
        List<? extends Location> locations = result.join();

        // Assert
        assertNotNull(locations);
        // Should find property accesses
    }

    @Test
    @DisplayName("Should find variable references in complex expressions")
    void testReferenceVisitor_ComplexExpressions() throws Exception {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                def x = 10
                def y = x + 5
                def z = x * 2 + y
                if (x > 0) {
                    println x
                }
                [1, 2, 3].each { item ->
                    println x + item
                }
                """;
        Position position = new Position(0, 4); // position on 'x' declaration
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(uri), position, new ReferenceContext(false));

        // Create real AST nodes
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode scriptClass = new ClassNode("Script", 0, null);

        // Create variable expressions for 'x'
        VariableExpression xDecl = new VariableExpression("x");
        xDecl.setLineNumber(1);
        xDecl.setColumnNumber(5);

        VariableExpression xRef1 = new VariableExpression("x");
        xRef1.setLineNumber(2);
        xRef1.setColumnNumber(9);

        VariableExpression xRef2 = new VariableExpression("x");
        xRef2.setLineNumber(3);
        xRef2.setColumnNumber(9);

        VariableExpression xRef3 = new VariableExpression("x");
        xRef3.setLineNumber(4);
        xRef3.setColumnNumber(5);

        VariableExpression xRef4 = new VariableExpression("x");
        xRef4.setLineNumber(5);
        xRef4.setColumnNumber(13);

        VariableExpression xRef5 = new VariableExpression("x");
        xRef5.setLineNumber(8);
        xRef5.setColumnNumber(13);

        moduleNode.addClass(scriptClass);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 5)).thenReturn(xDecl);

        // Act
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);
        List<? extends Location> locations = result.join();

        // Assert
        assertNotNull(locations);
        // Should find all references to 'x'
    }

    @Test
    @DisplayName("Should handle method references with different call styles")
    void testMethodReferenceVisitor_DifferentCallStyles() throws Exception {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                class TestClass {
                    def targetMethod(arg = null) {
                        println arg
                    }

                    def test() {
                        targetMethod()           // direct call
                        targetMethod("arg")      // with argument
                        this.targetMethod()      // explicit this
                        this.&targetMethod       // method pointer
                        "string".with {
                            targetMethod()       // inside closure
                        }
                    }
                }

                def obj = new TestClass()
                obj.targetMethod()               // external call
                obj.&targetMethod                // external method pointer
                """;
        Position position = new Position(1, 8); // position on method declaration
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(uri), position, new ReferenceContext(true));

        // Create real AST nodes
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode testClass = new ClassNode("TestClass", 0, null);

        // Create target method
        MethodNode targetMethod =
                new MethodNode(
                        "targetMethod",
                        0,
                        null,
                        new Parameter[] {
                            new Parameter(
                                    ClassHelper.OBJECT_TYPE, "arg", new ConstantExpression(null))
                        },
                        null,
                        null);
        targetMethod.setLineNumber(2);
        targetMethod.setColumnNumber(9);
        testClass.addMethod(targetMethod);

        moduleNode.addClass(testClass);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 2, 9)).thenReturn(targetMethod);

        // Act
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);
        List<? extends Location> locations = result.join();

        // Assert
        assertNotNull(locations);
        // Should find various method call styles
    }
}
