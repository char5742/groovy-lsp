package com.groovy.lsp.server.launcher;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for the Main launcher class.
 */
class MainTest {

    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalErr = System.err;
    private final PrintStream originalOut = System.out;

    @TempDir Path tempDir;

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

    @Test
    void testHelpOption() {
        // Test that --help prints usage information
        try {
            Main.main(new String[] {"--help"});
        } catch (SecurityException e) {
            // Expected when System.exit is called
        }

        String output = errContent.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(
                output.contains("Groovy Language Server") || output.contains("Usage:"),
                "Help output should contain usage information");
    }

    @Test
    void testInvalidPort() {
        // Test that invalid port number throws exception
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    Main.main(new String[] {"--socket", "--port", "invalid"});
                });
    }

    @Test
    void testMissingPortValue() {
        // Test that missing port value throws exception
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    Main.main(new String[] {"--socket", "--port"});
                });
    }

    @Test
    void testMissingHostValue() {
        // Test that missing host value throws exception
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    Main.main(new String[] {"--socket", "--host"});
                });
    }

    @Test
    void testValidSocketMode() {
        // Test valid socket mode arguments
        String[] args = {"--socket", "--host", "localhost", "--port", "8080"};

        // This will try to launch the server, which will fail in test environment
        // but we're testing argument parsing
        try {
            Main.main(args);
        } catch (Exception e) {
            // Expected - server launch will fail in test
            // But argument parsing should succeed
            if (e instanceof IllegalArgumentException) {
                fail("Should not throw IllegalArgumentException for valid arguments");
            }
        }
    }

    @Test
    void testSocketModeDefaults() {
        // Test socket mode with default host and port
        String[] args = {"--socket"};

        try {
            Main.main(args);
        } catch (Exception e) {
            // Expected - server launch will fail in test
            if (e instanceof IllegalArgumentException) {
                fail("Should not throw IllegalArgumentException for valid arguments");
            }
        }
    }

    @Test
    void testWorkspaceValidation() throws Exception {
        // Test workspace directory validation
        Path validWorkspace = tempDir.resolve("workspace");
        Files.createDirectory(validWorkspace);

        String[] args = {"--workspace", validWorkspace.toString()};

        try {
            Main.main(args);
        } catch (Exception e) {
            // Expected - server launch will fail in test
            if (e instanceof IllegalArgumentException && e.getMessage().contains("workspace")) {
                fail("Should not throw IllegalArgumentException for valid workspace");
            }
        }
    }

    @Test
    void testStdioModeDefault() {
        // Test that stdio mode is the default
        String[] args = {};

        try {
            Main.main(args);
        } catch (Exception e) {
            // Expected - server launch will fail in test
            if (e instanceof IllegalArgumentException) {
                fail("Should not throw IllegalArgumentException for default mode");
            }
        }
    }

    @Test
    void testPortBoundaryValues() {
        // Test port at boundaries
        assertThrows(
                IllegalArgumentException.class,
                () -> Main.main(new String[] {"--socket", "--port", "0"}),
                "Port 0 should be invalid");

        // Port 65535 should be valid (max port)
        try {
            Main.main(new String[] {"--socket", "--port", "65535"});
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("port")) {
                fail("Port 65535 should be valid");
            }
        } catch (Exception e) {
            // Expected - server launch will fail
        }
    }

    @Test
    void testMissingWorkspaceValue() {
        // Test that missing workspace value throws exception
        assertThrows(
                IllegalArgumentException.class,
                () -> Main.main(new String[] {"--workspace"}),
                "Missing workspace value should throw exception");
    }
}
