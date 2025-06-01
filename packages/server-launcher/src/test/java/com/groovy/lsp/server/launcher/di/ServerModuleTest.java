package com.groovy.lsp.server.launcher.di;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.groovy.lsp.protocol.api.GroovyLanguageServer;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for ServerModule dependency injection configuration.
 */
class ServerModuleTest {

    @TempDir Path tempDir;

    @Test
    void serverModule_shouldProvideLanguageServer() {
        // given
        String workspaceRoot = tempDir.toString();
        ServerModule module = new ServerModule(workspaceRoot);
        Injector injector = Guice.createInjector(module);

        // when
        GroovyLanguageServer server = injector.getInstance(GroovyLanguageServer.class);

        // then
        assertThat(server).isNotNull();
    }

    @Test
    void serverModule_shouldProvideSingletonLanguageServer() {
        // given
        String workspaceRoot = tempDir.toString();
        ServerModule module = new ServerModule(workspaceRoot);
        Injector injector = Guice.createInjector(module);

        // when
        GroovyLanguageServer server1 = injector.getInstance(GroovyLanguageServer.class);
        GroovyLanguageServer server2 = injector.getInstance(GroovyLanguageServer.class);

        // then
        assertThat(server1).isSameAs(server2);
    }

    @Test
    void serverModule_shouldHandleNullWorkspaceRoot() {
        // given
        ServerModule module = new ServerModule(null);

        // when/then - Should not throw during construction
        assertThat(module).isNotNull();
    }

    @Test
    void serverModule_shouldHandleRelativeWorkspacePath() {
        // given
        ServerModule module = new ServerModule("./relative/path");

        // when/then - Should not throw during construction
        assertThat(module).isNotNull();
    }

    @Test
    void serverConstants_shouldDefineDefaultValues() {
        // then
        assertThat(ServerConstants.DEFAULT_SOCKET_HOST).isEqualTo("localhost");
        assertThat(ServerConstants.DEFAULT_SOCKET_PORT).isEqualTo(4389);
        assertThat(ServerConstants.DEFAULT_SCHEDULER_THREADS).isEqualTo(2);
        assertThat(ServerConstants.SCHEDULER_THREADS_ENV_KEY)
                .isEqualTo("groovy.lsp.scheduler.threads");
        assertThat(ServerConstants.WORKSPACE_ROOT_ENV_KEY).isEqualTo("groovy.lsp.workspace.root");
        assertThat(ServerConstants.MAX_THREAD_POOL_SIZE).isEqualTo(50);
        assertThat(ServerConstants.CORE_THREAD_POOL_SIZE).isEqualTo(10);
        assertThat(ServerConstants.THREAD_KEEP_ALIVE_TIME).isEqualTo(60L);
        assertThat(ServerConstants.EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS).isEqualTo(5);
        assertThat(ServerConstants.SERVER_THREAD_PREFIX).isEqualTo("groovy-lsp-server");
        assertThat(ServerConstants.SCHEDULER_THREAD_PREFIX).isEqualTo("groovy-lsp-scheduler");
    }
}
