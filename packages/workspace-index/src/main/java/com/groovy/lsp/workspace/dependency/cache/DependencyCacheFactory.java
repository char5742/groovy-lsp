package com.groovy.lsp.workspace.dependency.cache;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Factory for creating and managing DependencyCache instances.
 * Provides a singleton cache instance with automatic memory pressure monitoring.
 */
public class DependencyCacheFactory {
    @SuppressWarnings("NullAway")
    private static volatile DependencyCache instance;

    private static final Object lock = new Object();

    @SuppressWarnings("NullAway")
    private static ScheduledExecutorService memoryMonitor;

    private DependencyCacheFactory() {
        // Private constructor to prevent instantiation
    }

    /**
     * Get the singleton DependencyCache instance.
     * Creates it lazily on first access.
     *
     * @return the shared DependencyCache instance
     */
    public static DependencyCache getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new LRUDependencyCache();
                    startMemoryMonitoring();
                }
            }
        }
        return instance;
    }

    /**
     * Reset the cache instance. Useful for testing.
     */
    @SuppressWarnings("NullAway")
    public static void reset() {
        synchronized (lock) {
            if (instance != null) {
                instance.invalidateAll();
                instance = null;
            }
            stopMemoryMonitoring();
        }
    }

    private static void startMemoryMonitoring() {
        memoryMonitor =
                Executors.newSingleThreadScheduledExecutor(
                        r -> {
                            Thread thread = new Thread(r, "DependencyCache-MemoryMonitor");
                            thread.setDaemon(true);
                            return thread;
                        });

        // Monitor memory pressure every 5 minutes
        @SuppressWarnings("FutureReturnValueIgnored")
        var unused =
                memoryMonitor.scheduleAtFixedRate(
                        () -> {
                            if (instance != null) {
                                // Get max heap size and target 70% usage
                                long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
                                long targetMemory = (long) (maxMemory * 0.7);
                                instance.evictIfNeeded(targetMemory);
                            }
                        },
                        5,
                        5,
                        TimeUnit.MINUTES);
    }

    @SuppressWarnings("NullAway")
    private static void stopMemoryMonitoring() {
        if (memoryMonitor != null) {
            memoryMonitor.shutdown();
            try {
                if (!memoryMonitor.awaitTermination(5, TimeUnit.SECONDS)) {
                    memoryMonitor.shutdownNow();
                }
            } catch (InterruptedException e) {
                memoryMonitor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            memoryMonitor = null;
        }
    }
}
