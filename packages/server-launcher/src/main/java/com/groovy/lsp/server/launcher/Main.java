package com.groovy.lsp.server.launcher;

import static com.groovy.lsp.server.launcher.di.ServerConstants.DEFAULT_SOCKET_HOST;
import static com.groovy.lsp.server.launcher.di.ServerConstants.DEFAULT_SOCKET_PORT;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.groovy.lsp.protocol.api.GroovyLanguageServer;
import com.groovy.lsp.server.launcher.di.ServerModule;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the Groovy Language Server.
 *
 * This class sets up the LSP4J launcher and initializes the server with Guice DI container.
 * It supports both stdio and socket communication modes.
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    // Constants moved to ServerConstants to avoid duplication

    public static void main(String[] args) {
        try {
            runServer(args);
        } catch (HelpRequestedException e) {
            // Help was printed, exit normally
            System.exit(0);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid arguments: {}", e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            logger.error("Failed to start Groovy Language Server", e);
            System.exit(1);
        }
    }

    /**
     * Run the server without calling System.exit() - for testing purposes.
     * @param args command line arguments
     * @throws Exception if server fails to start
     */
    static void runServer(String[] args) throws Exception {
        logger.info("Starting Groovy Language Server...");

        // Parse command line arguments
        LaunchMode mode = parseArguments(args);

        // If dry-run mode, just return after parsing
        if (mode.dryRun) {
            logger.info("Dry run mode - arguments parsed successfully:");
            logger.info("  Launch type: {}", mode.type);
            logger.info(
                    "  Workspace: {}",
                    mode.workspaceRoot != null
                            ? mode.workspaceRoot
                            : System.getProperty("user.dir"));
            if (mode.type == LaunchType.SOCKET) {
                logger.info("  Host: {}", mode.host);
                logger.info("  Port: {}", mode.port);
            }
            return;
        }

        // Get workspace root (from command line or current directory)
        String workspaceRoot =
                mode.workspaceRoot != null ? mode.workspaceRoot : System.getProperty("user.dir");
        logger.info("Using workspace root: {}", workspaceRoot);

        // Create Guice injector with workspace root
        Injector injector = Guice.createInjector(new ServerModule(workspaceRoot));
        logger.info("Dependency injection container initialized");

        // Create the server instance through DI
        GroovyLanguageServer server = injector.getInstance(GroovyLanguageServer.class);

        // Launch the server based on the mode
        switch (mode.type) {
            case STDIO -> launchStdio(server);
            case SOCKET -> launchSocket(server, mode.host, mode.port);
            default -> throw new IllegalArgumentException("Unknown launch mode: " + mode.type);
        }
    }

    /**
     * Launch the server using stdio for communication (default mode).
     */
    private static void launchStdio(LanguageServer server) throws Exception {
        logger.info("Launching server in stdio mode");

        InputStream in = System.in;
        OutputStream out = System.out;

        // Create launcher
        Launcher<LanguageClient> launcher =
                LSPLauncher.createServerLauncher(
                        server, in, out, createExecutorService(), wrapper -> wrapper);

        // Connect the server to the client
        LanguageClient client = launcher.getRemoteProxy();
        ((GroovyLanguageServer) server).connect(client);

        // Start listening
        Future<Void> listening = launcher.startListening();
        logger.info("Groovy Language Server started in stdio mode");

        // Wait for the server to shutdown
        listening.get();
        logger.info("Groovy Language Server stopped");
    }

    /**
     * Launch the server using socket for communication.
     */
    private static void launchSocket(LanguageServer server, String host, int port)
            throws Exception {
        logger.info("Launching server in socket mode on {}:{}", host, port);

        ServerSocket serverSocket = null;
        try {
            // Create server socket with error handling for port conflicts
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(host, port));
                logger.info("Server socket listening on {}:{}", host, port);
            } catch (IOException e) {
                // Check for BindException which indicates port is already in use
                if (e instanceof java.net.BindException) {
                    logger.error(
                            "Port {} is already in use. Please choose a different port or stop the"
                                    + " conflicting process.",
                            port);
                    throw new IllegalStateException("Port " + port + " is already in use", e);
                } else {
                    logger.error("Failed to bind to {}:{}", host, port, e);
                    throw e;
                }
            }

            // Accept client connection
            logger.info("Waiting for client connection on {}:{}...", host, port);
            Socket clientSocket = serverSocket.accept();
            logger.info("Client connected from {}", clientSocket.getRemoteSocketAddress());

            try {
                InputStream in = clientSocket.getInputStream();
                OutputStream out = clientSocket.getOutputStream();

                // Create launcher
                Launcher<LanguageClient> launcher =
                        LSPLauncher.createServerLauncher(
                                server, in, out, createExecutorService(), wrapper -> wrapper);

                // Connect the server to the client
                LanguageClient client = launcher.getRemoteProxy();
                ((GroovyLanguageServer) server).connect(client);

                // Start listening
                Future<Void> listening = launcher.startListening();
                logger.info("Groovy Language Server started in socket mode");

                // Wait for the server to shutdown
                listening.get();
                logger.info("Groovy Language Server stopped");
            } finally {
                clientSocket.close();
            }
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    logger.warn("Error closing server socket", e);
                }
            }
        }
    }

    /**
     * Create an executor service for the JSON-RPC communication.
     */
    private static ExecutorService createExecutorService() {
        return Executors.newCachedThreadPool(
                runnable -> {
                    Thread thread = new Thread(runnable, "groovy-lsp-jsonrpc");
                    thread.setDaemon(true);
                    return thread;
                });
    }

    /**
     * Parse command line arguments to determine launch mode.
     * Using statement switch (not expression switch) because we need to modify loop index 'i'
     * and handle multiple statements with side effects in some cases.
     */
    @SuppressWarnings("StatementSwitchToExpressionSwitch")
    static LaunchMode parseArguments(String[] args) throws HelpRequestedException {
        LaunchMode mode = new LaunchMode();
        mode.type = LaunchType.STDIO; // Default to stdio

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            switch (arg) {
                case "--socket":
                case "-s":
                    mode.type = LaunchType.SOCKET;
                    break;

                case "--host":
                case "-h":
                    if (i + 1 < args.length) {
                        mode.host = args[++i];
                    } else {
                        throw new IllegalArgumentException("Missing value for " + arg);
                    }
                    break;

                case "--port":
                case "-p":
                    if (i + 1 < args.length) {
                        try {
                            mode.port = Integer.parseInt(args[++i]);
                            if (mode.port <= 0 || mode.port > 65535) {
                                throw new IllegalArgumentException(
                                        "Port number must be between 1 and 65535: " + mode.port);
                            }
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Invalid port number: " + args[i]);
                        }
                    } else {
                        throw new IllegalArgumentException("Missing value for " + arg);
                    }
                    break;

                case "--workspace":
                case "-w":
                    if (i + 1 < args.length) {
                        mode.workspaceRoot = args[++i];
                    } else {
                        throw new IllegalArgumentException("Missing value for " + arg);
                    }
                    break;

                case "--help":
                    printHelp();
                    throw new HelpRequestedException();
                // Note: HelpRequestedException is handled in main() method

                case "--dry-run":
                    mode.dryRun = true;
                    logger.info("Dry run mode enabled - will parse arguments only");
                    break;

                default:
                    logger.warn("Unknown argument: {}", arg);
            }
        }

        // Set defaults for socket mode
        if (mode.type == LaunchType.SOCKET) {
            if (mode.host == null) {
                mode.host = DEFAULT_SOCKET_HOST;
            }
            if (mode.port == 0) {
                mode.port = DEFAULT_SOCKET_PORT;
            }
        }

        // Validate workspace path if provided
        if (mode.workspaceRoot != null) {
            File workspace = new File(mode.workspaceRoot);
            if (!workspace.exists()) {
                throw new IllegalArgumentException(
                        "Workspace directory does not exist: " + mode.workspaceRoot);
            }
            if (!workspace.isDirectory()) {
                throw new IllegalArgumentException(
                        "Workspace path is not a directory: " + mode.workspaceRoot);
            }
            if (!workspace.canRead()) {
                throw new IllegalArgumentException(
                        "Cannot read workspace directory: " + mode.workspaceRoot);
            }
            logger.debug("Validated workspace path: {}", mode.workspaceRoot);
        }

        return mode;
    }

    /**
     * Print help message.
     */
    private static void printHelp() {
        System.out.println("Groovy Language Server");
        System.out.println();
        System.out.println("Usage: groovy-language-server [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --socket, -s              Use socket mode instead of stdio");
        System.out.println("  --host, -h <host>         Host for socket mode (default: localhost)");
        System.out.println("  --port, -p <port>         Port for socket mode (default: 4389)");
        System.out.println(
                "  --workspace, -w <path>    Workspace root directory (default: current"
                        + " directory)");
        System.out.println("  --dry-run                 Parse arguments only, don't start server");
        System.out.println("  --help                    Show this help message");
        System.out.println();
        System.out.println("Environment variables:");
        System.out.println(
                "  groovy.lsp.scheduler.threads    Number of threads for scheduled executor"
                        + " (default: 2)");
        System.out.println(
                "  groovy.lsp.workspace.root       Workspace root directory (can be overridden by"
                        + " --workspace)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  groovy-language-server                    # Start in stdio mode");
        System.out.println(
                "  groovy-language-server --socket           # Start in socket mode on"
                        + " localhost:4389");
        System.out.println(
                "  groovy-language-server -s -h 0.0.0.0 -p 8080  # Start on all interfaces, port"
                        + " 8080");
    }

    /**
     * Launch mode configuration.
     */
    static class LaunchMode {
        LaunchType type = LaunchType.STDIO; // Default to STDIO mode
        String host = DEFAULT_SOCKET_HOST; // Default host
        int port = DEFAULT_SOCKET_PORT; // Default LSP port
        boolean dryRun = false; // Dry run mode - parse arguments only, don't start server

        @Nullable String workspaceRoot =
                null; // Workspace root directory (null = use current directory)
    }

    /**
     * Launch type enumeration.
     */
    enum LaunchType {
        STDIO,
        SOCKET
    }

    /**
     * Exception thrown when help is requested.
     */
    static class HelpRequestedException extends Exception {
        HelpRequestedException() {
            super("Help requested");
        }
    }
}
