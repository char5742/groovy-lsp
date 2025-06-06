package com.groovy.lsp.protocol.api;

import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.groovy.core.api.TypeInferenceService;

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
}
