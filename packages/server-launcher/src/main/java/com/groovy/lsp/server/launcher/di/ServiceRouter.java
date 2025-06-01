package com.groovy.lsp.server.launcher.di;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.groovy.lsp.codenarc.LintEngine;
import com.groovy.lsp.formatting.service.FormattingService;
import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.groovy.core.api.CompilerConfigurationService;
import com.groovy.lsp.groovy.core.api.TypeInferenceService;
import com.groovy.lsp.workspace.api.WorkspaceIndexService;
import java.util.Objects;
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
            LintEngine lintEngine) {
        // Validate all services are non-null
        this.astService = Objects.requireNonNull(astService, "ASTService must not be null");
        this.compilerConfigurationService =
                Objects.requireNonNull(
                        compilerConfigurationService,
                        "CompilerConfigurationService must not be null");
        this.typeInferenceService =
                Objects.requireNonNull(
                        typeInferenceService, "TypeInferenceService must not be null");
        this.workspaceIndexService =
                Objects.requireNonNull(
                        workspaceIndexService, "WorkspaceIndexService must not be null");
        this.formattingService =
                Objects.requireNonNull(formattingService, "FormattingService must not be null");
        this.lintEngine = Objects.requireNonNull(lintEngine, "LintEngine must not be null");

        // Validate services are properly initialized
        validateServices();

        logger.info("ServiceRouter initialized with all services successfully");
    }

    /**
     * Validates that all services are properly initialized.
     *
     * @throws IllegalStateException if any service is not properly initialized
     */
    private void validateServices() {
        try {
            // Perform basic validation on each service
            // This helps catch initialization issues early
            logger.debug("Validating services...");

            // You can add specific validation logic here if services have health check methods
            // For now, we just ensure they're not null (already done above)

            logger.debug("All services validated successfully");
        } catch (Exception e) {
            logger.error("Service validation failed", e);
            throw new IllegalStateException(
                    "Failed to initialize ServiceRouter: " + e.getMessage(), e);
        }
    }

    /**
     * Get the AST service for parsing and AST operations.
     *
     * @return the AST service
     * @throws IllegalStateException if the service is not available
     */
    public ASTService getAstService() {
        ensureServiceAvailable(astService, "ASTService");
        return astService;
    }

    /**
     * Get the compiler configuration service.
     *
     * @return the compiler configuration service
     * @throws IllegalStateException if the service is not available
     */
    public CompilerConfigurationService getCompilerConfigurationService() {
        ensureServiceAvailable(compilerConfigurationService, "CompilerConfigurationService");
        return compilerConfigurationService;
    }

    /**
     * Get the type inference service.
     *
     * @return the type inference service
     * @throws IllegalStateException if the service is not available
     */
    public TypeInferenceService getTypeInferenceService() {
        ensureServiceAvailable(typeInferenceService, "TypeInferenceService");
        return typeInferenceService;
    }

    /**
     * Get the workspace index service.
     *
     * @return the workspace index service
     * @throws IllegalStateException if the service is not available
     */
    public WorkspaceIndexService getWorkspaceIndexService() {
        ensureServiceAvailable(workspaceIndexService, "WorkspaceIndexService");
        return workspaceIndexService;
    }

    /**
     * Get the formatting service.
     *
     * @return the formatting service
     * @throws IllegalStateException if the service is not available
     */
    public FormattingService getFormattingService() {
        ensureServiceAvailable(formattingService, "FormattingService");
        return formattingService;
    }

    /**
     * Get the lint engine for code analysis.
     *
     * @return the lint engine
     * @throws IllegalStateException if the service is not available
     */
    public LintEngine getLintEngine() {
        ensureServiceAvailable(lintEngine, "LintEngine");
        return lintEngine;
    }

    /**
     * Ensures that a service is available before returning it.
     *
     * @param service the service to check
     * @param serviceName the name of the service for error messages
     * @throws IllegalStateException if the service is null
     */
    private void ensureServiceAvailable(Object service, String serviceName) {
        if (service == null) {
            String message = serviceName + " is not available";
            logger.error(message);
            throw new IllegalStateException(message);
        }
    }

    /**
     * Check if all services are available.
     *
     * @return true if all services are available, false otherwise
     */
    public boolean areAllServicesAvailable() {
        return astService != null
                && compilerConfigurationService != null
                && typeInferenceService != null
                && workspaceIndexService != null
                && formattingService != null
                && lintEngine != null;
    }
}
