package com.groovy.lsp.protocol.internal.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.protocol.api.IServiceRouter;
import com.groovy.lsp.protocol.internal.document.DocumentManager;
import com.groovy.lsp.test.annotations.UnitTest;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.DynamicVariable;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.Variable;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.CatchStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.ast.stmt.TryCatchStatement;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.Token;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for DefinitionHandler's variable declaration finding functionality.
 * These tests specifically target the VariableDeclarationVisitor implementation.
 */
@ExtendWith(MockitoExtension.class)
class DefinitionHandlerVariableTest {

    @Mock private IServiceRouter serviceRouter;
    @Mock private DocumentManager documentManager;
    @Mock private ASTService astService;

    private DefinitionHandler handler;

    @BeforeEach
    void setUp() {
        when(serviceRouter.getAstService()).thenReturn(astService);
        handler = new DefinitionHandler(serviceRouter, documentManager);
    }

    @UnitTest
    @DisplayName("Should find variable declaration in method body")
    void testFindVariableDeclaration_LocalVariable() throws Exception {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                def method() {
                    def localVar = "value"
                    println localVar
                }
                """;
        Position position = new Position(2, 12); // position on 'localVar' usage
        DefinitionParams params = new DefinitionParams(new TextDocumentIdentifier(uri), position);

        // Create real AST nodes
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode classNode = new ClassNode("Script", 0, null);
        MethodNode methodNode = new MethodNode("method", 0, null, new Parameter[0], null, null);

        // Create block statement
        BlockStatement blockStatement = new BlockStatement();

        // Create variable declaration: def localVar = "value"
        VariableExpression varDeclExpr = new VariableExpression("localVar");
        varDeclExpr.setLineNumber(2);
        varDeclExpr.setColumnNumber(9);
        varDeclExpr.setLastLineNumber(2);
        varDeclExpr.setLastColumnNumber(16);

        DeclarationExpression declExpr =
                new DeclarationExpression(
                        varDeclExpr, Token.newSymbol("=", 2, 17), new ConstantExpression("value"));
        declExpr.setLineNumber(2);
        declExpr.setColumnNumber(5);
        declExpr.setLastLineNumber(2);
        declExpr.setLastColumnNumber(23);

        ExpressionStatement declStmt = new ExpressionStatement(declExpr);
        blockStatement.addStatement(declStmt);

        // Create variable usage: println localVar
        Variable variable = new DynamicVariable("localVar", false);
        VariableExpression varUsageExpr =
                new VariableExpression("localVar", ClassHelper.OBJECT_TYPE);
        varUsageExpr.setAccessedVariable(variable);
        varUsageExpr.setLineNumber(3);
        varUsageExpr.setColumnNumber(13);

        methodNode.setCode(blockStatement);
        classNode.addMethod(methodNode);
        moduleNode.addClass(classNode);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 3, 13)).thenReturn(varUsageExpr);

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
        assertEquals(1, locations.size());

        Location location = locations.get(0);
        assertEquals(uri, location.getUri());
        assertEquals(1, location.getRange().getStart().getLine());
        assertEquals(4, location.getRange().getStart().getCharacter());
    }

    @UnitTest
    @DisplayName("Should find method parameter declaration")
    void testFindVariableDeclaration_MethodParameter() throws Exception {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                def method(String param) {
                    println param
                }
                """;
        Position position = new Position(1, 12); // position on 'param' usage
        DefinitionParams params = new DefinitionParams(new TextDocumentIdentifier(uri), position);

        // Create real AST nodes
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode classNode = new ClassNode("Script", 0, null);

        // Create method with parameter
        Parameter parameter = new Parameter(ClassHelper.STRING_TYPE, "param");
        parameter.setLineNumber(1);
        parameter.setColumnNumber(19);
        parameter.setLastLineNumber(1);
        parameter.setLastColumnNumber(23);

        MethodNode methodNode =
                new MethodNode("method", 0, null, new Parameter[] {parameter}, null, null);

        // Create block statement
        BlockStatement blockStatement = new BlockStatement();

        // Create variable usage: println param
        Variable variable = parameter;
        VariableExpression varUsageExpr = new VariableExpression("param", ClassHelper.STRING_TYPE);
        varUsageExpr.setAccessedVariable(variable);
        varUsageExpr.setLineNumber(2);
        varUsageExpr.setColumnNumber(13);

        methodNode.setCode(blockStatement);
        classNode.addMethod(methodNode);
        moduleNode.addClass(classNode);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 2, 13)).thenReturn(varUsageExpr);

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
        assertEquals(1, locations.size());

        Location location = locations.get(0);
        assertEquals(uri, location.getUri());
        assertEquals(0, location.getRange().getStart().getLine());
        assertEquals(18, location.getRange().getStart().getCharacter());
    }

    @UnitTest
    @Disabled("TODO: Fix field variable resolution")
    @DisplayName("Should find field declaration")
    void testFindVariableDeclaration_Field() throws Exception {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                class MyClass {
                    String fieldName

                    def method() {
                        println fieldName
                    }
                }
                """;
        Position position = new Position(4, 16); // position on 'fieldName' usage
        DefinitionParams params = new DefinitionParams(new TextDocumentIdentifier(uri), position);

        // Create real AST nodes
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode classNode = new ClassNode("MyClass", 0, null);

        // Create field
        FieldNode fieldNode =
                new FieldNode("fieldName", 0, ClassHelper.STRING_TYPE, classNode, null);
        fieldNode.setLineNumber(2);
        fieldNode.setColumnNumber(12);
        fieldNode.setLastLineNumber(2);
        fieldNode.setLastColumnNumber(20);
        classNode.addField(fieldNode);

        // Create method
        MethodNode methodNode = new MethodNode("method", 0, null, new Parameter[0], null, null);

        // Create variable usage
        Variable variable = fieldNode;
        VariableExpression varUsageExpr =
                new VariableExpression("fieldName", ClassHelper.STRING_TYPE);
        varUsageExpr.setAccessedVariable(variable);
        varUsageExpr.setLineNumber(5);
        varUsageExpr.setColumnNumber(17);

        classNode.addMethod(methodNode);
        moduleNode.addClass(classNode);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 5, 17)).thenReturn(varUsageExpr);

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
        assertEquals(1, locations.size());

        Location location = locations.get(0);
        assertEquals(uri, location.getUri());
        assertEquals(1, location.getRange().getStart().getLine());
        assertEquals(11, location.getRange().getStart().getCharacter());
    }

    @UnitTest
    @Disabled("TODO: Fix property variable resolution")
    @DisplayName("Should find property declaration")
    void testFindVariableDeclaration_Property() throws Exception {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                class MyClass {
                    String propertyName

                    def method() {
                        println propertyName
                    }
                }
                """;
        Position position = new Position(4, 16); // position on 'propertyName' usage
        DefinitionParams params = new DefinitionParams(new TextDocumentIdentifier(uri), position);

        // Create real AST nodes
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode classNode = new ClassNode("MyClass", 0, null);

        // Create property
        PropertyNode propertyNode =
                new PropertyNode(
                        "propertyName", 0, ClassHelper.STRING_TYPE, classNode, null, null, null);
        propertyNode.setLineNumber(2);
        propertyNode.setColumnNumber(12);
        propertyNode.setLastLineNumber(2);
        propertyNode.setLastColumnNumber(23);
        classNode.addProperty(propertyNode);

        // Create method
        MethodNode methodNode = new MethodNode("method", 0, null, new Parameter[0], null, null);

        // Create variable usage
        Variable variable = propertyNode.getField();
        VariableExpression varUsageExpr =
                new VariableExpression("propertyName", ClassHelper.STRING_TYPE);
        varUsageExpr.setAccessedVariable(variable);
        varUsageExpr.setLineNumber(5);
        varUsageExpr.setColumnNumber(17);

        classNode.addMethod(methodNode);
        moduleNode.addClass(classNode);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 5, 17)).thenReturn(varUsageExpr);

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
        assertEquals(1, locations.size());

        Location location = locations.get(0);
        assertEquals(uri, location.getUri());
        assertEquals(1, location.getRange().getStart().getLine());
        assertEquals(11, location.getRange().getStart().getCharacter());
    }

    @UnitTest
    @Disabled("TODO: Fix for loop variable resolution")
    @DisplayName("Should find for loop variable declaration")
    void testFindVariableDeclaration_ForLoopVariable() throws Exception {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                def method() {
                    for (int i = 0; i < 10; i++) {
                        println i
                    }
                }
                """;
        Position position = new Position(2, 16); // position on 'i' usage in println
        DefinitionParams params = new DefinitionParams(new TextDocumentIdentifier(uri), position);

        // Create real AST nodes
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode classNode = new ClassNode("Script", 0, null);
        MethodNode methodNode = new MethodNode("method", 0, null, new Parameter[0], null, null);

        // Create for loop
        Parameter loopVariable = new Parameter(ClassHelper.int_TYPE, "i");
        loopVariable.setLineNumber(2);
        loopVariable.setColumnNumber(14);
        loopVariable.setLastLineNumber(2);
        loopVariable.setLastColumnNumber(14);

        ForStatement forStatement = new ForStatement(loopVariable, null, null);

        // Create variable usage inside loop
        Variable variable = loopVariable;
        VariableExpression varUsageExpr = new VariableExpression("i", ClassHelper.int_TYPE);
        varUsageExpr.setAccessedVariable(variable);
        varUsageExpr.setLineNumber(3);
        varUsageExpr.setColumnNumber(17);

        BlockStatement blockStatement = new BlockStatement();
        blockStatement.addStatement(forStatement);
        methodNode.setCode(blockStatement);
        classNode.addMethod(methodNode);
        moduleNode.addClass(classNode);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 3, 17)).thenReturn(varUsageExpr);

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
        assertEquals(1, locations.size());

        Location location = locations.get(0);
        assertEquals(uri, location.getUri());
        assertEquals(1, location.getRange().getStart().getLine());
        assertEquals(13, location.getRange().getStart().getCharacter());
    }

    @UnitTest
    @DisplayName("Should find catch block parameter declaration")
    void testFindVariableDeclaration_CatchParameter() throws Exception {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                def method() {
                    try {
                        // something
                    } catch (Exception ex) {
                        println ex.message
                    }
                }
                """;
        Position position = new Position(4, 16); // position on 'ex' usage
        DefinitionParams params = new DefinitionParams(new TextDocumentIdentifier(uri), position);

        // Create real AST nodes
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode classNode = new ClassNode("Script", 0, null);
        MethodNode methodNode = new MethodNode("method", 0, null, new Parameter[0], null, null);

        // Create catch statement
        Parameter catchParameter = new Parameter(ClassHelper.make(Exception.class), "ex");
        catchParameter.setLineNumber(4);
        catchParameter.setColumnNumber(24);
        catchParameter.setLastLineNumber(4);
        catchParameter.setLastColumnNumber(25);

        CatchStatement catchStatement = new CatchStatement(catchParameter, new BlockStatement());

        // Create variable usage
        Variable variable = catchParameter;
        VariableExpression varUsageExpr =
                new VariableExpression("ex", ClassHelper.make(Exception.class));
        varUsageExpr.setAccessedVariable(variable);
        varUsageExpr.setLineNumber(5);
        varUsageExpr.setColumnNumber(17);

        TryCatchStatement tryCatchStatement =
                new TryCatchStatement(new BlockStatement(), new BlockStatement());
        tryCatchStatement.addCatch(catchStatement);

        BlockStatement blockStatement = new BlockStatement();
        blockStatement.addStatement(tryCatchStatement);
        methodNode.setCode(blockStatement);
        classNode.addMethod(methodNode);
        moduleNode.addClass(classNode);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 5, 17)).thenReturn(varUsageExpr);

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
        assertEquals(1, locations.size());

        Location location = locations.get(0);
        assertEquals(uri, location.getUri());
        assertEquals(3, location.getRange().getStart().getLine());
        assertEquals(23, location.getRange().getStart().getCharacter());
    }

    @UnitTest
    @DisplayName("Should handle script-level variable declaration")
    void testFindVariableDeclaration_ScriptLevel() throws Exception {
        // Arrange
        String uri = "file:///test.groovy";
        String content =
                """
                def scriptVar = "value"
                println scriptVar
                """;
        Position position = new Position(1, 8); // position on 'scriptVar' usage
        DefinitionParams params = new DefinitionParams(new TextDocumentIdentifier(uri), position);

        // Create real AST nodes
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);

        // Create block statement for script
        BlockStatement scriptBlock = new BlockStatement();

        // Create variable declaration
        VariableExpression varDeclExpr = new VariableExpression("scriptVar");
        varDeclExpr.setLineNumber(1);
        varDeclExpr.setColumnNumber(5);
        varDeclExpr.setLastLineNumber(1);
        varDeclExpr.setLastColumnNumber(13);

        DeclarationExpression declExpr =
                new DeclarationExpression(
                        varDeclExpr, Token.newSymbol("=", 1, 14), new ConstantExpression("value"));
        declExpr.setLineNumber(1);
        declExpr.setColumnNumber(1);
        declExpr.setLastLineNumber(1);
        declExpr.setLastColumnNumber(23);

        ExpressionStatement declStmt = new ExpressionStatement(declExpr);
        scriptBlock.addStatement(declStmt);

        // Create variable usage
        Variable variable = new DynamicVariable("scriptVar", false);
        VariableExpression varUsageExpr =
                new VariableExpression("scriptVar", ClassHelper.OBJECT_TYPE);
        varUsageExpr.setAccessedVariable(variable);
        varUsageExpr.setLineNumber(2);
        varUsageExpr.setColumnNumber(9);

        // Add a script class to hold the statements
        ClassNode scriptClass = new ClassNode("Script", 0, null);
        MethodNode runMethod = new MethodNode("run", 0, null, new Parameter[0], null, scriptBlock);
        scriptClass.addMethod(runMethod);
        moduleNode.addClass(scriptClass);

        when(documentManager.getDocumentContent(uri)).thenReturn(content);
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 2, 9)).thenReturn(varUsageExpr);

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
        assertEquals(1, locations.size());

        Location location = locations.get(0);
        assertEquals(uri, location.getUri());
        assertEquals(0, location.getRange().getStart().getLine());
        assertEquals(0, location.getRange().getStart().getCharacter());
    }
}
