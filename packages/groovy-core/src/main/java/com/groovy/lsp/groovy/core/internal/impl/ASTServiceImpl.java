package com.groovy.lsp.groovy.core.internal.impl;

import com.groovy.lsp.groovy.core.api.ASTService;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.ErrorCollector;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.io.StringReaderSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal implementation of ASTService.
 * Provides methods for parsing, traversing, and analyzing Groovy AST.
 */
public class ASTServiceImpl implements ASTService {
    private static final Logger logger = LoggerFactory.getLogger(ASTServiceImpl.class);

    // Cache for parsed ASTs to improve performance
    private final Map<String, ModuleNode> astCache = new ConcurrentHashMap<>();

    /**
     * Parses Groovy source code and returns the AST.
     *
     * @param sourceCode the source code to parse
     * @param sourceName the name of the source (e.g., filename)
     * @return the parsed ModuleNode or null if parsing failed
     */
    @Override
    public @Nullable ModuleNode parseSource(String sourceCode, String sourceName) {
        return parseSource(
                sourceCode, sourceName, CompilerFactoryImpl.createDefaultConfigurationStatic());
    }

    /**
     * Parses Groovy source code with custom compiler configuration.
     *
     * @param sourceCode the source code to parse
     * @param sourceName the name of the source
     * @param config the compiler configuration to use
     * @return the parsed ModuleNode or null if parsing failed
     */
    @Override
    public @Nullable ModuleNode parseSource(
            String sourceCode, String sourceName, CompilerConfiguration config) {
        Objects.requireNonNull(sourceCode, "Source code cannot be null");
        Objects.requireNonNull(sourceName, "Source name cannot be null");
        Objects.requireNonNull(config, "Compiler configuration cannot be null");

        String cacheKey = sourceName + ":" + sourceCode.hashCode();

        // Check cache first
        ModuleNode cached = astCache.get(cacheKey);
        if (cached != null) {
            logger.debug("Returning cached AST for {}", sourceName);
            return cached;
        }

        try {
            CompilationUnit unit = new CompilationUnit(config);
            SourceUnit sourceUnit =
                    new SourceUnit(
                            sourceName,
                            new StringReaderSource(sourceCode, config),
                            config,
                            unit.getClassLoader(),
                            new ErrorCollector(config));

            unit.addSource(sourceUnit);
            unit.compile(Phases.SEMANTIC_ANALYSIS);

            ModuleNode moduleNode = sourceUnit.getAST();

            // Cache the result
            astCache.put(cacheKey, moduleNode);

            logger.debug("Successfully parsed source: {}", sourceName);
            return moduleNode;

        } catch (Exception e) {
            logger.error("Failed to parse source: {}", sourceName, e);
            return null;
        }
    }

    /**
     * Finds a node at the specified position in the AST.
     *
     * @param moduleNode the module node to search
     * @param line the line number (1-based)
     * @param column the column number (1-based)
     * @return the ASTNode at the position or null if not found
     */
    @Override
    public @Nullable ASTNode findNodeAtPosition(ModuleNode moduleNode, int line, int column) {
        if (moduleNode == null) {
            return null;
        }

        NodeFinder finder = new NodeFinder(line, column);
        moduleNode.visit(finder);
        return finder.getFoundNode();
    }

    /**
     * Finds all variable declarations in the AST.
     *
     * @param moduleNode the module node to search
     * @return list of variable expressions
     */
    @Override
    public List<VariableExpression> findAllVariables(ModuleNode moduleNode) {
        if (moduleNode == null) {
            return Collections.emptyList();
        }

        VariableCollector collector = new VariableCollector();
        moduleNode.visit(collector);
        return collector.getVariables();
    }

    /**
     * Finds all method calls in the AST.
     *
     * @param moduleNode the module node to search
     * @return list of method call expressions
     */
    @Override
    public List<MethodCallExpression> findAllMethodCalls(ModuleNode moduleNode) {
        if (moduleNode == null) {
            return Collections.emptyList();
        }

        MethodCallCollector collector = new MethodCallCollector();
        moduleNode.visit(collector);
        return collector.getMethodCalls();
    }

    /**
     * Clears the AST cache.
     */
    public void clearCache() {
        astCache.clear();
        logger.debug("AST cache cleared");
    }

    /**
     * Removes a specific entry from the cache.
     *
     * @param sourceName the source name to remove
     */
    public void invalidateCache(String sourceName) {
        astCache.entrySet().removeIf(entry -> entry.getKey().startsWith(sourceName + ":"));
        logger.debug("Invalidated cache entries for: {}", sourceName);
    }

    /**
     * Visitor for finding nodes at specific positions.
     */
    private static class NodeFinder extends ClassCodeVisitorSupport {
        private final int targetLine;
        private final int targetColumn;
        private @Nullable ASTNode foundNode = null;

        public NodeFinder(int line, int column) {
            this.targetLine = line;
            this.targetColumn = column;
        }

        @Override
        protected SourceUnit getSourceUnit() {
            // Not used in this visitor context
            throw new UnsupportedOperationException("SourceUnit not available in this context");
        }

        @Override
        public void visitStatement(Statement statement) {
            checkNode(statement);
            super.visitStatement(statement);
        }

        @Override
        public void visitMethodCallExpression(MethodCallExpression call) {
            checkNode(call);
            super.visitMethodCallExpression(call);
        }

        @Override
        public void visitBinaryExpression(BinaryExpression expression) {
            checkNode(expression);
            super.visitBinaryExpression(expression);
        }

        @Override
        public void visitVariableExpression(VariableExpression expression) {
            checkNode(expression);
            super.visitVariableExpression(expression);
        }

        private void checkNode(ASTNode node) {
            if (node.getLineNumber() <= targetLine && node.getLastLineNumber() >= targetLine) {
                if (node.getColumnNumber() <= targetColumn
                        && node.getLastColumnNumber() >= targetColumn) {
                    foundNode = node;
                }
            }
        }

        public @Nullable ASTNode getFoundNode() {
            return foundNode;
        }
    }

    /**
     * Visitor for collecting variable expressions.
     */
    private static class VariableCollector extends ClassCodeVisitorSupport {
        private final List<VariableExpression> variables = new ArrayList<>();

        @Override
        protected SourceUnit getSourceUnit() {
            // Not used in this visitor context
            throw new UnsupportedOperationException("SourceUnit not available in this context");
        }

        @Override
        public void visitVariableExpression(VariableExpression expression) {
            variables.add(expression);
            super.visitVariableExpression(expression);
        }

        public List<VariableExpression> getVariables() {
            return new ArrayList<>(variables);
        }
    }

    /**
     * Visitor for collecting method call expressions.
     */
    private static class MethodCallCollector extends ClassCodeVisitorSupport {
        private final List<MethodCallExpression> methodCalls = new ArrayList<>();

        @Override
        protected SourceUnit getSourceUnit() {
            // Not used in this visitor context
            throw new UnsupportedOperationException("SourceUnit not available in this context");
        }

        @Override
        public void visitMethodCallExpression(MethodCallExpression call) {
            methodCalls.add(call);
            super.visitMethodCallExpression(call);
        }

        public List<MethodCallExpression> getMethodCalls() {
            return new ArrayList<>(methodCalls);
        }
    }
}
