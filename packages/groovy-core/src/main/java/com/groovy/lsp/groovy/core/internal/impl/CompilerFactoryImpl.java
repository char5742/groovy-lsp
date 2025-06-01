package com.groovy.lsp.groovy.core.internal.impl;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.groovy.lsp.groovy.core.api.CompilerConfigurationService;

/**
 * Internal implementation of CompilerConfigurationService.
 * This factory provides various configurations optimized for LSP usage.
 */
public class CompilerFactoryImpl implements CompilerConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(CompilerFactoryImpl.class);
    
    /**
     * Creates a default compiler configuration suitable for LSP operations.
     * 
     * @return a configured CompilerConfiguration instance
     */
    @Override
    public CompilerConfiguration createDefaultConfiguration() {
        return createDefaultConfigurationStatic();
    }
    
    public static CompilerConfiguration createDefaultConfigurationStatic() {
        CompilerConfiguration config = new CompilerConfiguration();
        
        // Set source encoding
        config.setSourceEncoding("UTF-8");
        
        // Set target bytecode version
        config.setTargetBytecode(CompilerConfiguration.JDK11);
        
        // Enable verbose error reporting for better diagnostics
        config.setVerbose(true);
        
        // Set optimization options (including Parrot parser)
        config.setOptimizationOptions(createOptimizationOptions());
        
        // Add common import customizer
        config.addCompilationCustomizers(createImportCustomizer());
        
        logger.debug("Created default compiler configuration");
        return config;
    }
    
    /**
     * Creates a compiler configuration with custom classpath.
     * 
     * @param classpath the classpath entries
     * @return a configured CompilerConfiguration instance
     */
    @Override
    public CompilerConfiguration createConfigurationWithClasspath(List<String> classpath) {
        return createConfigurationWithClasspathStatic(classpath);
    }
    
    public static CompilerConfiguration createConfigurationWithClasspathStatic(List<String> classpath) {
        Objects.requireNonNull(classpath, "Classpath cannot be null");
        
        CompilerConfiguration config = createDefaultConfigurationStatic();
        config.setClasspathList(classpath);
        
        logger.debug("Created compiler configuration with classpath: {}", classpath);
        return config;
    }
    
    /**
     * Creates a compiler configuration for script analysis.
     * This configuration is optimized for analyzing Groovy scripts in LSP context.
     * 
     * @return a configured CompilerConfiguration instance
     */
    @Override
    public CompilerConfiguration createScriptConfiguration() {
        return createScriptConfigurationStatic();
    }
    
    public static CompilerConfiguration createScriptConfigurationStatic() {
        CompilerConfiguration config = createDefaultConfigurationStatic();
        
        // Set script base class for better script support
        config.setScriptBaseClass("groovy.lang.Script");
        
        // Enable script extensions
        config.setScriptExtensions(Set.of("groovy", "gvy", "gy", "gsh"));
        
        logger.debug("Created script compiler configuration");
        return config;
    }
    
    /**
     * Creates a compiler configuration for type checking.
     * 
     * @param staticTypeChecking whether to enable static type checking
     * @return a configured CompilerConfiguration instance
     */
    @Override
    public CompilerConfiguration createTypeCheckingConfiguration(boolean staticTypeChecking) {
        return createTypeCheckingConfigurationStatic(staticTypeChecking);
    }
    
    public static CompilerConfiguration createTypeCheckingConfigurationStatic(boolean staticTypeChecking) {
        CompilerConfiguration config = createDefaultConfigurationStatic();
        
        if (staticTypeChecking) {
            // Add static type checking transformation
            config.addCompilationCustomizers(
                new ASTTransformationCustomizer(groovy.transform.TypeChecked.class)
            );
            logger.debug("Created compiler configuration with static type checking");
        } else {
            logger.debug("Created compiler configuration with dynamic type checking");
        }
        
        return config;
    }
    
    /**
     * Creates optimization options for the compiler.
     * 
     * @return optimization options map
     */
    private static java.util.Map<String, Boolean> createOptimizationOptions() {
        return java.util.Map.of(
            "indy", true,  // Use invokedynamic
            "groovydoc", true,  // Preserve groovydoc
            "int", false,  // Don't optimize int operations (for debugging)
            "parrot", true  // Use Parrot parser (Groovy 4.0+ default)
        );
    }
    
    /**
     * Creates an import customizer with common imports.
     * 
     * @return an ImportCustomizer instance
     */
    private static ImportCustomizer createImportCustomizer() {
        ImportCustomizer imports = new ImportCustomizer();
        
        // Add common imports
        imports.addImports(
            "java.util.List",
            "java.util.Map",
            "java.util.Set"
        );
        
        // Add star imports for commonly used packages
        imports.addStarImports(
            "java.util",
            "java.io",
            "java.nio.file"
        );
        
        // Add static imports
        imports.addStaticStars(
            "java.util.Collections"
        );
        
        return imports;
    }
}