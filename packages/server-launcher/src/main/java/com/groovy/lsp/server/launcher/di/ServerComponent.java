package com.groovy.lsp.server.launcher.di;

import com.groovy.lsp.protocol.api.GroovyLanguageServer;
import dagger.Component;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Dagger component that provides the Language Server and its dependencies.
 * This component is responsible for building the dependency graph and
 * providing access to the main server instance.
 */
@Singleton
@Component(modules = {ServerModule.class})
public interface ServerComponent {

    /**
     * Provides the main GroovyLanguageServer instance.
     *
     * @return the language server instance
     */
    GroovyLanguageServer languageServer();

    /**
     * Provides the server executor service for shutdown.
     *
     * @return the server executor service
     */
    @Named("serverExecutor")
    ExecutorService serverExecutor();

    /**
     * Provides the scheduled executor service for shutdown.
     *
     * @return the scheduled executor service
     */
    @Named("scheduledExecutor")
    ScheduledExecutorService scheduledExecutor();

    /**
     * Builder for creating the ServerComponent with a specific workspace root.
     */
    @Component.Builder
    interface Builder {
        /**
         * Sets the server module with workspace configuration.
         *
         * @param module the server module
         * @return the builder
         */
        Builder serverModule(ServerModule module);

        /**
         * Builds the server component.
         *
         * @return the built component
         */
        ServerComponent build();
    }
}
