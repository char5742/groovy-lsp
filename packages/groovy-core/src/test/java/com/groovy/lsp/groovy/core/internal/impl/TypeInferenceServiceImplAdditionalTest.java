package com.groovy.lsp.groovy.core.internal.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.groovy.lsp.groovy.core.api.ASTService;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.control.SourceUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @Test
    void inferExpressionType_shouldHandleConstructorCall() {
        // given
        String sourceCode =
                """
                def list = new ArrayList()
                def map = new HashMap<String, Integer>()
                """;
        ModuleNode moduleNode = astService.parseSource(sourceCode, "test.groovy");

        // Find the constructor call expressions
        ConstructorCallExpression constructorCall =
                findFirstNode(moduleNode, ConstructorCallExpression.class);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(constructorCall, moduleNode);

        // then
        assertThat(type.getName()).contains("ArrayList");
    }

    @Test
    void inferExpressionType_shouldHandleCastExpression() {
        // given
        String sourceCode =
                """
                def obj = "test"
                String str = (String) obj
                """;
        ModuleNode moduleNode = astService.parseSource(sourceCode, "test.groovy");

        CastExpression castExpr = findFirstNode(moduleNode, CastExpression.class);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(castExpr, moduleNode);

        // then
        assertThat(type).isEqualTo(ClassHelper.STRING_TYPE);
    }

    @Test
    void inferExpressionType_shouldHandleClosureExpression() {
        // given
        String sourceCode =
                """
                def closure = { String name ->
                    println name
                }
                """;
        ModuleNode moduleNode = astService.parseSource(sourceCode, "test.groovy");

        ClosureExpression closureExpr = findFirstNode(moduleNode, ClosureExpression.class);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(closureExpr, moduleNode);

        // then
        assertThat(type.getName()).contains("Closure");
    }

    @Test
    void inferExpressionType_shouldHandleArrayExpression() {
        // given
        String sourceCode =
                """
                String[] arr = ["a", "b", "c"] as String[]
                """;
        ModuleNode moduleNode = astService.parseSource(sourceCode, "test.groovy");

        ArrayExpression arrayExpr = findFirstNode(moduleNode, ArrayExpression.class);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(arrayExpr, moduleNode);

        // then
        // ArrayExpression is not handled, so it returns OBJECT_TYPE
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @Test
    void inferExpressionType_shouldHandleRangeExpression() {
        // given
        String sourceCode =
                """
                def range = 1..10
                """;
        ModuleNode moduleNode = astService.parseSource(sourceCode, "test.groovy");

        RangeExpression rangeExpr = findFirstNode(moduleNode, RangeExpression.class);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(rangeExpr, moduleNode);

        // then
        assertThat(type.getName()).contains("Range");
    }

    @Test
    void inferExpressionType_shouldHandleGStringExpression() {
        // given
        String sourceCode =
                """
                def name = "World"
                def greeting = "Hello, ${name}!"
                """;
        ModuleNode moduleNode = astService.parseSource(sourceCode, "test.groovy");

        GStringExpression gstringExpr = findFirstNode(moduleNode, GStringExpression.class);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(gstringExpr, moduleNode);

        // then
        assertThat(type.getName()).contains("GString");
    }

    @Test
    void inferExpressionType_shouldHandleTernaryExpression() {
        // given
        String sourceCode =
                """
                def result = true ? "yes" : "no"
                """;
        ModuleNode moduleNode = astService.parseSource(sourceCode, "test.groovy");

        TernaryExpression ternaryExpr = findFirstNode(moduleNode, TernaryExpression.class);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(ternaryExpr, moduleNode);

        // then
        // TernaryExpression is not handled, so it returns OBJECT_TYPE
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @Test
    void inferExpressionType_shouldHandleElvisExpression() {
        // given
        String sourceCode =
                """
                def value = null
                def result = value ?: "default"
                """;
        ModuleNode moduleNode = astService.parseSource(sourceCode, "test.groovy");

        ElvisOperatorExpression elvisExpr =
                findFirstNode(moduleNode, ElvisOperatorExpression.class);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(elvisExpr, moduleNode);

        // then
        // ElvisOperatorExpression is not handled, so it returns OBJECT_TYPE
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @Test
    void inferExpressionType_shouldHandleSpreadExpression() {
        // given
        String sourceCode =
                """
                def list = [1, 2, 3]
                def spread = [*list, 4, 5]
                """;
        ModuleNode moduleNode = astService.parseSource(sourceCode, "test.groovy");

        SpreadExpression spreadExpr = findFirstNode(moduleNode, SpreadExpression.class);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(spreadExpr, moduleNode);

        // then
        // SpreadExpression is not handled, so it returns OBJECT_TYPE
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @Test
    void inferExpressionType_shouldHandleUnaryExpression() {
        // given
        String sourceCode =
                """
                def num = 5
                def negated = -num
                def notBool = !true
                """;
        ModuleNode moduleNode = astService.parseSource(sourceCode, "test.groovy");

        UnaryMinusExpression unaryExpr = findFirstNode(moduleNode, UnaryMinusExpression.class);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(unaryExpr, moduleNode);

        // then
        // UnaryMinusExpression is not handled, so it returns OBJECT_TYPE
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @Test
    void inferExpressionType_shouldHandlePostfixExpression() {
        // given
        String sourceCode =
                """
                def i = 0
                def result = i++
                """;
        ModuleNode moduleNode = astService.parseSource(sourceCode, "test.groovy");

        PostfixExpression postfixExpr = findFirstNode(moduleNode, PostfixExpression.class);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(postfixExpr, moduleNode);

        // then
        // PostfixExpression is not handled, so it returns OBJECT_TYPE
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @Test
    void inferExpressionType_shouldHandleFieldExpression() {
        // given
        String sourceCode =
                """
                class TestClass {
                    String myField = "test"

                    def method() {
                        this.myField
                    }
                }
                """;
        ModuleNode moduleNode = astService.parseSource(sourceCode, "test.groovy");

        FieldExpression fieldExpr = findFirstNode(moduleNode, FieldExpression.class);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(fieldExpr, moduleNode);

        // then
        // FieldExpression is not handled, so it returns OBJECT_TYPE
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @Test
    void inferExpressionType_shouldHandleBitwiseNegationExpression() {
        // given
        String sourceCode =
                """
                def bits = ~0xFF
                """;
        ModuleNode moduleNode = astService.parseSource(sourceCode, "test.groovy");

        BitwiseNegationExpression bitwiseExpr =
                findFirstNode(moduleNode, BitwiseNegationExpression.class);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(bitwiseExpr, moduleNode);

        // then
        assertThat(type).isEqualTo(ClassHelper.int_TYPE);
    }

    @Test
    void inferExpressionType_shouldHandleNotExpression() {
        // given
        String sourceCode =
                """
                def bool = true
                def result = !bool
                """;
        ModuleNode moduleNode = astService.parseSource(sourceCode, "test.groovy");

        NotExpression notExpr = findFirstNode(moduleNode, NotExpression.class);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(notExpr, moduleNode);

        // then
        assertThat(type).isEqualTo(ClassHelper.boolean_TYPE);
    }

    @Test
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
        ModuleNode moduleNode = astService.parseSource(sourceCode, "test.groovy");

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

    @Test
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

    @Test
    void inferExpressionType_shouldHandleComplexBinaryExpression() {
        // given
        String sourceCode =
                """
                def a = 5
                def b = 10
                def result = (a << 2) | (b & 0xFF)
                """;
        ModuleNode moduleNode = astService.parseSource(sourceCode, "test.groovy");

        // Find the binary expression with bitwise OR
        BinaryExpression complexExpr = null;
        for (ClassNode cn : moduleNode.getClasses()) {
            for (Statement stmt : cn.getModule().getStatementBlock().getStatements()) {
                if (stmt instanceof ExpressionStatement) {
                    Expression expr = ((ExpressionStatement) stmt).getExpression();
                    if (expr instanceof BinaryExpression) {
                        BinaryExpression binExpr = (BinaryExpression) expr;
                        if (binExpr.getOperation().getType()
                                == org.codehaus.groovy.syntax.Types.BITWISE_OR) {
                            complexExpr = binExpr;
                            break;
                        }
                    }
                }
            }
        }

        // when
        ClassNode type = typeInferenceService.inferExpressionType(complexExpr, moduleNode);

        // then
        // Bitwise operators are not handled as comparison operators, so it returns the left operand
        // type
        // Since 'a' is inferred as OBJECT_TYPE (def), the result is OBJECT_TYPE
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    // Helper method to find first node of specific type
    @SuppressWarnings("unchecked")
    private <T extends ASTNode> T findFirstNode(ModuleNode moduleNode, Class<T> nodeType) {
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
    private <T extends ASTNode> T findInNode(ASTNode node, Class<T> nodeType) {
        if (nodeType.isInstance(node)) {
            return (T) node;
        }

        if (node instanceof Expression) {
            return findInExpression((Expression) node, nodeType);
        } else if (node instanceof Statement) {
            return findInStatement((Statement) node, nodeType);
        } else if (node instanceof ClassNode) {
            ClassNode cn = (ClassNode) node;
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
    private <T extends ASTNode> T findInExpression(Expression expr, Class<T> nodeType) {
        if (nodeType.isInstance(expr)) {
            return (T) expr;
        }

        if (expr instanceof BinaryExpression) {
            BinaryExpression binExpr = (BinaryExpression) expr;
            T result = findInExpression(binExpr.getLeftExpression(), nodeType);
            if (result != null) return result;
            return findInExpression(binExpr.getRightExpression(), nodeType);
        } else if (expr instanceof MethodCallExpression) {
            MethodCallExpression call = (MethodCallExpression) expr;
            T result = findInExpression(call.getObjectExpression(), nodeType);
            if (result != null) return result;
            if (call.getArguments() instanceof ArgumentListExpression) {
                ArgumentListExpression args = (ArgumentListExpression) call.getArguments();
                for (Expression arg : args.getExpressions()) {
                    result = findInExpression(arg, nodeType);
                    if (result != null) return result;
                }
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private <T extends ASTNode> T findInStatement(Statement stmt, Class<T> nodeType) {
        if (nodeType.isInstance(stmt)) {
            return (T) stmt;
        }

        if (stmt instanceof ExpressionStatement) {
            return findInExpression(((ExpressionStatement) stmt).getExpression(), nodeType);
        } else if (stmt instanceof BlockStatement) {
            for (Statement s : ((BlockStatement) stmt).getStatements()) {
                T result = findInStatement(s, nodeType);
                if (result != null) return result;
            }
        }

        return null;
    }
}
