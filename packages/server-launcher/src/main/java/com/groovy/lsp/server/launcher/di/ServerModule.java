package com.groovy.lsp.server.launcher.di;

import static com.groovy.lsp.server.launcher.di.ServerConstants.CORE_THREAD_POOL_SIZE;
import static com.groovy.lsp.server.launcher.di.ServerConstants.DEFAULT_SCHEDULER_THREADS;
import static com.groovy.lsp.server.launcher.di.ServerConstants.DEFAULT_WORKSPACE_ROOT;
import static com.groovy.lsp.server.launcher.di.ServerConstants.MAX_THREADS_ENV_KEY;
import static com.groovy.lsp.server.launcher.di.ServerConstants.MAX_THREAD_POOL_SIZE;
import static com.groovy.lsp.server.launcher.di.ServerConstants.SCHEDULER_THREADS_ENV_KEY;
import static com.groovy.lsp.server.launcher.di.ServerConstants.SCHEDULER_THREAD_PREFIX;
import static com.groovy.lsp.server.launcher.di.ServerConstants.SERVER_THREAD_PREFIX;
import static com.groovy.lsp.server.launcher.di.ServerConstants.THREAD_KEEP_ALIVE_TIME;
import static com.groovy.lsp.server.launcher.di.ServerConstants.WORKSPACE_ROOT_ENV_KEY;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guice module for configuring the Language Server dependencies.
 *
 * This module sets up all the necessary bindings for the server components,
 * including services, executors, and event handling.
 */
public class ServerModule extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(ServerModule.class);

    private final String workspaceRoot;

    public ServerModule() {
        this(System.getProperty(WORKSPACE_ROOT_ENV_KEY, DEFAULT_WORKSPACE_ROOT));
    }

    public ServerModule(String workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
    }

    @Override
    protected void configure() {
        // Bind GroovyLanguageServer as singleton
        bind(GroovyLanguageServer.class).in(Singleton.class);

        // Bind LanguageServer interface to our implementation
        bind(LanguageServer.class).to(GroovyLanguageServer.class);

        // Bind text document and workspace services
        bind(GroovyTextDocumentService.class).in(Singleton.class);
        bind(GroovyWorkspaceService.class).in(Singleton.class);

        // Bind document manager
        bind(DocumentManager.class).in(Singleton.class);

        // Bind service router
        bind(ServiceRouter.class).in(Singleton.class);
        bind(IServiceRouter.class).to(ServiceRouter.class);

        logger.info("Server module configured");
    }

    @Provides
    @Singleton
    EventBus provideEventBus() {
        return EventBusFactory.create();
    }

    @Provides
    @Singleton
    ASTService provideASTService() {
        return GroovyCoreFactory.getInstance().createASTService();
    }

    @Provides
    @Singleton
    CompilerConfigurationService provideCompilerConfigurationService() {
        return GroovyCoreFactory.getInstance().createCompilerConfigurationService();
    }

    @Provides
    @Singleton
    TypeInferenceService provideTypeInferenceService(ASTService astService) {
        return GroovyCoreFactory.getInstance().createTypeInferenceService(astService);
    }

    @Provides
    @Singleton
    IncrementalCompilationService provideIncrementalCompilationService() {
        return GroovyCoreFactory.getInstance().createIncrementalCompilationService();
    }

    @Provides
    @Singleton
    WorkspaceIndexService provideWorkspaceIndexService(
            @SuppressWarnings("UnusedVariable") EventBus eventBus) {
        // TODO: Use EventBus when workspace index service supports event publishing
        logger.info("Creating WorkspaceIndexService with root: {}", workspaceRoot);
        return WorkspaceIndexFactory.createWorkspaceIndexService(
                java.nio.file.Paths.get(workspaceRoot));
    }

    @Provides
    @Singleton
    FormattingService provideFormattingService() {
        return new FormattingService();
    }

    @Provides
    @Singleton
    LintEngine provideLintEngine() {
        return new LintEngine(new RuleSetProvider(), new QuickFixMapper());
    }

    @Provides
    @Singleton
    @ServerExecutor
    ExecutorService provideServerExecutor() {
        int maxThreads = Integer.getInteger(MAX_THREADS_ENV_KEY, MAX_THREAD_POOL_SIZE);
        logger.info("Creating thread pool with max {} threads", maxThreads);

        return new ThreadPoolExecutor(
                CORE_THREAD_POOL_SIZE,
                maxThreads,
                THREAD_KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new NamedThreadFactory(SERVER_THREAD_PREFIX));
    }

    @Provides
    @Singleton
    @ScheduledServerExecutor
    ScheduledExecutorService provideScheduledExecutor() {
        int poolSize = Integer.getInteger(SCHEDULER_THREADS_ENV_KEY, DEFAULT_SCHEDULER_THREADS);
        logger.info("Creating scheduled thread pool with {} threads", poolSize);
        return Executors.newScheduledThreadPool(
                poolSize, new NamedThreadFactory(SCHEDULER_THREAD_PREFIX));
    }

    /**
     * Custom thread factory for named threads.
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger();

        NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, prefix + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
