package com.groovy.lsp.protocol.api;

import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.groovy.core.api.CompilerConfigurationService;
import com.groovy.lsp.groovy.core.api.IncrementalCompilationService;
import com.groovy.lsp.groovy.core.api.TypeInferenceService;
import com.groovy.lsp.shared.workspace.api.WorkspaceIndexService;

/**
 * Interface for service routing to avoid circular dependencies.
 *
 * Provides access to core services required by LSP handlers.
 */
public interface IServiceRouter {

    /**
     * Get the AST service for parsing and AST operations.
     *
     * @return the AST service
     * @throws IllegalStateException if the service is not available
     */
    ASTService getAstService();

    /**
     * Get the type inference service.
     *
     * @return the type inference service
     * @throws IllegalStateException if the service is not available
     */
    TypeInferenceService getTypeInferenceService();

    /**
     * Get the incremental compilation service.
     *
     * @return the incremental compilation service
     * @throws IllegalStateException if the service is not available
     */
    IncrementalCompilationService getIncrementalCompilationService();

    /**
     * Get the workspace index service.
     *
     * @return the workspace index service
     * @throws IllegalStateException if the service is not available
     */
    WorkspaceIndexService getWorkspaceIndexService();

    /**
     * Get the compiler configuration service.
     *
     * @return the compiler configuration service
     * @throws IllegalStateException if the service is not available
     */
    CompilerConfigurationService getCompilerConfigurationService();
}
