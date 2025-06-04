package com.groovy.lsp.server.launcher;

import static org.assertj.core.api.Assertions.assertThat;
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
        // Test that --help throws HelpRequestedException
        assertThrows(
                Main.HelpRequestedException.class,
                () -> Main.parseArguments(new String[] {"--help"}));
    }

    @Test
    void testInvalidPort() {
        // Test that invalid port number throws exception
        assertThrows(
                IllegalArgumentException.class,
                () -> Main.parseArguments(new String[] {"--socket", "--port", "invalid"}));
    }

    @Test
    void testMissingPortValue() {
        // Test that missing port value throws exception
        assertThrows(
                IllegalArgumentException.class,
                () -> Main.parseArguments(new String[] {"--socket", "--port"}));
    }

    @Test
    void testMissingHostValue() {
        // Test that missing host value throws exception
        assertThrows(
                IllegalArgumentException.class,
                () -> Main.parseArguments(new String[] {"--socket", "--host"}));
    }

    @Test
    void testValidSocketMode() throws Exception {
        // Test valid socket mode arguments
        String[] args = {"--socket", "--host", "localhost", "--port", "8080"};

        Main.LaunchMode mode = Main.parseArguments(args);

        assertThat(mode.type).isEqualTo(Main.LaunchType.SOCKET);
        assertThat(mode.host).isEqualTo("localhost");
        assertThat(mode.port).isEqualTo(8080);
    }

    @Test
    void testSocketModeDefaults() throws Exception {
        // Test socket mode with default host and port
        String[] args = {"--socket"};

        Main.LaunchMode mode = Main.parseArguments(args);

        assertThat(mode.type).isEqualTo(Main.LaunchType.SOCKET);
        assertThat(mode.host).isEqualTo("localhost"); // default
        assertThat(mode.port).isEqualTo(4389); // default
    }

    @Test
    void testWorkspaceValidation() throws Exception {
        // Test workspace directory validation
        Path validWorkspace = tempDir.resolve("workspace");
        Files.createDirectory(validWorkspace);

        String[] args = {"--workspace", validWorkspace.toString()};

        Main.LaunchMode mode = Main.parseArguments(args);

        assertThat(mode.workspaceRoot).isEqualTo(validWorkspace.toString());
    }

    @Test
    void testStdioModeDefault() throws Exception {
        // Test that stdio mode is the default
        String[] args = {};

        Main.LaunchMode mode = Main.parseArguments(args);

        assertThat(mode.type).isEqualTo(Main.LaunchType.STDIO);
        assertThat(mode.workspaceRoot).isNull();
    }

    @Test
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

    @Test
    void testMissingWorkspaceValue() {
        // Test that missing workspace value throws exception
        assertThrows(
                IllegalArgumentException.class,
                () -> Main.parseArguments(new String[] {"--workspace"}),
                "Missing workspace value should throw exception");
    }
}
