package com.groovy.lsp.groovy.core.api;

import com.groovy.lsp.groovy.core.internal.impl.ASTServiceImpl;
import com.groovy.lsp.groovy.core.internal.impl.CompilerFactoryImpl;
import com.groovy.lsp.groovy.core.internal.impl.TypeInferenceServiceImpl;
import com.groovy.lsp.groovy.core.internal.impl.IncrementalCompilationServiceImpl;
import org.jmolecules.ddd.annotation.Factory;

/**
 * Factory for creating instances of Groovy core services.
 * This is the main entry point for external modules to access Groovy core functionality.
 * 
 * Follows the Factory pattern to encapsulate the creation of domain services
 * and maintain proper boundaries between modules.
 */
@Factory
public class GroovyCoreFactory {
    
    private static final GroovyCoreFactory INSTANCE = new GroovyCoreFactory();
    
    private final ASTService astService;
    private final CompilerConfigurationService compilerConfigurationService;
    private final TypeInferenceService typeInferenceService;
    private final IncrementalCompilationService incrementalCompilationService;
    
    private GroovyCoreFactory() {
        this.astService = new ASTServiceImpl();
        this.compilerConfigurationService = new CompilerFactoryImpl();
        this.typeInferenceService = new TypeInferenceServiceImpl(this.astService);
        this.incrementalCompilationService = new IncrementalCompilationServiceImpl();
    }
    
    /**
     * Gets the singleton instance of the factory.
     * 
     * @return the factory instance
     */
    public static GroovyCoreFactory getInstance() {
        return INSTANCE;
    }
    
    /**
     * Creates a new ASTService instance.
     * 
     * @return a new ASTService instance
     */
    public ASTService createASTService() {
        return new ASTServiceImpl();
    }
    
    /**
     * Creates a new CompilerConfigurationService instance.
     * 
     * @return a new CompilerConfigurationService instance
     */
    public CompilerConfigurationService createCompilerConfigurationService() {
        return new CompilerFactoryImpl();
    }
    
    /**
     * Creates a new TypeInferenceService instance.
     * 
     * @param astService the AST service to use for type inference
     * @return a new TypeInferenceService instance
     */
    public TypeInferenceService createTypeInferenceService(ASTService astService) {
        return new TypeInferenceServiceImpl(astService);
    }
    
    /**
     * Gets the shared ASTService instance.
     * 
     * @return the shared ASTService instance
     */
    public ASTService getASTService() {
        return astService;
    }
    
    /**
     * Gets the shared CompilerConfigurationService instance.
     * 
     * @return the shared CompilerConfigurationService instance
     */
    public CompilerConfigurationService getCompilerConfigurationService() {
        return compilerConfigurationService;
    }
    
    /**
     * Gets the shared TypeInferenceService instance.
     * 
     * @return the shared TypeInferenceService instance
     */
    public TypeInferenceService getTypeInferenceService() {
        return typeInferenceService;
    }
    
    /**
     * Creates a new IncrementalCompilationService instance.
     * 
     * @return a new IncrementalCompilationService instance
     */
    public IncrementalCompilationService createIncrementalCompilationService() {
        return new IncrementalCompilationServiceImpl();
    }
    
    /**
     * Gets the shared IncrementalCompilationService instance.
     * 
     * @return the shared IncrementalCompilationService instance
     */
    public IncrementalCompilationService getIncrementalCompilationService() {
        return incrementalCompilationService;
    }
}