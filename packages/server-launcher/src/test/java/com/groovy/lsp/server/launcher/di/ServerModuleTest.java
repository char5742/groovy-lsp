package com.groovy.lsp.server.launcher.di;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.groovy.lsp.protocol.api.GroovyLanguageServer;
import com.groovy.lsp.test.annotations.UnitTest;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for ServerModule dependency injection configuration.
 */
class ServerModuleTest {

    @TempDir @Nullable Path tempDir;

    @UnitTest
    void serverModule_shouldProvideLanguageServer() {
        // given
        String workspaceRoot =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .toString();
        ServerComponent component =
                DaggerServerComponent.builder()
                        .serverModule(new ServerModule(workspaceRoot))
                        .build();

        // when
        GroovyLanguageServer server = component.languageServer();

        // then
        assertThat(server).isNotNull();
    }

    @UnitTest
    void serverModule_shouldProvideSingletonLanguageServer() {
        // given
        String workspaceRoot =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .toString();
        ServerComponent component =
                DaggerServerComponent.builder()
                        .serverModule(new ServerModule(workspaceRoot))
                        .build();

        // when
        GroovyLanguageServer server1 = component.languageServer();
        GroovyLanguageServer server2 = component.languageServer();

        // then
        assertThat(server1).isSameAs(server2);
    }

    @UnitTest
    void serverModule_shouldProvideExecutorServices() {
        // given
        String workspaceRoot =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .toString();
        ServerComponent component =
                DaggerServerComponent.builder()
                        .serverModule(new ServerModule(workspaceRoot))
                        .build();

        // when
        ExecutorService serverExecutor = component.serverExecutor();
        ScheduledExecutorService scheduledExecutor = component.scheduledExecutor();

        // then
        assertThat(serverExecutor).isNotNull();
        assertThat(scheduledExecutor).isNotNull();

        // Clean up
        serverExecutor.shutdown();
        scheduledExecutor.shutdown();
    }

    @UnitTest
    void serverModule_shouldUseSystemPropertyForDefaultWorkspace() {
        // given
        String expectedWorkspace =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .toString();
        String originalProperty = System.getProperty(ServerConstants.WORKSPACE_ROOT_ENV_KEY);
        try {
            System.setProperty(ServerConstants.WORKSPACE_ROOT_ENV_KEY, expectedWorkspace);
            ServerModule module = new ServerModule();
            ServerComponent component =
                    DaggerServerComponent.builder().serverModule(module).build();

            // when
            GroovyLanguageServer server = component.languageServer();

            // then
            assertThat(server).isNotNull();
        } finally {
            // Restore original property
            if (originalProperty != null) {
                System.setProperty(ServerConstants.WORKSPACE_ROOT_ENV_KEY, originalProperty);
            } else {
                System.clearProperty(ServerConstants.WORKSPACE_ROOT_ENV_KEY);
            }
        }
    }

    @UnitTest
    void serverModule_shouldUseDefaultWorkspaceWhenNoSystemProperty() {
        // given
        String originalProperty = System.getProperty(ServerConstants.WORKSPACE_ROOT_ENV_KEY);
        try {
            System.clearProperty(ServerConstants.WORKSPACE_ROOT_ENV_KEY);
            ServerModule module = new ServerModule();

            // when/then - Should not throw during construction
            assertThatCode(() -> DaggerServerComponent.builder().serverModule(module).build())
                    .doesNotThrowAnyException();
        } finally {
            // Restore original property
            if (originalProperty != null) {
                System.setProperty(ServerConstants.WORKSPACE_ROOT_ENV_KEY, originalProperty);
            }
        }
    }

    @UnitTest
    void serverModule_shouldHandleNullWorkspaceRoot() {
        // given
        // Use reflection to test null handling since direct null passing causes NullAway warning
        ServerModule module;
        try {
            java.lang.reflect.Constructor<ServerModule> constructor =
                    ServerModule.class.getConstructor(String.class);
            module = constructor.newInstance((String) null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ServerModule with null", e);
        }

        // when/then - Should not throw during construction
        assertThat(module).isNotNull();
    }

    @UnitTest
    void serverModule_shouldHandleRelativeWorkspacePath() {
        // given
        ServerModule module = new ServerModule("./relative/path");

        // when/then - Should not throw during construction
        assertThat(module).isNotNull();
    }

    @UnitTest
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

    @UnitTest
    void serverModule_shouldRespectMaxThreadsSystemProperty() {
        // given
        String originalProperty = System.getProperty(ServerConstants.MAX_THREADS_ENV_KEY);
        try {
            System.setProperty(ServerConstants.MAX_THREADS_ENV_KEY, "100");
            ServerModule module =
                    new ServerModule(
                            Objects.requireNonNull(
                                            tempDir, "tempDir should be initialized by JUnit")
                                    .toString());
            ServerComponent component =
                    DaggerServerComponent.builder().serverModule(module).build();

            // when
            ExecutorService serverExecutor = component.serverExecutor();

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

    @UnitTest
    void serverModule_shouldRespectSchedulerThreadsSystemProperty() {
        // given
        String originalProperty = System.getProperty(ServerConstants.SCHEDULER_THREADS_ENV_KEY);
        try {
            System.setProperty(ServerConstants.SCHEDULER_THREADS_ENV_KEY, "4");
            ServerModule module =
                    new ServerModule(
                            Objects.requireNonNull(
                                            tempDir, "tempDir should be initialized by JUnit")
                                    .toString());
            ServerComponent component =
                    DaggerServerComponent.builder().serverModule(module).build();

            // when
            ScheduledExecutorService scheduledExecutor = component.scheduledExecutor();

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

    @UnitTest
    void serverModule_shouldUseDefaultThreadsWhenNoSystemProperty() {
        // given
        String originalMaxThreads = System.getProperty(ServerConstants.MAX_THREADS_ENV_KEY);
        String originalSchedulerThreads =
                System.getProperty(ServerConstants.SCHEDULER_THREADS_ENV_KEY);
        try {
            // Clear properties to test defaults
            System.clearProperty(ServerConstants.MAX_THREADS_ENV_KEY);
            System.clearProperty(ServerConstants.SCHEDULER_THREADS_ENV_KEY);

            ServerModule module =
                    new ServerModule(
                            Objects.requireNonNull(
                                            tempDir, "tempDir should be initialized by JUnit")
                                    .toString());
            ServerComponent component =
                    DaggerServerComponent.builder().serverModule(module).build();

            // when
            ExecutorService serverExecutor = component.serverExecutor();
            ScheduledExecutorService scheduledExecutor = component.scheduledExecutor();

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

    @UnitTest
    void serverModule_shouldHandleInvalidThreadCountSystemProperty() {
        // given
        String originalProperty = System.getProperty(ServerConstants.MAX_THREADS_ENV_KEY);
        try {
            // Set invalid value (non-numeric)
            System.setProperty(ServerConstants.MAX_THREADS_ENV_KEY, "invalid");

            ServerModule module =
                    new ServerModule(
                            Objects.requireNonNull(
                                            tempDir, "tempDir should be initialized by JUnit")
                                    .toString());
            ServerComponent component =
                    DaggerServerComponent.builder().serverModule(module).build();

            // when - should fall back to default
            ExecutorService serverExecutor = component.serverExecutor();

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

    @UnitTest
    void serverModule_shouldHandleInvalidSchedulerThreadsSystemProperty() {
        // given
        String originalProperty = System.getProperty(ServerConstants.SCHEDULER_THREADS_ENV_KEY);
        try {
            // Set invalid value (non-numeric)
            System.setProperty(ServerConstants.SCHEDULER_THREADS_ENV_KEY, "invalid");

            ServerModule module =
                    new ServerModule(
                            Objects.requireNonNull(
                                            tempDir, "tempDir should be initialized by JUnit")
                                    .toString());
            ServerComponent component =
                    DaggerServerComponent.builder().serverModule(module).build();

            // when - should fall back to default
            ScheduledExecutorService scheduledExecutor = component.scheduledExecutor();

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

    @UnitTest
    void namedThreadFactory_shouldCreateDaemonThreadsWithCorrectNames() {
        // given
        String workspaceRoot =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .toString();
        ServerModule module = new ServerModule(workspaceRoot);
        ServerComponent component = DaggerServerComponent.builder().serverModule(module).build();

        // when
        ExecutorService serverExecutor = component.serverExecutor();

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

    @UnitTest
    void namedThreadFactory_shouldCreateSchedulerThreadsWithCorrectNames() {
        // given
        String workspaceRoot =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .toString();
        ServerModule module = new ServerModule(workspaceRoot);
        ServerComponent component = DaggerServerComponent.builder().serverModule(module).build();

        // when
        ScheduledExecutorService scheduledExecutor = component.scheduledExecutor();

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

    @UnitTest
    void serverModule_shouldCreateExecutorWithCorrectConfiguration() {
        // given
        String workspaceRoot =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .toString();
        ServerModule module = new ServerModule(workspaceRoot);
        ServerComponent component = DaggerServerComponent.builder().serverModule(module).build();

        // when
        ExecutorService serverExecutor = component.serverExecutor();

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

    @UnitTest
    void serverModule_shouldHandleEmptyStringSystemProperties() {
        // given
        String originalMaxThreads = System.getProperty(ServerConstants.MAX_THREADS_ENV_KEY);
        String originalSchedulerThreads =
                System.getProperty(ServerConstants.SCHEDULER_THREADS_ENV_KEY);
        try {
            // Set empty string values
            System.setProperty(ServerConstants.MAX_THREADS_ENV_KEY, "");
            System.setProperty(ServerConstants.SCHEDULER_THREADS_ENV_KEY, "");

            ServerModule module =
                    new ServerModule(
                            Objects.requireNonNull(
                                            tempDir, "tempDir should be initialized by JUnit")
                                    .toString());
            ServerComponent component =
                    DaggerServerComponent.builder().serverModule(module).build();

            // when - should fall back to defaults
            ExecutorService serverExecutor = component.serverExecutor();
            ScheduledExecutorService scheduledExecutor = component.scheduledExecutor();

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

    @UnitTest
    void serverModule_shouldHandleZeroAndNegativeThreadCounts() {
        // given
        String originalProperty = System.getProperty(ServerConstants.MAX_THREADS_ENV_KEY);
        try {
            // Test zero - should fail gracefully during injector creation
            System.setProperty(ServerConstants.MAX_THREADS_ENV_KEY, "0");
            final ServerModule zeroModule =
                    new ServerModule(
                            Objects.requireNonNull(
                                            tempDir, "tempDir should be initialized by JUnit")
                                    .toString());

            assertThatCode(
                            () -> {
                                ServerComponent component =
                                        DaggerServerComponent.builder()
                                                .serverModule(zeroModule)
                                                .build();
                                component.serverExecutor();
                            })
                    .isInstanceOf(IllegalArgumentException.class);

            // Test negative - should also fail gracefully
            System.setProperty(ServerConstants.MAX_THREADS_ENV_KEY, "-5");
            final ServerModule negativeModule =
                    new ServerModule(
                            Objects.requireNonNull(
                                            tempDir, "tempDir should be initialized by JUnit")
                                    .toString());

            assertThatCode(
                            () -> {
                                ServerComponent component =
                                        DaggerServerComponent.builder()
                                                .serverModule(negativeModule)
                                                .build();
                                component.serverExecutor();
                            })
                    .isInstanceOf(IllegalArgumentException.class);

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
