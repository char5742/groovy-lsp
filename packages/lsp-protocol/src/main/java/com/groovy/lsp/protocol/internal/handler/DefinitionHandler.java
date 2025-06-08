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
                        // TODO: Get WorkspaceIndexService when circular dependency is resolved
                        // WorkspaceIndexService indexService =
                        // serviceRouter.getWorkspaceIndexService();

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
                        List<Location> locations = findDefinitions(node, moduleNode, uri, null);

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
            Object indexService) { // TODO: Change back to WorkspaceIndexService when dependency
        // resolved

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
                Location location = createLocation(currentUri, declaringNode);
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
            Object indexService) { // TODO: Change back to WorkspaceIndexService

        String methodName = methodCall.getMethodAsString();
        if (methodName == null) {
            return Collections.emptyList();
        }

        List<Location> locations = new ArrayList<>();

        // First, check local methods in the current class
        for (ClassNode classNode : moduleNode.getClasses()) {
            for (MethodNode method : classNode.getMethods()) {
                if (method.getName().equals(methodName)) {
                    Location location = createLocation(currentUri, method);
                    if (location != null) {
                        locations.add(location);
                    }
                }
            }
        }

        // TODO: Enable workspace search when circular dependency is resolved
        // If not found locally, search in the workspace index
        // if (locations.isEmpty() && indexService != null) {
        //     try {
        //         List<SymbolInfo> symbols = indexService.searchSymbols(methodName)
        //                 .get()
        //                 .filter(symbol -> symbol.kind() == SymbolKind.METHOD)
        //                 .collect(Collectors.toList());
        //
        //         for (SymbolInfo symbol : symbols) {
        //             Location location = createLocation(symbol);
        //             if (location != null) {
        //                 locations.add(location);
        //             }
        //         }
        //     } catch (Exception e) {
        //         logger.error("Error searching workspace index for method: {}", methodName, e);
        //     }
        // }

        return locations;
    }

    private List<Location> findPropertyDefinition(
            PropertyExpression propExpr,
            ModuleNode moduleNode,
            String currentUri,
            Object indexService) { // TODO: Change back to WorkspaceIndexService

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
                    Location location = createLocation(currentUri, field);
                    if (location != null) {
                        locations.add(location);
                    }
                }
            }

            // Check properties
            for (PropertyNode property : classNode.getProperties()) {
                if (property.getName().equals(propertyName)) {
                    Location location = createLocation(currentUri, property);
                    if (location != null) {
                        locations.add(location);
                    }
                }
            }
        }

        // TODO: Enable workspace search when circular dependency is resolved
        // Search in workspace if not found locally
        // if (locations.isEmpty() && indexService != null) {
        //     try {
        //         List<SymbolInfo> symbols = indexService.searchSymbols(propertyName)
        //                 .get()
        //                 .filter(symbol -> symbol.kind() == SymbolKind.FIELD ||
        //                          symbol.kind() == SymbolKind.PROPERTY)
        //                 .collect(Collectors.toList());
        //
        //         for (SymbolInfo symbol : symbols) {
        //             Location location = createLocation(symbol);
        //             if (location != null) {
        //                 locations.add(location);
        //             }
        //         }
        //     } catch (Exception e) {
        //         logger.error("Error searching workspace index for property: {}", propertyName,
        // e);
        //     }
        // }

        return locations;
    }

    private List<Location> findClassDefinition(
            ClassNode classNode,
            String currentUri,
            Object indexService) { // TODO: Change back to WorkspaceIndexService

        String className = classNode.getName();
        List<Location> locations = new ArrayList<>();

        // Check if it's a local class
        if (!classNode.isPrimaryClassNode()) {
            Location location = createLocation(currentUri, classNode);
            if (location != null) {
                return Collections.singletonList(location);
            }
        }

        // TODO: Enable workspace search when circular dependency is resolved
        // Search in workspace index
        // if (indexService != null) {
        //     try {
        //         List<SymbolInfo> symbols = indexService.searchSymbols(className)
        //                 .get()
        //                 .filter(symbol -> symbol.kind() == SymbolKind.CLASS ||
        //                          symbol.kind() == SymbolKind.INTERFACE)
        //                 .collect(Collectors.toList());
        //
        //         for (SymbolInfo symbol : symbols) {
        //             // Match fully qualified name or simple name
        //             if (symbol.name().equals(className) || symbol.name().endsWith("." +
        // className)) {
        //                 Location location = createLocation(symbol);
        //                 if (location != null) {
        //                     locations.add(location);
        //                 }
        //             }
        //         }
        //     } catch (Exception e) {
        //         logger.error("Error searching workspace index for class: {}", className, e);
        //     }
        // }

        return locations;
    }

    private List<Location> findClassDefinition(
            ClassExpression classExpr,
            String currentUri,
            Object indexService) { // TODO: Change back to WorkspaceIndexService
        return findClassDefinition(classExpr.getType(), currentUri, indexService);
    }

    private @Nullable ASTNode findDeclaringNode(Variable variable, ModuleNode moduleNode) {
        // This is a simplified implementation
        // In a real implementation, we would need to traverse the AST
        // to find where the variable is declared
        return null;
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

    // TODO: Enable when circular dependency is resolved
    // private @Nullable Location createLocation(SymbolInfo symbol) {
    //     Path location = symbol.location();
    //     if (location == null) {
    //         return null;
    //     }
    //
    //     String uri = location.toUri().toString();
    //     Range range = new Range(
    //             new Position(symbol.line() - 1, symbol.column() - 1),
    //             new Position(symbol.line() - 1, symbol.column() - 1)
    //     );
    //
    //     return new Location(uri, range);
    // }
}
