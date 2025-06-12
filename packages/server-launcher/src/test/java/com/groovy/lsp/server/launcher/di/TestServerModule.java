package com.groovy.lsp.server.launcher.di;

import static org.mockito.Mockito.mock;

import com.groovy.lsp.codenarc.LintEngine;
import com.groovy.lsp.codenarc.QuickFixMapper;
import com.groovy.lsp.codenarc.RuleSetProvider;
import com.groovy.lsp.formatting.service.FormattingService;
import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.groovy.core.api.CompilerConfigurationService;
import com.groovy.lsp.groovy.core.api.GroovyCoreFactory;
import com.groovy.lsp.groovy.core.api.IncrementalCompilationService;
import com.groovy.lsp.groovy.core.api.TypeInferenceService;
import com.groovy.lsp.protocol.api.GroovyLanguageServer;
import com.groovy.lsp.protocol.api.IServiceRouter;
import com.groovy.lsp.protocol.internal.document.DocumentManager;
import com.groovy.lsp.protocol.internal.impl.GroovyTextDocumentService;
import com.groovy.lsp.protocol.internal.impl.GroovyWorkspaceService;
import com.groovy.lsp.shared.event.EventBus;
import com.groovy.lsp.shared.event.EventBusFactory;
import com.groovy.lsp.shared.workspace.api.WorkspaceIndexService;
import com.groovy.lsp.workspace.api.WorkspaceIndexFactory;
import dagger.Module;
import dagger.Provides;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Named;
import javax.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * Test module for Dagger that provides test doubles and configurations for unit testing.
 * This module provides flexible test configurations without extending ServerModule.
 *
 * <p>Use this module when you need:
 * <ul>
 *   <li>Mock implementations of services for isolated unit tests</li>
 *   <li>Real implementations with test-specific configurations</li>
 *   <li>Custom executors suitable for testing (e.g., direct executors)</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * TestServerComponent component = DaggerTestServerComponent.builder()
 *     .testServerModule(new TestServerModule.Builder()
 *         .withMocks()
 *         .withTestWorkspace("/test/workspace")
 *         .build())
 *     .build();
 * }</pre>
 */
@Module
public class TestServerModule {

    private final boolean useMocks;
    private final boolean useDirectExecutor;
    private final String workspaceRoot;

    // Optional custom instances for testing
    private final @Nullable ASTService customAstService;
    private final @Nullable TypeInferenceService customTypeInferenceService;
    private final @Nullable WorkspaceIndexService customWorkspaceIndexService;
    private final @Nullable CompilerConfigurationService customCompilerConfigurationService;
    private final @Nullable IncrementalCompilationService customIncrementalCompilationService;
    private final @Nullable FormattingService customFormattingService;
    private final @Nullable LintEngine customLintEngine;
    private final @Nullable EventBus customEventBus;

    private TestServerModule(Builder builder) {
        this.workspaceRoot = builder.workspaceRoot;
        this.useMocks = builder.useMocks;
        this.useDirectExecutor = builder.useDirectExecutor;
        this.customAstService = builder.customAstService;
        this.customTypeInferenceService = builder.customTypeInferenceService;
        this.customWorkspaceIndexService = builder.customWorkspaceIndexService;
        this.customCompilerConfigurationService = builder.customCompilerConfigurationService;
        this.customIncrementalCompilationService = builder.customIncrementalCompilationService;
        this.customFormattingService = builder.customFormattingService;
        this.customLintEngine = builder.customLintEngine;
        this.customEventBus = builder.customEventBus;
    }

    @Provides
    @Singleton
    public EventBus provideEventBus() {
        if (customEventBus != null) {
            return customEventBus;
        }
        return useMocks ? mock(EventBus.class) : EventBusFactory.create();
    }

    @Provides
    @Singleton
    public ASTService provideASTService() {
        if (customAstService != null) {
            return customAstService;
        }
        return useMocks
                ? mock(ASTService.class)
                : GroovyCoreFactory.getInstance().createASTService();
    }

    @Provides
    @Singleton
    public CompilerConfigurationService provideCompilerConfigurationService() {
        if (customCompilerConfigurationService != null) {
            return customCompilerConfigurationService;
        }
        return useMocks
                ? mock(CompilerConfigurationService.class)
                : GroovyCoreFactory.getInstance().createCompilerConfigurationService();
    }

    @Provides
    @Singleton
    public TypeInferenceService provideTypeInferenceService(ASTService astService) {
        if (customTypeInferenceService != null) {
            return customTypeInferenceService;
        }
        return useMocks
                ? mock(TypeInferenceService.class)
                : GroovyCoreFactory.getInstance().createTypeInferenceService(astService);
    }

    @Provides
    @Singleton
    public IncrementalCompilationService provideIncrementalCompilationService() {
        if (customIncrementalCompilationService != null) {
            return customIncrementalCompilationService;
        }
        return useMocks
                ? mock(IncrementalCompilationService.class)
                : GroovyCoreFactory.getInstance().createIncrementalCompilationService();
    }

    @Provides
    @Singleton
    public WorkspaceIndexService provideWorkspaceIndexService(
            @SuppressWarnings("UnusedVariable") EventBus eventBus) {
        if (customWorkspaceIndexService != null) {
            return customWorkspaceIndexService;
        }
        return useMocks
                ? mock(WorkspaceIndexService.class)
                : WorkspaceIndexFactory.createWorkspaceIndexService(Paths.get(workspaceRoot));
    }

    @Provides
    @Singleton
    public FormattingService provideFormattingService() {
        if (customFormattingService != null) {
            return customFormattingService;
        }
        return useMocks ? mock(FormattingService.class) : new FormattingService();
    }

    @Provides
    @Singleton
    public LintEngine provideLintEngine() {
        if (customLintEngine != null) {
            return customLintEngine;
        }
        if (useMocks) {
            return mock(LintEngine.class);
        }
        // For real instances in tests, create with default configuration
        return new LintEngine(new RuleSetProvider(), new QuickFixMapper());
    }

    @Provides
    @Singleton
    @Named("serverExecutor")
    public ExecutorService provideServerExecutor() {
        if (useDirectExecutor) {
            // For testing, use a direct executor that runs tasks synchronously
            return new DirectExecutorService();
        }
        // Use a single thread executor for predictable test behavior
        return Executors.newSingleThreadExecutor(
                r -> {
                    Thread t = new Thread(r, "test-server-thread");
                    t.setDaemon(true);
                    return t;
                });
    }

    @Provides
    @Singleton
    @Named("scheduledExecutor")
    public ScheduledExecutorService provideScheduledExecutor() {
        // For testing, use a single thread scheduled executor
        return Executors.newSingleThreadScheduledExecutor(
                r -> {
                    Thread t = new Thread(r, "test-scheduler-thread");
                    t.setDaemon(true);
                    return t;
                });
    }

    // Additional test-specific providers

    @Provides
    @Singleton
    public IServiceRouter provideServiceRouter(
            ASTService astService,
            CompilerConfigurationService compilerConfigurationService,
            IncrementalCompilationService incrementalCompilationService,
            TypeInferenceService typeInferenceService,
            WorkspaceIndexService workspaceIndexService,
            FormattingService formattingService,
            LintEngine lintEngine) {
        // Always use real ServiceRouter, but with potentially mocked dependencies
        return new ServiceRouter(
                astService,
                compilerConfigurationService,
                incrementalCompilationService,
                typeInferenceService,
                workspaceIndexService,
                formattingService,
                lintEngine);
    }

    @Provides
    @Singleton
    public DocumentManager provideDocumentManager() {
        // Always use real DocumentManager for testing
        return new DocumentManager();
    }

    @Provides
    @Singleton
    public GroovyTextDocumentService provideTextDocumentService(
            IServiceRouter serviceRouter, DocumentManager documentManager) {
        // Always use real service implementations
        return new GroovyTextDocumentService(serviceRouter, documentManager);
    }

    @Provides
    @Singleton
    public GroovyWorkspaceService provideWorkspaceService() {
        // Always use real service implementation
        return new GroovyWorkspaceService();
    }

    @Provides
    @Singleton
    public GroovyLanguageServer provideLanguageServer(
            GroovyTextDocumentService textDocumentService,
            GroovyWorkspaceService workspaceService,
            IServiceRouter serviceRouter) {
        return new GroovyLanguageServer(textDocumentService, workspaceService, serviceRouter);
    }

    /**
     * Builder for creating TestServerModule instances with various configurations.
     */
    public static class Builder {
        private boolean useMocks = false;
        private boolean useDirectExecutor = false;
        private String workspaceRoot = "/tmp/test-workspace";

        // Custom service instances
        private @Nullable ASTService customAstService;
        private @Nullable TypeInferenceService customTypeInferenceService;
        private @Nullable WorkspaceIndexService customWorkspaceIndexService;
        private @Nullable CompilerConfigurationService customCompilerConfigurationService;
        private @Nullable IncrementalCompilationService customIncrementalCompilationService;
        private @Nullable FormattingService customFormattingService;
        private @Nullable LintEngine customLintEngine;
        private @Nullable EventBus customEventBus;

        /**
         * Configure the module to use mock implementations for all services.
         */
        public Builder withMocks() {
            this.useMocks = true;
            return this;
        }

        /**
         * Configure the module to use real implementations for all services.
         */
        public Builder withRealImplementations() {
            this.useMocks = false;
            return this;
        }

        /**
         * Use a direct executor that runs tasks synchronously (useful for deterministic tests).
         */
        public Builder withDirectExecutor() {
            this.useDirectExecutor = true;
            return this;
        }

        /**
         * Set the test workspace root directory.
         */
        public Builder withTestWorkspace(String workspaceRoot) {
            this.workspaceRoot = workspaceRoot;
            return this;
        }

        /**
         * Set the test workspace root directory using a Path.
         */
        public Builder withTestWorkspace(Path workspaceRoot) {
            this.workspaceRoot = workspaceRoot.toString();
            return this;
        }

        /**
         * Provide a custom ASTService instance (overrides useMocks for this service).
         */
        public Builder withAstService(ASTService astService) {
            this.customAstService = astService;
            return this;
        }

        /**
         * Provide a custom TypeInferenceService instance (overrides useMocks for this service).
         */
        public Builder withTypeInferenceService(TypeInferenceService typeInferenceService) {
            this.customTypeInferenceService = typeInferenceService;
            return this;
        }

        /**
         * Provide a custom WorkspaceIndexService instance (overrides useMocks for this service).
         */
        public Builder withWorkspaceIndexService(WorkspaceIndexService workspaceIndexService) {
            this.customWorkspaceIndexService = workspaceIndexService;
            return this;
        }

        /**
         * Provide a custom CompilerConfigurationService instance (overrides useMocks for this service).
         */
        public Builder withCompilerConfigurationService(CompilerConfigurationService service) {
            this.customCompilerConfigurationService = service;
            return this;
        }

        /**
         * Provide a custom IncrementalCompilationService instance (overrides useMocks for this service).
         */
        public Builder withIncrementalCompilationService(IncrementalCompilationService service) {
            this.customIncrementalCompilationService = service;
            return this;
        }

        /**
         * Provide a custom FormattingService instance (overrides useMocks for this service).
         */
        public Builder withFormattingService(FormattingService formattingService) {
            this.customFormattingService = formattingService;
            return this;
        }

        /**
         * Provide a custom LintEngine instance (overrides useMocks for this service).
         */
        public Builder withLintEngine(LintEngine lintEngine) {
            this.customLintEngine = lintEngine;
            return this;
        }

        /**
         * Provide a custom EventBus instance (overrides useMocks for this service).
         */
        public Builder withEventBus(EventBus eventBus) {
            this.customEventBus = eventBus;
            return this;
        }

        /**
         * Build the TestServerModule with the configured settings.
         */
        public TestServerModule build() {
            return new TestServerModule(this);
        }
    }

    /**
     * A simple direct executor service for testing that runs tasks synchronously.
     */
    private static class DirectExecutorService
            extends java.util.concurrent.AbstractExecutorService {
        private boolean shutdown = false;

        @Override
        public void execute(Runnable command) {
            if (!shutdown) {
                command.run();
            }
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public java.util.List<Runnable> shutdownNow() {
            shutdown = true;
            return java.util.Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, java.util.concurrent.TimeUnit unit) {
            return shutdown;
        }
    }
}
