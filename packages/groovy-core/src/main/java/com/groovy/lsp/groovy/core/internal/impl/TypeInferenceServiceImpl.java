package com.groovy.lsp.groovy.core.internal.impl;

import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.groovy.core.api.TypeInferenceService;
import java.util.List;
import java.util.Objects;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.Variable;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.SourceUnit;

/**
 * Internal implementation of TypeInferenceService.
 * Wraps Groovy's type inference capabilities for LSP usage.
 */
public class TypeInferenceServiceImpl implements TypeInferenceService {

    private final ASTService astService;

    public TypeInferenceServiceImpl(ASTService astService) {
        this.astService = Objects.requireNonNull(astService, "ASTService cannot be null");
    }

    /**
     * Infers the type of an expression at a specific position.
     *
     * @param sourceCode the source code
     * @param sourceName the source name
     * @param line the line number (1-based)
     * @param column the column number (1-based)
     * @return the inferred ClassNode or null if type cannot be determined
     */
    @Override
    public ClassNode inferTypeAtPosition(
            String sourceCode, String sourceName, int line, int column) {
        ModuleNode moduleNode = astService.parseSource(sourceCode, sourceName);
        if (moduleNode == null) {
            return ClassHelper.OBJECT_TYPE;
        }

        ASTNode node = astService.findNodeAtPosition(moduleNode, line, column);
        if (node instanceof Expression expression) {
            return inferExpressionType(expression, moduleNode);
        }

        return ClassHelper.OBJECT_TYPE;
    }

    /**
     * Infers the type of an expression.
     *
     * @param expression the expression to analyze
     * @param moduleNode the module context
     * @return the inferred ClassNode or null
     */
    @Override
    public ClassNode inferExpressionType(Expression expression, ModuleNode moduleNode) {
        if (expression == null) {
            return ClassHelper.OBJECT_TYPE;
        }

        // Check if type is already set
        ClassNode existingType = expression.getType();
        if (existingType != null && !existingType.equals(ClassHelper.OBJECT_TYPE)) {
            // For method calls, we should compute the type instead of using pre-set type
            if (!(expression instanceof MethodCallExpression)) {
                return existingType;
            }
        }

        // Handle different expression types
        if (expression instanceof VariableExpression variableExpression) {
            return inferVariableType(variableExpression, moduleNode);
        } else if (expression instanceof ConstantExpression constantExpression) {
            return inferConstantType(constantExpression);
        } else if (expression instanceof MethodCallExpression methodCallExpression) {
            return inferMethodCallType(methodCallExpression, moduleNode);
        } else if (expression instanceof PropertyExpression propertyExpression) {
            return inferPropertyType(propertyExpression, moduleNode);
        } else if (expression instanceof BinaryExpression binaryExpression) {
            return inferBinaryExpressionType(binaryExpression, moduleNode);
        } else if (expression instanceof ListExpression) {
            return ClassHelper.LIST_TYPE;
        } else if (expression instanceof MapExpression) {
            return ClassHelper.MAP_TYPE;
        }

        // Default to Object type
        return ClassHelper.OBJECT_TYPE;
    }

    /**
     * Infers the type of a variable.
     *
     * @param varExpr the variable expression
     * @param moduleNode the module context
     * @return the inferred type or null
     */
    private ClassNode inferVariableType(VariableExpression varExpr, ModuleNode moduleNode) {
        String varName = varExpr.getName();

        // Check if it's a special variable
        if ("this".equals(varName)) {
            ClassNode classNode = findEnclosingClass(varExpr, moduleNode);
            return classNode != null ? classNode : ClassHelper.OBJECT_TYPE;
        }

        // Look for variable declaration
        Variable accessedVar = varExpr.getAccessedVariable();
        if (accessedVar != null && accessedVar.getType() != null) {
            return accessedVar.getType();
        }

        // Try to find declaration in scope
        ClassNode declarationType = findVariableDeclarationType(varName, moduleNode);
        if (declarationType != null) {
            return declarationType;
        }

        return ClassHelper.OBJECT_TYPE;
    }

    /**
     * Infers the type of a constant expression.
     *
     * @param constExpr the constant expression
     * @return the inferred type
     */
    private ClassNode inferConstantType(ConstantExpression constExpr) {
        Object value = constExpr.getValue();
        if (value == null) {
            return ClassHelper.OBJECT_TYPE;
        }

        if (value instanceof String) {
            return ClassHelper.STRING_TYPE;
        } else if (value instanceof Integer) {
            return ClassHelper.int_TYPE;
        } else if (value instanceof Long) {
            return ClassHelper.long_TYPE;
        } else if (value instanceof Double) {
            return ClassHelper.double_TYPE;
        } else if (value instanceof Float) {
            return ClassHelper.float_TYPE;
        } else if (value instanceof Boolean) {
            return ClassHelper.boolean_TYPE;
        }

        // For other types, use the constant expression's type if available
        ClassNode type = constExpr.getType();
        if (type != null && !type.equals(ClassHelper.OBJECT_TYPE)) {
            return type;
        }

        return ClassHelper.make(value.getClass());
    }

    /**
     * Infers the type of a method call.
     *
     * @param call the method call expression
     * @param moduleNode the module context
     * @return the inferred return type or null
     */
    private ClassNode inferMethodCallType(MethodCallExpression call, ModuleNode moduleNode) {
        Expression objectExpr = call.getObjectExpression();
        ClassNode receiverType = inferExpressionType(objectExpr, moduleNode);

        if (receiverType == null) {
            return ClassHelper.OBJECT_TYPE;
        }

        String methodName = call.getMethodAsString();
        if (methodName == null) {
            return ClassHelper.OBJECT_TYPE;
        }

        // Look for method in receiver type
        List<MethodNode> methods = receiverType.getMethods(methodName);
        if (!methods.isEmpty()) {
            // Return type of first matching method
            return methods.get(0).getReturnType();
        }

        // Check for getter/setter patterns
        if (methodName.startsWith("get") && methodName.length() > 3) {
            String propertyName =
                    Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
            PropertyNode property = receiverType.getProperty(propertyName);
            if (property != null) {
                return property.getType();
            }
        }

        return ClassHelper.OBJECT_TYPE;
    }

    /**
     * Infers the type of a property expression.
     *
     * @param propExpr the property expression
     * @param moduleNode the module context
     * @return the inferred type or null
     */
    private ClassNode inferPropertyType(PropertyExpression propExpr, ModuleNode moduleNode) {
        Expression objectExpr = propExpr.getObjectExpression();
        ClassNode receiverType = inferExpressionType(objectExpr, moduleNode);

        if (receiverType == null) {
            return ClassHelper.OBJECT_TYPE;
        }

        String propertyName = propExpr.getPropertyAsString();
        if (propertyName == null) {
            return ClassHelper.OBJECT_TYPE;
        }

        // Look for property in receiver type
        PropertyNode property = receiverType.getProperty(propertyName);
        if (property != null) {
            return property.getType();
        }

        // Look for field
        FieldNode field = receiverType.getField(propertyName);
        if (field != null) {
            return field.getType();
        }

        // Look for getter method
        String getterName =
                "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        List<MethodNode> getters = receiverType.getMethods(getterName);
        if (!getters.isEmpty()) {
            return getters.get(0).getReturnType();
        }

        return ClassHelper.OBJECT_TYPE;
    }

    /**
     * Infers the type of a binary expression.
     *
     * @param binExpr the binary expression
     * @param moduleNode the module context
     * @return the inferred type
     */
    private ClassNode inferBinaryExpressionType(BinaryExpression binExpr, ModuleNode moduleNode) {
        int op = binExpr.getOperation().getType();

        // Comparison operators return boolean
        if (isComparisonOperator(op)) {
            return ClassHelper.boolean_TYPE;
        }

        // For arithmetic operators, return the type of the left operand
        // (simplified - real type inference would be more complex)
        Expression leftExpr = binExpr.getLeftExpression();
        return inferExpressionType(leftExpr, moduleNode);
    }

    /**
     * Checks if an operator is a comparison operator.
     *
     * @param operatorType the operator type
     * @return true if it's a comparison operator
     */
    private boolean isComparisonOperator(int operatorType) {
        return operatorType == org.codehaus.groovy.syntax.Types.COMPARE_EQUAL
                || operatorType == org.codehaus.groovy.syntax.Types.COMPARE_NOT_EQUAL
                || operatorType == org.codehaus.groovy.syntax.Types.COMPARE_LESS_THAN
                || operatorType == org.codehaus.groovy.syntax.Types.COMPARE_LESS_THAN_EQUAL
                || operatorType == org.codehaus.groovy.syntax.Types.COMPARE_GREATER_THAN
                || operatorType == org.codehaus.groovy.syntax.Types.COMPARE_GREATER_THAN_EQUAL;
    }

    /**
     * Finds the enclosing class of a node.
     *
     * @param node the AST node
     * @param moduleNode the module node
     * @return the enclosing ClassNode or null
     */
    private ClassNode findEnclosingClass(ASTNode node, ModuleNode moduleNode) {
        for (ClassNode classNode : moduleNode.getClasses()) {
            if (isNodeWithinClass(node, classNode)) {
                return classNode;
            }
        }
        return ClassHelper.OBJECT_TYPE;
    }

    /**
     * Checks if a node is within a class.
     *
     * @param node the AST node
     * @param classNode the class node
     * @return true if the node is within the class
     */
    private boolean isNodeWithinClass(ASTNode node, ClassNode classNode) {
        return node.getLineNumber() >= classNode.getLineNumber()
                && node.getLineNumber() <= classNode.getLastLineNumber();
    }

    /**
     * Finds the declaration type of a variable.
     *
     * @param varName the variable name
     * @param moduleNode the module node
     * @return the declaration type or null
     */
    private ClassNode findVariableDeclarationType(String varName, ModuleNode moduleNode) {
        VariableDeclarationFinder finder = new VariableDeclarationFinder(varName);

        // Visit classes
        for (ClassNode classNode : moduleNode.getClasses()) {
            classNode.visitContents(finder);
        }

        // Visit script body (statements outside classes)
        BlockStatement statementBlock = moduleNode.getStatementBlock();
        if (statementBlock != null) {
            for (Statement stmt : statementBlock.getStatements()) {
                if (stmt instanceof ExpressionStatement exprStmt) {
                    Expression expr = exprStmt.getExpression();
                    if (expr instanceof DeclarationExpression) {
                        expr.visit(finder);
                    }
                }
            }
        }

        return finder.getVariableType();
    }

    /**
     * Visitor for finding variable declarations.
     */
    private class VariableDeclarationFinder extends ClassCodeVisitorSupport {
        private final String targetVarName;
        private ClassNode variableType = null;

        public VariableDeclarationFinder(String varName) {
            this.targetVarName = varName;
        }

        @Override
        protected SourceUnit getSourceUnit() {
            // This method is not called in the context of this visitor
            // as we're only traversing the AST without compilation
            throw new UnsupportedOperationException("SourceUnit not available in this context");
        }

        @Override
        public void visitDeclarationExpression(DeclarationExpression expression) {
            Expression leftExpr = expression.getLeftExpression();
            if (leftExpr instanceof VariableExpression varExpr) {
                if (targetVarName.equals(varExpr.getName())) {
                    variableType = varExpr.getType();
                    if (variableType == null || variableType.equals(ClassHelper.OBJECT_TYPE)) {
                        // Try to infer from right side
                        Expression rightExpr = expression.getRightExpression();
                        if (rightExpr instanceof ConstantExpression constantExpression) {
                            // Use inferConstantType method to properly infer the type
                            variableType = inferConstantType(constantExpression);
                        } else if (rightExpr != null) {
                            // For other expression types, try to infer their type
                            variableType = inferExpressionType(rightExpr, null);
                        }
                    }
                }
            }
            super.visitDeclarationExpression(expression);
        }

        public ClassNode getVariableType() {
            return variableType != null ? variableType : ClassHelper.OBJECT_TYPE;
        }
    }
}
