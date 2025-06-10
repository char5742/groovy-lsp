package com.groovy.lsp.protocol.internal.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.groovy.lsp.test.annotations.UnitTest;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.DynamicVariable;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.Variable;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test for DefinitionHandler internal methods and edge cases.
 */
@ExtendWith(MockitoExtension.class)
class DefinitionHandlerSourceUnitTest {

    @UnitTest
    void testVariableDeclarationVisitor_getSourceUnit() throws Exception {
        // リフレクションを使用して内部クラスのインスタンスを作成
        Class<?> visitorClass = null;
        for (Class<?> innerClass : DefinitionHandler.class.getDeclaredClasses()) {
            if (innerClass.getSimpleName().equals("VariableDeclarationVisitor")) {
                visitorClass = innerClass;
                break;
            }
        }

        assertThat(visitorClass).isNotNull();
        if (visitorClass == null) {
            throw new IllegalStateException("VariableDeclarationVisitor not found");
        }

        // コンストラクタを取得
        Constructor<?> constructor = visitorClass.getDeclaredConstructor(Variable.class);
        constructor.setAccessible(true);

        // インスタンスを作成
        Variable targetVar = new DynamicVariable("test", false);
        Object visitor = constructor.newInstance(targetVar);

        // getSourceUnitメソッドを呼び出し
        Method getSourceUnitMethod = visitorClass.getDeclaredMethod("getSourceUnit");
        getSourceUnitMethod.setAccessible(true);

        SourceUnit result = (SourceUnit) getSourceUnitMethod.invoke(visitor);
        assertNull(result);
    }

    @UnitTest
    void testVariableDeclarationVisitor_visitField() throws Exception {
        // リフレクションを使用して内部クラスのインスタンスを作成
        Class<?> visitorClass = null;
        for (Class<?> innerClass : DefinitionHandler.class.getDeclaredClasses()) {
            if (innerClass.getSimpleName().equals("VariableDeclarationVisitor")) {
                visitorClass = innerClass;
                break;
            }
        }

        assertThat(visitorClass).isNotNull();
        if (visitorClass == null) {
            throw new IllegalStateException("VariableDeclarationVisitor not found");
        }

        // コンストラクタを取得
        Constructor<?> constructor = visitorClass.getDeclaredConstructor(Variable.class);
        constructor.setAccessible(true);

        // インスタンスを作成
        Variable targetVar = new DynamicVariable("myField", false);
        Object visitor = constructor.newInstance(targetVar);

        // visitFieldメソッドを呼び出し
        Method visitFieldMethod = visitorClass.getDeclaredMethod("visitField", FieldNode.class);
        visitFieldMethod.setAccessible(true);

        FieldNode fieldNode = new FieldNode("myField", 1, ClassHelper.STRING_TYPE, null, null);
        visitFieldMethod.invoke(visitor, fieldNode);

        // getDeclarationNodeメソッドで結果を確認
        Method getDeclarationNodeMethod = visitorClass.getDeclaredMethod("getDeclarationNode");
        getDeclarationNodeMethod.setAccessible(true);

        Object declarationNode = getDeclarationNodeMethod.invoke(visitor);
        assertThat(declarationNode).isEqualTo(fieldNode);
    }

    @UnitTest
    void testVariableDeclarationVisitor_visitProperty() throws Exception {
        // リフレクションを使用して内部クラスのインスタンスを作成
        Class<?> visitorClass = null;
        for (Class<?> innerClass : DefinitionHandler.class.getDeclaredClasses()) {
            if (innerClass.getSimpleName().equals("VariableDeclarationVisitor")) {
                visitorClass = innerClass;
                break;
            }
        }

        assertThat(visitorClass).isNotNull();
        if (visitorClass == null) {
            throw new IllegalStateException("VariableDeclarationVisitor not found");
        }

        // コンストラクタを取得
        Constructor<?> constructor = visitorClass.getDeclaredConstructor(Variable.class);
        constructor.setAccessible(true);

        // インスタンスを作成
        Variable targetVar = new DynamicVariable("myProperty", false);
        Object visitor = constructor.newInstance(targetVar);

        // visitPropertyメソッドを呼び出し
        Method visitPropertyMethod =
                visitorClass.getDeclaredMethod("visitProperty", PropertyNode.class);
        visitPropertyMethod.setAccessible(true);

        FieldNode fieldNode = new FieldNode("myProperty", 1, ClassHelper.STRING_TYPE, null, null);
        PropertyNode propertyNode = new PropertyNode(fieldNode, 1, null, null);
        visitPropertyMethod.invoke(visitor, propertyNode);

        // getDeclarationNodeメソッドで結果を確認
        Method getDeclarationNodeMethod = visitorClass.getDeclaredMethod("getDeclarationNode");
        getDeclarationNodeMethod.setAccessible(true);

        Object declarationNode = getDeclarationNodeMethod.invoke(visitor);
        assertThat(declarationNode).isEqualTo(propertyNode);
    }

    @UnitTest
    void testVariableDeclarationVisitor_visitForLoop() throws Exception {
        // リフレクションを使用して内部クラスのインスタンスを作成
        Class<?> visitorClass = null;
        for (Class<?> innerClass : DefinitionHandler.class.getDeclaredClasses()) {
            if (innerClass.getSimpleName().equals("VariableDeclarationVisitor")) {
                visitorClass = innerClass;
                break;
            }
        }

        assertThat(visitorClass).isNotNull();
        if (visitorClass == null) {
            throw new IllegalStateException("VariableDeclarationVisitor not found");
        }

        // コンストラクタを取得
        Constructor<?> constructor = visitorClass.getDeclaredConstructor(Variable.class);
        constructor.setAccessible(true);

        // インスタンスを作成
        Variable targetVar = new DynamicVariable("item", false);
        Object visitor = constructor.newInstance(targetVar);

        // visitForLoopメソッドを呼び出し
        Method visitForLoopMethod =
                visitorClass.getDeclaredMethod("visitForLoop", ForStatement.class);
        visitForLoopMethod.setAccessible(true);

        Parameter forParam = new Parameter(ClassHelper.STRING_TYPE, "item");
        ForStatement forStmt =
                new ForStatement(forParam, new ListExpression(), new BlockStatement());
        visitForLoopMethod.invoke(visitor, forStmt);

        // getDeclarationNodeメソッドで結果を確認
        Method getDeclarationNodeMethod = visitorClass.getDeclaredMethod("getDeclarationNode");
        getDeclarationNodeMethod.setAccessible(true);

        Object declarationNode = getDeclarationNodeMethod.invoke(visitor);
        assertThat(declarationNode).isEqualTo(forParam);
    }

    @UnitTest
    void testVariableDeclarationVisitor_visitForLoop_nullParameter() throws Exception {
        // リフレクションを使用して内部クラスのインスタンスを作成
        Class<?> visitorClass = null;
        for (Class<?> innerClass : DefinitionHandler.class.getDeclaredClasses()) {
            if (innerClass.getSimpleName().equals("VariableDeclarationVisitor")) {
                visitorClass = innerClass;
                break;
            }
        }

        assertThat(visitorClass).isNotNull();
        if (visitorClass == null) {
            throw new IllegalStateException("VariableDeclarationVisitor not found");
        }

        // コンストラクタを取得
        Constructor<?> constructor = visitorClass.getDeclaredConstructor(Variable.class);
        constructor.setAccessible(true);

        // インスタンスを作成
        Variable targetVar = new DynamicVariable("item", false);
        Object visitor = constructor.newInstance(targetVar);

        // visitForLoopメソッドを呼び出し（nullパラメータ）
        Method visitForLoopMethod =
                visitorClass.getDeclaredMethod("visitForLoop", ForStatement.class);
        visitForLoopMethod.setAccessible(true);

        ForStatement forStmt = new ForStatement(null, new ListExpression(), new BlockStatement());
        visitForLoopMethod.invoke(visitor, forStmt);

        // getDeclarationNodeメソッドで結果を確認
        Method getDeclarationNodeMethod = visitorClass.getDeclaredMethod("getDeclarationNode");
        getDeclarationNodeMethod.setAccessible(true);

        Object declarationNode = getDeclarationNodeMethod.invoke(visitor);
        assertNull(declarationNode);
    }

    @UnitTest
    void testVariableDeclarationVisitor_visitDeclarationExpression_notMatch() throws Exception {
        // リフレクションを使用して内部クラスのインスタンスを作成
        Class<?> visitorClass = null;
        for (Class<?> innerClass : DefinitionHandler.class.getDeclaredClasses()) {
            if (innerClass.getSimpleName().equals("VariableDeclarationVisitor")) {
                visitorClass = innerClass;
                break;
            }
        }

        assertThat(visitorClass).isNotNull();
        if (visitorClass == null) {
            throw new IllegalStateException("VariableDeclarationVisitor not found");
        }

        // コンストラクタを取得
        Constructor<?> constructor = visitorClass.getDeclaredConstructor(Variable.class);
        constructor.setAccessible(true);

        // インスタンスを作成
        Variable targetVar = new DynamicVariable("target", false);
        Object visitor = constructor.newInstance(targetVar);

        // visitDeclarationExpressionメソッドを呼び出し（名前が一致しない）
        Method visitDeclMethod =
                visitorClass.getDeclaredMethod(
                        "visitDeclarationExpression", DeclarationExpression.class);
        visitDeclMethod.setAccessible(true);

        VariableExpression varExpr = new VariableExpression("different");
        DeclarationExpression declExpr =
                new DeclarationExpression(
                        varExpr,
                        Token.newSymbol(Types.EQUAL, 0, 0),
                        new VariableExpression("value"));
        visitDeclMethod.invoke(visitor, declExpr);

        // getDeclarationNodeメソッドで結果を確認
        Method getDeclarationNodeMethod = visitorClass.getDeclaredMethod("getDeclarationNode");
        getDeclarationNodeMethod.setAccessible(true);

        Object declarationNode = getDeclarationNodeMethod.invoke(visitor);
        assertNull(declarationNode);
    }
}
