package com.groovy.lsp.server.launcher.di;

import com.groovy.lsp.codenarc.LintEngine;
import com.groovy.lsp.formatting.service.FormattingService;
import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.groovy.core.api.CompilerConfigurationService;
import com.groovy.lsp.groovy.core.api.IncrementalCompilationService;
import com.groovy.lsp.groovy.core.api.TypeInferenceService;
import com.groovy.lsp.protocol.api.GroovyLanguageServer;
import com.groovy.lsp.protocol.api.IServiceRouter;
import com.groovy.lsp.protocol.internal.document.DocumentManager;
import com.groovy.lsp.protocol.internal.impl.GroovyTextDocumentService;
import com.groovy.lsp.protocol.internal.impl.GroovyWorkspaceService;
import com.groovy.lsp.shared.event.EventBus;
import com.groovy.lsp.shared.workspace.api.WorkspaceIndexService;
import dagger.Component;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Test component for Dagger that provides access to all services for testing purposes.
 * This component provides a complete dependency graph with test-specific configurations.
 *
 * <p>Use this component when you need:
 * <ul>
 *   <li>Access to individual services for verification in tests</li>
 *   <li>A complete dependency graph with test-specific configurations</li>
 *   <li>Mock or real implementations based on test requirements</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * TestServerComponent component = DaggerTestServerComponent.builder()
 *     .testServerModule(new TestServerModule.Builder()
 *         .withMocks()
 *         .build())
 *     .build();
 *
 * // Access services for testing
 * ASTService astService = component.astService();
 * verify(astService).parseSource(any());
 * }</pre>
 */
@Singleton
@Component(modules = {TestServerModule.class})
public interface TestServerComponent {

    // Expose all services for testing

    /**
     * Provides the main GroovyLanguageServer instance.
     */
    GroovyLanguageServer languageServer();

    /**
     * Provides the server executor service for shutdown.
     */
    @Named("serverExecutor")
    ExecutorService serverExecutor();

    /**
     * Provides the scheduled executor service for shutdown.
     */
    @Named("scheduledExecutor")
    ScheduledExecutorService scheduledExecutor();

    /**
     * Provides the EventBus for testing event publishing and subscription.
     */
    EventBus eventBus();

    /**
     * Provides the ASTService for testing AST operations.
     */
    ASTService astService();

    /**
     * Provides the CompilerConfigurationService for testing compiler configuration.
     */
    CompilerConfigurationService compilerConfigurationService();

    /**
     * Provides the TypeInferenceService for testing type inference.
     */
    TypeInferenceService typeInferenceService();

    /**
     * Provides the IncrementalCompilationService for testing incremental compilation.
     */
    IncrementalCompilationService incrementalCompilationService();

    /**
     * Provides the WorkspaceIndexService for testing workspace indexing.
     */
    WorkspaceIndexService workspaceIndexService();

    /**
     * Provides the FormattingService for testing code formatting.
     */
    FormattingService formattingService();

    /**
     * Provides the LintEngine for testing code analysis.
     */
    LintEngine lintEngine();

    /**
     * Provides the ServiceRouter for testing service routing.
     */
    IServiceRouter serviceRouter();

    /**
     * Provides the DocumentManager for testing document management.
     */
    DocumentManager documentManager();

    /**
     * Provides the GroovyTextDocumentService for testing text document operations.
     */
    GroovyTextDocumentService textDocumentService();

    /**
     * Provides the GroovyWorkspaceService for testing workspace operations.
     */
    GroovyWorkspaceService workspaceService();

    /**
     * Builder for creating the TestServerComponent with custom configurations.
     */
    @Component.Builder
    interface Builder {
        /**
         * Sets the test server module with specific test configurations.
         *
         * @param module the test server module
         * @return the builder
         */
        Builder testServerModule(TestServerModule module);

        /**
         * Builds the test server component.
         *
         * @return the built test component
         */
        TestServerComponent build();
    }
}
