package com.groovy.lsp.groovy.core.internal.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.groovy.lsp.groovy.core.api.ASTService;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.Token;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * TypeInferenceServiceImplのテストクラス。
 */
@ExtendWith(MockitoExtension.class)
class TypeInferenceServiceImplTest {

    @Mock private ASTService mockAstService;

    private TypeInferenceServiceImpl typeInferenceService;

    @BeforeEach
    void setUp() {
        typeInferenceService = new TypeInferenceServiceImpl(mockAstService);
    }

    @Test
    void constructor_shouldThrowExceptionForNullASTService() {
        assertThatThrownBy(() -> new TypeInferenceServiceImpl(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ASTService cannot be null");
    }

    @Test
    void inferTypeAtPosition_shouldReturnObjectTypeWhenParseFails() {
        // given
        when(mockAstService.parseSource(anyString(), anyString())).thenReturn(null);

        // when
        ClassNode type = typeInferenceService.inferTypeAtPosition("code", "test.groovy", 1, 1);

        // then
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @Test
    void inferTypeAtPosition_shouldReturnObjectTypeWhenNodeNotFound() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        when(mockAstService.parseSource(anyString(), anyString())).thenReturn(moduleNode);
        when(mockAstService.findNodeAtPosition(moduleNode, 1, 1)).thenReturn(null);

        // when
        ClassNode type = typeInferenceService.inferTypeAtPosition("code", "test.groovy", 1, 1);

        // then
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @Test
    void inferTypeAtPosition_shouldInferTypeOfExpressionNode() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ConstantExpression constantExpr = new ConstantExpression("test");
        constantExpr.setType(ClassHelper.STRING_TYPE);

        when(mockAstService.parseSource(anyString(), anyString())).thenReturn(moduleNode);
        when(mockAstService.findNodeAtPosition(moduleNode, 1, 1)).thenReturn(constantExpr);

        // when
        ClassNode type = typeInferenceService.inferTypeAtPosition("code", "test.groovy", 1, 1);

        // then
        assertThat(type).isEqualTo(ClassHelper.STRING_TYPE);
    }

    @Test
    void inferExpressionType_shouldReturnObjectTypeForNullExpression() {
        // when
        ClassNode type =
                typeInferenceService.inferExpressionType(null, new ModuleNode((SourceUnit) null));

        // then
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @Test
    void inferExpressionType_shouldReturnExistingType() {
        // given
        Expression expr = new ConstantExpression("test");
        expr.setType(ClassHelper.STRING_TYPE);
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(expr, moduleNode);

        // then
        assertThat(type).isEqualTo(ClassHelper.STRING_TYPE);
    }

    @Test
    void inferExpressionType_shouldInferConstantExpressionTypes() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);

        // Test String
        ConstantExpression stringExpr = new ConstantExpression("hello");
        ClassNode stringType = typeInferenceService.inferExpressionType(stringExpr, moduleNode);
        assertThat(stringType).isEqualTo(ClassHelper.STRING_TYPE);

        // Test Integer
        ConstantExpression intExpr = new ConstantExpression(42);
        // Force the constant expression to use primitive type
        intExpr.setType(ClassHelper.int_TYPE);
        ClassNode intType = typeInferenceService.inferExpressionType(intExpr, moduleNode);
        assertThat(intType.getName()).isEqualTo("int");

        // Test Boolean
        ConstantExpression boolExpr = new ConstantExpression(true);
        boolExpr.setType(ClassHelper.boolean_TYPE);
        ClassNode boolType = typeInferenceService.inferExpressionType(boolExpr, moduleNode);
        assertThat(boolType.getName()).isEqualTo("boolean");

        // Test Double
        ConstantExpression doubleExpr = new ConstantExpression(3.14);
        doubleExpr.setType(ClassHelper.double_TYPE);
        ClassNode doubleType = typeInferenceService.inferExpressionType(doubleExpr, moduleNode);
        assertThat(doubleType.getName()).isEqualTo("double");

        // Test null
        ConstantExpression nullExpr = new ConstantExpression(null);
        ClassNode nullType = typeInferenceService.inferExpressionType(nullExpr, moduleNode);
        assertThat(nullType).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @Test
    void inferExpressionType_shouldReturnListTypeForListExpression() {
        // given
        ListExpression listExpr = new ListExpression();
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(listExpr, moduleNode);

        // then
        assertThat(type).isEqualTo(ClassHelper.LIST_TYPE);
    }

    @Test
    void inferExpressionType_shouldReturnMapTypeForMapExpression() {
        // given
        MapExpression mapExpr = new MapExpression();
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(mapExpr, moduleNode);

        // then
        assertThat(type).isEqualTo(ClassHelper.MAP_TYPE);
    }

    @Test
    void inferExpressionType_shouldReturnEnclosingClassTypeForThisKeyword() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode testClass = new ClassNode("TestClass", 1, ClassHelper.OBJECT_TYPE);
        testClass.setLineNumber(1);
        testClass.setLastLineNumber(10);
        moduleNode.addClass(testClass);

        VariableExpression thisExpr = new VariableExpression("this");
        thisExpr.setLineNumber(5);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(thisExpr, moduleNode);

        // then
        assertThat(type.getName()).isEqualTo("TestClass");
    }

    @Test
    void inferExpressionType_shouldReturnAccessedVariableTypeForVariableExpression() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        VariableExpression varExpr = new VariableExpression("myVar", ClassHelper.STRING_TYPE);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(varExpr, moduleNode);

        // then
        assertThat(type).isEqualTo(ClassHelper.STRING_TYPE);
    }

    @Test
    void inferExpressionType_shouldReturnBooleanTypeForComparisonOperators() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        BinaryExpression compareExpr =
                new BinaryExpression(
                        new ConstantExpression(1),
                        Token.newSymbol(org.codehaus.groovy.syntax.Types.COMPARE_EQUAL, 0, 0),
                        new ConstantExpression(2));

        // when
        ClassNode type = typeInferenceService.inferExpressionType(compareExpr, moduleNode);

        // then
        assertThat(type).isEqualTo(ClassHelper.boolean_TYPE);
    }

    @Test
    void inferExpressionType_shouldReturnLeftTypeForArithmeticOperators() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ConstantExpression leftExpr = new ConstantExpression(10);
        leftExpr.setType(ClassHelper.int_TYPE);
        BinaryExpression plusExpr =
                new BinaryExpression(
                        leftExpr,
                        Token.newSymbol(org.codehaus.groovy.syntax.Types.PLUS, 0, 0),
                        new ConstantExpression(5));

        // when
        ClassNode type = typeInferenceService.inferExpressionType(plusExpr, moduleNode);

        // then
        assertThat(type).isEqualTo(ClassHelper.int_TYPE);
    }

    @Test
    void inferExpressionType_shouldReturnPropertyTypeForPropertyExpression() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode personClass = new ClassNode("Person", 1, ClassHelper.OBJECT_TYPE);
        PropertyNode nameProperty =
                new PropertyNode("name", 1, ClassHelper.STRING_TYPE, personClass, null, null, null);
        personClass.addProperty(nameProperty);

        VariableExpression objectExpr = new VariableExpression("person");
        objectExpr.setType(personClass);
        PropertyExpression propExpr = new PropertyExpression(objectExpr, "name");

        // when
        ClassNode type = typeInferenceService.inferExpressionType(propExpr, moduleNode);

        // then
        assertThat(type).isEqualTo(ClassHelper.STRING_TYPE);
    }

    @Test
    void inferExpressionType_shouldReturnFieldTypeForPropertyExpression() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode testClass = new ClassNode("TestClass", 1, ClassHelper.OBJECT_TYPE);
        FieldNode field = new FieldNode("myField", 1, ClassHelper.int_TYPE, testClass, null);
        testClass.addField(field);

        VariableExpression objectExpr = new VariableExpression("obj");
        objectExpr.setType(testClass);
        PropertyExpression propExpr = new PropertyExpression(objectExpr, "myField");

        // when
        ClassNode type = typeInferenceService.inferExpressionType(propExpr, moduleNode);

        // then
        assertThat(type).isEqualTo(ClassHelper.int_TYPE);
    }

    @Test
    void inferExpressionType_shouldReturnMethodReturnTypeForMethodCall() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        // Create a mock class without methods
        ClassNode mockClass = new ClassNode("MockClass", 1, ClassHelper.OBJECT_TYPE);

        VariableExpression objectExpr = new VariableExpression("obj");
        objectExpr.setType(mockClass);
        MethodCallExpression methodCall =
                new MethodCallExpression(
                        objectExpr, "unknownMethod", ArgumentListExpression.EMPTY_ARGUMENTS);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(methodCall, moduleNode);

        // then
        // Since mockClass doesn't have the method, it should return OBJECT_TYPE
        assertThat(type.getName()).isEqualTo("java.lang.Object");
    }

    @Test
    void inferExpressionType_shouldReturnPropertyTypeForGetterPattern() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode testClass = new ClassNode("TestClass", 1, ClassHelper.OBJECT_TYPE);
        PropertyNode property =
                new PropertyNode("age", 1, ClassHelper.int_TYPE, testClass, null, null, null);
        testClass.addProperty(property);

        VariableExpression objectExpr = new VariableExpression("obj");
        objectExpr.setType(testClass);
        MethodCallExpression getterCall =
                new MethodCallExpression(
                        objectExpr, "getAge", ArgumentListExpression.EMPTY_ARGUMENTS);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(getterCall, moduleNode);

        // then
        assertThat(type).isEqualTo(ClassHelper.int_TYPE);
    }

    @Test
    void inferExpressionType_shouldReturnObjectTypeForUnknownExpression() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        Expression unknownExpr =
                new Expression() {
                    @Override
                    public Expression transformExpression(ExpressionTransformer transformer) {
                        return this;
                    }

                    @Override
                    public void visit(GroovyCodeVisitor visitor) {
                        // No-op
                    }
                };

        // when
        ClassNode type = typeInferenceService.inferExpressionType(unknownExpr, moduleNode);

        // then
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @Test
    void inferExpressionType_shouldFindVariableDeclarationInModule() {
        // given - Use real AST service to test VariableDeclarationFinder
        ASTService realAstService = new ASTServiceImpl();
        TypeInferenceServiceImpl realTypeInference = new TypeInferenceServiceImpl(realAstService);

        String sourceCode =
                """
                String myVariable = "Hello World"
                def anotherVar = myVariable
                """;

        ModuleNode moduleNode = realAstService.parseSource(sourceCode, "test.groovy");

        // Create a variable expression for 'myVariable'
        VariableExpression varExpr = new VariableExpression("myVariable");
        varExpr.setLineNumber(2);

        // when
        ClassNode type = realTypeInference.inferExpressionType(varExpr, moduleNode);

        // then - Should find the String type from declaration
        assertThat(type.getName()).isEqualTo("java.lang.String");
    }

    @Test
    void inferExpressionType_shouldInferTypeFromConstantInDeclaration() {
        // given - Test VariableDeclarationFinder's ability to infer from right side
        ASTService realAstService = new ASTServiceImpl();
        TypeInferenceServiceImpl realTypeInference = new TypeInferenceServiceImpl(realAstService);

        String sourceCode =
                """
                def inferredString = "This is a string"
                def inferredInt = 42
                def inferredBoolean = true
                def inferredList = [1, 2, 3]
                """;

        ModuleNode moduleNode = realAstService.parseSource(sourceCode, "test.groovy");

        // when - Test string inference
        VariableExpression stringVar = new VariableExpression("inferredString");
        ClassNode stringType = realTypeInference.inferExpressionType(stringVar, moduleNode);

        // then
        assertThat(stringType.getName()).isEqualTo("java.lang.String");
    }

    @Test
    void inferExpressionType_shouldHandleVariableNotFound() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        VariableExpression unknownVar = new VariableExpression("nonExistentVariable");

        // when
        ClassNode type = typeInferenceService.inferExpressionType(unknownVar, moduleNode);

        // then
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @Test
    void inferExpressionType_shouldHandlePropertyExpressionWithNullReceiver() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        // Create a mock object expression that will return null type
        VariableExpression nullReceiver = new VariableExpression("nullVar");
        PropertyExpression propExpr = new PropertyExpression(nullReceiver, "someProperty");

        // when
        ClassNode type = typeInferenceService.inferExpressionType(propExpr, moduleNode);

        // then
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @Test
    void inferExpressionType_shouldHandlePropertyExpressionWithNullPropertyName() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode testClass = new ClassNode("TestClass", 1, ClassHelper.OBJECT_TYPE);
        VariableExpression objectExpr = new VariableExpression("obj");
        objectExpr.setType(testClass);

        // Create property expression with dynamic property (not a constant)
        Expression dynamicProperty = new VariableExpression("dynamicProp");
        PropertyExpression propExpr = new PropertyExpression(objectExpr, dynamicProperty);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(propExpr, moduleNode);

        // then
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @Test
    void inferExpressionType_shouldReturnGetterTypeFromPropertyExpression() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode testClass = new ClassNode("TestClass", 1, ClassHelper.OBJECT_TYPE);

        // Add a getter method instead of property
        MethodNode getter =
                new MethodNode(
                        "getName",
                        1,
                        ClassHelper.STRING_TYPE,
                        Parameter.EMPTY_ARRAY,
                        ClassNode.EMPTY_ARRAY,
                        null);
        testClass.addMethod(getter);

        VariableExpression objectExpr = new VariableExpression("obj");
        objectExpr.setType(testClass);
        PropertyExpression propExpr = new PropertyExpression(objectExpr, "name");

        // when
        ClassNode type = typeInferenceService.inferExpressionType(propExpr, moduleNode);

        // then
        assertThat(type).isEqualTo(ClassHelper.STRING_TYPE);
    }

    @Test
    void inferExpressionType_shouldHandleMethodCallWithNullMethodName() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode mockClass = new ClassNode("MockClass", 1, ClassHelper.OBJECT_TYPE);
        VariableExpression objectExpr = new VariableExpression("obj");
        objectExpr.setType(mockClass);

        // Create method call with dynamic method name (not a constant)
        Expression dynamicMethod = new VariableExpression("dynamicMethod");
        MethodCallExpression methodCall =
                new MethodCallExpression(
                        objectExpr, dynamicMethod, ArgumentListExpression.EMPTY_ARGUMENTS);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(methodCall, moduleNode);

        // then
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @Test
    void inferExpressionType_shouldHandleMethodCallWithNullReceiverType() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        VariableExpression nullReceiver = new VariableExpression("nullVar");
        MethodCallExpression methodCall =
                new MethodCallExpression(
                        nullReceiver, "someMethod", ArgumentListExpression.EMPTY_ARGUMENTS);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(methodCall, moduleNode);

        // then
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @Test
    void inferExpressionType_shouldReturnMethodReturnType() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode testClass = new ClassNode("TestClass", 1, ClassHelper.OBJECT_TYPE);

        // Add a method with return type
        MethodNode method =
                new MethodNode(
                        "getValue",
                        1,
                        ClassHelper.int_TYPE,
                        Parameter.EMPTY_ARRAY,
                        ClassNode.EMPTY_ARRAY,
                        null);
        testClass.addMethod(method);

        VariableExpression objectExpr = new VariableExpression("obj");
        objectExpr.setType(testClass);
        MethodCallExpression methodCall =
                new MethodCallExpression(
                        objectExpr, "getValue", ArgumentListExpression.EMPTY_ARGUMENTS);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(methodCall, moduleNode);

        // then
        assertThat(type).isEqualTo(ClassHelper.int_TYPE);
    }

    @Test
    void inferExpressionType_shouldHandleAllComparisonOperators() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ConstantExpression left = new ConstantExpression(1);
        ConstantExpression right = new ConstantExpression(2);

        // Test all comparison operators
        int[] comparisonOps = {
            org.codehaus.groovy.syntax.Types.COMPARE_EQUAL,
            org.codehaus.groovy.syntax.Types.COMPARE_NOT_EQUAL,
            org.codehaus.groovy.syntax.Types.COMPARE_LESS_THAN,
            org.codehaus.groovy.syntax.Types.COMPARE_LESS_THAN_EQUAL,
            org.codehaus.groovy.syntax.Types.COMPARE_GREATER_THAN,
            org.codehaus.groovy.syntax.Types.COMPARE_GREATER_THAN_EQUAL
        };

        for (int op : comparisonOps) {
            BinaryExpression binExpr = new BinaryExpression(left, Token.newSymbol(op, 0, 0), right);

            // when
            ClassNode type = typeInferenceService.inferExpressionType(binExpr, moduleNode);

            // then
            assertThat(type).isEqualTo(ClassHelper.boolean_TYPE);
        }
    }

    @Test
    void inferExpressionType_shouldHandleConstantExpressionWithCustomType() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);

        // Test with a custom object type
        Object customValue = new java.util.Date();
        ConstantExpression customExpr = new ConstantExpression(customValue);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(customExpr, moduleNode);

        // then
        assertThat(type.getName()).isEqualTo("java.util.Date");
    }

    @Test
    void inferExpressionType_shouldHandleLongConstant() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ConstantExpression longExpr = new ConstantExpression(123456789L);
        longExpr.setType(ClassHelper.long_TYPE);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(longExpr, moduleNode);

        // then
        assertThat(type).isEqualTo(ClassHelper.long_TYPE);
    }

    @Test
    void inferExpressionType_shouldHandleFloatConstant() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ConstantExpression floatExpr = new ConstantExpression(3.14f);
        floatExpr.setType(ClassHelper.float_TYPE);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(floatExpr, moduleNode);

        // then
        assertThat(type).isEqualTo(ClassHelper.float_TYPE);
    }

    @Test
    void inferExpressionType_shouldNotRecomputeExistingTypeForNonMethodCall() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        VariableExpression varExpr = new VariableExpression("myVar");
        // Set a specific type that's not OBJECT_TYPE
        ClassNode customType = new ClassNode("CustomType", 1, ClassHelper.OBJECT_TYPE);
        varExpr.setType(customType);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(varExpr, moduleNode);

        // then - Should return the existing type without trying to infer
        assertThat(type).isEqualTo(customType);
    }

    @Test
    void inferExpressionType_shouldRecomputeTypeForMethodCall() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode testClass = new ClassNode("TestClass", 1, ClassHelper.OBJECT_TYPE);

        // Add a method
        MethodNode method =
                new MethodNode(
                        "toString",
                        1,
                        ClassHelper.STRING_TYPE,
                        Parameter.EMPTY_ARRAY,
                        ClassNode.EMPTY_ARRAY,
                        null);
        testClass.addMethod(method);

        VariableExpression objectExpr = new VariableExpression("obj");
        objectExpr.setType(testClass);
        MethodCallExpression methodCall =
                new MethodCallExpression(
                        objectExpr, "toString", ArgumentListExpression.EMPTY_ARGUMENTS);

        // Pre-set a type on the method call (simulating pre-existing type)
        methodCall.setType(ClassHelper.OBJECT_TYPE);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(methodCall, moduleNode);

        // then - Should compute the actual return type, not use pre-set type
        assertThat(type).isEqualTo(ClassHelper.STRING_TYPE);
    }

    @Test
    void findEnclosingClass_shouldHandleNodeOutsideAnyClass() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode class1 = new ClassNode("Class1", 1, ClassHelper.OBJECT_TYPE);
        class1.setLineNumber(10);
        class1.setLastLineNumber(20);
        moduleNode.addClass(class1);

        // Create a node outside the class range
        VariableExpression outsideNode = new VariableExpression("outside");
        outsideNode.setLineNumber(5); // Before class1

        // when
        ClassNode type = typeInferenceService.inferExpressionType(outsideNode, moduleNode);

        // then - Should return OBJECT_TYPE when not in any class
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }
}
