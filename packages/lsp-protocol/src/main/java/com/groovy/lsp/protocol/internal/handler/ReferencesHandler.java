package com.groovy.lsp.protocol.internal.handler;

import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.groovy.core.api.CompilerConfigurationService;
import com.groovy.lsp.protocol.api.IServiceRouter;
import com.groovy.lsp.protocol.internal.document.DocumentManager;
import com.groovy.lsp.protocol.internal.util.LocationUtils;
import com.groovy.lsp.shared.workspace.api.WorkspaceIndexService;
import com.groovy.lsp.shared.workspace.api.dto.SymbolInfo;
import com.groovy.lsp.shared.workspace.api.dto.SymbolKind;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.control.SourceUnit;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceParams;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles references requests for Groovy documents.
 *
 * This handler provides find-all-references functionality:
 * - Finding all references to variables
 * - Finding all references to methods
 * - Finding all references to classes
 * - Finding all references to fields/properties
 * - Cross-file reference search support
 */
public class ReferencesHandler {

    private static final Logger logger = LoggerFactory.getLogger(ReferencesHandler.class);

    private final IServiceRouter serviceRouter;
    private final DocumentManager documentManager;

    public ReferencesHandler(IServiceRouter serviceRouter, DocumentManager documentManager) {
        this.serviceRouter = serviceRouter;
        this.documentManager = documentManager;
    }

    public CompletableFuture<List<? extends Location>> handleReferences(ReferenceParams params) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        String uri = params.getTextDocument().getUri();
                        Position position = params.getPosition();
                        boolean includeDeclaration = params.getContext().isIncludeDeclaration();

                        logger.debug(
                                "Processing references request at {}:{}:{} (includeDeclaration:"
                                        + " {})",
                                uri,
                                position.getLine(),
                                position.getCharacter(),
                                includeDeclaration);

                        // Get services
                        ASTService astService = serviceRouter.getAstService();
                        CompilerConfigurationService configService =
                                serviceRouter.getCompilerConfigurationService();
                        WorkspaceIndexService indexService =
                                serviceRouter.getWorkspaceIndexService();

                        // Get document content
                        String sourceCode = documentManager.getDocumentContent(uri);
                        if (sourceCode == null) {
                            logger.debug("Document not found in document manager: {}", uri);
                            return Collections.emptyList();
                        }

                        // Parse the document with workspace-aware compiler configuration
                        ModuleNode moduleNode =
                                astService.parseSource(
                                        sourceCode,
                                        uri,
                                        configService.createDefaultConfiguration());
                        if (moduleNode == null) {
                            logger.debug("Failed to parse module for {}", uri);
                            return Collections.emptyList();
                        }

                        // Find node at position
                        ASTNode node =
                                astService.findNodeAtPosition(
                                        moduleNode,
                                        position.getLine() + 1,
                                        position.getCharacter() + 1);

                        if (node == null) {
                            logger.debug(
                                    "No node found at position {}:{}",
                                    position.getLine(),
                                    position.getCharacter());
                            return Collections.emptyList();
                        }

                        // Find references
                        List<Location> references =
                                findReferences(
                                        node, moduleNode, uri, includeDeclaration, indexService);

                        return references;

                    } catch (Exception e) {
                        logger.error(
                                "Error processing references request for URI: {} at position {}:{}",
                                params.getTextDocument().getUri(),
                                params.getPosition().getLine(),
                                params.getPosition().getCharacter(),
                                e);
                        return Collections.emptyList();
                    }
                });
    }

    private List<Location> findReferences(
            ASTNode node,
            ModuleNode moduleNode,
            String currentUri,
            boolean includeDeclaration,
            WorkspaceIndexService indexService) {

        List<Location> references = new ArrayList<>();

        if (node instanceof VariableExpression variableExpression) {
            references.addAll(
                    findVariableReferences(
                            variableExpression, moduleNode, currentUri, includeDeclaration));
        } else if (node instanceof MethodNode methodNode) {
            references.addAll(
                    findMethodReferences(
                            methodNode, moduleNode, currentUri, includeDeclaration, indexService));
        } else if (node instanceof MethodCallExpression methodCallExpression) {
            references.addAll(
                    findMethodCallReferences(
                            methodCallExpression,
                            moduleNode,
                            currentUri,
                            includeDeclaration,
                            indexService));
        } else if (node instanceof ClassNode classNode) {
            references.addAll(
                    findClassReferences(
                            classNode, moduleNode, currentUri, includeDeclaration, indexService));
        } else if (node instanceof FieldNode fieldNode) {
            references.addAll(
                    findFieldReferences(
                            fieldNode, moduleNode, currentUri, includeDeclaration, indexService));
        } else if (node instanceof PropertyNode propertyNode) {
            references.addAll(
                    findPropertyReferences(
                            propertyNode,
                            moduleNode,
                            currentUri,
                            includeDeclaration,
                            indexService));
        }

        return references;
    }

    private List<Location> findVariableReferences(
            VariableExpression varExpr,
            ModuleNode moduleNode,
            String currentUri,
            boolean includeDeclaration) {
        // Check includeDeclaration to avoid unused parameter warning
        if (includeDeclaration) {
            // Currently this implementation doesn't distinguish between declaration inclusion
            // but the parameter is kept for future enhancement and API consistency
        }

        String varName = varExpr.getName();
        List<Location> references = new ArrayList<>();

        // Use a visitor to find all references in the current file
        ReferenceVisitor visitor = new ReferenceVisitor(varName, currentUri, references);
        moduleNode.getClasses().forEach(classNode -> classNode.visitContents(visitor));

        return references;
    }

    private List<Location> findMethodReferences(
            MethodNode method,
            ModuleNode moduleNode,
            String currentUri,
            boolean includeDeclaration,
            WorkspaceIndexService indexService) {

        String methodName = method.getName();
        return findMethodReferencesByName(
                methodName, moduleNode, currentUri, includeDeclaration, indexService);
    }

    private List<Location> findMethodCallReferences(
            MethodCallExpression methodCall,
            ModuleNode moduleNode,
            String currentUri,
            boolean includeDeclaration,
            WorkspaceIndexService indexService) {

        String methodName = methodCall.getMethodAsString();
        if (methodName == null) {
            return Collections.emptyList();
        }

        return findMethodReferencesByName(
                methodName, moduleNode, currentUri, includeDeclaration, indexService);
    }

    private List<Location> findMethodReferencesByName(
            String methodName,
            ModuleNode moduleNode,
            String currentUri,
            boolean includeDeclaration,
            WorkspaceIndexService indexService) {

        List<Location> references = new ArrayList<>();

        // Find references in current file
        MethodReferenceVisitor visitor =
                new MethodReferenceVisitor(methodName, currentUri, references, includeDeclaration);
        moduleNode.getClasses().forEach(classNode -> classNode.visitContents(visitor));

        // Search in workspace using indexService
        if (indexService != null) {
            try {
                List<SymbolInfo> symbols =
                        indexService
                                .searchSymbols(methodName)
                                .get()
                                .filter(symbol -> symbol.kind() == SymbolKind.METHOD)
                                .collect(Collectors.toList());

                for (SymbolInfo symbol : symbols) {
                    Location location = createLocation(symbol);
                    if (location != null) {
                        references.add(location);
                    }
                }
            } catch (Exception e) {
                logger.warn(
                        "Error searching workspace index for method references: {}. Falling back to"
                                + " local search only.",
                        methodName,
                        e);
                // フォールバック処理：ワークスペース検索が失敗した場合は、ローカル検索結果のみを使用
            }
        }

        return references;
    }

    private List<Location> findClassReferences(
            ClassNode classNode,
            ModuleNode moduleNode,
            String currentUri,
            boolean includeDeclaration,
            WorkspaceIndexService indexService) {
        // Check includeDeclaration to avoid unused parameter warning
        if (includeDeclaration) {
            // Currently this implementation doesn't distinguish between declaration inclusion
            // but the parameter is kept for future enhancement and API consistency
        }

        String className = classNode.getName();
        List<Location> references = new ArrayList<>();

        // Find references in current file
        ClassReferenceVisitor visitor =
                new ClassReferenceVisitor(className, currentUri, references);
        moduleNode.getClasses().forEach(cn -> cn.visitContents(visitor));

        // Search in workspace
        if (indexService != null) {
            try {
                List<SymbolInfo> symbols =
                        indexService
                                .searchSymbols(className)
                                .get()
                                .filter(
                                        symbol ->
                                                symbol.kind() == SymbolKind.CLASS
                                                        || symbol.kind() == SymbolKind.INTERFACE)
                                .collect(Collectors.toList());

                for (SymbolInfo symbol : symbols) {
                    // Match fully qualified name or simple name with strict matching
                    String symbolName = symbol.name();
                    boolean isExactMatch = symbolName.equals(className);
                    boolean isQualifiedMatch = false;

                    if (!isExactMatch && symbolName.contains(".")) {
                        // Ensure we match the exact class name after the last dot
                        int lastDotIndex = symbolName.lastIndexOf('.');
                        String simpleSymbolName = symbolName.substring(lastDotIndex + 1);
                        isQualifiedMatch = simpleSymbolName.equals(className);
                    }

                    if (isExactMatch || isQualifiedMatch) {
                        Location location = createLocation(symbol);
                        if (location != null) {
                            references.add(location);
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn(
                        "Error searching workspace index for class references: {}. Falling back to"
                                + " local search only.",
                        className,
                        e);
                // フォールバック処理：ワークスペース検索が失敗した場合は、ローカル検索結果のみを使用
            }
        }

        return references;
    }

    private List<Location> findFieldReferences(
            FieldNode field,
            ModuleNode moduleNode,
            String currentUri,
            boolean includeDeclaration,
            WorkspaceIndexService indexService) {

        return findPropertyOrFieldReferences(
                field.getName(), moduleNode, currentUri, includeDeclaration, indexService);
    }

    private List<Location> findPropertyReferences(
            PropertyNode property,
            ModuleNode moduleNode,
            String currentUri,
            boolean includeDeclaration,
            WorkspaceIndexService indexService) {

        return findPropertyOrFieldReferences(
                property.getName(), moduleNode, currentUri, includeDeclaration, indexService);
    }

    private List<Location> findPropertyOrFieldReferences(
            String name,
            ModuleNode moduleNode,
            String currentUri,
            boolean includeDeclaration,
            WorkspaceIndexService indexService) {
        // Check includeDeclaration to avoid unused parameter warning
        if (includeDeclaration) {
            // Currently this implementation doesn't distinguish between declaration inclusion
            // but the parameter is kept for future enhancement and API consistency
        }

        List<Location> references = new ArrayList<>();

        // Find references in current file
        PropertyReferenceVisitor visitor =
                new PropertyReferenceVisitor(name, currentUri, references);
        moduleNode.getClasses().forEach(classNode -> classNode.visitContents(visitor));

        // Search in workspace
        if (indexService != null) {
            try {
                List<SymbolInfo> symbols =
                        indexService
                                .searchSymbols(name)
                                .get()
                                .filter(
                                        symbol ->
                                                symbol.kind() == SymbolKind.FIELD
                                                        || symbol.kind() == SymbolKind.PROPERTY)
                                .collect(Collectors.toList());

                for (SymbolInfo symbol : symbols) {
                    Location location = createLocation(symbol);
                    if (location != null) {
                        references.add(location);
                    }
                }
            } catch (Exception e) {
                logger.warn(
                        "Error searching workspace index for property/field references: {}. Falling"
                                + " back to local search only.",
                        name,
                        e);
                // フォールバック処理：ワークスペース検索が失敗した場合は、ローカル検索結果のみを使用
            }
        }

        return references;
    }

    /**
     * Visitor to find variable references
     */
    private static class ReferenceVisitor extends ClassCodeVisitorSupport {
        private final String targetName;
        private final String uri;
        private final List<Location> references;

        public ReferenceVisitor(String targetName, String uri, List<Location> references) {
            this.targetName = targetName;
            this.uri = uri;
            this.references = references;
        }

        @Override
        public void visitVariableExpression(VariableExpression expression) {
            if (targetName.equals(expression.getName())) {
                Location location = LocationUtils.createLocation(uri, expression);
                if (location != null) {
                    references.add(location);
                }
            }
            super.visitVariableExpression(expression);
        }

        @Override
        protected @Nullable SourceUnit getSourceUnit() {
            return null; // Not needed for our use case
        }
    }

    /**
     * Visitor to find method references
     */
    private static class MethodReferenceVisitor extends ClassCodeVisitorSupport {
        private final String targetName;
        private final String uri;
        private final List<Location> references;
        private final boolean includeDeclaration;

        public MethodReferenceVisitor(
                String targetName,
                String uri,
                List<Location> references,
                boolean includeDeclaration) {
            this.targetName = targetName;
            this.uri = uri;
            this.references = references;
            this.includeDeclaration = includeDeclaration;
        }

        @Override
        public void visitMethod(MethodNode method) {
            if (includeDeclaration && targetName.equals(method.getName())) {
                Location location = LocationUtils.createLocation(uri, method);
                if (location != null) {
                    references.add(location);
                }
            }
            super.visitMethod(method);
        }

        @Override
        public void visitMethodCallExpression(MethodCallExpression call) {
            if (targetName.equals(call.getMethodAsString())) {
                Location location = LocationUtils.createLocation(uri, call);
                if (location != null) {
                    references.add(location);
                }
            }
            super.visitMethodCallExpression(call);
        }

        @Override
        protected @Nullable SourceUnit getSourceUnit() {
            return null;
        }
    }

    /**
     * Visitor to find class references
     */
    private static class ClassReferenceVisitor extends ClassCodeVisitorSupport {
        private final String targetName;
        private final String uri;
        private final List<Location> references;

        public ClassReferenceVisitor(String targetName, String uri, List<Location> references) {
            this.targetName = targetName;
            this.uri = uri;
            this.references = references;
        }

        @Override
        public void visitClassExpression(ClassExpression expression) {
            if (targetName.equals(expression.getType().getName())) {
                Location location = LocationUtils.createLocation(uri, expression);
                if (location != null) {
                    references.add(location);
                }
            }
            super.visitClassExpression(expression);
        }

        @Override
        public void visitConstructorCallExpression(ConstructorCallExpression call) {
            if (targetName.equals(call.getType().getName())) {
                Location location = LocationUtils.createLocation(uri, call);
                if (location != null) {
                    references.add(location);
                }
            }
            super.visitConstructorCallExpression(call);
        }

        @Override
        protected @Nullable SourceUnit getSourceUnit() {
            return null;
        }
    }

    /**
     * Visitor to find property/field references
     */
    private static class PropertyReferenceVisitor extends ClassCodeVisitorSupport {
        private final String targetName;
        private final String uri;
        private final List<Location> references;

        public PropertyReferenceVisitor(String targetName, String uri, List<Location> references) {
            this.targetName = targetName;
            this.uri = uri;
            this.references = references;
        }

        @Override
        public void visitPropertyExpression(PropertyExpression expression) {
            if (targetName.equals(expression.getPropertyAsString())) {
                Location location = LocationUtils.createLocation(uri, expression);
                if (location != null) {
                    references.add(location);
                }
            }
            super.visitPropertyExpression(expression);
        }

        @Override
        protected @Nullable SourceUnit getSourceUnit() {
            return null;
        }
    }

    private @Nullable Location createLocation(SymbolInfo symbol) {
        Path location = symbol.location();
        if (location == null) {
            logger.debug(
                    "Symbol location is null for symbol: {} (kind: {})",
                    symbol.name(),
                    symbol.kind());
            return null;
        }

        String uri = location.toUri().toString();
        Range range =
                new Range(
                        new Position(symbol.line() - 1, symbol.column() - 1),
                        new Position(symbol.line() - 1, symbol.column() - 1));

        logger.debug(
                "Created location for symbol: {} at {}:{}:{}",
                symbol.name(),
                uri,
                symbol.line(),
                symbol.column());
        return new Location(uri, range);
    }
}
