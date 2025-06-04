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

    @Test
    void inferConstantType_shouldHandleAllPrimitiveTypes() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);

        // Test null value
        ConstantExpression nullExpr = new ConstantExpression(null);
        // Don't set type - let inferConstantType handle it
        ClassNode nullType = typeInferenceService.inferExpressionType(nullExpr, moduleNode);
        assertThat(nullType).isEqualTo(ClassHelper.OBJECT_TYPE);

        // Test String
        ConstantExpression stringExpr = new ConstantExpression("hello");
        ClassNode stringType = typeInferenceService.inferExpressionType(stringExpr, moduleNode);
        assertThat(stringType).isEqualTo(ClassHelper.STRING_TYPE);

        // Test Integer - when using primitive value, it gets autoboxed
        ConstantExpression intExpr = new ConstantExpression(42);
        ClassNode intType = typeInferenceService.inferExpressionType(intExpr, moduleNode);
        assertThat(intType.getName()).isEqualTo("java.lang.Integer");

        // Test Long
        ConstantExpression longExpr = new ConstantExpression(123456789L);
        ClassNode longType = typeInferenceService.inferExpressionType(longExpr, moduleNode);
        assertThat(longType.getName()).isEqualTo("java.lang.Long");

        // Test Double
        ConstantExpression doubleExpr = new ConstantExpression(3.14);
        ClassNode doubleType = typeInferenceService.inferExpressionType(doubleExpr, moduleNode);
        assertThat(doubleType.getName()).isEqualTo("java.lang.Double");

        // Test Float
        ConstantExpression floatExpr = new ConstantExpression(2.5f);
        ClassNode floatType = typeInferenceService.inferExpressionType(floatExpr, moduleNode);
        assertThat(floatType.getName()).isEqualTo("java.lang.Float");

        // Test Boolean
        ConstantExpression boolExpr = new ConstantExpression(true);
        ClassNode boolType = typeInferenceService.inferExpressionType(boolExpr, moduleNode);
        assertThat(boolType.getName()).isEqualTo("java.lang.Boolean");
    }

    @Test
    void inferConstantType_shouldHandleOtherTypeWithExpressionType() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);

        // Test with a BigDecimal that has a specific type set
        ConstantExpression bigDecimalExpr =
                new ConstantExpression(new java.math.BigDecimal("123.45"));
        ClassNode customType = ClassHelper.make(java.math.BigDecimal.class);
        bigDecimalExpr.setType(customType);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(bigDecimalExpr, moduleNode);

        // then - should use the expression's type
        assertThat(type.getName()).isEqualTo("java.math.BigDecimal");
    }

    @Test
    void inferConstantType_shouldUseValueClassForOtherTypes() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);

        // Test with various object types without setting expression type

        // Test with Date
        ConstantExpression dateExpr = new ConstantExpression(new java.util.Date());
        ClassNode dateType = typeInferenceService.inferExpressionType(dateExpr, moduleNode);
        assertThat(dateType.getName()).isEqualTo("java.util.Date");

        // Test with ArrayList
        ConstantExpression listExpr = new ConstantExpression(new java.util.ArrayList<>());
        ClassNode listType = typeInferenceService.inferExpressionType(listExpr, moduleNode);
        assertThat(listType.getName()).isEqualTo("java.util.ArrayList");

        // Test with custom object
        ConstantExpression customExpr = new ConstantExpression(new StringBuilder("test"));
        ClassNode customType = typeInferenceService.inferExpressionType(customExpr, moduleNode);
        assertThat(customType.getName()).isEqualTo("java.lang.StringBuilder");
    }

    @Test
    void inferConstantType_shouldHandleByteAndShort() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);

        // Test Byte (should fall through to value.getClass())
        ConstantExpression byteExpr = new ConstantExpression(Byte.valueOf((byte) 127));
        ClassNode byteType = typeInferenceService.inferExpressionType(byteExpr, moduleNode);
        assertThat(byteType.getName()).isEqualTo("java.lang.Byte");

        // Test Short (should fall through to value.getClass())
        ConstantExpression shortExpr = new ConstantExpression(Short.valueOf((short) 32767));
        ClassNode shortType = typeInferenceService.inferExpressionType(shortExpr, moduleNode);
        assertThat(shortType.getName()).isEqualTo("java.lang.Short");

        // Test Character (should fall through to value.getClass())
        ConstantExpression charExpr = new ConstantExpression(Character.valueOf('A'));
        ClassNode charType = typeInferenceService.inferExpressionType(charExpr, moduleNode);
        assertThat(charType.getName()).isEqualTo("java.lang.Character");
    }

    @Test
    void inferConstantType_shouldHandleObjectTypeInExpression() throws Exception {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);

        // Test when expression has OBJECT_TYPE set - should use value.getClass()
        ConstantExpression expr = new ConstantExpression(new java.net.URL("http://example.com"));
        expr.setType(ClassHelper.OBJECT_TYPE); // Set to OBJECT_TYPE explicitly

        // when
        ClassNode type = typeInferenceService.inferExpressionType(expr, moduleNode);

        // then - should fall through to value.getClass()
        assertThat(type.getName()).isEqualTo("java.net.URL");
    }

    @Test
    void inferMethodCallType_shouldReturnObjectTypeForGetterWithoutMatchingProperty() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode testClass = new ClassNode("TestClass", 1, ClassHelper.OBJECT_TYPE);
        // Don't add any property named "nonExistent"

        VariableExpression objectExpr = new VariableExpression("obj");
        objectExpr.setType(testClass);
        MethodCallExpression getterCall =
                new MethodCallExpression(
                        objectExpr, "getNonExistent", ArgumentListExpression.EMPTY_ARGUMENTS);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(getterCall, moduleNode);

        // then - Should return OBJECT_TYPE when getter pattern doesn't match any property
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @Test
    void inferMethodCallType_shouldHandleGetterWithShortName() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode testClass = new ClassNode("TestClass", 1, ClassHelper.OBJECT_TYPE);

        VariableExpression objectExpr = new VariableExpression("obj");
        objectExpr.setType(testClass);
        // Method name "get" is too short (length <= 3)
        MethodCallExpression getterCall =
                new MethodCallExpression(objectExpr, "get", ArgumentListExpression.EMPTY_ARGUMENTS);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(getterCall, moduleNode);

        // then - Should return OBJECT_TYPE when method name is too short for getter pattern
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @Test
    void inferPropertyType_shouldReturnObjectTypeWhenPropertyNotFound() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode testClass = new ClassNode("TestClass", 1, ClassHelper.OBJECT_TYPE);
        // Don't add any property, field, or getter method

        VariableExpression objectExpr = new VariableExpression("obj");
        objectExpr.setType(testClass);
        PropertyExpression propExpr = new PropertyExpression(objectExpr, "nonExistentProperty");

        // when
        ClassNode type = typeInferenceService.inferExpressionType(propExpr, moduleNode);

        // then - Should return OBJECT_TYPE when property is not found
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @Test
    void inferBinaryExpressionType_shouldReturnLeftTypeForAssignmentOperator() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ConstantExpression leftExpr = new ConstantExpression("left");
        leftExpr.setType(ClassHelper.STRING_TYPE);
        ConstantExpression rightExpr = new ConstantExpression("right");

        // Test assignment operator (not a comparison)
        BinaryExpression assignExpr =
                new BinaryExpression(
                        leftExpr,
                        Token.newSymbol(org.codehaus.groovy.syntax.Types.ASSIGN, 0, 0),
                        rightExpr);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(assignExpr, moduleNode);

        // then - Should return the left expression type for non-comparison operators
        assertThat(type).isEqualTo(ClassHelper.STRING_TYPE);
    }

    @Test
    void inferBinaryExpressionType_shouldReturnLeftTypeForBitwiseOperator() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ConstantExpression leftExpr = new ConstantExpression(10);
        leftExpr.setType(ClassHelper.int_TYPE);
        ConstantExpression rightExpr = new ConstantExpression(5);

        // Test bitwise AND operator (not a comparison)
        BinaryExpression bitwiseExpr =
                new BinaryExpression(
                        leftExpr,
                        Token.newSymbol(org.codehaus.groovy.syntax.Types.BITWISE_AND, 0, 0),
                        rightExpr);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(bitwiseExpr, moduleNode);

        // then - Should return the left expression type for non-comparison operators
        assertThat(type).isEqualTo(ClassHelper.int_TYPE);
    }

    @Test
    void inferVariableType_shouldReturnObjectTypeForThisOutsideClass() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        // Don't add any classes to the module

        VariableExpression thisExpr = new VariableExpression("this");
        thisExpr.setLineNumber(1);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(thisExpr, moduleNode);

        // then - Should return OBJECT_TYPE when "this" is used outside any class
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @Test
    void inferExpressionType_shouldInferTypeFromVariableInClassField() {
        // given - Test variable declaration finder with field initialization
        ASTService realAstService = new ASTServiceImpl();
        TypeInferenceServiceImpl realTypeInference = new TypeInferenceServiceImpl(realAstService);

        String sourceCode =
                """
                class TestClass {
                    def myField = "Hello"

                    def method() {
                        def local = myField
                    }
                }
                """;

        ModuleNode moduleNode = realAstService.parseSource(sourceCode, "test.groovy");

        // Create a variable expression for 'myField' inside the method
        VariableExpression fieldVar = new VariableExpression("myField");
        fieldVar.setLineNumber(5);

        // when
        ClassNode type = realTypeInference.inferExpressionType(fieldVar, moduleNode);

        // then - Should find the field type
        assertThat(type.getName()).isEqualTo("java.lang.Object"); // def defaults to Object
    }

    @Test
    void inferVariableType_shouldHandleVariableWithNullAccessedVariable() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        VariableExpression varExpr = new VariableExpression("myVar");
        // Don't set accessed variable (it's null by default)
        // Don't add any declaration in the module

        // when
        ClassNode type = typeInferenceService.inferExpressionType(varExpr, moduleNode);

        // then - Should return OBJECT_TYPE when variable is not found
        assertThat(type).isEqualTo(ClassHelper.OBJECT_TYPE);
    }

    @Test
    void inferExpressionType_shouldInferTypeFromNonConstantRightExpression() {
        // given - Test VariableDeclarationFinder with non-constant right expression
        ASTService realAstService = new ASTServiceImpl();
        TypeInferenceServiceImpl realTypeInference = new TypeInferenceServiceImpl(realAstService);

        String sourceCode =
                """
                def list = []
                def myVar = list
                """;

        ModuleNode moduleNode = realAstService.parseSource(sourceCode, "test.groovy");

        // Create a variable expression for 'myVar'
        VariableExpression varExpr = new VariableExpression("myVar");
        varExpr.setLineNumber(2);

        // when
        ClassNode type = realTypeInference.inferExpressionType(varExpr, moduleNode);

        // then - Should infer type from the right expression
        assertThat(type.getName()).isEqualTo("java.lang.Object"); // def defaults to Object
    }

    @Test
    void inferMethodCallType_shouldHandleGetterPatternCaseSensitivity() {
        // given - Test getter pattern with different case scenarios
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        ClassNode testClass = new ClassNode("TestClass", 1, ClassHelper.OBJECT_TYPE);

        // Add property with lowercase name
        PropertyNode property =
                new PropertyNode("value", 1, ClassHelper.int_TYPE, testClass, null, null, null);
        testClass.addProperty(property);

        VariableExpression objectExpr = new VariableExpression("obj");
        objectExpr.setType(testClass);

        // Test getter with correct case
        MethodCallExpression getterCall =
                new MethodCallExpression(
                        objectExpr, "getValue", ArgumentListExpression.EMPTY_ARGUMENTS);

        // when
        ClassNode type = typeInferenceService.inferExpressionType(getterCall, moduleNode);

        // then
        assertThat(type).isEqualTo(ClassHelper.int_TYPE);
    }

    @Test
    void inferConstantType_shouldReturnPrimitiveTypesDirectly() {
        // given - Force the branches for primitive type checks to be covered
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);

        // Use reflection to directly call inferConstantType
        try {
            java.lang.reflect.Method inferConstantTypeMethod =
                    TypeInferenceServiceImpl.class.getDeclaredMethod(
                            "inferConstantType", ConstantExpression.class);
            inferConstantTypeMethod.setAccessible(true);

            // Test Integer primitive branch
            ConstantExpression intExpr = new ConstantExpression(Integer.valueOf(42));
            // Force value to be primitive int
            java.lang.reflect.Field valueField = ConstantExpression.class.getDeclaredField("value");
            valueField.setAccessible(true);
            valueField.set(intExpr, 42); // Primitive int

            ClassNode intType =
                    (ClassNode) inferConstantTypeMethod.invoke(typeInferenceService, intExpr);
            assertThat(intType).isEqualTo(ClassHelper.int_TYPE);

            // Test Long primitive branch
            ConstantExpression longExpr = new ConstantExpression(123L);
            valueField.set(longExpr, 123L); // Primitive long
            ClassNode longType =
                    (ClassNode) inferConstantTypeMethod.invoke(typeInferenceService, longExpr);
            assertThat(longType).isEqualTo(ClassHelper.long_TYPE);

            // Test Double primitive branch
            ConstantExpression doubleExpr = new ConstantExpression(3.14);
            valueField.set(doubleExpr, 3.14); // Primitive double
            ClassNode doubleType =
                    (ClassNode) inferConstantTypeMethod.invoke(typeInferenceService, doubleExpr);
            assertThat(doubleType).isEqualTo(ClassHelper.double_TYPE);

            // Test Float primitive branch
            ConstantExpression floatExpr = new ConstantExpression(2.5f);
            valueField.set(floatExpr, 2.5f); // Primitive float
            ClassNode floatType =
                    (ClassNode) inferConstantTypeMethod.invoke(typeInferenceService, floatExpr);
            assertThat(floatType).isEqualTo(ClassHelper.float_TYPE);

            // Test Boolean primitive branch
            ConstantExpression boolExpr = new ConstantExpression(true);
            valueField.set(boolExpr, true); // Primitive boolean
            ClassNode boolType =
                    (ClassNode) inferConstantTypeMethod.invoke(typeInferenceService, boolExpr);
            assertThat(boolType).isEqualTo(ClassHelper.boolean_TYPE);

        } catch (Exception e) {
            throw new RuntimeException("Test failed", e);
        }
    }

    @Test
    void inferConstantType_shouldUseExpressionTypeWhenNotObjectType() {
        // given - Test the branch where expression has a non-OBJECT_TYPE set
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);

        try {
            java.lang.reflect.Method inferConstantTypeMethod =
                    TypeInferenceServiceImpl.class.getDeclaredMethod(
                            "inferConstantType", ConstantExpression.class);
            inferConstantTypeMethod.setAccessible(true);

            // Create a constant expression with a custom object type that won't match any
            // instanceof checks
            Object customObject =
                    new Object() {
                        @Override
                        public String toString() {
                            return "CustomObject";
                        }
                    };
            ConstantExpression expr = new ConstantExpression(customObject);
            ClassNode customType = new ClassNode("CustomType", 1, ClassHelper.OBJECT_TYPE);
            expr.setType(customType);

            // when
            ClassNode type = (ClassNode) inferConstantTypeMethod.invoke(typeInferenceService, expr);

            // then - Should return the custom type from the expression
            assertThat(type).isEqualTo(customType);

        } catch (Exception e) {
            throw new RuntimeException("Test failed", e);
        }
    }
}
