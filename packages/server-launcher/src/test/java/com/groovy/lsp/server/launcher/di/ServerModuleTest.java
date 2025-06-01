package com.groovy.lsp.server.launcher.di;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ServerModule dependency injection configuration.
 */
class ServerModuleTest {

    @Test
    void testModuleCreation() {
        assertThatCode(() -> new ServerModule()).doesNotThrowAnyException();
    }

    @Test
    void testInjectorCreation() {
        ServerModule module = new ServerModule();
        assertThatCode(() -> Guice.createInjector(module)).doesNotThrowAnyException();
    }

    @Test
    void testServerExecutorBinding() {
        Injector injector = Guice.createInjector(new ServerModule());
        ExecutorService executor =
                injector.getInstance(Key.get(ExecutorService.class, ServerExecutor.class));

        assertThat(executor).isNotNull();
        assertThat(executor.isShutdown()).isFalse();

        // Clean up
        executor.shutdown();
    }

    @Test
    void testScheduledServerExecutorBinding() {
        Injector injector = Guice.createInjector(new ServerModule());
        ScheduledExecutorService scheduledExecutor =
                injector.getInstance(
                        Key.get(ScheduledExecutorService.class, ScheduledServerExecutor.class));

        assertThat(scheduledExecutor).isNotNull();
        assertThat(scheduledExecutor.isShutdown()).isFalse();

        // Clean up
        scheduledExecutor.shutdown();
    }

    @Test
    void testServiceRouterBinding() {
        Injector injector = Guice.createInjector(new ServerModule());
        ServiceRouter router = injector.getInstance(ServiceRouter.class);

        assertThat(router).isNotNull();
    }

    @Test
    void testServerConstantsValues() {
        assertThat(ServerConstants.MAX_THREAD_POOL_SIZE).isEqualTo(50);
        assertThat(ServerConstants.CORE_THREAD_POOL_SIZE).isEqualTo(10);
        assertThat(ServerConstants.THREAD_KEEP_ALIVE_TIME).isEqualTo(60L);
        assertThat(ServerConstants.DEFAULT_SCHEDULER_THREADS).isEqualTo(2);
        assertThat(ServerConstants.EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS).isEqualTo(5);
        assertThat(ServerConstants.DEFAULT_SOCKET_PORT).isEqualTo(4389);
        assertThat(ServerConstants.DEFAULT_SOCKET_HOST).isEqualTo("localhost");
        assertThat(ServerConstants.DEFAULT_WORKSPACE_ROOT).isEqualTo(".");
        assertThat(ServerConstants.SERVER_THREAD_PREFIX).isEqualTo("groovy-lsp-server");
        assertThat(ServerConstants.SCHEDULER_THREAD_PREFIX).isEqualTo("groovy-lsp-scheduler");
    }
}
