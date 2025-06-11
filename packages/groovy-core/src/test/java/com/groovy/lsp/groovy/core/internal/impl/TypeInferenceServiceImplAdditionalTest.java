package com.groovy.lsp.groovy.core.internal.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.test.annotations.UnitTest;
import java.util.Objects;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ArrayExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BitwiseNegationExpression;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.ElvisOperatorExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.FieldExpression;
import org.codehaus.groovy.ast.expr.GStringExpression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.NotExpression;
import org.codehaus.groovy.ast.expr.PostfixExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.RangeExpression;
import org.codehaus.groovy.ast.expr.SpreadExpression;
import org.codehaus.groovy.ast.expr.TernaryExpression;
import org.codehaus.groovy.ast.expr.UnaryMinusExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.SourceUnit;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;

/**
 * Additional tests for TypeInferenceServiceImpl to improve branch coverage.
 */
class TypeInferenceServiceImplAdditionalTest {

    private ASTService astService;
    private TypeInferenceServiceImpl typeInferenceService;

    @BeforeEach
    void setUp() {
        astService = new ASTServiceImpl();
        typeInferenceService = new TypeInferenceServiceImpl(astService);
    }

    @UnitTest
    void inferExpressionType_shouldHandleConstructorCall() {
        // given
        String sourceCode =
                """
                def list = new ArrayList()
                def map = new HashMap<String, Integer>()
                """;
        ModuleNode moduleNode = parseSourceNotNull(sourceCode, "test.groovy");

        // Find the constructor call expressions
        ConstructorCallExpression constructorCall =
                findFirstNodeNotNull(moduleNode, ConstructorCallExpression.class);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(constructorCall, moduleNode);

        // then
        assertThat(type.getName()).contains("ArrayList");
    }

    @UnitTest
    void inferExpressionType_shouldHandleCastExpression() {
        // given
        String sourceCode =
                """
                def obj = "test"
                String str = (String) obj
                """;
        ModuleNode moduleNode = parseSourceNotNull(sourceCode, "test.groovy");

        CastExpression castExpr = findFirstNodeNotNull(moduleNode, CastExpression.class);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(castExpr, moduleNode);

        // then
        assertThat(type).isEqualTo(ClassHelper.STRING_TYPE);
    }

    @UnitTest
    void inferExpressionType_shouldHandleClosureExpression() {
        // given
        String sourceCode =
                """
                def closure = { String name ->
                    println name
                }
                """;
        ModuleNode moduleNode = parseSourceNotNull(sourceCode, "test.groovy");

        ClosureExpression closureExpr = findFirstNodeNotNull(moduleNode, ClosureExpression.class);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(closureExpr, moduleNode);

        // then
        assertThat(type.getName()).contains("Closure");
    }

    @UnitTest
    void inferExpressionType_shouldHandleArrayExpression() {
        // given
        // Create an ArrayExpression programmatically since they're not created from source code
        ArrayExpression arrayExpr =
                new ArrayExpression(
                        ClassHelper.STRING_TYPE,
                        java.util.Arrays.asList(
                                new ConstantExpression("a"),
                                new ConstantExpression("b"),
                                new ConstantExpression("c")));

        // when
        ClassNode type = typeInferenceService.inferExpressionType(arrayExpr, null);

        // then
        // ArrayExpression returns its existing type (String[])
        assertThat(type).isEqualTo(ClassHelper.STRING_TYPE.makeArray());
    }

    @UnitTest
    void inferExpressionType_shouldHandleRangeExpression() {
        // given
        String sourceCode =
                """
                def range = 1..10
                """;
        ModuleNode moduleNode = parseSourceNotNull(sourceCode, "test.groovy");

        RangeExpression rangeExpr = findFirstNodeNotNull(moduleNode, RangeExpression.class);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(rangeExpr, moduleNode);

        // then
        assertThat(type.getName()).contains("Range");
    }

    @UnitTest
    void inferExpressionType_shouldHandleGStringExpression() {
        // given
        String sourceCode =
                """
                def name = "World"
                def greeting = "Hello, ${name}!"
                """;
        ModuleNode moduleNode = parseSourceNotNull(sourceCode, "test.groovy");

        GStringExpression gstringExpr = findFirstNodeNotNull(moduleNode, GStringExpression.class);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(gstringExpr, moduleNode);

        // then
        assertThat(type.getName()).contains("GString");
    }

    @UnitTest
    void inferExpressionType_shouldHandleTernaryExpression() {
        // given
        String sourceCode =
                """
                def result = true ? "yes" : "no"
                """;
        ModuleNode moduleNode = parseSourceNotNull(sourceCode, "test.groovy");

        TernaryExpression ternaryExpr = findFirstNodeNotNull(moduleNode, TernaryExpression.class);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(ternaryExpr, moduleNode);

        // then
        // TernaryExpression is not handled, so it returns OBJECT_TYPE
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @UnitTest
    void inferExpressionType_shouldHandleElvisExpression() {
        // given
        String sourceCode =
                """
                def value = null
                def result = value ?: "default"
                """;
        ModuleNode moduleNode = parseSourceNotNull(sourceCode, "test.groovy");

        ElvisOperatorExpression elvisExpr =
                findFirstNodeNotNull(moduleNode, ElvisOperatorExpression.class);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(elvisExpr, moduleNode);

        // then
        // ElvisOperatorExpression is not handled, so it returns OBJECT_TYPE
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @UnitTest
    void inferExpressionType_shouldHandleSpreadExpression() {
        // given
        // Create a SpreadExpression programmatically
        SpreadExpression spreadExpr = new SpreadExpression(new VariableExpression("list"));

        // when
        ClassNode type = typeInferenceService.inferExpressionType(spreadExpr, null);

        // then
        // SpreadExpression is not handled, so it returns OBJECT_TYPE
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @UnitTest
    void inferExpressionType_shouldHandleUnaryExpression() {
        // given
        String sourceCode =
                """
                def num = 5
                def negated = -num
                def notBool = !true
                """;
        ModuleNode moduleNode = parseSourceNotNull(sourceCode, "test.groovy");

        UnaryMinusExpression unaryExpr =
                findFirstNodeNotNull(moduleNode, UnaryMinusExpression.class);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(unaryExpr, moduleNode);

        // then
        // UnaryMinusExpression is not handled, so it returns OBJECT_TYPE
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @UnitTest
    void inferExpressionType_shouldHandlePostfixExpression() {
        // given
        String sourceCode =
                """
                def i = 0
                def result = i++
                """;
        ModuleNode moduleNode = parseSourceNotNull(sourceCode, "test.groovy");

        PostfixExpression postfixExpr = findFirstNodeNotNull(moduleNode, PostfixExpression.class);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(postfixExpr, moduleNode);

        // then
        // PostfixExpression is not handled, so it returns OBJECT_TYPE
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @UnitTest
    void inferExpressionType_shouldHandleFieldExpression() {
        // given
        // Create a FieldExpression programmatically since they're not created from source code
        ClassNode testClass = new ClassNode("TestClass", 1, ClassHelper.OBJECT_TYPE);
        FieldNode fieldNode =
                new FieldNode(
                        "myField",
                        1,
                        ClassHelper.STRING_TYPE,
                        testClass,
                        new ConstantExpression("test"));
        testClass.addField(fieldNode);

        FieldExpression fieldExpr = new FieldExpression(fieldNode);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(fieldExpr, null);

        // then
        // FieldExpression returns its field's type
        assertThat(type).isEqualTo(ClassHelper.STRING_TYPE);
    }

    @UnitTest
    void inferExpressionType_shouldHandleBitwiseNegationExpression() {
        // given
        String sourceCode =
                """
                def bits = ~0xFF
                """;
        ModuleNode moduleNode = parseSourceNotNull(sourceCode, "test.groovy");

        BitwiseNegationExpression bitwiseExpr =
                findFirstNodeNotNull(moduleNode, BitwiseNegationExpression.class);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(bitwiseExpr, moduleNode);

        // then
        assertThat(type).isEqualTo(ClassHelper.int_TYPE);
    }

    @UnitTest
    void inferExpressionType_shouldHandleNotExpression() {
        // given
        String sourceCode =
                """
                def bool = true
                def result = !bool
                """;
        ModuleNode moduleNode = parseSourceNotNull(sourceCode, "test.groovy");

        NotExpression notExpr = findFirstNodeNotNull(moduleNode, NotExpression.class);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(notExpr, moduleNode);

        // then
        assertThat(type).isEqualTo(ClassHelper.boolean_TYPE);
    }

    @UnitTest
    void inferTypeAtPosition_shouldHandleNonExpressionNode() {
        // given
        String sourceCode =
                """
                class TestClass {
                    void method() {
                        println "Hello"
                    }
                }
                """;

        // Mock finding a statement node instead of expression
        ASTService mockAstService =
                new ASTServiceImpl() {
                    @Override
                    public ASTNode findNodeAtPosition(ModuleNode moduleNode, int line, int column) {
                        // Return a statement node instead of expression
                        return new ExpressionStatement(new ConstantExpression("test"));
                    }
                };

        TypeInferenceServiceImpl serviceWithMock = new TypeInferenceServiceImpl(mockAstService);

        // when
        ClassNode type = serviceWithMock.inferTypeAtPosition(sourceCode, "test.groovy", 3, 10);

        // then
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @UnitTest
    void inferExpressionType_shouldHandlePropertyWithGetterNotStartingWithGet() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode testClass = new ClassNode("TestClass", 1, ClassHelper.OBJECT_TYPE);

        // Add a method that doesn't follow getter naming convention
        MethodNode method =
                new MethodNode(
                        "isActive",
                        1,
                        ClassHelper.boolean_TYPE,
                        Parameter.EMPTY_ARRAY,
                        ClassNode.EMPTY_ARRAY,
                        null);
        testClass.addMethod(method);

        VariableExpression objectExpr = new VariableExpression("obj");
        objectExpr.setType(testClass);
        PropertyExpression propExpr = new PropertyExpression(objectExpr, "active");

        // when
        ClassNode type = typeInferenceService.inferExpressionType(propExpr, moduleNode);

        // then
        // The implementation only looks for getters starting with "get", not "is"
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @UnitTest
    void inferExpressionType_shouldHandleComplexBinaryExpression() {
        // given
        String sourceCode =
                """
                def a = 5
                def b = 10
                def result = (a << 2) | (b & 0xFF)
                """;
        ModuleNode moduleNode = parseSourceNotNull(sourceCode, "test.groovy");

        // Find the binary expression with bitwise OR
        BinaryExpression complexExpr = null;
        // For script code, check the module's statement block directly
        if (moduleNode.getStatementBlock() != null) {
            for (Statement stmt : moduleNode.getStatementBlock().getStatements()) {
                if (stmt instanceof ExpressionStatement expressionStatement) {
                    Expression expr = expressionStatement.getExpression();
                    if (expr instanceof BinaryExpression binExpr) {
                        // The assignment expression contains the bitwise OR on the right side
                        Expression rightExpr = binExpr.getRightExpression();
                        if (rightExpr instanceof BinaryExpression rightBinExpr) {
                            if (rightBinExpr.getOperation().getType()
                                    == org.codehaus.groovy.syntax.Types.BITWISE_OR) {
                                complexExpr = rightBinExpr;
                                break;
                            }
                        }
                    }
                }
            }
        }
        Objects.requireNonNull(complexExpr, "Could not find bitwise OR expression");

        // when
        ClassNode type = typeInferenceService.inferExpressionType(complexExpr, moduleNode);

        // then
        // Bitwise operators are not handled as comparison operators, so it returns the left operand
        // type
        // Since 'a' is inferred as OBJECT_TYPE (def), the result is OBJECT_TYPE
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    // Helper method to parse source and ensure it's not null
    private ModuleNode parseSourceNotNull(String sourceCode, String sourceName) {
        @Nullable ModuleNode moduleNode = astService.parseSource(sourceCode, sourceName);
        return Objects.requireNonNull(moduleNode, "Failed to parse source code");
    }

    // Helper method to find first node of specific type and ensure it's not null
    @SuppressWarnings("unchecked")
    private <T extends ASTNode> T findFirstNodeNotNull(ModuleNode moduleNode, Class<T> nodeType) {
        T node = findFirstNode(moduleNode, nodeType);
        return Objects.requireNonNull(
                node, "Could not find node of type " + nodeType.getSimpleName());
    }

    // Helper method to find first node of specific type
    @SuppressWarnings("unchecked")
    private <T extends ASTNode> @Nullable T findFirstNode(
            ModuleNode moduleNode, Class<T> nodeType) {
        for (ClassNode cn : moduleNode.getClasses()) {
            T node = findInNode(cn, nodeType);
            if (node != null) {
                return node;
            }
        }

        // Check module statements
        if (moduleNode.getStatementBlock() != null) {
            for (Statement stmt : moduleNode.getStatementBlock().getStatements()) {
                T node = findInStatement(stmt, nodeType);
                if (node != null) {
                    return node;
                }
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private <T extends ASTNode> @Nullable T findInNode(ASTNode node, Class<T> nodeType) {
        if (nodeType.isInstance(node)) {
            return (T) node;
        }

        if (node instanceof Expression expression) {
            return findInExpression(expression, nodeType);
        } else if (node instanceof Statement statement) {
            return findInStatement(statement, nodeType);
        } else if (node instanceof ClassNode cn) {
            for (MethodNode method : cn.getMethods()) {
                if (method.getCode() != null) {
                    T result = findInStatement(method.getCode(), nodeType);
                    if (result != null) return result;
                }
            }
            for (FieldNode field : cn.getFields()) {
                if (field.getInitialExpression() != null) {
                    T result = findInExpression(field.getInitialExpression(), nodeType);
                    if (result != null) return result;
                }
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private <T extends ASTNode> @Nullable T findInExpression(Expression expr, Class<T> nodeType) {
        if (nodeType.isInstance(expr)) {
            return (T) expr;
        }

        if (expr instanceof BinaryExpression binExpr) {
            T result = findInExpression(binExpr.getLeftExpression(), nodeType);
            if (result != null) return result;
            return findInExpression(binExpr.getRightExpression(), nodeType);
        } else if (expr instanceof MethodCallExpression call) {
            T result = findInExpression(call.getObjectExpression(), nodeType);
            if (result != null) return result;
            if (call.getArguments() instanceof ArgumentListExpression args) {
                for (Expression arg : args.getExpressions()) {
                    result = findInExpression(arg, nodeType);
                    if (result != null) return result;
                }
            }
        } else if (expr instanceof CastExpression castExpr) {
            return findInExpression(castExpr.getExpression(), nodeType);
        } else if (expr instanceof PropertyExpression propExpr) {
            T result = findInExpression(propExpr.getObjectExpression(), nodeType);
            if (result != null) return result;
            return findInExpression(propExpr.getProperty(), nodeType);
        } else if (expr instanceof ListExpression listExpr) {
            for (Expression e : listExpr.getExpressions()) {
                T result = findInExpression(e, nodeType);
                if (result != null) return result;
            }
        } else if (expr instanceof ArrayExpression arrayExpr) {
            for (Expression e : arrayExpr.getExpressions()) {
                T result = findInExpression(e, nodeType);
                if (result != null) return result;
            }
        } else if (expr instanceof TernaryExpression ternaryExpr) {
            T result = findInExpression(ternaryExpr.getBooleanExpression(), nodeType);
            if (result != null) return result;
            result = findInExpression(ternaryExpr.getTrueExpression(), nodeType);
            if (result != null) return result;
            return findInExpression(ternaryExpr.getFalseExpression(), nodeType);
        } else if (expr instanceof ElvisOperatorExpression elvisExpr) {
            T result = findInExpression(elvisExpr.getTrueExpression(), nodeType);
            if (result != null) return result;
            return findInExpression(elvisExpr.getFalseExpression(), nodeType);
        } else if (expr instanceof SpreadExpression spreadExpr) {
            return findInExpression(spreadExpr.getExpression(), nodeType);
        } else if (expr instanceof UnaryMinusExpression unaryExpr) {
            return findInExpression(unaryExpr.getExpression(), nodeType);
        } else if (expr instanceof NotExpression notExpr) {
            return findInExpression(notExpr.getExpression(), nodeType);
        } else if (expr instanceof BitwiseNegationExpression bitwiseExpr) {
            return findInExpression(bitwiseExpr.getExpression(), nodeType);
        } else if (expr instanceof PostfixExpression postfixExpr) {
            return findInExpression(postfixExpr.getExpression(), nodeType);
        } else if (expr instanceof ClosureExpression closureExpr) {
            if (closureExpr.getCode() != null) {
                return findInStatement(closureExpr.getCode(), nodeType);
            }
        } else if (expr instanceof ConstructorCallExpression constructorExpr) {
            if (constructorExpr.getArguments() instanceof ArgumentListExpression args) {
                for (Expression arg : args.getExpressions()) {
                    T result = findInExpression(arg, nodeType);
                    if (result != null) return result;
                }
            }
        } else if (expr instanceof GStringExpression gstringExpr) {
            for (Expression e : gstringExpr.getValues()) {
                T result = findInExpression(e, nodeType);
                if (result != null) return result;
            }
        } else if (expr instanceof RangeExpression rangeExpr) {
            T result = findInExpression(rangeExpr.getFrom(), nodeType);
            if (result != null) return result;
            return findInExpression(rangeExpr.getTo(), nodeType);
        } else if (expr instanceof FieldExpression) {
            // FieldExpression might have an owner expression
            return null;
        } else if (expr instanceof VariableExpression) {
            // Variable expressions don't have child expressions
            return null;
        } else if (expr instanceof ConstantExpression) {
            // Constant expressions don't have child expressions
            return null;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private <T extends ASTNode> @Nullable T findInStatement(Statement stmt, Class<T> nodeType) {
        if (nodeType.isInstance(stmt)) {
            return (T) stmt;
        }

        if (stmt instanceof ExpressionStatement exprStmt) {
            return findInExpression(exprStmt.getExpression(), nodeType);
        } else if (stmt instanceof BlockStatement blockStmt) {
            for (Statement s : blockStmt.getStatements()) {
                T result = findInStatement(s, nodeType);
                if (result != null) return result;
            }
        }

        return null;
    }
}
