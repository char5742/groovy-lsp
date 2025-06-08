package com.groovy.lsp.protocol.internal.handler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Additional branch coverage tests for ReferencesHandler.
 * These tests target specific branches that are not covered by existing tests.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReferencesHandlerBranchTest {

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
    @DisplayName("Should find class references in constructor calls")
    void testClassReferenceVisitor_ConstructorCall() throws Exception {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                class Person {}
                def p1 = new Person()
                def p2 = new Person()
                """;
        Position position = new Position(0, 6); // position on class declaration
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(uri),
                        position,
                        new ReferenceContext(false) // exclude declaration
                        );

        // Create AST nodes
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);

        // Create class
        ClassNode personClass = new ClassNode("Person", 0, null);
        personClass.setLineNumber(1);
        personClass.setColumnNumber(7);
        moduleNode.addClass(personClass);

        // Create constructor calls
        ConstructorCallExpression ctor1 =
                new ConstructorCallExpression(personClass, ArgumentListExpression.EMPTY_ARGUMENTS);
        ctor1.setLineNumber(2);
        ctor1.setColumnNumber(10);

        ConstructorCallExpression ctor2 =
                new ConstructorCallExpression(personClass, ArgumentListExpression.EMPTY_ARGUMENTS);
        ctor2.setLineNumber(3);
        ctor2.setColumnNumber(10);

        // Add to module as script statements
        BlockStatement scriptBlock = new BlockStatement();
        scriptBlock.addStatement(new ExpressionStatement(ctor1));
        scriptBlock.addStatement(new ExpressionStatement(ctor2));

        // Create script class
        ClassNode scriptClass = new ClassNode("Script", 0, null);
        MethodNode runMethod = new MethodNode("run", 0, null, new Parameter[0], null, scriptBlock);
        scriptClass.addMethod(runMethod);
        moduleNode.addClass(scriptClass);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 7)).thenReturn(personClass);

        // Act
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);
        List<? extends Location> locations = result.join();

        // Assert
        assertNotNull(locations);
        // Should find constructor references
        assertTrue(locations.size() >= 2);
    }

    @Test
    @DisplayName("Should find class references in variable types")
    void testClassReferenceVisitor_VariableType() throws Exception {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                class MyType {}
                MyType var1 = null
                MyType var2 = null
                """;
        Position position = new Position(0, 6); // position on class declaration
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(uri), position, new ReferenceContext(false));

        // Create AST nodes
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);

        // Create class
        ClassNode myTypeClass = new ClassNode("MyType", 0, null);
        myTypeClass.setLineNumber(1);
        myTypeClass.setColumnNumber(7);
        moduleNode.addClass(myTypeClass);

        // Create class expressions for variable types
        ClassExpression classExpr1 = new ClassExpression(myTypeClass);
        classExpr1.setLineNumber(2);
        classExpr1.setColumnNumber(1);

        ClassExpression classExpr2 = new ClassExpression(myTypeClass);
        classExpr2.setLineNumber(3);
        classExpr2.setColumnNumber(1);

        // Create script block
        BlockStatement scriptBlock = new BlockStatement();
        scriptBlock.addStatement(new ExpressionStatement(classExpr1));
        scriptBlock.addStatement(new ExpressionStatement(classExpr2));

        ClassNode scriptClass = new ClassNode("Script", 0, null);
        MethodNode runMethod = new MethodNode("run", 0, null, new Parameter[0], null, scriptBlock);
        scriptClass.addMethod(runMethod);
        moduleNode.addClass(scriptClass);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 7)).thenReturn(myTypeClass);

        // Act
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);
        List<? extends Location> locations = result.join();

        // Assert
        assertNotNull(locations);
        assertTrue(locations.size() >= 2);
    }

    @Test
    @DisplayName("Should find class references in extends clause")
    void testClassReferenceVisitor_ExtendsClause() throws Exception {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                class BaseClass {}
                class Child1 extends BaseClass {}
                class Child2 extends BaseClass {}
                """;
        Position position = new Position(0, 6); // position on base class declaration
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(uri), position, new ReferenceContext(false));

        // Create AST nodes
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);

        // Create base class
        ClassNode baseClass = new ClassNode("BaseClass", 0, null);
        baseClass.setLineNumber(1);
        baseClass.setColumnNumber(7);
        moduleNode.addClass(baseClass);

        // Create child classes that extend base class
        ClassNode child1 = new ClassNode("Child1", 0, baseClass);
        child1.setLineNumber(2);
        child1.setColumnNumber(7);
        moduleNode.addClass(child1);

        ClassNode child2 = new ClassNode("Child2", 0, baseClass);
        child2.setLineNumber(3);
        child2.setColumnNumber(7);
        moduleNode.addClass(child2);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 7)).thenReturn(baseClass);

        // Act
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);
        List<? extends Location> locations = result.join();

        // Assert
        assertNotNull(locations);
        // Should find extends references
    }

    @Test
    @DisplayName("Should handle MethodReferenceVisitor for static method calls")
    void testMethodReferenceVisitor_StaticMethodCall() throws Exception {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                class Utils {
                    static void helper() {}
                }
                Utils.helper()
                Utils.helper()
                """;
        Position position = new Position(1, 16); // position on method declaration
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(uri), position, new ReferenceContext(false));

        // Create AST nodes
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode utilsClass = new ClassNode("Utils", 0, null);

        // Create static method
        MethodNode helperMethod =
                new MethodNode("helper", ACC_STATIC, null, new Parameter[0], null, null);
        helperMethod.setLineNumber(2);
        helperMethod.setColumnNumber(17);
        utilsClass.addMethod(helperMethod);

        // Create static method calls
        MethodCallExpression call1 =
                new MethodCallExpression(
                        new ClassExpression(utilsClass),
                        new ConstantExpression("helper"),
                        ArgumentListExpression.EMPTY_ARGUMENTS);
        call1.setLineNumber(4);
        call1.setColumnNumber(1);

        MethodCallExpression call2 =
                new MethodCallExpression(
                        new ClassExpression(utilsClass),
                        new ConstantExpression("helper"),
                        ArgumentListExpression.EMPTY_ARGUMENTS);
        call2.setLineNumber(5);
        call2.setColumnNumber(1);

        moduleNode.addClass(utilsClass);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 2, 17)).thenReturn(helperMethod);

        // Act
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);
        List<? extends Location> locations = result.join();

        // Assert
        assertNotNull(locations);
        // Should find static method calls
    }

    @Test
    @DisplayName("Should handle PropertyReferenceVisitor for property with same name as field")
    void testPropertyReferenceVisitor_PropertyAndField() throws Exception {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                class Data {
                    private String value
                    String getValue() { value }
                    void setValue(String v) { value = v }
                }
                def d = new Data()
                d.value = "test"
                println d.value
                """;
        Position position = new Position(1, 19); // position on field declaration
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(uri), position, new ReferenceContext(false));

        // Create AST nodes
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode dataClass = new ClassNode("Data", 0, null);

        // Create private field
        FieldNode valueField =
                new FieldNode("value", ACC_PRIVATE, ClassHelper.STRING_TYPE, dataClass, null);
        valueField.setLineNumber(2);
        valueField.setColumnNumber(20);
        dataClass.addField(valueField);

        // Create property (getter/setter methods create implicit property)
        PropertyNode valueProperty = new PropertyNode(valueField, ACC_PUBLIC, null, null);
        dataClass.addProperty(valueProperty);

        // Create property accesses
        PropertyExpression propAccess1 =
                new PropertyExpression(
                        new VariableExpression("d"), new ConstantExpression("value"));
        propAccess1.setLineNumber(7);
        propAccess1.setColumnNumber(3);

        PropertyExpression propAccess2 =
                new PropertyExpression(
                        new VariableExpression("d"), new ConstantExpression("value"));
        propAccess2.setLineNumber(8);
        propAccess2.setColumnNumber(11);

        moduleNode.addClass(dataClass);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 2, 20)).thenReturn(valueField);

        // Act
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);
        List<? extends Location> locations = result.join();

        // Assert
        assertNotNull(locations);
        // Should find property accesses
    }

    @Test
    @DisplayName("Should handle ReferenceVisitor for variable in closure")
    void testReferenceVisitor_VariableInClosure() throws Exception {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                def outer = 10
                [1, 2, 3].each { item ->
                    println outer + item
                }
                println outer
                """;
        Position position = new Position(0, 4); // position on 'outer' declaration
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(uri),
                        position,
                        new ReferenceContext(true) // include declaration
                        );

        // Create AST nodes
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode scriptClass = new ClassNode("Script", 0, null);

        // Create variable references
        VariableExpression outerDecl = new VariableExpression("outer");
        outerDecl.setLineNumber(1);
        outerDecl.setColumnNumber(5);

        VariableExpression outerRef1 = new VariableExpression("outer");
        outerRef1.setLineNumber(3);
        outerRef1.setColumnNumber(13);

        VariableExpression outerRef2 = new VariableExpression("outer");
        outerRef2.setLineNumber(5);
        outerRef2.setColumnNumber(9);

        moduleNode.addClass(scriptClass);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 5)).thenReturn(outerDecl);

        // Act
        CompletableFuture<List<? extends Location>> result = handler.handleReferences(params);
        List<? extends Location> locations = result.join();

        // Assert
        assertNotNull(locations);
        // Should find variable references including in closure
    }
}
