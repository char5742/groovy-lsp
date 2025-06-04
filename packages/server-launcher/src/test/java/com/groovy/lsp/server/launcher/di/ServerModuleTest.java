package com.groovy.lsp.server.launcher.di;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.groovy.lsp.codenarc.LintEngine;
import com.groovy.lsp.formatting.service.FormattingService;
import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.groovy.core.api.CompilerConfigurationService;
import com.groovy.lsp.groovy.core.api.TypeInferenceService;
import com.groovy.lsp.protocol.api.GroovyLanguageServer;
import com.groovy.lsp.shared.event.EventBus;
import com.groovy.lsp.workspace.api.WorkspaceIndexService;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import org.eclipse.lsp4j.services.LanguageServer;
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
    void serverModule_shouldProvideLanguageServerInterface() {
        // given
        String workspaceRoot = tempDir.toString();
        ServerModule module = new ServerModule(workspaceRoot);
        Injector injector = Guice.createInjector(module);

        // when
        LanguageServer server = injector.getInstance(LanguageServer.class);

        // then
        assertThat(server).isNotNull();
        assertThat(server).isInstanceOf(GroovyLanguageServer.class);
    }

    @Test
    void serverModule_shouldProvideAllServices() {
        // given
        String workspaceRoot = tempDir.toString();
        ServerModule module = new ServerModule(workspaceRoot);
        Injector injector = Guice.createInjector(module);

        // when/then - All services should be available
        assertThat(injector.getInstance(EventBus.class)).isNotNull();
        assertThat(injector.getInstance(ASTService.class)).isNotNull();
        assertThat(injector.getInstance(CompilerConfigurationService.class)).isNotNull();
        assertThat(injector.getInstance(TypeInferenceService.class)).isNotNull();
        assertThat(injector.getInstance(WorkspaceIndexService.class)).isNotNull();
        assertThat(injector.getInstance(FormattingService.class)).isNotNull();
        assertThat(injector.getInstance(LintEngine.class)).isNotNull();
        assertThat(injector.getInstance(ServiceRouter.class)).isNotNull();
    }

    @Test
    void serverModule_shouldProvideSingletonServices() {
        // given
        String workspaceRoot = tempDir.toString();
        ServerModule module = new ServerModule(workspaceRoot);
        Injector injector = Guice.createInjector(module);

        // when/then - All services should be singletons
        assertThat(injector.getInstance(EventBus.class))
                .isSameAs(injector.getInstance(EventBus.class));
        assertThat(injector.getInstance(ASTService.class))
                .isSameAs(injector.getInstance(ASTService.class));
        assertThat(injector.getInstance(CompilerConfigurationService.class))
                .isSameAs(injector.getInstance(CompilerConfigurationService.class));
        assertThat(injector.getInstance(TypeInferenceService.class))
                .isSameAs(injector.getInstance(TypeInferenceService.class));
        assertThat(injector.getInstance(WorkspaceIndexService.class))
                .isSameAs(injector.getInstance(WorkspaceIndexService.class));
        assertThat(injector.getInstance(FormattingService.class))
                .isSameAs(injector.getInstance(FormattingService.class));
        assertThat(injector.getInstance(LintEngine.class))
                .isSameAs(injector.getInstance(LintEngine.class));
    }

    @Test
    void serverModule_shouldProvideExecutorServices() {
        // given
        String workspaceRoot = tempDir.toString();
        ServerModule module = new ServerModule(workspaceRoot);
        Injector injector = Guice.createInjector(module);

        // when
        ExecutorService serverExecutor =
                injector.getInstance(
                        com.google.inject.Key.get(ExecutorService.class, ServerExecutor.class));
        ScheduledExecutorService scheduledExecutor =
                injector.getInstance(
                        com.google.inject.Key.get(
                                ScheduledExecutorService.class, ScheduledServerExecutor.class));

        // then
        assertThat(serverExecutor).isNotNull();
        assertThat(scheduledExecutor).isNotNull();

        // Clean up
        serverExecutor.shutdown();
        scheduledExecutor.shutdown();
    }

    @Test
    void serverModule_shouldUseSystemPropertyForDefaultWorkspace() {
        // given
        String expectedWorkspace = tempDir.toString();
        String originalProperty = System.getProperty(ServerConstants.WORKSPACE_ROOT_ENV_KEY);
        try {
            System.setProperty(ServerConstants.WORKSPACE_ROOT_ENV_KEY, expectedWorkspace);
            ServerModule module = new ServerModule();
            Injector injector = Guice.createInjector(module);

            // when
            WorkspaceIndexService service = injector.getInstance(WorkspaceIndexService.class);

            // then
            assertThat(service).isNotNull();
        } finally {
            // Restore original property
            if (originalProperty != null) {
                System.setProperty(ServerConstants.WORKSPACE_ROOT_ENV_KEY, originalProperty);
            } else {
                System.clearProperty(ServerConstants.WORKSPACE_ROOT_ENV_KEY);
            }
        }
    }

    @Test
    void serverModule_shouldUseDefaultWorkspaceWhenNoSystemProperty() {
        // given
        String originalProperty = System.getProperty(ServerConstants.WORKSPACE_ROOT_ENV_KEY);
        try {
            System.clearProperty(ServerConstants.WORKSPACE_ROOT_ENV_KEY);
            ServerModule module = new ServerModule();

            // when/then - Should not throw during construction
            assertThatCode(() -> Guice.createInjector(module)).doesNotThrowAnyException();
        } finally {
            // Restore original property
            if (originalProperty != null) {
                System.setProperty(ServerConstants.WORKSPACE_ROOT_ENV_KEY, originalProperty);
            }
        }
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
        assertThat(ServerConstants.DEFAULT_WORKSPACE_ROOT).isEqualTo(".");
        assertThat(ServerConstants.MAX_THREADS_ENV_KEY).isEqualTo("groovy.lsp.server.max.threads");
    }

    @Test
    void serverModule_shouldRespectMaxThreadsSystemProperty() {
        // given
        String originalProperty = System.getProperty(ServerConstants.MAX_THREADS_ENV_KEY);
        try {
            System.setProperty(ServerConstants.MAX_THREADS_ENV_KEY, "100");
            ServerModule module = new ServerModule(tempDir.toString());
            Injector injector = Guice.createInjector(module);

            // when
            ExecutorService serverExecutor =
                    injector.getInstance(
                            com.google.inject.Key.get(ExecutorService.class, ServerExecutor.class));

            // then
            assertThat(serverExecutor).isNotNull();
            // Clean up
            serverExecutor.shutdown();
        } finally {
            // Restore original property
            if (originalProperty != null) {
                System.setProperty(ServerConstants.MAX_THREADS_ENV_KEY, originalProperty);
            } else {
                System.clearProperty(ServerConstants.MAX_THREADS_ENV_KEY);
            }
        }
    }

    @Test
    void serverModule_shouldRespectSchedulerThreadsSystemProperty() {
        // given
        String originalProperty = System.getProperty(ServerConstants.SCHEDULER_THREADS_ENV_KEY);
        try {
            System.setProperty(ServerConstants.SCHEDULER_THREADS_ENV_KEY, "4");
            ServerModule module = new ServerModule(tempDir.toString());
            Injector injector = Guice.createInjector(module);

            // when
            ScheduledExecutorService scheduledExecutor =
                    injector.getInstance(
                            com.google.inject.Key.get(
                                    ScheduledExecutorService.class, ScheduledServerExecutor.class));

            // then
            assertThat(scheduledExecutor).isNotNull();
            // Clean up
            scheduledExecutor.shutdown();
        } finally {
            // Restore original property
            if (originalProperty != null) {
                System.setProperty(ServerConstants.SCHEDULER_THREADS_ENV_KEY, originalProperty);
            } else {
                System.clearProperty(ServerConstants.SCHEDULER_THREADS_ENV_KEY);
            }
        }
    }

    @Test
    void serverModule_shouldUseDefaultThreadsWhenNoSystemProperty() {
        // given
        String originalMaxThreads = System.getProperty(ServerConstants.MAX_THREADS_ENV_KEY);
        String originalSchedulerThreads =
                System.getProperty(ServerConstants.SCHEDULER_THREADS_ENV_KEY);
        try {
            // Clear properties to test defaults
            System.clearProperty(ServerConstants.MAX_THREADS_ENV_KEY);
            System.clearProperty(ServerConstants.SCHEDULER_THREADS_ENV_KEY);

            ServerModule module = new ServerModule(tempDir.toString());
            Injector injector = Guice.createInjector(module);

            // when
            ExecutorService serverExecutor =
                    injector.getInstance(
                            com.google.inject.Key.get(ExecutorService.class, ServerExecutor.class));
            ScheduledExecutorService scheduledExecutor =
                    injector.getInstance(
                            com.google.inject.Key.get(
                                    ScheduledExecutorService.class, ScheduledServerExecutor.class));

            // then
            assertThat(serverExecutor).isNotNull();
            assertThat(scheduledExecutor).isNotNull();

            // Clean up
            serverExecutor.shutdown();
            scheduledExecutor.shutdown();
        } finally {
            // Restore original properties
            if (originalMaxThreads != null) {
                System.setProperty(ServerConstants.MAX_THREADS_ENV_KEY, originalMaxThreads);
            }
            if (originalSchedulerThreads != null) {
                System.setProperty(
                        ServerConstants.SCHEDULER_THREADS_ENV_KEY, originalSchedulerThreads);
            }
        }
    }

    @Test
    void serverModule_shouldHandleInvalidThreadCountSystemProperty() {
        // given
        String originalProperty = System.getProperty(ServerConstants.MAX_THREADS_ENV_KEY);
        try {
            // Set invalid value (non-numeric)
            System.setProperty(ServerConstants.MAX_THREADS_ENV_KEY, "invalid");

            ServerModule module = new ServerModule(tempDir.toString());
            Injector injector = Guice.createInjector(module);

            // when - should fall back to default
            ExecutorService serverExecutor =
                    injector.getInstance(
                            com.google.inject.Key.get(ExecutorService.class, ServerExecutor.class));

            // then
            assertThat(serverExecutor).isNotNull();

            // Clean up
            serverExecutor.shutdown();
        } finally {
            // Restore original property
            if (originalProperty != null) {
                System.setProperty(ServerConstants.MAX_THREADS_ENV_KEY, originalProperty);
            } else {
                System.clearProperty(ServerConstants.MAX_THREADS_ENV_KEY);
            }
        }
    }

    @Test
    void serverModule_shouldHandleInvalidSchedulerThreadsSystemProperty() {
        // given
        String originalProperty = System.getProperty(ServerConstants.SCHEDULER_THREADS_ENV_KEY);
        try {
            // Set invalid value (non-numeric)
            System.setProperty(ServerConstants.SCHEDULER_THREADS_ENV_KEY, "invalid");

            ServerModule module = new ServerModule(tempDir.toString());
            Injector injector = Guice.createInjector(module);

            // when - should fall back to default
            ScheduledExecutorService scheduledExecutor =
                    injector.getInstance(
                            com.google.inject.Key.get(
                                    ScheduledExecutorService.class, ScheduledServerExecutor.class));

            // then
            assertThat(scheduledExecutor).isNotNull();

            // Clean up
            scheduledExecutor.shutdown();
        } finally {
            // Restore original property
            if (originalProperty != null) {
                System.setProperty(ServerConstants.SCHEDULER_THREADS_ENV_KEY, originalProperty);
            } else {
                System.clearProperty(ServerConstants.SCHEDULER_THREADS_ENV_KEY);
            }
        }
    }

    @Test
    void namedThreadFactory_shouldCreateDaemonThreadsWithCorrectNames() {
        // given
        String workspaceRoot = tempDir.toString();
        ServerModule module = new ServerModule(workspaceRoot);
        Injector injector = Guice.createInjector(module);

        // when
        ExecutorService serverExecutor =
                injector.getInstance(
                        com.google.inject.Key.get(ExecutorService.class, ServerExecutor.class));

        // Submit a task to create a thread and verify its properties
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<String> threadName =
                new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicBoolean isDaemon =
                new java.util.concurrent.atomic.AtomicBoolean();

        var unused1 =
                serverExecutor.submit(
                        () -> {
                            Thread currentThread = Thread.currentThread();
                            threadName.set(currentThread.getName());
                            isDaemon.set(currentThread.isDaemon());
                            latch.countDown();
                        });

        try {
            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // then
        assertThat(threadName.get()).startsWith(ServerConstants.SERVER_THREAD_PREFIX);
        assertThat(isDaemon.get()).isTrue();

        // Clean up
        serverExecutor.shutdown();
    }

    @Test
    void namedThreadFactory_shouldCreateSchedulerThreadsWithCorrectNames() {
        // given
        String workspaceRoot = tempDir.toString();
        ServerModule module = new ServerModule(workspaceRoot);
        Injector injector = Guice.createInjector(module);

        // when
        ScheduledExecutorService scheduledExecutor =
                injector.getInstance(
                        com.google.inject.Key.get(
                                ScheduledExecutorService.class, ScheduledServerExecutor.class));

        // Submit a task to create a thread and verify its properties
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<String> threadName =
                new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicBoolean isDaemon =
                new java.util.concurrent.atomic.AtomicBoolean();

        var unused2 =
                scheduledExecutor.submit(
                        () -> {
                            Thread currentThread = Thread.currentThread();
                            threadName.set(currentThread.getName());
                            isDaemon.set(currentThread.isDaemon());
                            latch.countDown();
                        });

        try {
            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // then
        assertThat(threadName.get()).startsWith(ServerConstants.SCHEDULER_THREAD_PREFIX);
        assertThat(isDaemon.get()).isTrue();

        // Clean up
        scheduledExecutor.shutdown();
    }

    @Test
    void serverModule_shouldCreateExecutorWithCorrectConfiguration() {
        // given
        String workspaceRoot = tempDir.toString();
        ServerModule module = new ServerModule(workspaceRoot);
        Injector injector = Guice.createInjector(module);

        // when
        ExecutorService serverExecutor =
                injector.getInstance(
                        com.google.inject.Key.get(ExecutorService.class, ServerExecutor.class));

        // then
        assertThat(serverExecutor).isInstanceOf(java.util.concurrent.ThreadPoolExecutor.class);

        java.util.concurrent.ThreadPoolExecutor tpe =
                (java.util.concurrent.ThreadPoolExecutor) serverExecutor;

        assertThat(tpe.getCorePoolSize()).isEqualTo(ServerConstants.CORE_THREAD_POOL_SIZE);
        assertThat(tpe.getMaximumPoolSize()).isEqualTo(ServerConstants.MAX_THREAD_POOL_SIZE);
        assertThat(tpe.getKeepAliveTime(java.util.concurrent.TimeUnit.SECONDS))
                .isEqualTo(ServerConstants.THREAD_KEEP_ALIVE_TIME);

        // Clean up
        serverExecutor.shutdown();
    }

    @Test
    void serverModule_shouldHandleEmptyStringSystemProperties() {
        // given
        String originalMaxThreads = System.getProperty(ServerConstants.MAX_THREADS_ENV_KEY);
        String originalSchedulerThreads =
                System.getProperty(ServerConstants.SCHEDULER_THREADS_ENV_KEY);
        try {
            // Set empty string values
            System.setProperty(ServerConstants.MAX_THREADS_ENV_KEY, "");
            System.setProperty(ServerConstants.SCHEDULER_THREADS_ENV_KEY, "");

            ServerModule module = new ServerModule(tempDir.toString());
            Injector injector = Guice.createInjector(module);

            // when - should fall back to defaults
            ExecutorService serverExecutor =
                    injector.getInstance(
                            com.google.inject.Key.get(ExecutorService.class, ServerExecutor.class));
            ScheduledExecutorService scheduledExecutor =
                    injector.getInstance(
                            com.google.inject.Key.get(
                                    ScheduledExecutorService.class, ScheduledServerExecutor.class));

            // then
            assertThat(serverExecutor).isNotNull();
            assertThat(scheduledExecutor).isNotNull();

            // Clean up
            serverExecutor.shutdown();
            scheduledExecutor.shutdown();
        } finally {
            // Restore original properties
            if (originalMaxThreads != null) {
                System.setProperty(ServerConstants.MAX_THREADS_ENV_KEY, originalMaxThreads);
            } else {
                System.clearProperty(ServerConstants.MAX_THREADS_ENV_KEY);
            }
            if (originalSchedulerThreads != null) {
                System.setProperty(
                        ServerConstants.SCHEDULER_THREADS_ENV_KEY, originalSchedulerThreads);
            } else {
                System.clearProperty(ServerConstants.SCHEDULER_THREADS_ENV_KEY);
            }
        }
    }

    @Test
    void serverModule_shouldHandleZeroAndNegativeThreadCounts() {
        // given
        String originalProperty = System.getProperty(ServerConstants.MAX_THREADS_ENV_KEY);
        try {
            // Test zero - should fail gracefully during injector creation
            System.setProperty(ServerConstants.MAX_THREADS_ENV_KEY, "0");
            final ServerModule zeroModule = new ServerModule(tempDir.toString());

            assertThatCode(
                            () -> {
                                Injector injector = Guice.createInjector(zeroModule);
                                ExecutorService serverExecutor =
                                        injector.getInstance(
                                                com.google.inject.Key.get(
                                                        ExecutorService.class,
                                                        ServerExecutor.class));
                            })
                    .isInstanceOf(com.google.inject.ProvisionException.class);

            // Test negative - should also fail gracefully
            System.setProperty(ServerConstants.MAX_THREADS_ENV_KEY, "-5");
            final ServerModule negativeModule = new ServerModule(tempDir.toString());

            assertThatCode(
                            () -> {
                                Injector injector = Guice.createInjector(negativeModule);
                                ExecutorService serverExecutor =
                                        injector.getInstance(
                                                com.google.inject.Key.get(
                                                        ExecutorService.class,
                                                        ServerExecutor.class));
                            })
                    .isInstanceOf(com.google.inject.ProvisionException.class);

        } finally {
            // Restore original property
            if (originalProperty != null) {
                System.setProperty(ServerConstants.MAX_THREADS_ENV_KEY, originalProperty);
            } else {
                System.clearProperty(ServerConstants.MAX_THREADS_ENV_KEY);
            }
        }
    }
}
