package com.groovy.lsp.server.launcher.di;

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
import com.groovy.lsp.groovy.core.api.TypeInferenceService;
import com.groovy.lsp.protocol.api.GroovyLanguageServer;
import com.groovy.lsp.shared.event.EventBus;
import com.groovy.lsp.shared.event.EventBusFactory;
import com.groovy.lsp.workspace.api.WorkspaceIndexFactory;
import com.groovy.lsp.workspace.api.WorkspaceIndexService;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Guice module for configuring the Language Server dependencies.
 * 
 * This module sets up all the necessary bindings for the server components,
 * including services, executors, and event handling.
 */
public class ServerModule extends AbstractModule {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerModule.class);
    
    // Configuration keys
    private static final String THREAD_POOL_SIZE_KEY = "groovy.lsp.scheduler.threads";
    private static final String WORKSPACE_ROOT_KEY = "groovy.lsp.workspace.root";
    
    // Default values
    private static final int DEFAULT_SCHEDULER_THREADS = 2;
    private static final String DEFAULT_WORKSPACE_ROOT = ".";
    
    private final String workspaceRoot;
    
    public ServerModule() {
        this(System.getProperty(WORKSPACE_ROOT_KEY, DEFAULT_WORKSPACE_ROOT));
    }
    
    public ServerModule(String workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
    }
    
    @Override
    protected void configure() {
        // Bind LanguageServer interface to our implementation
        bind(LanguageServer.class).to(GroovyLanguageServer.class).in(Singleton.class);
        
        // Bind service router
        bind(ServiceRouter.class).in(Singleton.class);
        
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
    WorkspaceIndexService provideWorkspaceIndexService(EventBus eventBus) {
        logger.info("Creating WorkspaceIndexService with root: {}", workspaceRoot);
        return WorkspaceIndexFactory.createWorkspaceIndexService(java.nio.file.Paths.get(workspaceRoot));
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
        return Executors.newCachedThreadPool(new NamedThreadFactory("groovy-lsp-server"));
    }
    
    @Provides
    @Singleton
    @ScheduledServerExecutor
    ScheduledExecutorService provideScheduledExecutor() {
        int poolSize = Integer.getInteger(THREAD_POOL_SIZE_KEY, DEFAULT_SCHEDULER_THREADS);
        logger.info("Creating scheduled thread pool with {} threads", poolSize);
        return Executors.newScheduledThreadPool(poolSize, new NamedThreadFactory("groovy-lsp-scheduler"));
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