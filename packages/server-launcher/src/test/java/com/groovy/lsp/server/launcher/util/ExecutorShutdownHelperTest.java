package com.groovy.lsp.server.launcher.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.groovy.lsp.server.launcher.util.ExecutorShutdownHelper.ExecutorWithName;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Tests for ExecutorShutdownHelper utility class.
 */
class ExecutorShutdownHelperTest {

    @Test
    void constructor_shouldThrowAssertionError() throws Exception {
        // given
        java.lang.reflect.Constructor<ExecutorShutdownHelper> constructor =
                ExecutorShutdownHelper.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // when/then
        assertThatThrownBy(
                        () -> {
                            try {
                                constructor.newInstance();
                            } catch (java.lang.reflect.InvocationTargetException e) {
                                throw e.getCause();
                            }
                        })
                .isInstanceOf(AssertionError.class)
                .hasMessage("Cannot instantiate utility class");
    }

    @Test
    void shutdownExecutor_shouldHandleNullExecutor() {
        // when - should not throw
        ExecutorShutdownHelper.shutdownExecutor(null, "test-executor");

        // then - method completes without error
        assertThat(true).isTrue(); // Dummy assertion to show test passed
    }

    @Test
    @Timeout(10)
    void shutdownExecutor_shouldShutdownSuccessfully() throws InterruptedException {
        // given
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch taskCompleted = new CountDownLatch(1);

        // Submit a task that signals when it starts and completes
        executor.submit(
                () -> {
                    taskStarted.countDown();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    taskCompleted.countDown();
                });

        // Wait for task to start
        taskStarted.await();

        // when
        ExecutorShutdownHelper.shutdownExecutor(executor, "test-executor");

        // then
        assertThat(executor.isShutdown()).isTrue();
        assertThat(executor.isTerminated()).isTrue();
    }

    @Test
    void shutdownExecutor_shouldForceShutdownWhenTimeout() throws InterruptedException {
        // given
        ExecutorService mockExecutor = mock(ExecutorService.class);
        when(mockExecutor.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(false);

        // when
        ExecutorShutdownHelper.shutdownExecutor(mockExecutor, "test-executor", 1, TimeUnit.SECONDS);

        // then
        verify(mockExecutor).shutdown();
        verify(mockExecutor, times(2)).awaitTermination(1, TimeUnit.SECONDS);
        verify(mockExecutor).shutdownNow();
    }

    @Test
    void shutdownExecutor_shouldHandleInterruptedException() throws InterruptedException {
        // given
        ExecutorService mockExecutor = mock(ExecutorService.class);
        when(mockExecutor.awaitTermination(anyLong(), any(TimeUnit.class)))
                .thenThrow(new InterruptedException("Test interruption"));

        AtomicBoolean interrupted = new AtomicBoolean(false);
        Thread testThread =
                new Thread(
                        () -> {
                            ExecutorShutdownHelper.shutdownExecutor(mockExecutor, "test-executor");
                            interrupted.set(Thread.currentThread().isInterrupted());
                        });

        // when
        testThread.start();
        testThread.join();

        // then
        assertThat(interrupted.get()).isTrue();
        verify(mockExecutor).shutdown();
        verify(mockExecutor).shutdownNow();
    }

    @Test
    void shutdownMultipleExecutors_shouldHandleNullArray() {
        // when - should not throw
        ExecutorShutdownHelper.shutdownMultipleExecutors((ExecutorWithName[]) null);

        // then - method completes without error
        assertThat(true).isTrue(); // Dummy assertion to show test passed
    }

    @Test
    void shutdownMultipleExecutors_shouldHandleEmptyArray() {
        // when - should not throw
        ExecutorShutdownHelper.shutdownMultipleExecutors();

        // then - method completes without error
        assertThat(true).isTrue(); // Dummy assertion to show test passed
    }

    @Test
    void shutdownMultipleExecutors_shouldShutdownAllExecutors() throws InterruptedException {
        // given
        ExecutorService executor1 = mock(ExecutorService.class);
        ExecutorService executor2 = mock(ExecutorService.class);
        when(executor1.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(executor2.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);

        ExecutorWithName exec1 = new ExecutorWithName(executor1, "executor-1");
        ExecutorWithName exec2 = new ExecutorWithName(executor2, "executor-2");

        // when
        ExecutorShutdownHelper.shutdownMultipleExecutors(exec1, exec2);

        // then
        verify(executor1).shutdown();
        verify(executor2).shutdown();
        verify(executor1).awaitTermination(anyLong(), any(TimeUnit.class));
        verify(executor2).awaitTermination(anyLong(), any(TimeUnit.class));
    }

    @Test
    void shutdownMultipleExecutors_shouldHandleNullExecutorInArray() throws InterruptedException {
        // given
        ExecutorService executor = mock(ExecutorService.class);
        when(executor.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);

        ExecutorWithName exec1 = new ExecutorWithName(executor, "executor-1");
        ExecutorWithName exec2 = null; // null entry

        // when
        ExecutorShutdownHelper.shutdownMultipleExecutors(exec1, exec2);

        // then
        verify(executor).shutdown();
        verify(executor).awaitTermination(anyLong(), any(TimeUnit.class));
    }

    @Test
    void shutdownMultipleExecutors_shouldHandleNullExecutorService() throws InterruptedException {
        // given
        ExecutorService executor = mock(ExecutorService.class);
        when(executor.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);

        ExecutorWithName exec1 = new ExecutorWithName(executor, "executor-1");
        ExecutorWithName exec2 = new ExecutorWithName(null, "executor-2"); // null executor

        // when
        ExecutorShutdownHelper.shutdownMultipleExecutors(exec1, exec2);

        // then
        verify(executor).shutdown();
        verify(executor).awaitTermination(anyLong(), any(TimeUnit.class));
    }

    @Test
    void shutdownMultipleExecutors_shouldForceShutdownOnTimeout() throws InterruptedException {
        // given
        ExecutorService executor1 = mock(ExecutorService.class);
        ExecutorService executor2 = mock(ExecutorService.class);
        when(executor1.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(executor2.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(false);

        ExecutorWithName exec1 = new ExecutorWithName(executor1, "executor-1");
        ExecutorWithName exec2 = new ExecutorWithName(executor2, "executor-2");

        // when
        ExecutorShutdownHelper.shutdownMultipleExecutors(exec1, exec2);

        // then
        verify(executor1).shutdown();
        verify(executor2).shutdown();
        verify(executor1).awaitTermination(anyLong(), any(TimeUnit.class));
        verify(executor2).awaitTermination(anyLong(), any(TimeUnit.class));
        verify(executor2).shutdownNow();
    }

    @Test
    void shutdownMultipleExecutors_shouldHandleInterruption() throws InterruptedException {
        // given
        ExecutorService executor1 = mock(ExecutorService.class);
        ExecutorService executor2 = mock(ExecutorService.class);
        when(executor1.awaitTermination(anyLong(), any(TimeUnit.class)))
                .thenThrow(new InterruptedException("Test interruption"));

        ExecutorWithName exec1 = new ExecutorWithName(executor1, "executor-1");
        ExecutorWithName exec2 = new ExecutorWithName(executor2, "executor-2");

        AtomicBoolean interrupted = new AtomicBoolean(false);
        Thread testThread =
                new Thread(
                        () -> {
                            ExecutorShutdownHelper.shutdownMultipleExecutors(exec1, exec2);
                            interrupted.set(Thread.currentThread().isInterrupted());
                        });

        // when
        testThread.start();
        testThread.join();

        // then
        assertThat(interrupted.get()).isTrue();
        verify(executor1).shutdown();
        verify(executor2).shutdown();
        verify(executor1).shutdownNow();
        // executor2 should not be processed due to early return on interrupt
        verify(executor2, never()).awaitTermination(anyLong(), any(TimeUnit.class));
    }

    @Test
    void executorWithName_shouldStoreValues() {
        // given
        ExecutorService executor = mock(ExecutorService.class);
        String name = "test-executor";

        // when
        ExecutorWithName execWithName = new ExecutorWithName(executor, name);

        // then
        assertThat(execWithName.executor).isSameAs(executor);
        assertThat(execWithName.name).isEqualTo(name);
    }

    @Test
    void shutdownExecutor_shouldHandleSecondAwaitTerminationFailure() throws InterruptedException {
        // given
        ExecutorService mockExecutor = mock(ExecutorService.class);
        // First awaitTermination returns false (timeout), second also returns false
        when(mockExecutor.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(false);

        // when
        ExecutorShutdownHelper.shutdownExecutor(mockExecutor, "test-executor", 1, TimeUnit.SECONDS);

        // then
        verify(mockExecutor).shutdown();
        verify(mockExecutor, times(2)).awaitTermination(1, TimeUnit.SECONDS);
        verify(mockExecutor).shutdownNow();
    }

    @Test
    @Timeout(10)
    void shutdownExecutor_withDefaultTimeout_shouldShutdownSuccessfully()
            throws InterruptedException {
        // given
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch taskCompleted = new CountDownLatch(1);

        // Submit a quick task
        executor.submit(
                () -> {
                    taskStarted.countDown();
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    taskCompleted.countDown();
                });

        // Wait for task to start
        taskStarted.await();

        // when - using the overloaded method with default timeout
        ExecutorShutdownHelper.shutdownExecutor(executor, "test-executor");

        // then
        assertThat(executor.isShutdown()).isTrue();
        assertThat(executor.isTerminated()).isTrue();
    }

    @Test
    void shutdownExecutor_shouldLogSuccessfulShutdown() throws InterruptedException {
        // given
        ExecutorService mockExecutor = mock(ExecutorService.class);
        when(mockExecutor.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);

        // when
        ExecutorShutdownHelper.shutdownExecutor(mockExecutor, "test-executor", 1, TimeUnit.SECONDS);

        // then
        verify(mockExecutor).shutdown();
        verify(mockExecutor).awaitTermination(1, TimeUnit.SECONDS);
        verify(mockExecutor, never()).shutdownNow();
    }
}
