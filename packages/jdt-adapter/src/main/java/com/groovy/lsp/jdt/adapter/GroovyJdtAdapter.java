package com.groovy.lsp.jdt.adapter;

import com.groovy.lsp.jdt.adapter.ast.AstConverter;
import com.groovy.lsp.jdt.adapter.type.TypeConverter;
import org.codehaus.groovy.ast.CompileUnit;
import org.codehaus.groovy.ast.ModuleNode;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main adapter class for converting between Groovy AST and Eclipse JDT AST.
 * This class provides the main entry point for bridging Groovy and JDT
 * to enable better IDE features and type resolution.
 */
public class GroovyJdtAdapter {
    private static final Logger logger = LoggerFactory.getLogger(GroovyJdtAdapter.class);

    private final AstConverter astConverter;
    private final TypeConverter typeConverter;

    /**
     * Constructs a new GroovyJdtAdapter with default converters.
     */
    public GroovyJdtAdapter() {
        this.astConverter = new AstConverter();
        this.typeConverter = new TypeConverter();
    }

    /**
     * Converts a Groovy ModuleNode to a JDT CompilationUnit.
     * This enables JDT-based tools to work with Groovy code.
     *
     * @param moduleNode the Groovy module node to convert
     * @return the corresponding JDT compilation unit
     */
    public CompilationUnit convertToJdt(ModuleNode moduleNode) {
        logger.debug(
                "Converting Groovy ModuleNode to JDT CompilationUnit: {}",
                moduleNode.getContext().getName());

        try {
            AST ast = AST.newAST(AST.JLS17);
            CompilationUnit compilationUnit = ast.newCompilationUnit();

            // Convert package declaration
            if (moduleNode.getPackage() != null) {
                astConverter.convertPackage(moduleNode.getPackage(), compilationUnit);
            }

            // Convert imports
            moduleNode
                    .getImports()
                    .forEach(importNode -> astConverter.convertImport(importNode, compilationUnit));

            // Convert star imports
            moduleNode
                    .getStarImports()
                    .forEach(
                            starImport ->
                                    astConverter.convertStarImport(starImport, compilationUnit));

            // Convert static imports
            moduleNode
                    .getStaticImports()
                    .forEach(
                            (alias, importNode) ->
                                    astConverter.convertStaticImport(
                                            importNode, alias, compilationUnit));

            // Convert static star imports
            moduleNode
                    .getStaticStarImports()
                    .forEach(
                            (alias, importNode) ->
                                    astConverter.convertStaticStarImport(
                                            importNode, alias, compilationUnit));

            // Convert classes
            moduleNode
                    .getClasses()
                    .forEach(classNode -> astConverter.convertClass(classNode, compilationUnit));

            return compilationUnit;
        } catch (Exception e) {
            logger.error("Failed to convert Groovy AST to JDT AST", e);
            throw new AdapterException("Conversion failed", e);
        }
    }

    /**
     * Converts a JDT CompilationUnit back to a Groovy ModuleNode.
     * This is useful for round-trip conversions and applying JDT-based
     * modifications back to Groovy code.
     *
     * @param compilationUnit the JDT compilation unit to convert
     * @param compileUnit the Groovy compile unit context
     * @return the corresponding Groovy module node
     */
    public ModuleNode convertToGroovy(CompilationUnit compilationUnit, CompileUnit compileUnit) {
        logger.debug("Converting JDT CompilationUnit to Groovy ModuleNode");

        try {
            ModuleNode moduleNode = new ModuleNode(compileUnit);

            // Convert package
            if (compilationUnit.getPackage() != null) {
                astConverter.convertJdtPackage(compilationUnit.getPackage(), moduleNode);
            }

            // Convert imports
            compilationUnit
                    .imports()
                    .forEach(importDecl -> astConverter.convertJdtImport(importDecl, moduleNode));

            // Convert types
            compilationUnit.types().forEach(type -> astConverter.convertJdtType(type, moduleNode));

            return moduleNode;
        } catch (Exception e) {
            logger.error("Failed to convert JDT AST to Groovy AST", e);
            throw new AdapterException("Conversion failed", e);
        }
    }

    /**
     * Gets the type converter for converting between Groovy and JDT types.
     *
     * @return the type converter instance
     */
    public TypeConverter getTypeConverter() {
        return typeConverter;
    }

    /**
     * Gets the AST converter for converting between Groovy and JDT AST nodes.
     *
     * @return the AST converter instance
     */
    public AstConverter getAstConverter() {
        return astConverter;
    }

    /**
     * Exception thrown when adapter operations fail.
     */
    public static class AdapterException extends RuntimeException {
        /**
         * Constructs a new AdapterException with the specified message.
         * @param message the error message
         */
        public AdapterException(String message) {
            super(message);
        }

        /**
         * Constructs a new AdapterException with the specified message and cause.
         * @param message the error message
         * @param cause the underlying cause
         */
        public AdapterException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
