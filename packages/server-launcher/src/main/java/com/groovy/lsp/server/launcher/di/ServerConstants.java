package com.groovy.lsp.server.launcher.di;

/**
 * Constants for server configuration.
 *
 * This class contains all the configuration constants used throughout the server,
 * including executor settings, timeouts, and default values.
 */
public final class ServerConstants {

    // Executor configuration
    /** Maximum number of threads for the cached thread pool */
    public static final int MAX_THREAD_POOL_SIZE = 50;

    /** Core number of threads for the cached thread pool */
    public static final int CORE_THREAD_POOL_SIZE = 10;

    /** Keep-alive time for idle threads in seconds */
    public static final long THREAD_KEEP_ALIVE_TIME = 60L;

    /** Default number of threads for scheduled executor */
    public static final int DEFAULT_SCHEDULER_THREADS = 2;

    /** Timeout for executor shutdown in seconds */
    public static final int EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS = 5;

    // Server configuration
    /** Default port for socket mode */
    public static final int DEFAULT_SOCKET_PORT = 4389;

    /** Default host for socket mode */
    public static final String DEFAULT_SOCKET_HOST = "localhost";

    /** Default workspace root directory */
    public static final String DEFAULT_WORKSPACE_ROOT = ".";

    // Environment variable keys
    /** Environment variable for scheduler thread count */
    public static final String SCHEDULER_THREADS_ENV_KEY = "groovy.lsp.scheduler.threads";

    /** Environment variable for workspace root */
    public static final String WORKSPACE_ROOT_ENV_KEY = "groovy.lsp.workspace.root";

    /** Environment variable for max thread pool size */
    public static final String MAX_THREADS_ENV_KEY = "groovy.lsp.max.threads";

    // Thread name prefixes
    /** Thread name prefix for main executor */
    public static final String SERVER_THREAD_PREFIX = "groovy-lsp-server";

    /** Thread name prefix for scheduled executor */
    public static final String SCHEDULER_THREAD_PREFIX = "groovy-lsp-scheduler";

    // Private constructor to prevent instantiation
    private ServerConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}
