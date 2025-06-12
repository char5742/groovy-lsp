package com.groovy.lsp.protocol.internal.handler;

import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.groovy.core.api.CompilerConfigurationService;
import com.groovy.lsp.groovy.core.api.TypeInferenceService;
import com.groovy.lsp.protocol.api.IServiceRouter;
import com.groovy.lsp.protocol.internal.document.DocumentManager;
import java.util.concurrent.CompletableFuture;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.Variable;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles hover requests for Groovy documents.
 *
 * This handler provides hover information including:
 * - Type information
 * - Javadoc/Groovydoc documentation
 * - Method signatures
 * - Field/property information
 */
public class HoverHandler {

    private static final Logger logger = LoggerFactory.getLogger(HoverHandler.class);

    private final IServiceRouter serviceRouter;
    private final DocumentManager documentManager;

    public HoverHandler(IServiceRouter serviceRouter, DocumentManager documentManager) {
        this.serviceRouter = serviceRouter;
        this.documentManager = documentManager;
    }

    public CompletableFuture<Hover> handleHover(HoverParams params) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        String uri = params.getTextDocument().getUri();
                        Position position = params.getPosition();

                        logger.debug(
                                "Processing hover at {}:{}:{}",
                                uri,
                                position.getLine(),
                                position.getCharacter());

                        // Get AST service
                        ASTService astService = serviceRouter.getAstService();
                        CompilerConfigurationService configService =
                                serviceRouter.getCompilerConfigurationService();
                        TypeInferenceService typeService = serviceRouter.getTypeInferenceService();

                        // Get document content
                        String sourceCode = documentManager.getDocumentContent(uri);
                        if (sourceCode == null) {
                            logger.debug("Document not found in document manager: {}", uri);
                            return null;
                        }

                        // Parse the document with workspace-aware compiler configuration
                        ModuleNode moduleNode =
                                astService.parseSource(
                                        sourceCode,
                                        uri,
                                        configService.createDefaultConfiguration());
                        if (moduleNode == null) {
                            logger.debug("Failed to parse module for {}", uri);
                            return null;
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
                            return null;
                        }

                        // Generate hover content
                        String hoverContent = generateHoverContent(node, typeService, moduleNode);
                        if (hoverContent == null || hoverContent.isEmpty()) {
                            return null;
                        }

                        // Create hover result
                        MarkupContent markupContent = new MarkupContent();
                        markupContent.setKind(MarkupKind.MARKDOWN);
                        markupContent.setValue(hoverContent);

                        Hover hover = new Hover();
                        hover.setContents(markupContent);

                        return hover;

                    } catch (Exception e) {
                        logger.error(
                                "Error processing hover request for URI: {} at position {}:{}",
                                params.getTextDocument().getUri(),
                                params.getPosition().getLine(),
                                params.getPosition().getCharacter(),
                                e);
                        return null;
                    }
                });
    }

    private @Nullable String generateHoverContent(
            ASTNode node, TypeInferenceService typeService, ModuleNode moduleNode) {
        StringBuilder content = new StringBuilder();

        // Check more specific types first before checking interfaces
        if (node instanceof FieldNode fieldNode) {
            generateFieldHover(fieldNode, content);
        } else if (node instanceof PropertyNode propertyNode) {
            generatePropertyHover(propertyNode, content);
        } else if (node instanceof MethodNode methodNode) {
            generateMethodHover(methodNode, content);
        } else if (node instanceof ClassNode classNode) {
            generateClassHover(classNode, content);
        } else if (node instanceof VariableExpression varExpr) {
            Variable variable = varExpr.getAccessedVariable();
            if (variable != null) {
                generateVariableHover(variable, content);
            } else {
                generateExpressionHover(varExpr, content, typeService, moduleNode);
            }
        } else if (node instanceof Variable variable) {
            generateVariableHover(variable, content);
        } else if (node instanceof Expression expression) {
            generateExpressionHover(expression, content, typeService, moduleNode);
        }

        return content.length() > 0 ? content.toString() : null;
    }

    private void generateVariableHover(Variable variable, StringBuilder content) {
        content.append("```groovy\n");

        // Variable declaration
        ClassNode type = variable.getType();
        if (type != null && !type.getName().equals("java.lang.Object")) {
            content.append(type.getName()).append(" ");
        } else {
            content.append("def ");
        }

        content.append(variable.getName());
        content.append("\n```\n");

        // Add additional information
        if (variable instanceof Parameter) {
            content.append("\n**Parameter**");
        } else {
            content.append("\n**Local variable**");
        }
    }

    private void generateMethodHover(MethodNode method, StringBuilder content) {
        content.append("```groovy\n");

        // Method signature
        if (method.isPublic()) content.append("public ");
        else if (method.isProtected()) content.append("protected ");
        else if (method.isPrivate()) content.append("private ");

        if (method.isStatic()) content.append("static ");
        if (method.isFinal()) content.append("final ");

        // Return type
        ClassNode returnType = method.getReturnType();
        content.append(returnType.getName()).append(" ");

        // Method name
        content.append(method.getName()).append("(");

        // Parameters
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) content.append(", ");
            Parameter param = parameters[i];
            content.append(param.getType().getName()).append(" ").append(param.getName());
        }

        content.append(")\n```\n");

        // Javadoc if available
        String javadoc = extractJavadoc(method);
        if (javadoc != null) {
            content.append("\n").append(javadoc);
        }
    }

    private void generateFieldHover(FieldNode field, StringBuilder content) {
        logger.debug("generateFieldHover called for field: {}", field.getName());
        content.append("```groovy\n");

        // Field declaration
        if (field.isPublic()) content.append("public ");
        else if (field.isProtected()) content.append("protected ");
        else if (field.isPrivate()) content.append("private ");

        if (field.isStatic()) content.append("static ");
        if (field.isFinal()) content.append("final ");

        content.append(field.getType().getName()).append(" ");
        content.append(field.getName());

        content.append("\n```\n");

        content.append("\n**Field** in ").append(field.getOwner().getName());

        // Javadoc if available
        logger.debug("Calling extractJavadoc for field: {}", field.getName());
        String javadoc = extractJavadoc(field);
        logger.debug("extractJavadoc returned: {}", javadoc);
        if (javadoc != null) {
            content.append("\n").append(javadoc);
        }
    }

    private void generatePropertyHover(PropertyNode property, StringBuilder content) {
        content.append("```groovy\n");

        // Property declaration
        if (property.isPublic()) content.append("public ");
        else if (property.isPrivate()) content.append("private ");

        if (property.isStatic()) content.append("static ");

        content.append(property.getType().getName()).append(" ");
        content.append(property.getName());

        content.append("\n```\n");

        content.append("\n**Property** in ").append(property.getDeclaringClass().getName());

        // Javadoc if available
        String javadoc = extractJavadoc(property);
        if (javadoc != null) {
            content.append("\n").append(javadoc);
        }
    }

    private void generateClassHover(ClassNode classNode, StringBuilder content) {
        content.append("```groovy\n");

        // Class declaration
        if (classNode.isInterface()) {
            content.append("interface ");
        } else if (classNode.isEnum()) {
            content.append("enum ");
        } else {
            content.append("class ");
        }

        content.append(classNode.getName());

        // Superclass
        ClassNode superClass = classNode.getSuperClass();
        if (superClass != null
                && superClass.getName() != null
                && !superClass.getName().equals("java.lang.Object")) {
            content.append(" extends ").append(superClass.getName());
        }

        // Interfaces
        ClassNode[] interfaces = classNode.getInterfaces();
        if (interfaces.length > 0) {
            content.append(" implements ");
            for (int i = 0; i < interfaces.length; i++) {
                if (i > 0) content.append(", ");
                content.append(interfaces[i].getName());
            }
        }

        content.append("\n```\n");

        // Package info
        if (classNode.getPackageName() != null) {
            content.append("\n**Package:** ").append(classNode.getPackageName());
        }

        // Javadoc if available
        String javadoc = extractJavadoc(classNode);
        if (javadoc != null) {
            content.append("\n").append(javadoc);
        }
    }

    private void generateExpressionHover(
            Expression expr,
            StringBuilder content,
            TypeInferenceService typeService,
            ModuleNode moduleNode) {
        // Infer type for expression
        ClassNode inferredType = typeService.inferExpressionType(expr, moduleNode);

        if (inferredType != null) {
            content.append("```groovy\n");
            content.append(inferredType.getName());
            content.append("\n```\n");

            if (expr instanceof VariableExpression varExpr) {
                content.append("\n**Variable:** ").append(varExpr.getName());
            } else if (expr instanceof MethodCallExpression methodCall) {
                content.append("\n**Method call:** ").append(methodCall.getMethodAsString());
            } else if (expr instanceof PropertyExpression propExpr) {
                content.append("\n**Property:** ").append(propExpr.getPropertyAsString());
            }
        }
    }

    private @Nullable String extractJavadoc(AnnotatedNode node) {
        StringBuilder doc = new StringBuilder();
        logger.debug("extractJavadoc called with node type: {}", node.getClass().getName());

        // Extract Groovydoc content if available
        try {
            var groovydoc = node.getGroovydoc();
            if (groovydoc != null) {
                logger.debug("Found Groovydoc for node: {}", node);
                String content = groovydoc.getContent();
                if (content != null && !content.trim().isEmpty()) {
                    logger.debug("Groovydoc content: {}", content);
                    doc.append(content.trim());
                }
            }
        } catch (Exception e) {
            logger.debug("Error accessing Groovydoc: {}", e.getMessage());
        }

        // Extract annotation information
        if (node.getAnnotations() != null && !node.getAnnotations().isEmpty()) {
            logger.debug("Node has {} annotations", node.getAnnotations().size());
            doc.append("\n\n**Annotations:**\n");
            node.getAnnotations()
                    .forEach(
                            annotation -> {
                                doc.append("- @")
                                        .append(annotation.getClassNode().getName())
                                        .append("\n");
                            });
        }

        // Add basic documentation based on node type
        if (node instanceof MethodNode method) {
            logger.debug("Node is MethodNode");
            if (method.isAbstract()) {
                doc.append("\n*Abstract method*");
            }
            if (method.isSynthetic()) {
                doc.append("\n*Synthetic method*");
            }
        } else if (node instanceof FieldNode field) {
            logger.debug("Node is FieldNode");
            logger.debug("Field.isEnum() = {}", field.isEnum());
            if (field.isEnum()) {
                doc.append("\n*Enum constant*");
                logger.debug("Added '*Enum constant*' to doc");
            }
            if (field.isSynthetic()) {
                doc.append("\n*Synthetic field*");
            }
        } else if (node instanceof ClassNode clazz) {
            logger.debug("Node is ClassNode");
            if (clazz.isAbstract()) {
                doc.append("\n*Abstract class*");
            }
            if (clazz.isInterface()) {
                doc.append("\n*Interface*");
            }
            if (clazz.isEnum()) {
                doc.append("\n*Enumeration*");
            }
        }

        String result = doc.length() > 0 ? doc.toString() : null;
        logger.debug("extractJavadoc returning: {}", result);
        return result;
    }
}
