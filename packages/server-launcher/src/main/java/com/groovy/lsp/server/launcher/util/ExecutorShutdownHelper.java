package com.groovy.lsp.server.launcher.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.groovy.lsp.server.launcher.di.ServerConstants.EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS;

/**
 * Utility class for managing executor service shutdown.
 * 
 * This class provides helper methods for gracefully shutting down executor services
 * with proper timeout handling and error management.
 */
public final class ExecutorShutdownHelper {
    
    private static final Logger logger = LoggerFactory.getLogger(ExecutorShutdownHelper.class);
    
    // Private constructor to prevent instantiation
    private ExecutorShutdownHelper() {
        throw new AssertionError("Cannot instantiate utility class");
    }
    
    /**
     * Gracefully shuts down an executor service with the default timeout.
     * 
     * @param executor the executor service to shut down
     * @param executorName the name of the executor for logging purposes
     */
    public static void shutdownExecutor(ExecutorService executor, String executorName) {
        shutdownExecutor(executor, executorName, EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
    
    /**
     * Gracefully shuts down an executor service with a custom timeout.
     * 
     * @param executor the executor service to shut down
     * @param executorName the name of the executor for logging purposes
     * @param timeout the timeout value
     * @param unit the timeout unit
     */
    public static void shutdownExecutor(ExecutorService executor, String executorName, 
                                      long timeout, TimeUnit unit) {
        if (executor == null) {
            logger.warn("Attempted to shutdown null executor: {}", executorName);
            return;
        }
        
        logger.info("Shutting down {}", executorName);
        
        // Initiate orderly shutdown
        executor.shutdown();
        
        try {
            // Wait for tasks to complete
            if (!executor.awaitTermination(timeout, unit)) {
                logger.warn("{} did not terminate within {} {}, forcing shutdown", 
                           executorName, timeout, unit.toString().toLowerCase());
                
                // Force shutdown
                executor.shutdownNow();
                
                // Wait again after forced shutdown
                if (!executor.awaitTermination(timeout, unit)) {
                    logger.error("{} did not terminate after forced shutdown", executorName);
                }
            } else {
                logger.info("{} shut down successfully", executorName);
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted while waiting for {} termination", executorName, e);
            
            // Force shutdown on interrupt
            executor.shutdownNow();
            
            // Restore interrupt status
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Shuts down multiple executor services concurrently.
     * 
     * @param executors array of executor services with their names
     */
    public static void shutdownMultipleExecutors(ExecutorWithName... executors) {
        if (executors == null || executors.length == 0) {
            return;
        }
        
        logger.info("Shutting down {} executor services", executors.length);
        
        // Initiate shutdown for all executors
        for (ExecutorWithName exec : executors) {
            if (exec != null && exec.executor != null) {
                exec.executor.shutdown();
            }
        }
        
        // Wait for all executors to terminate
        for (ExecutorWithName exec : executors) {
            if (exec != null && exec.executor != null) {
                try {
                    if (!exec.executor.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        logger.warn("{} did not terminate in time, forcing shutdown", exec.name);
                        exec.executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    logger.error("Interrupted while waiting for {} termination", exec.name, e);
                    exec.executor.shutdownNow();
                    Thread.currentThread().interrupt();
                    return; // Exit early on interrupt
                }
            }
        }
        
        logger.info("All executor services shut down");
    }
    
    /**
     * Helper class to pair an executor with its name.
     */
    public static class ExecutorWithName {
        public final ExecutorService executor;
        public final String name;
        
        public ExecutorWithName(ExecutorService executor, String name) {
            this.executor = executor;
            this.name = name;
        }
    }
}