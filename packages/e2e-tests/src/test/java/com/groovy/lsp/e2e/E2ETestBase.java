package com.groovy.lsp.e2e;

import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stop.ProcessStopper;

/**
 * Base class for E2E tests that run the language server as a separate process.
 */
public abstract class E2ETestBase {
    private static final String SERVER_JAR_PROPERTY = "groovy.lsp.server.jar";
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(30);

    protected Path workspaceRoot;
    protected int serverPort;
    protected StartedProcess serverProcess;
    protected String serverJar;

    @BeforeEach
    void setUp(TestInfo testInfo) throws Exception {
        serverJar = System.getProperty(SERVER_JAR_PROPERTY);
        if (serverJar == null || !Files.exists(Path.of(serverJar))) {
            throw new IllegalStateException(
                    "Server JAR not found. Set system property: " + SERVER_JAR_PROPERTY);
        }

        // Create test workspace
        workspaceRoot = createTestWorkspace(testInfo);

        // Find free port
        serverPort = findFreePort();

        // Start server
        startServer();

        // Wait for server to be ready
        waitForServerReady();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Stop server
        if (serverProcess != null) {
            serverProcess.getProcess().destroyForcibly();
            await().atMost(Duration.ofSeconds(5))
                    .until(() -> !serverProcess.getProcess().isAlive());
        }

        // Clean up workspace
        if (workspaceRoot != null && Files.exists(workspaceRoot)) {
            deleteRecursively(workspaceRoot);
        }
    }

    /**
     * Create a temporary workspace for the test.
     */
    protected Path createTestWorkspace(TestInfo testInfo) throws IOException {
        String testName = testInfo.getTestMethod().map(m -> m.getName()).orElse("unknown");

        Path workspace = Files.createTempDirectory("groovy-lsp-e2e-" + testName);

        // Create basic project structure
        Files.createDirectories(workspace.resolve("src/main/groovy"));
        Files.createDirectories(workspace.resolve("src/test/groovy"));

        return workspace;
    }

    /**
     * Start the language server process.
     */
    protected void startServer() throws Exception {
        List<String> command = new ArrayList<>();
        command.add("java");

        // Add JVM options
        command.add("-Xmx512m");
        command.add("-XX:+UseG1GC");

        // Add server JAR
        command.add("-jar");
        command.add(serverJar);

        // Add server options
        command.add("--port");
        command.add(String.valueOf(serverPort));

        ProcessExecutor executor =
                new ProcessExecutor()
                        .command(command)
                        .directory(workspaceRoot.toFile())
                        .redirectOutput(System.out)
                        .redirectError(System.err)
                        .stopper(ProcessStopper.Stopper.NOOP);

        serverProcess = executor.start();
    }

    /**
     * Wait for the server to be ready.
     */
    protected void waitForServerReady() {
        await().atMost(STARTUP_TIMEOUT)
                .pollInterval(Duration.ofMillis(500))
                .until(this::isServerReady);
    }

    /**
     * Check if the server is ready.
     */
    protected abstract boolean isServerReady();

    /**
     * Find a free port to use.
     */
    protected int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /**
     * Delete a directory recursively.
     */
    protected void deleteRecursively(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(
                            p -> {
                                try {
                                    Files.delete(p);
                                } catch (IOException e) {
                                    // Ignore errors during cleanup
                                }
                            });
        }
    }

    /**
     * Execute a command and wait for result.
     */
    protected ProcessResult execute(String... command) throws Exception {
        return new ProcessExecutor()
                .command(command)
                .directory(workspaceRoot.toFile())
                .readOutput(true)
                .execute();
    }

    /**
     * Create a Groovy file in the workspace.
     */
    protected Path createGroovyFile(String relativePath, String content) throws IOException {
        Path file = workspaceRoot.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }
}
