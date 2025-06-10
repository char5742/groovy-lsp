package com.groovy.lsp.protocol.internal.handler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.protocol.api.IServiceRouter;
import com.groovy.lsp.protocol.internal.document.DocumentManager;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.DynamicVariable;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.control.SourceUnit;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Additional branch coverage tests for DefinitionHandler.
 * These tests target specific branches that are not covered by existing tests.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefinitionHandlerBranchTest {

    @Mock private IServiceRouter serviceRouter;
    @Mock private DocumentManager documentManager;
    @Mock private ASTService astService;

    private DefinitionHandler handler;

    @BeforeEach
    void setUp() {
        when(serviceRouter.getAstService()).thenReturn(astService);
        handler = new DefinitionHandler(serviceRouter, documentManager);
    }

    @Test
    @DisplayName("Should handle PropertyExpression for property definition")
    void testHandleDefinition_PropertyExpression() throws Exception {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                class Person {
                    String name
                }
                def person = new Person()
                person.name = "John"
                """;
        Position position = new Position(4, 7); // position on 'name' in person.name
        DefinitionParams params = new DefinitionParams(new TextDocumentIdentifier(uri), position);

        // Create AST nodes
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode personClass = new ClassNode("Person", 0, null);

        // Create property
        PropertyNode nameProperty =
                new PropertyNode("name", 0, ClassHelper.STRING_TYPE, personClass, null, null, null);
        nameProperty.setLineNumber(2);
        nameProperty.setColumnNumber(12);
        nameProperty.setLastLineNumber(2);
        nameProperty.setLastColumnNumber(15);
        personClass.addProperty(nameProperty);

        // Create property expression
        PropertyExpression propExpr =
                new PropertyExpression(
                        new VariableExpression("person"), new ConstantExpression("name"));
        propExpr.setLineNumber(5);
        propExpr.setColumnNumber(8);

        moduleNode.addClass(personClass);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 5, 8)).thenReturn(propExpr);

        // Act
        CompletableFuture<
                        Either<
                                List<? extends Location>,
                                List<? extends org.eclipse.lsp4j.LocationLink>>>
                result = handler.handleDefinition(params);

        Either<List<? extends Location>, List<? extends org.eclipse.lsp4j.LocationLink>> either =
                result.join();

        // Assert
        assertTrue(either.isLeft());
        assertNotNull(either.getLeft());
    }

    @Test
    @DisplayName("Should handle ClassExpression for class definition")
    void testHandleDefinition_ClassExpression() throws Exception {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                class MyClass {}
                def clazz = MyClass.class
                """;
        Position position = new Position(1, 12); // position on 'MyClass'
        DefinitionParams params = new DefinitionParams(new TextDocumentIdentifier(uri), position);

        // Create AST nodes
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode myClass = new ClassNode("MyClass", 0, null);
        myClass.setLineNumber(1);
        myClass.setColumnNumber(7);
        myClass.setLastLineNumber(1);
        myClass.setLastColumnNumber(13);

        // Create class expression
        ClassExpression classExpr = new ClassExpression(myClass);
        classExpr.setLineNumber(2);
        classExpr.setColumnNumber(13);

        moduleNode.addClass(myClass);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 2, 13)).thenReturn(classExpr);

        // Act
        CompletableFuture<
                        Either<
                                List<? extends Location>,
                                List<? extends org.eclipse.lsp4j.LocationLink>>>
                result = handler.handleDefinition(params);

        Either<List<? extends Location>, List<? extends org.eclipse.lsp4j.LocationLink>> either =
                result.join();

        // Assert
        assertTrue(either.isLeft());
        List<? extends Location> locations = either.getLeft();
        assertNotNull(locations);
        // Current implementation returns empty list for class expressions
        assertTrue(locations.isEmpty());
    }

    @Test
    @DisplayName("Should handle ConstructorCallExpression for class definition")
    void testHandleDefinition_ConstructorCallExpression() throws Exception {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                class MyClass {}
                def obj = new MyClass()
                """;
        Position position = new Position(1, 14); // position on 'MyClass' in constructor
        DefinitionParams params = new DefinitionParams(new TextDocumentIdentifier(uri), position);

        // Create AST nodes
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode myClass = new ClassNode("MyClass", 0, null);
        myClass.setLineNumber(1);
        myClass.setColumnNumber(7);
        myClass.setLastLineNumber(1);
        myClass.setLastColumnNumber(13);

        // Create constructor call expression
        ConstructorCallExpression ctorCall =
                new ConstructorCallExpression(myClass, ArgumentListExpression.EMPTY_ARGUMENTS);
        ctorCall.setLineNumber(2);
        ctorCall.setColumnNumber(11);

        moduleNode.addClass(myClass);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 2, 15)).thenReturn(ctorCall);

        // Act
        CompletableFuture<
                        Either<
                                List<? extends Location>,
                                List<? extends org.eclipse.lsp4j.LocationLink>>>
                result = handler.handleDefinition(params);

        Either<List<? extends Location>, List<? extends org.eclipse.lsp4j.LocationLink>> either =
                result.join();

        // Assert
        assertTrue(either.isLeft());
        List<? extends Location> locations = either.getLeft();
        assertNotNull(locations);
        // Current implementation returns empty list for class expressions
        assertTrue(locations.isEmpty());
    }

    @Test
    @DisplayName("Should handle exception during AST parsing")
    void testHandleDefinition_ExceptionHandling() throws Exception {
        // Arrange
        String uri = "file:///test.groovy";
        String content = "def x = 10";
        Position position = new Position(0, 4);
        DefinitionParams params = new DefinitionParams(new TextDocumentIdentifier(uri), position);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenThrow(new RuntimeException("Parse error"));

        // Act
        CompletableFuture<
                        Either<
                                List<? extends Location>,
                                List<? extends org.eclipse.lsp4j.LocationLink>>>
                result = handler.handleDefinition(params);

        Either<List<? extends Location>, List<? extends org.eclipse.lsp4j.LocationLink>> either =
                result.join();

        // Assert
        assertTrue(either.isLeft());
        List<? extends Location> locations = either.getLeft();
        assertTrue(locations.isEmpty());
    }

    @Test
    @DisplayName("Should handle null variable in findVariableDefinition")
    void testFindVariableDefinition_NullVariable() throws Exception {
        // Arrange
        String uri = "file:///test.groovy";
        String content = "def x = 10";
        Position position = new Position(0, 4);
        DefinitionParams params = new DefinitionParams(new TextDocumentIdentifier(uri), position);

        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);

        // Create variable expression with null accessed variable
        VariableExpression varExpr = new VariableExpression("x");
        varExpr.setAccessedVariable(null); // null variable
        varExpr.setLineNumber(1);
        varExpr.setColumnNumber(5);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 5)).thenReturn(varExpr);

        // Act
        CompletableFuture<
                        Either<
                                List<? extends Location>,
                                List<? extends org.eclipse.lsp4j.LocationLink>>>
                result = handler.handleDefinition(params);

        Either<List<? extends Location>, List<? extends org.eclipse.lsp4j.LocationLink>> either =
                result.join();

        // Assert
        assertTrue(either.isLeft());
        List<? extends Location> locations = either.getLeft();
        assertTrue(locations.isEmpty());
    }

    @Test
    @DisplayName("Should handle method with null method name")
    void testFindMethodDefinition_NullMethodName() throws Exception {
        // Arrange
        String uri = "file:///test.groovy";
        String content = "def test() {}; test()";
        Position position = new Position(0, 15);
        DefinitionParams params = new DefinitionParams(new TextDocumentIdentifier(uri), position);

        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);

        // Create method call with null method name
        MethodCallExpression methodCall =
                new MethodCallExpression(
                        VariableExpression.THIS_EXPRESSION,
                        (Expression) null, // null method name
                        ArgumentListExpression.EMPTY_ARGUMENTS);
        methodCall.setLineNumber(1);
        methodCall.setColumnNumber(16);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 16)).thenReturn(methodCall);

        // Act
        CompletableFuture<
                        Either<
                                List<? extends Location>,
                                List<? extends org.eclipse.lsp4j.LocationLink>>>
                result = handler.handleDefinition(params);

        Either<List<? extends Location>, List<? extends org.eclipse.lsp4j.LocationLink>> either =
                result.join();

        // Assert
        assertTrue(either.isLeft());
        List<? extends Location> locations = either.getLeft();
        assertTrue(locations.isEmpty());
    }

    @Test
    @DisplayName("Should handle property with null property name")
    void testFindPropertyDefinition_NullPropertyName() throws Exception {
        // Arrange
        String uri = "file:///test.groovy";
        String content = "class Test { String name }; test.name";
        Position position = new Position(0, 33);
        DefinitionParams params = new DefinitionParams(new TextDocumentIdentifier(uri), position);

        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode testClass = new ClassNode("Test", 0, null);
        moduleNode.addClass(testClass);

        // Create property expression with null property
        PropertyExpression propExpr =
                new PropertyExpression(
                        new VariableExpression("test"), (Expression) null // null property
                        );
        propExpr.setLineNumber(1);
        propExpr.setColumnNumber(34);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 34)).thenReturn(propExpr);

        // Act
        CompletableFuture<
                        Either<
                                List<? extends Location>,
                                List<? extends org.eclipse.lsp4j.LocationLink>>>
                result = handler.handleDefinition(params);

        Either<List<? extends Location>, List<? extends org.eclipse.lsp4j.LocationLink>> either =
                result.join();

        // Assert
        assertTrue(either.isLeft());
        List<? extends Location> locations = either.getLeft();
        assertTrue(locations.isEmpty());
    }

    @Test
    @DisplayName("Should handle script with multiple classes for findDeclaringNode")
    void testFindDeclaringNode_MultipleClasses() throws Exception {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                class First {}
                class Second {
                    def method() {
                        def x = 10
                    }
                }
                """;
        Position position = new Position(3, 12);
        DefinitionParams params = new DefinitionParams(new TextDocumentIdentifier(uri), position);

        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);

        // Add two classes
        ClassNode firstClass = new ClassNode("First", 0, null);
        ClassNode secondClass = new ClassNode("Second", 0, null);

        // Add method to second class
        MethodNode methodNode = new MethodNode("method", 0, null, new Parameter[0], null, null);
        BlockStatement blockStatement = new BlockStatement();
        methodNode.setCode(blockStatement);
        secondClass.addMethod(methodNode);

        moduleNode.addClass(firstClass);
        moduleNode.addClass(secondClass);

        // Create variable expression
        VariableExpression varExpr = new VariableExpression("x");
        varExpr.setAccessedVariable(new DynamicVariable("x", false));
        varExpr.setLineNumber(4);
        varExpr.setColumnNumber(13);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 4, 13)).thenReturn(varExpr);

        // Act
        CompletableFuture<
                        Either<
                                List<? extends Location>,
                                List<? extends org.eclipse.lsp4j.LocationLink>>>
                result = handler.handleDefinition(params);

        Either<List<? extends Location>, List<? extends org.eclipse.lsp4j.LocationLink>> either =
                result.join();

        // Assert
        assertTrue(either.isLeft());
        assertNotNull(either.getLeft());
    }
}
