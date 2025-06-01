package com.groovy.lsp.groovy.core.api;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.jmolecules.ddd.annotation.Service;

/**
 * Service interface for Groovy type inference.
 * Provides methods to infer types of expressions and variables.
 *
 * This is a domain service that implements type inference logic
 * for Groovy's dynamic type system.
 */
@Service
public interface TypeInferenceService {

    /**
     * Infers the type at a specific position in the source code.
     *
     * @param sourceCode the source code to analyze
     * @param sourceName the name of the source
     * @param line the line number (1-based)
     * @param column the column number (1-based)
     * @return the inferred type or Object type if unable to infer
     */
    ClassNode inferTypeAtPosition(String sourceCode, String sourceName, int line, int column);

    /**
     * Infers the type of a given expression.
     *
     * @param expression the expression to analyze
     * @param moduleNode the module containing the expression
     * @return the inferred type or Object type if unable to infer
     */
    ClassNode inferExpressionType(Expression expression, ModuleNode moduleNode);
}
