package com.groovy.lsp.workspace.internal.parser;

import com.groovy.lsp.workspace.api.dto.SymbolInfo;
import com.groovy.lsp.workspace.api.dto.SymbolKind;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ConstructorNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.control.SourceUnit;
import org.jspecify.annotations.Nullable;

/**
 * AST visitor that extracts symbol information from Groovy AST nodes.
 */
public class SymbolExtractorVisitor extends ClassCodeVisitorSupport {
    private final Path file;
    private final List<SymbolInfo> symbols = new ArrayList<>();
    private final Set<String> visitedClasses = new HashSet<>();
    @Nullable private String currentClassName = null;

    public SymbolExtractorVisitor(Path file) {
        this.file = file;
    }

    /**
     * Process a class node directly (not through visitor pattern).
     * This is called before visiting the class contents.
     */
    public void processClassNode(ClassNode node) {
        if (node.isScript()) {
            // Skip script class nodes
            return;
        }

        String className = node.getName();

        // Prevent duplicate class processing
        if (visitedClasses.contains(className)) {
            return;
        }
        visitedClasses.add(className);

        currentClassName = className;

        // Ensure valid line and column numbers (minimum 1)
        int lineNumber = Math.max(1, node.getLineNumber());
        int columnNumber = Math.max(1, node.getColumnNumber());

        symbols.add(
                new SymbolInfo(
                        className, determineClassSymbolKind(node), file, lineNumber, columnNumber));
    }

    @Override
    public void visitClass(ClassNode node) {
        // This method is called when nested classes are encountered
        // Process the nested class the same way as top-level classes
        processClassNode(node);
        super.visitClass(node);
    }

    @Override
    public void visitConstructor(ConstructorNode node) {
        symbols.add(
                new SymbolInfo(
                        currentClassName + ".<init>",
                        SymbolKind.CONSTRUCTOR,
                        file,
                        Math.max(1, node.getLineNumber()),
                        Math.max(1, node.getColumnNumber())));
        super.visitConstructor(node);
    }

    @Override
    public void visitMethod(MethodNode node) {
        if (node.isAbstract() || node.isSynthetic()) {
            return;
        }

        String methodName = node.getName();
        if (currentClassName != null) {
            methodName = currentClassName + "." + methodName;
        }

        symbols.add(
                new SymbolInfo(
                        methodName,
                        SymbolKind.METHOD,
                        file,
                        Math.max(1, node.getLineNumber()),
                        Math.max(1, node.getColumnNumber())));
        super.visitMethod(node);
    }

    @Override
    public void visitField(FieldNode node) {
        if (node.isSynthetic()) {
            return;
        }

        String fieldName = node.getName();
        if (currentClassName != null) {
            fieldName = currentClassName + "." + fieldName;
        }

        symbols.add(
                new SymbolInfo(
                        fieldName,
                        SymbolKind.FIELD,
                        file,
                        Math.max(1, node.getLineNumber()),
                        Math.max(1, node.getColumnNumber())));
        super.visitField(node);
    }

    @Override
    public void visitProperty(PropertyNode node) {
        String propertyName = node.getName();
        if (currentClassName != null) {
            propertyName = currentClassName + "." + propertyName;
        }

        symbols.add(
                new SymbolInfo(
                        propertyName,
                        SymbolKind.PROPERTY,
                        file,
                        Math.max(1, node.getLineNumber()),
                        Math.max(1, node.getColumnNumber())));
        super.visitProperty(node);
    }

    @Override
    public void visitClosureExpression(ClosureExpression expression) {
        // Closures can be treated as anonymous methods
        String closureName =
                currentClassName != null ? currentClassName + ".<closure>" : "<closure>";

        symbols.add(
                new SymbolInfo(
                        closureName,
                        SymbolKind.CLOSURE,
                        file,
                        Math.max(1, expression.getLineNumber()),
                        Math.max(1, expression.getColumnNumber())));
        super.visitClosureExpression(expression);
    }

    @Override
    public void visitVariableExpression(VariableExpression expression) {
        // Skip variable expressions for now - they are too numerous
        // and not typically needed for workspace indexing
        // If needed in the future, can be enabled with filtering
    }

    @Override
    @Nullable
    protected SourceUnit getSourceUnit() {
        // Return null as we don't have access to SourceUnit in this context
        return null;
    }

    /**
     * Determine the symbol kind for a class node.
     */
    private SymbolKind determineClassSymbolKind(ClassNode node) {
        if (node.isInterface()) {
            return SymbolKind.INTERFACE;
        } else if (node.isEnum()) {
            return SymbolKind.ENUM;
        } else if (node.isAnnotationDefinition()) {
            return SymbolKind.ANNOTATION;
        } else if (isTrait(node)) {
            return SymbolKind.TRAIT;
        } else {
            return SymbolKind.CLASS;
        }
    }

    /**
     * Check if a class node represents a trait.
     */
    private boolean isTrait(ClassNode node) {
        // In Groovy, traits are compiled to interfaces with helper classes
        // We can identify traits by checking:
        // 1. If this interface has associated trait helper classes
        // 2. By the trait annotation markers

        if (!node.isInterface()) {
            return false;
        }

        // Check for @groovy.transform.Trait annotation
        if (node.getAnnotations().stream()
                .anyMatch(ann -> ann.getClassNode().getName().equals("groovy.transform.Trait"))) {
            return true;
        }

        // Check if this interface has the groovy.lang.Trait marker
        if (Arrays.stream(node.getInterfaces())
                .anyMatch(iface -> iface.getName().equals("groovy.lang.Trait"))) {
            return true;
        }

        // As a fallback, check if there's a helper class pattern in the same module
        String className = node.getName();
        if (node.getModule() != null) {
            return node.getModule().getClasses().stream()
                    .anyMatch(c -> c.getName().equals(className + "$Trait$Helper"));
        }

        return false;
    }

    /**
     * Get the collected symbols.
     */
    public List<SymbolInfo> getSymbols() {
        return new ArrayList<>(symbols);
    }
}
