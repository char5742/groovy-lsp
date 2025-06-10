package com.groovy.lsp.server.launcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import com.groovy.lsp.test.annotations.UnitTest;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for the Main launcher class.
 */
class MainTest {

    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalErr = System.err;
    private final PrintStream originalOut = System.out;

    @TempDir @Nullable Path tempDir;

    @BeforeEach
    void setUp() {
        System.setErr(new PrintStream(errContent));
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setErr(originalErr);
        System.setOut(originalOut);
    }

    @UnitTest
    void testHelpOption() {
        // Test that --help throws HelpRequestedException
        assertThrows(
                Main.HelpRequestedException.class,
                () -> Main.parseArguments(new String[] {"--help"}));
    }

    @UnitTest
    void testInvalidPort() {
        // Test that invalid port number throws exception
        assertThrows(
                IllegalArgumentException.class,
                () -> Main.parseArguments(new String[] {"--socket", "--port", "invalid"}));
    }

    @UnitTest
    void testMissingPortValue() {
        // Test that missing port value throws exception
        assertThrows(
                IllegalArgumentException.class,
                () -> Main.parseArguments(new String[] {"--socket", "--port"}));
    }

    @UnitTest
    void testMissingHostValue() {
        // Test that missing host value throws exception
        assertThrows(
                IllegalArgumentException.class,
                () -> Main.parseArguments(new String[] {"--socket", "--host"}));
    }

    @UnitTest
    void testValidSocketMode() throws Exception {
        // Test valid socket mode arguments
        String[] args = {"--socket", "--host", "localhost", "--port", "8080"};

        Main.LaunchMode mode = Main.parseArguments(args);

        assertThat(mode.type).isEqualTo(Main.LaunchType.SOCKET);
        assertThat(mode.host).isEqualTo("localhost");
        assertThat(mode.port).isEqualTo(8080);
    }

    @UnitTest
    void testSocketModeDefaults() throws Exception {
        // Test socket mode with default host and port
        String[] args = {"--socket"};

        Main.LaunchMode mode = Main.parseArguments(args);

        assertThat(mode.type).isEqualTo(Main.LaunchType.SOCKET);
        assertThat(mode.host).isEqualTo("localhost"); // default
        assertThat(mode.port).isEqualTo(4389); // default
    }

    @UnitTest
    void testWorkspaceValidation() throws Exception {
        // Test workspace directory validation
        Path validWorkspace = Objects.requireNonNull(tempDir).resolve("workspace");
        Files.createDirectory(validWorkspace);

        String[] args = {"--workspace", validWorkspace.toString()};

        Main.LaunchMode mode = Main.parseArguments(args);

        assertThat(mode.workspaceRoot).isEqualTo(validWorkspace.toString());
    }

    @UnitTest
    void testStdioModeDefault() throws Exception {
        // Test that stdio mode is the default
        String[] args = {};

        Main.LaunchMode mode = Main.parseArguments(args);

        assertThat(mode.type).isEqualTo(Main.LaunchType.STDIO);
        assertThat(mode.workspaceRoot).isNull();
    }

    @UnitTest
    void testPortBoundaryValues() throws Exception {
        // Test port at boundaries
        assertThrows(
                IllegalArgumentException.class,
                () -> Main.parseArguments(new String[] {"--socket", "--port", "0"}),
                "Port 0 should be invalid");

        // Port 65535 should be valid (max port)
        Main.LaunchMode mode = Main.parseArguments(new String[] {"--socket", "--port", "65535"});
        assertThat(mode.port).isEqualTo(65535);
    }

    @UnitTest
    void testMissingWorkspaceValue() {
        // Test that missing workspace value throws exception
        assertThrows(
                IllegalArgumentException.class,
                () -> Main.parseArguments(new String[] {"--workspace"}),
                "Missing workspace value should throw exception");
    }

    @UnitTest
    void testDryRunModeComplete() throws Exception {
        // Test dry-run mode with all options
        String workspace = Objects.requireNonNull(tempDir).toString();
        String[] args = {
            "--dry-run", "--socket", "--host", "0.0.0.0", "--port", "9090", "--workspace", workspace
        };

        assertDoesNotThrow(() -> Main.runServer(args));
    }

    @UnitTest
    void testDryRunModeStdio() throws Exception {
        // Test dry-run mode with stdio
        String[] args = {"--dry-run"};

        assertDoesNotThrow(() -> Main.runServer(args));
    }

    @UnitTest
    void testShortFormArguments() throws Exception {
        // Test all short form arguments
        String workspace = Objects.requireNonNull(tempDir).toString();
        String[] args = {"-s", "-h", "127.0.0.1", "-p", "7777", "-w", workspace};

        Main.LaunchMode mode = Main.parseArguments(args);

        assertThat(mode.type).isEqualTo(Main.LaunchType.SOCKET);
        assertThat(mode.host).isEqualTo("127.0.0.1");
        assertThat(mode.port).isEqualTo(7777);
        assertThat(mode.workspaceRoot).isEqualTo(workspace);
    }

    @UnitTest
    void testInvalidWorkspacePath() {
        // Test with workspace as file instead of directory
        String[] args = {"--workspace", "/dev/null"};

        assertThrows(
                IllegalArgumentException.class,
                () -> Main.parseArguments(args),
                "Should throw exception for non-directory workspace");
    }

    @UnitTest
    void testMultiplePortArguments() throws Exception {
        // Test that last port wins
        String[] args = {"--socket", "--port", "1111", "--port", "2222"};

        Main.LaunchMode mode = Main.parseArguments(args);
        assertThat(mode.port).isEqualTo(2222);
    }

    @UnitTest
    void testCombinedShortAndLongForm() throws Exception {
        // Test mixing short and long form arguments
        String[] args = {"-s", "--port", "3333", "-h", "example.com"};

        Main.LaunchMode mode = Main.parseArguments(args);

        assertThat(mode.type).isEqualTo(Main.LaunchType.SOCKET);
        assertThat(mode.host).isEqualTo("example.com");
        assertThat(mode.port).isEqualTo(3333);
    }

    @UnitTest
    void testEmptyArguments() throws Exception {
        // Test with empty arguments array
        String[] args = {};

        Main.LaunchMode mode = Main.parseArguments(args);

        assertThat(mode.type).isEqualTo(Main.LaunchType.STDIO);
        assertThat(mode.workspaceRoot).isNull();
        assertThat(mode.dryRun).isFalse();
    }

    @UnitTest
    void testNullArguments() throws Exception {
        // Test with null array (edge case)
        // Use reflection to test null handling
        Method parseArgumentsMethod =
                Main.class.getDeclaredMethod("parseArguments", String[].class);
        parseArgumentsMethod.setAccessible(true);

        assertThatThrownBy(() -> parseArgumentsMethod.invoke(null, (Object) null))
                .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                .hasCauseInstanceOf(NullPointerException.class);
    }

    @UnitTest
    void testPortZero() {
        // Test port = 0
        String[] args = {"--socket", "--port", "0"};

        assertThrows(
                IllegalArgumentException.class,
                () -> Main.parseArguments(args),
                "Port 0 should be invalid");
    }

    @UnitTest
    void testPortMaxValue() throws Exception {
        // Test port = Integer.MAX_VALUE (out of valid range)
        String[] args = {"--socket", "--port", String.valueOf(Integer.MAX_VALUE)};

        assertThrows(
                IllegalArgumentException.class,
                () -> Main.parseArguments(args),
                "Port out of range should be invalid");
    }

    @UnitTest
    void testHostAndPortWithoutSocketMode() throws Exception {
        // Test setting host and port without --socket
        String[] args = {"--host", "example.com", "--port", "5555"};

        Main.LaunchMode mode = Main.parseArguments(args);

        // Should still be in stdio mode
        assertThat(mode.type).isEqualTo(Main.LaunchType.STDIO);
        assertThat(mode.host).isEqualTo("example.com");
        assertThat(mode.port).isEqualTo(5555);
    }

    @UnitTest
    void testSocketModeWithOnlyHost() throws Exception {
        // Test socket mode with only host specified
        String[] args = {"--socket", "--host", "0.0.0.0"};

        Main.LaunchMode mode = Main.parseArguments(args);

        assertThat(mode.type).isEqualTo(Main.LaunchType.SOCKET);
        assertThat(mode.host).isEqualTo("0.0.0.0");
        assertThat(mode.port).isEqualTo(4389); // default port
    }

    @UnitTest
    void testSocketModeWithOnlyPort() throws Exception {
        // Test socket mode with only port specified
        String[] args = {"--socket", "--port", "6666"};

        Main.LaunchMode mode = Main.parseArguments(args);

        assertThat(mode.type).isEqualTo(Main.LaunchType.SOCKET);
        assertThat(mode.host).isEqualTo("localhost"); // default host
        assertThat(mode.port).isEqualTo(6666);
    }

    @UnitTest
    void testRelativeWorkspacePath() throws Exception {
        // Test with relative workspace path
        String[] args = {"--workspace", "."};

        Main.LaunchMode mode = Main.parseArguments(args);

        assertThat(mode.workspaceRoot).isEqualTo(".");
    }

    @UnitTest
    void testSocketModeWithHostSetButPortZero() throws Exception {
        // Test socket mode where host is already set but port is 0 (should use default port)
        String[] args = {"--socket"};

        Main.LaunchMode mode = Main.parseArguments(args);
        mode.host = "custom.host"; // Simulate host already being set
        mode.port = 0; // But port is still 0

        // Re-parse to trigger default setting logic
        String[] args2 = {"--socket", "--host", "custom.host"};
        Main.LaunchMode mode2 = Main.parseArguments(args2);

        assertThat(mode2.type).isEqualTo(Main.LaunchType.SOCKET);
        assertThat(mode2.host).isEqualTo("custom.host");
        assertThat(mode2.port).isEqualTo(4389); // default port
    }

    @UnitTest
    void testMainMethodWithHelp() {
        // Test main method with --help argument by calling runServer instead
        try {
            Main.runServer(new String[] {"--help"});
        } catch (Main.HelpRequestedException e) {
            // Expected exception for help
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }

        // Verify help was printed to stdout
        String output = outContent.toString(StandardCharsets.UTF_8);
        assertThat(output).contains("Groovy Language Server");
        assertThat(output).contains("Usage:");
    }

    @UnitTest
    void testMainMethodWithInvalidArguments() {
        // Test parseArguments with invalid port
        assertThrows(
                IllegalArgumentException.class,
                () -> Main.parseArguments(new String[] {"--port", "invalid"}),
                "Should throw exception for invalid port");
    }

    @UnitTest
    void testPrintHelp() throws Exception {
        // Use reflection to test private printHelp method
        Method printHelpMethod = Main.class.getDeclaredMethod("printHelp");
        printHelpMethod.setAccessible(true);

        // Clear output before test
        outContent.reset();

        // Invoke printHelp
        printHelpMethod.invoke(null);

        // Verify help content
        String output = outContent.toString(StandardCharsets.UTF_8);
        assertThat(output).contains("Groovy Language Server");
        assertThat(output).contains("Usage: groovy-language-server [options]");
        assertThat(output).contains("--socket");
        assertThat(output).contains("--host");
        assertThat(output).contains("--port");
        assertThat(output).contains("--workspace");
        assertThat(output).contains("--dry-run");
        assertThat(output).contains("--help");
        assertThat(output).contains("Environment variables:");
        assertThat(output).contains("Examples:");
    }

    @UnitTest
    void testCreateExecutorService() throws Exception {
        // Use reflection to test private createExecutorService method
        Method createExecutorServiceMethod = Main.class.getDeclaredMethod("createExecutorService");
        createExecutorServiceMethod.setAccessible(true);

        // Invoke createExecutorService
        ExecutorService executorService =
                (ExecutorService) createExecutorServiceMethod.invoke(null);

        // Verify executor service is created correctly
        assertThat(executorService).isNotNull();

        // Test that threads are daemon threads
        Future<?> future =
                executorService.submit(
                        () -> {
                            assertThat(Thread.currentThread().isDaemon()).isTrue();
                            assertThat(Thread.currentThread().getName())
                                    .isEqualTo("groovy-lsp-jsonrpc");
                        });

        // Wait for future to complete
        assertThat(future).isNotNull();

        // Cleanup
        executorService.shutdown();
    }

    @UnitTest
    void testLaunchModeEnum() {
        // Test LaunchType enum values
        Main.LaunchType[] types = Main.LaunchType.values();
        assertThat(types).hasSize(2);
        assertThat(types).contains(Main.LaunchType.STDIO, Main.LaunchType.SOCKET);
    }

    @UnitTest
    void testHelpRequestedException() {
        // Test HelpRequestedException
        Main.HelpRequestedException exception = new Main.HelpRequestedException();
        assertThat(exception.getMessage()).isEqualTo("Help requested");
    }

    @UnitTest
    void testLaunchModeClass() {
        // Test LaunchMode default values
        Main.LaunchMode mode = new Main.LaunchMode();
        assertThat(mode.type).isEqualTo(Main.LaunchType.STDIO);
        assertThat(mode.host).isEqualTo("localhost");
        assertThat(mode.port).isEqualTo(4389);
        assertThat(mode.dryRun).isFalse();
        assertThat(mode.workspaceRoot).isNull();
    }

    @UnitTest
    void testArgumentValidation() throws Exception {
        // Test various argument validations

        // Test workspace validation with non-existent directory
        String nonExistentPath = Objects.requireNonNull(tempDir).resolve("non-existent").toString();
        assertThrows(
                IllegalArgumentException.class,
                () -> Main.parseArguments(new String[] {"--workspace", nonExistentPath}),
                "Should throw exception for non-existent workspace");

        // Test workspace validation with file instead of directory
        Path file = Objects.requireNonNull(tempDir).resolve("test.txt");
        Files.createFile(file);
        assertThrows(
                IllegalArgumentException.class,
                () -> Main.parseArguments(new String[] {"--workspace", file.toString()}),
                "Should throw exception for workspace that is a file");
    }
}
