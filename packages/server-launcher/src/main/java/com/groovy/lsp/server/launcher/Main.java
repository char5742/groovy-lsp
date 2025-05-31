package com.groovy.lsp.server.launcher;

import com.groovy.lsp.protocol.GroovyLanguageServer;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Main entry point for the Groovy Language Server.
 * 
 * This class sets up the LSP4J launcher and initializes the server with Guice DI container.
 * It supports both stdio and socket communication modes.
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5007;
    
    public static void main(String[] args) {
        try {
            logger.info("Starting Groovy Language Server...");
            
            // Parse command line arguments
            LaunchMode mode = parseArguments(args);
            
            // Create the server instance
            GroovyLanguageServer server = new GroovyLanguageServer();
            
            // Launch the server based on the mode
            switch (mode.type) {
                case STDIO:
                    launchStdio(server);
                    break;
                case SOCKET:
                    launchSocket(server, mode.host, mode.port);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown launch mode: " + mode.type);
            }
        } catch (Exception e) {
            logger.error("Failed to start Groovy Language Server", e);
            System.exit(1);
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
        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(
            server, in, out, createExecutorService(), wrapper -> wrapper
        );
        
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
    private static void launchSocket(LanguageServer server, String host, int port) throws Exception {
        logger.info("Launching server in socket mode on {}:{}", host, port);
        
        try (Socket socket = new Socket(host, port)) {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            
            // Create launcher
            Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(
                server, in, out, createExecutorService(), wrapper -> wrapper
            );
            
            // Connect the server to the client
            LanguageClient client = launcher.getRemoteProxy();
            ((GroovyLanguageServer) server).connect(client);
            
            // Start listening
            Future<Void> listening = launcher.startListening();
            logger.info("Groovy Language Server started in socket mode on {}:{}", host, port);
            
            // Wait for the server to shutdown
            listening.get();
            logger.info("Groovy Language Server stopped");
        }
    }
    
    /**
     * Create an executor service for the JSON-RPC communication.
     */
    private static ExecutorService createExecutorService() {
        return Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "groovy-lsp-jsonrpc");
            thread.setDaemon(true);
            return thread;
        });
    }
    
    /**
     * Parse command line arguments to determine launch mode.
     */
    private static LaunchMode parseArguments(String[] args) {
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
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Invalid port number: " + args[i]);
                        }
                    } else {
                        throw new IllegalArgumentException("Missing value for " + arg);
                    }
                    break;
                    
                case "--help":
                    printHelp();
                    System.exit(0);
                    break;
                    
                default:
                    logger.warn("Unknown argument: {}", arg);
            }
        }
        
        // Set defaults for socket mode
        if (mode.type == LaunchType.SOCKET) {
            if (mode.host == null) {
                mode.host = DEFAULT_HOST;
            }
            if (mode.port == 0) {
                mode.port = DEFAULT_PORT;
            }
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
        System.out.println("  --socket, -s        Use socket mode instead of stdio");
        System.out.println("  --host, -h <host>   Host for socket mode (default: localhost)");
        System.out.println("  --port, -p <port>   Port for socket mode (default: 5007)");
        System.out.println("  --help              Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  groovy-language-server                    # Start in stdio mode");
        System.out.println("  groovy-language-server --socket           # Start in socket mode on localhost:5007");
        System.out.println("  groovy-language-server -s -h 0.0.0.0 -p 8080  # Start on all interfaces, port 8080");
    }
    
    /**
     * Launch mode configuration.
     */
    private static class LaunchMode {
        LaunchType type;
        String host;
        int port;
    }
    
    /**
     * Launch type enumeration.
     */
    private enum LaunchType {
        STDIO,
        SOCKET
    }
}