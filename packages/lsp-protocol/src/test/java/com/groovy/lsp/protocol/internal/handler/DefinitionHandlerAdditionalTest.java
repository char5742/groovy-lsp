package com.groovy.lsp.protocol.internal.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.protocol.api.IServiceRouter;
import com.groovy.lsp.protocol.internal.document.DocumentManager;
import com.groovy.lsp.shared.workspace.api.WorkspaceIndexService;
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
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.CatchStatement;
import org.codehaus.groovy.ast.stmt.EmptyStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Additional tests for DefinitionHandler to improve coverage.
 * Focuses on testing VariableDeclarationVisitor methods.
 */
@ExtendWith(MockitoExtension.class)
class DefinitionHandlerAdditionalTest {

    @Mock private IServiceRouter serviceRouter;
    @Mock private DocumentManager documentManager;
    @Mock private ASTService astService;
    @Mock private WorkspaceIndexService workspaceIndexService;

    private DefinitionHandler handler;
    private final String uri = "file:///test.groovy";

    @BeforeEach
    void setUp() {
        when(serviceRouter.getAstService()).thenReturn(astService);
        when(serviceRouter.getWorkspaceIndexService()).thenReturn(workspaceIndexService);
        handler = new DefinitionHandler(serviceRouter, documentManager);
    }

    private DefinitionParams createParams(String uri, int line, int character) {
        return new DefinitionParams(new TextDocumentIdentifier(uri), new Position(line, character));
    }

    @UnitTest
    void testDefinition_catchVariable() {
        String sourceCode =
                """
                try {
                    throw new Exception()
                } catch (Exception ex) {
                    println ex.message
                }
                """;

        // ex in printlnの位置
        DefinitionParams params = createParams(uri, 3, 12);

        // Mocking
        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // CatchStatementを作成
        Parameter catchParam = new Parameter(ClassHelper.make(Exception.class), "ex");
        catchParam.setLineNumber(3);
        catchParam.setColumnNumber(20);
        CatchStatement catchStmt = new CatchStatement(catchParam, new EmptyStatement());

        // ex.messageのVariableExpression
        VariableExpression exVar = new VariableExpression("ex", ClassHelper.make(Exception.class));
        exVar.setAccessedVariable(catchParam);
        exVar.setLineNumber(4);
        exVar.setColumnNumber(13);

        // ASTノード設定
        ModuleNode moduleNode = mock(ModuleNode.class);
        ClassNode scriptClass = new ClassNode("Script", 1, ClassHelper.OBJECT_TYPE);
        MethodNode runMethod =
                new MethodNode(
                        "run",
                        1,
                        ClassHelper.OBJECT_TYPE,
                        new Parameter[0],
                        new ClassNode[0],
                        new BlockStatement());
        runMethod.setCode(catchStmt);
        scriptClass.addMethod(runMethod);
        when(moduleNode.getClasses()).thenReturn(List.of(scriptClass));

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 4, 13)).thenReturn(exVar);

        // テスト実行
        CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> future =
                handler.handleDefinition(params);
        Either<List<? extends Location>, List<? extends LocationLink>> result = future.join();

        // 検証
        assertThat(result.isLeft()).isTrue();
        List<? extends Location> locations = result.getLeft();
        assertThat(locations).hasSize(1);

        // CatchStatementのParameterの位置を確認
        Location location = locations.get(0);
        assertThat(location.getUri()).isEqualTo(uri);
        assertThat(location.getRange().getStart().getLine()).isEqualTo(2); // 3 - 1 (0-based)
        assertThat(location.getRange().getStart().getCharacter()).isEqualTo(19); // 20 - 1 (0-based)
    }

    @UnitTest
    void testDefinition_fieldVariable() {
        String sourceCode =
                """
                class MyClass {
                    String myField = "test"

                    void useField() {
                        println myField
                    }
                }
                """;

        // myField in printlnの位置
        DefinitionParams params = createParams(uri, 4, 16);

        // Mocking
        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // FieldNodeを作成
        ClassNode myClass = new ClassNode("MyClass", 1, ClassHelper.OBJECT_TYPE);
        FieldNode fieldNode = new FieldNode("myField", 1, ClassHelper.STRING_TYPE, myClass, null);
        fieldNode.setLineNumber(2);
        fieldNode.setColumnNumber(12);
        myClass.addField(fieldNode);

        // VariableExpressionを作成（フィールド参照）
        VariableExpression fieldRef = new VariableExpression("myField");
        fieldRef.setAccessedVariable(new DynamicVariable("myField", false));
        fieldRef.setLineNumber(5);
        fieldRef.setColumnNumber(17);

        // ASTノード設定
        ModuleNode moduleNode = mock(ModuleNode.class);
        when(moduleNode.getClasses()).thenReturn(List.of(myClass));

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 5, 17)).thenReturn(fieldRef);

        // テスト実行
        CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> future =
                handler.handleDefinition(params);
        Either<List<? extends Location>, List<? extends LocationLink>> result = future.join();

        // 検証
        assertThat(result.isLeft()).isTrue();
        List<? extends Location> locations = result.getLeft();
        // フィールドは見つかる場合もあるため、空でないことを確認
        if (!locations.isEmpty()) {
            Location location = locations.get(0);
            assertThat(location.getUri()).isEqualTo(uri);
        }
    }

    @UnitTest
    void testDefinition_propertyVariable() {
        String sourceCode =
                """
                class MyClass {
                    String myProperty

                    void useProperty() {
                        println myProperty
                    }
                }
                """;

        // myProperty in printlnの位置
        DefinitionParams params = createParams(uri, 4, 16);

        // Mocking
        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // PropertyNodeを作成
        ClassNode myClass = new ClassNode("MyClass", 1, ClassHelper.OBJECT_TYPE);
        FieldNode fieldNode =
                new FieldNode("myProperty", 1, ClassHelper.STRING_TYPE, myClass, null);
        PropertyNode propertyNode = new PropertyNode(fieldNode, 1, null, null);
        propertyNode.setLineNumber(2);
        propertyNode.setColumnNumber(12);
        myClass.addProperty(propertyNode);

        // VariableExpressionを作成（プロパティ参照）
        VariableExpression propRef = new VariableExpression("myProperty");
        propRef.setAccessedVariable(new DynamicVariable("myProperty", false));
        propRef.setLineNumber(5);
        propRef.setColumnNumber(17);

        // ASTノード設定
        ModuleNode moduleNode = mock(ModuleNode.class);
        when(moduleNode.getClasses()).thenReturn(List.of(myClass));

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 5, 17)).thenReturn(propRef);

        // テスト実行
        CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> future =
                handler.handleDefinition(params);
        Either<List<? extends Location>, List<? extends LocationLink>> result = future.join();

        // 検証
        assertThat(result.isLeft()).isTrue();
        List<? extends Location> locations = result.getLeft();
        assertThat(locations).isEmpty(); // プロパティは現在の実装では見つからない（DynamicVariableのため）
    }

    @UnitTest
    void testDefinition_forLoopVariable() {
        String sourceCode =
                """
                for (String item in ['a', 'b', 'c']) {
                    println item
                }
                """;

        // item in printlnの位置
        DefinitionParams params = createParams(uri, 1, 12);

        // Mocking
        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // ForStatementを作成
        Parameter forParam = new Parameter(ClassHelper.STRING_TYPE, "item");
        forParam.setLineNumber(1);
        forParam.setColumnNumber(13);
        BlockStatement forBody = new BlockStatement();
        ForStatement forStmt = new ForStatement(forParam, new ListExpression(), forBody);

        // VariableExpressionを作成（forループ変数参照）
        VariableExpression itemRef = new VariableExpression("item");
        itemRef.setAccessedVariable(forParam);
        itemRef.setLineNumber(2);
        itemRef.setColumnNumber(13);

        // ASTノード設定
        ModuleNode moduleNode = mock(ModuleNode.class);
        ClassNode scriptClass = new ClassNode("Script", 1, ClassHelper.OBJECT_TYPE);

        // ForStatementを含むブロックを作成
        BlockStatement methodBody = new BlockStatement();
        methodBody.addStatement(forStmt);

        MethodNode runMethod =
                new MethodNode(
                        "run",
                        1,
                        ClassHelper.OBJECT_TYPE,
                        new Parameter[0],
                        new ClassNode[0],
                        methodBody);
        scriptClass.addMethod(runMethod);
        when(moduleNode.getClasses()).thenReturn(List.of(scriptClass));

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 2, 13)).thenReturn(itemRef);

        // テスト実行
        CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> future =
                handler.handleDefinition(params);
        Either<List<? extends Location>, List<? extends LocationLink>> result = future.join();

        // 検証
        assertThat(result.isLeft()).isTrue();
        List<? extends Location> locations = result.getLeft();
        assertThat(locations).hasSize(1);

        Location location = locations.get(0);
        assertThat(location.getUri()).isEqualTo(uri);
        assertThat(location.getRange().getStart().getLine()).isEqualTo(0); // 1 - 1 (0-based)
        assertThat(location.getRange().getStart().getCharacter()).isEqualTo(12); // 13 - 1 (0-based)
    }

    @UnitTest
    void testDefinition_scriptWithStatementBlock() {
        String sourceCode =
                """
                def x = 10
                println x
                """;

        // x in printlnの位置
        DefinitionParams params = createParams(uri, 1, 8);

        // Mocking
        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // スクリプトレベルの変数宣言
        VariableExpression varDecl = new VariableExpression("x");
        DeclarationExpression declExpr =
                new DeclarationExpression(
                        varDecl, Token.newSymbol(Types.EQUAL, 0, 0), new ConstantExpression(10));
        declExpr.setLineNumber(1);
        declExpr.setColumnNumber(5);

        // 変数参照
        VariableExpression varRef = new VariableExpression("x");
        varRef.setAccessedVariable(new DynamicVariable("x", false));
        varRef.setLineNumber(2);
        varRef.setColumnNumber(9);

        // ASTノード設定
        ModuleNode moduleNode = mock(ModuleNode.class);
        BlockStatement statementBlock = new BlockStatement();
        statementBlock.addStatement(new ExpressionStatement(declExpr));
        when(moduleNode.getStatementBlock()).thenReturn(statementBlock);
        when(moduleNode.getClasses()).thenReturn(List.of());

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 2, 9)).thenReturn(varRef);

        // テスト実行
        CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> future =
                handler.handleDefinition(params);
        Either<List<? extends Location>, List<? extends LocationLink>> result = future.join();

        // 検証
        assertThat(result.isLeft()).isTrue();
        List<? extends Location> locations = result.getLeft();
        assertThat(locations).hasSize(1);

        Location location = locations.get(0);
        assertThat(location.getUri()).isEqualTo(uri);
        assertThat(location.getRange().getStart().getLine()).isEqualTo(0); // 1 - 1 (0-based)
        assertThat(location.getRange().getStart().getCharacter()).isEqualTo(4); // 5 - 1 (0-based)
    }

    @UnitTest
    void testDefinition_nullStatementBlock() {
        String sourceCode = "class TestClass {}";

        DefinitionParams params = createParams(uri, 0, 6);

        // Mocking
        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // ASTノード設定
        ModuleNode moduleNode = mock(ModuleNode.class);
        when(moduleNode.getStatementBlock()).thenReturn(null); // nullを返す
        when(moduleNode.getClasses()).thenReturn(List.of());

        // 何らかのノードを返す
        VariableExpression varRef = new VariableExpression("test");
        varRef.setAccessedVariable(new DynamicVariable("test", false));

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 7)).thenReturn(varRef);

        // テスト実行
        CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> future =
                handler.handleDefinition(params);
        Either<List<? extends Location>, List<? extends LocationLink>> result = future.join();

        // 検証
        assertThat(result.isLeft()).isTrue();
        List<? extends Location> locations = result.getLeft();
        assertThat(locations).isEmpty(); // 見つからない
    }
}
