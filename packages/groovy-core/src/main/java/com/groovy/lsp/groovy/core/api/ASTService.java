package com.groovy.lsp.groovy.core.api;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.jmolecules.ddd.annotation.Service;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Service interface for Groovy AST operations.
 * Provides methods for parsing, traversing, and analyzing Groovy AST.
 * 
 * This is a domain service that encapsulates the complexity of working
 * with Groovy's Abstract Syntax Tree.
 */
@Service
public interface ASTService {
    
    /**
     * Parses Groovy source code and returns the AST.
     * 
     * @param sourceCode the source code to parse
     * @param sourceName the name of the source (e.g., filename)
     * @return the parsed ModuleNode or null if parsing failed
     */
    @Nullable ModuleNode parseSource(String sourceCode, String sourceName);
    
    /**
     * Parses Groovy source code with custom compiler configuration.
     * 
     * @param sourceCode the source code to parse
     * @param sourceName the name of the source
     * @param config the compiler configuration to use
     * @return the parsed ModuleNode or null if parsing failed
     */
    @Nullable ModuleNode parseSource(String sourceCode, String sourceName, CompilerConfiguration config);
    
    /**
     * Finds the AST node at the specified position.
     * 
     * @param moduleNode the module to search in
     * @param line the line number (1-based)
     * @param column the column number (1-based)
     * @return the node at the position or null if not found
     */
    @Nullable ASTNode findNodeAtPosition(ModuleNode moduleNode, int line, int column);
    
    /**
     * Finds all variable expressions in the module.
     * 
     * @param moduleNode the module to search in
     * @return list of all variable expressions
     */
    List<VariableExpression> findAllVariables(ModuleNode moduleNode);
    
    /**
     * Finds all method call expressions in the module.
     * 
     * @param moduleNode the module to search in
     * @return list of all method call expressions
     */
    List<MethodCallExpression> findAllMethodCalls(ModuleNode moduleNode);
}