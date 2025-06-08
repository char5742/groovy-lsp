package com.groovy.lsp.protocol.internal.handler;

import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.protocol.api.IServiceRouter;
import com.groovy.lsp.protocol.internal.document.DocumentManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
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
                        // TODO: Get WorkspaceIndexService when circular dependency is resolved
                        // WorkspaceIndexService indexService =
                        // serviceRouter.getWorkspaceIndexService();

                        // Get document content
                        String sourceCode = documentManager.getDocumentContent(uri);
                        if (sourceCode == null) {
                            logger.debug("Document not found in document manager: {}", uri);
                            return Collections.emptyList();
                        }

                        // Parse the document
                        ModuleNode moduleNode = astService.parseSource(sourceCode, uri);
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
                                findReferences(node, moduleNode, uri, includeDeclaration, null);

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
            Object indexService) { // TODO: Change back to WorkspaceIndexService

        List<Location> references = new ArrayList<>();

        if (node instanceof VariableExpression) {
            references.addAll(
                    findVariableReferences(
                            (VariableExpression) node, moduleNode, currentUri, includeDeclaration));
        } else if (node instanceof MethodNode) {
            references.addAll(
                    findMethodReferences(
                            (MethodNode) node,
                            moduleNode,
                            currentUri,
                            includeDeclaration,
                            indexService));
        } else if (node instanceof MethodCallExpression) {
            references.addAll(
                    findMethodCallReferences(
                            (MethodCallExpression) node,
                            moduleNode,
                            currentUri,
                            includeDeclaration,
                            indexService));
        } else if (node instanceof ClassNode) {
            references.addAll(
                    findClassReferences(
                            (ClassNode) node,
                            moduleNode,
                            currentUri,
                            includeDeclaration,
                            indexService));
        } else if (node instanceof FieldNode) {
            references.addAll(
                    findFieldReferences(
                            (FieldNode) node,
                            moduleNode,
                            currentUri,
                            includeDeclaration,
                            indexService));
        } else if (node instanceof PropertyNode) {
            references.addAll(
                    findPropertyReferences(
                            (PropertyNode) node,
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
            Object indexService) { // TODO: Change back to WorkspaceIndexService

        String methodName = method.getName();
        return findMethodReferencesByName(
                methodName, moduleNode, currentUri, includeDeclaration, indexService);
    }

    private List<Location> findMethodCallReferences(
            MethodCallExpression methodCall,
            ModuleNode moduleNode,
            String currentUri,
            boolean includeDeclaration,
            Object indexService) { // TODO: Change back to WorkspaceIndexService

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
            Object indexService) { // TODO: Change back to WorkspaceIndexService

        List<Location> references = new ArrayList<>();

        // Find references in current file
        MethodReferenceVisitor visitor =
                new MethodReferenceVisitor(methodName, currentUri, references, includeDeclaration);
        moduleNode.getClasses().forEach(classNode -> classNode.visitContents(visitor));

        // TODO: Search in workspace using indexService
        // This would require an enhanced workspace index that tracks references
        // For now, we only search in the current file

        return references;
    }

    private List<Location> findClassReferences(
            ClassNode classNode,
            ModuleNode moduleNode,
            String currentUri,
            boolean includeDeclaration,
            Object indexService) { // TODO: Change back to WorkspaceIndexService

        String className = classNode.getName();
        List<Location> references = new ArrayList<>();

        // Find references in current file
        ClassReferenceVisitor visitor =
                new ClassReferenceVisitor(className, currentUri, references);
        moduleNode.getClasses().forEach(cn -> cn.visitContents(visitor));

        // TODO: Search in workspace

        return references;
    }

    private List<Location> findFieldReferences(
            FieldNode field,
            ModuleNode moduleNode,
            String currentUri,
            boolean includeDeclaration,
            Object indexService) { // TODO: Change back to WorkspaceIndexService

        return findPropertyOrFieldReferences(
                field.getName(), moduleNode, currentUri, includeDeclaration, indexService);
    }

    private List<Location> findPropertyReferences(
            PropertyNode property,
            ModuleNode moduleNode,
            String currentUri,
            boolean includeDeclaration,
            Object indexService) { // TODO: Change back to WorkspaceIndexService

        return findPropertyOrFieldReferences(
                property.getName(), moduleNode, currentUri, includeDeclaration, indexService);
    }

    private List<Location> findPropertyOrFieldReferences(
            String name,
            ModuleNode moduleNode,
            String currentUri,
            boolean includeDeclaration,
            Object indexService) { // TODO: Change back to WorkspaceIndexService

        List<Location> references = new ArrayList<>();

        // Find references in current file
        PropertyReferenceVisitor visitor =
                new PropertyReferenceVisitor(name, currentUri, references);
        moduleNode.getClasses().forEach(classNode -> classNode.visitContents(visitor));

        // TODO: Search in workspace

        return references;
    }

    private @Nullable Location createLocation(String uri, ASTNode node) {
        if (node.getLineNumber() < 0 || node.getColumnNumber() < 0) {
            return null;
        }

        Range range =
                new Range(
                        new Position(node.getLineNumber() - 1, node.getColumnNumber() - 1),
                        new Position(node.getLastLineNumber() - 1, node.getLastColumnNumber() - 1));

        return new Location(uri, range);
    }

    /**
     * Visitor to find variable references
     */
    private class ReferenceVisitor extends ClassCodeVisitorSupport {
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
                Location location = createLocation(uri, expression);
                if (location != null) {
                    references.add(location);
                }
            }
            super.visitVariableExpression(expression);
        }

        @Override
        protected SourceUnit getSourceUnit() {
            return null; // Not needed for our use case
        }
    }

    /**
     * Visitor to find method references
     */
    private class MethodReferenceVisitor extends ClassCodeVisitorSupport {
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
                Location location = createLocation(uri, method);
                if (location != null) {
                    references.add(location);
                }
            }
            super.visitMethod(method);
        }

        @Override
        public void visitMethodCallExpression(MethodCallExpression call) {
            if (targetName.equals(call.getMethodAsString())) {
                Location location = createLocation(uri, call);
                if (location != null) {
                    references.add(location);
                }
            }
            super.visitMethodCallExpression(call);
        }

        @Override
        protected SourceUnit getSourceUnit() {
            return null;
        }
    }

    /**
     * Visitor to find class references
     */
    private class ClassReferenceVisitor extends ClassCodeVisitorSupport {
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
                Location location = createLocation(uri, expression);
                if (location != null) {
                    references.add(location);
                }
            }
            super.visitClassExpression(expression);
        }

        @Override
        public void visitConstructorCallExpression(ConstructorCallExpression call) {
            if (targetName.equals(call.getType().getName())) {
                Location location = createLocation(uri, call);
                if (location != null) {
                    references.add(location);
                }
            }
            super.visitConstructorCallExpression(call);
        }

        @Override
        protected SourceUnit getSourceUnit() {
            return null;
        }
    }

    /**
     * Visitor to find property/field references
     */
    private class PropertyReferenceVisitor extends ClassCodeVisitorSupport {
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
                Location location = createLocation(uri, expression);
                if (location != null) {
                    references.add(location);
                }
            }
            super.visitPropertyExpression(expression);
        }

        @Override
        protected SourceUnit getSourceUnit() {
            return null;
        }
    }
}
