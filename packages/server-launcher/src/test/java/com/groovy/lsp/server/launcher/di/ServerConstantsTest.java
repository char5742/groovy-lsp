package com.groovy.lsp.server.launcher.di;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.groovy.lsp.test.annotations.UnitTest;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Tests for ServerConstants class.
 */
class ServerConstantsTest {

    @UnitTest
    void constructor_shouldThrowAssertionError() throws NoSuchMethodException {
        // given
        Constructor<ServerConstants> constructor = ServerConstants.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // when/then
        assertThatThrownBy(
                        () -> {
                            try {
                                constructor.newInstance();
                            } catch (InvocationTargetException e) {
                                throw e.getCause();
                            }
                        })
                .isInstanceOf(AssertionError.class)
                .hasMessage("Cannot instantiate constants class");
    }

    @UnitTest
    void constants_shouldHaveCorrectValues() {
        // Executor configuration
        assertThat(ServerConstants.MAX_THREAD_POOL_SIZE).isEqualTo(50);
        assertThat(ServerConstants.CORE_THREAD_POOL_SIZE).isEqualTo(10);
        assertThat(ServerConstants.THREAD_KEEP_ALIVE_TIME).isEqualTo(60L);
        assertThat(ServerConstants.DEFAULT_SCHEDULER_THREADS).isEqualTo(2);
        assertThat(ServerConstants.EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS).isEqualTo(5);

        // Server configuration
        assertThat(ServerConstants.DEFAULT_SOCKET_PORT).isEqualTo(4389);
        assertThat(ServerConstants.DEFAULT_SOCKET_HOST).isEqualTo("localhost");
        assertThat(ServerConstants.DEFAULT_WORKSPACE_ROOT).isEqualTo(".");

        // Environment variable keys
        assertThat(ServerConstants.SCHEDULER_THREADS_ENV_KEY)
                .isEqualTo("groovy.lsp.scheduler.threads");
        assertThat(ServerConstants.WORKSPACE_ROOT_ENV_KEY).isEqualTo("groovy.lsp.workspace.root");
        assertThat(ServerConstants.MAX_THREADS_ENV_KEY).isEqualTo("groovy.lsp.server.max.threads");

        // Thread name prefixes
        assertThat(ServerConstants.SERVER_THREAD_PREFIX).isEqualTo("groovy-lsp-server");
        assertThat(ServerConstants.SCHEDULER_THREAD_PREFIX).isEqualTo("groovy-lsp-scheduler");
    }

    @UnitTest
    void constants_shouldBePublicStaticFinal() throws NoSuchFieldException {
        // Check a few representative fields
        assertThat(ServerConstants.class.getField("MAX_THREAD_POOL_SIZE").getModifiers())
                .isEqualTo(
                        java.lang.reflect.Modifier.PUBLIC
                                | java.lang.reflect.Modifier.STATIC
                                | java.lang.reflect.Modifier.FINAL);

        assertThat(ServerConstants.class.getField("DEFAULT_SOCKET_HOST").getModifiers())
                .isEqualTo(
                        java.lang.reflect.Modifier.PUBLIC
                                | java.lang.reflect.Modifier.STATIC
                                | java.lang.reflect.Modifier.FINAL);
    }

    @UnitTest
    void executorConstants_shouldHaveReasonableValues() {
        // Verify executor configuration makes sense
        assertThat(ServerConstants.CORE_THREAD_POOL_SIZE)
                .isLessThanOrEqualTo(ServerConstants.MAX_THREAD_POOL_SIZE);

        assertThat(ServerConstants.THREAD_KEEP_ALIVE_TIME).isGreaterThan(0);

        assertThat(ServerConstants.DEFAULT_SCHEDULER_THREADS).isGreaterThan(0);

        assertThat(ServerConstants.EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS).isGreaterThan(0);
    }

    @UnitTest
    void serverConstants_shouldHaveValidNetworkValues() {
        // Verify port is in valid range
        assertThat(ServerConstants.DEFAULT_SOCKET_PORT).isBetween(1, 65535);

        // Verify host is not empty
        assertThat(ServerConstants.DEFAULT_SOCKET_HOST).isNotEmpty();
    }
}
