package com.groovy.lsp.server.launcher.di;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.groovy.lsp.codenarc.LintEngine;
import com.groovy.lsp.formatting.service.FormattingService;
import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.groovy.core.api.CompilerConfigurationService;
import com.groovy.lsp.groovy.core.api.TypeInferenceService;
import com.groovy.lsp.workspace.api.WorkspaceIndexService;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service router that provides centralized access to all LSP services.
 * 
 * This class is responsible for routing requests to appropriate service implementations
 * and managing service lifecycle through Guice dependency injection.
 */
@Singleton
public class ServiceRouter {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceRouter.class);
    
    private final ASTService astService;
    private final CompilerConfigurationService compilerConfigurationService;
    private final TypeInferenceService typeInferenceService;
    private final WorkspaceIndexService workspaceIndexService;
    private final FormattingService formattingService;
    private final LintEngine lintEngine;
    
    @Inject
    public ServiceRouter(
        ASTService astService,
        CompilerConfigurationService compilerConfigurationService,
        TypeInferenceService typeInferenceService,
        WorkspaceIndexService workspaceIndexService,
        FormattingService formattingService,
        LintEngine lintEngine
    ) {
        this.astService = astService;
        this.compilerConfigurationService = compilerConfigurationService;
        this.typeInferenceService = typeInferenceService;
        this.workspaceIndexService = workspaceIndexService;
        this.formattingService = formattingService;
        this.lintEngine = lintEngine;
        
        logger.info("ServiceRouter initialized with all services");
    }
    
    /**
     * Get the AST service for parsing and AST operations.
     */
    public ASTService getAstService() {
        return astService;
    }
    
    /**
     * Get the compiler configuration service.
     */
    public CompilerConfigurationService getCompilerConfigurationService() {
        return compilerConfigurationService;
    }
    
    /**
     * Get the type inference service.
     */
    public TypeInferenceService getTypeInferenceService() {
        return typeInferenceService;
    }
    
    /**
     * Get the workspace index service.
     */
    public WorkspaceIndexService getWorkspaceIndexService() {
        return workspaceIndexService;
    }
    
    /**
     * Get the formatting service.
     */
    public FormattingService getFormattingService() {
        return formattingService;
    }
    
    /**
     * Get the lint engine for code analysis.
     */
    public LintEngine getLintEngine() {
        return lintEngine;
    }
}