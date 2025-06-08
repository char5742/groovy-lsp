package com.groovy.lsp.protocol.internal.handler;

import com.groovy.lsp.groovy.core.api.ASTService;
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
import org.codehaus.groovy.ast.DynamicVariable;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.Variable;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.CatchStatement;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.control.SourceUnit;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles definition requests for Groovy documents.
 *
 * This handler provides go-to-definition functionality:
 * - Finding variable declarations
 * - Finding method definitions
 * - Finding class definitions
 * - Finding field/property definitions
 * - Cross-file navigation support
 */
public class DefinitionHandler {

    private static final Logger logger = LoggerFactory.getLogger(DefinitionHandler.class);

    private final IServiceRouter serviceRouter;
    private final DocumentManager documentManager;

    public DefinitionHandler(IServiceRouter serviceRouter, DocumentManager documentManager) {
        this.serviceRouter = serviceRouter;
        this.documentManager = documentManager;
    }

    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>>
            handleDefinition(DefinitionParams params) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        String uri = params.getTextDocument().getUri();
                        Position position = params.getPosition();

                        logger.debug(
                                "Processing definition request at {}:{}:{}",
                                uri,
                                position.getLine(),
                                position.getCharacter());

                        // Get services
                        ASTService astService = serviceRouter.getAstService();
                        WorkspaceIndexService indexService =
                                serviceRouter.getWorkspaceIndexService();

                        // Get document content
                        String sourceCode = documentManager.getDocumentContent(uri);
                        if (sourceCode == null) {
                            logger.debug("Document not found in document manager: {}", uri);
                            return Either.forLeft(Collections.emptyList());
                        }

                        // Parse the document
                        ModuleNode moduleNode = astService.parseSource(sourceCode, uri);
                        if (moduleNode == null) {
                            logger.debug("Failed to parse module for {}", uri);
                            return Either.forLeft(Collections.emptyList());
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
                            return Either.forLeft(Collections.emptyList());
                        }

                        // Find definition locations
                        List<Location> locations =
                                findDefinitions(node, moduleNode, uri, indexService);

                        return Either.forLeft(locations);

                    } catch (Exception e) {
                        logger.error(
                                "Error processing definition request for URI: {} at position {}:{}",
                                params.getTextDocument().getUri(),
                                params.getPosition().getLine(),
                                params.getPosition().getCharacter(),
                                e);
                        return Either.forLeft(Collections.emptyList());
                    }
                });
    }

    private List<Location> findDefinitions(
            ASTNode node,
            ModuleNode moduleNode,
            String currentUri,
            WorkspaceIndexService indexService) {

        List<Location> locations = new ArrayList<>();

        if (node instanceof VariableExpression) {
            locations.addAll(
                    findVariableDefinition((VariableExpression) node, moduleNode, currentUri));
        } else if (node instanceof MethodCallExpression) {
            locations.addAll(
                    findMethodDefinition(
                            (MethodCallExpression) node, moduleNode, currentUri, indexService));
        } else if (node instanceof PropertyExpression) {
            locations.addAll(
                    findPropertyDefinition(
                            (PropertyExpression) node, moduleNode, currentUri, indexService));
        } else if (node instanceof ClassExpression) {
            locations.addAll(findClassDefinition((ClassExpression) node, currentUri, indexService));
        } else if (node instanceof ConstructorCallExpression) {
            ConstructorCallExpression ctorCall = (ConstructorCallExpression) node;
            locations.addAll(findClassDefinition(ctorCall.getType(), currentUri, indexService));
        }

        return locations;
    }

    private List<Location> findVariableDefinition(
            VariableExpression varExpr, ModuleNode moduleNode, String currentUri) {

        Variable variable = varExpr.getAccessedVariable();
        if (variable == null) {
            return Collections.emptyList();
        }

        // Check if it's a local variable or parameter
        if (variable instanceof Parameter || variable instanceof DynamicVariable) {
            // Find the declaring node
            ASTNode declaringNode = findDeclaringNode(variable, moduleNode);
            if (declaringNode != null) {
                Location location = LocationUtils.createLocation(currentUri, declaringNode);
                if (location != null) {
                    return Collections.singletonList(location);
                }
            }
        }

        return Collections.emptyList();
    }

    private List<Location> findMethodDefinition(
            MethodCallExpression methodCall,
            ModuleNode moduleNode,
            String currentUri,
            WorkspaceIndexService indexService) {

        String methodName = methodCall.getMethodAsString();
        if (methodName == null) {
            return Collections.emptyList();
        }

        List<Location> locations = new ArrayList<>();

        // First, check local methods in the current class
        for (ClassNode classNode : moduleNode.getClasses()) {
            for (MethodNode method : classNode.getMethods()) {
                if (method.getName().equals(methodName)) {
                    Location location = LocationUtils.createLocation(currentUri, method);
                    if (location != null) {
                        locations.add(location);
                    }
                }
            }
        }

        // If not found locally, search in the workspace index
        if (locations.isEmpty() && indexService != null) {
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
                        locations.add(location);
                    }
                }
            } catch (Exception e) {
                logger.error("Error searching workspace index for method: {}", methodName, e);
            }
        }

        return locations;
    }

    private List<Location> findPropertyDefinition(
            PropertyExpression propExpr,
            ModuleNode moduleNode,
            String currentUri,
            WorkspaceIndexService indexService) {

        String propertyName = propExpr.getPropertyAsString();
        if (propertyName == null) {
            return Collections.emptyList();
        }

        List<Location> locations = new ArrayList<>();

        // Check local properties/fields
        for (ClassNode classNode : moduleNode.getClasses()) {
            // Check fields
            for (FieldNode field : classNode.getFields()) {
                if (field.getName().equals(propertyName)) {
                    Location location = LocationUtils.createLocation(currentUri, field);
                    if (location != null) {
                        locations.add(location);
                    }
                }
            }

            // Check properties
            for (PropertyNode property : classNode.getProperties()) {
                if (property.getName().equals(propertyName)) {
                    Location location = LocationUtils.createLocation(currentUri, property);
                    if (location != null) {
                        locations.add(location);
                    }
                }
            }
        }

        // Search in workspace if not found locally
        if (locations.isEmpty() && indexService != null) {
            try {
                List<SymbolInfo> symbols =
                        indexService
                                .searchSymbols(propertyName)
                                .get()
                                .filter(
                                        symbol ->
                                                symbol.kind() == SymbolKind.FIELD
                                                        || symbol.kind() == SymbolKind.PROPERTY)
                                .collect(Collectors.toList());

                for (SymbolInfo symbol : symbols) {
                    Location location = createLocation(symbol);
                    if (location != null) {
                        locations.add(location);
                    }
                }
            } catch (Exception e) {
                logger.error("Error searching workspace index for property: {}", propertyName, e);
            }
        }

        return locations;
    }

    private List<Location> findClassDefinition(
            ClassNode classNode, String currentUri, WorkspaceIndexService indexService) {

        String className = classNode.getName();
        List<Location> locations = new ArrayList<>();

        // Check if it's a local class
        if (!classNode.isPrimaryClassNode()) {
            Location location = LocationUtils.createLocation(currentUri, classNode);
            if (location != null) {
                return Collections.singletonList(location);
            }
        }

        // Search in workspace index
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
                    // Match fully qualified name or simple name
                    if (symbol.name().equals(className)
                            || symbol.name().endsWith("." + className)) {
                        Location location = createLocation(symbol);
                        if (location != null) {
                            locations.add(location);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error searching workspace index for class: {}", className, e);
            }
        }

        return locations;
    }

    private List<Location> findClassDefinition(
            ClassExpression classExpr, String currentUri, WorkspaceIndexService indexService) {
        return findClassDefinition(classExpr.getType(), currentUri, indexService);
    }

    private @Nullable ASTNode findDeclaringNode(Variable variable, ModuleNode moduleNode) {
        VariableDeclarationVisitor visitor = new VariableDeclarationVisitor(variable);

        // Visit all classes in the module
        for (ClassNode classNode : moduleNode.getClasses()) {
            classNode.visitContents(visitor);
            if (visitor.getDeclarationNode() != null) {
                return visitor.getDeclarationNode();
            }
        }

        // Check module-level statements (scripts)
        if (moduleNode.getStatementBlock() != null) {
            moduleNode.getStatementBlock().visit(visitor);
        }

        return visitor.getDeclarationNode();
    }

    /**
     * Visitor to find variable declarations
     */
    private static class VariableDeclarationVisitor extends ClassCodeVisitorSupport {
        private final Variable targetVariable;
        private ASTNode declarationNode;

        public VariableDeclarationVisitor(Variable targetVariable) {
            this.targetVariable = targetVariable;
        }

        public ASTNode getDeclarationNode() {
            return declarationNode;
        }

        @Override
        public void visitDeclarationExpression(DeclarationExpression expression) {
            Expression leftExpression = expression.getLeftExpression();
            if (leftExpression instanceof VariableExpression) {
                VariableExpression varExpr = (VariableExpression) leftExpression;
                if (varExpr.getName().equals(targetVariable.getName())) {
                    declarationNode = expression;
                }
            }
            super.visitDeclarationExpression(expression);
        }

        @Override
        public void visitMethod(MethodNode node) {
            // Check method parameters
            for (Parameter param : node.getParameters()) {
                if (param.getName().equals(targetVariable.getName())) {
                    declarationNode = param;
                    return;
                }
            }
            super.visitMethod(node);
        }

        @Override
        public void visitField(FieldNode node) {
            if (node.getName().equals(targetVariable.getName())) {
                declarationNode = node;
            }
            super.visitField(node);
        }

        @Override
        public void visitProperty(PropertyNode node) {
            if (node.getName().equals(targetVariable.getName())) {
                declarationNode = node;
            }
            super.visitProperty(node);
        }

        @Override
        public void visitForLoop(ForStatement forLoop) {
            Parameter param = forLoop.getVariable();
            if (param != null && param.getName().equals(targetVariable.getName())) {
                declarationNode = param;
            }
            super.visitForLoop(forLoop);
        }

        @Override
        public void visitCatchStatement(CatchStatement statement) {
            Parameter param = statement.getVariable();
            if (param != null && param.getName().equals(targetVariable.getName())) {
                declarationNode = param;
            }
            super.visitCatchStatement(statement);
        }

        @Override
        protected SourceUnit getSourceUnit() {
            return null; // Not needed for our use case
        }
    }

    private @Nullable Location createLocation(SymbolInfo symbol) {
        Path location = symbol.location();
        if (location == null) {
            return null;
        }

        String uri = location.toUri().toString();
        Range range =
                new Range(
                        new Position(symbol.line() - 1, symbol.column() - 1),
                        new Position(symbol.line() - 1, symbol.column() - 1));

        return new Location(uri, range);
    }
}
