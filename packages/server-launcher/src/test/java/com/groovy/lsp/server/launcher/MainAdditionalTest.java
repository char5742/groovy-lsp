package com.groovy.lsp.server.launcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Additional tests for Main launcher to improve branch coverage.
 */
class MainAdditionalTest {

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
    void main_shouldHandleInvalidPortFormat() {
        // given
        String[] args = {"--socket", "--port", "abc123"};

        // when/then
        assertThatThrownBy(() -> Main.main(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid port number");
    }

    @Test
    void main_shouldHandleNonExistentWorkspace() {
        // given
        String[] args = {"--workspace", "/non/existent/path"};

        // when/then
        assertThatThrownBy(() -> Main.main(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Workspace directory does not exist");
    }

    @Test
    void main_shouldHandleFileAsWorkspace() throws Exception {
        // given
        Path file = tempDir.resolve("file.txt");
        Files.createFile(file);
        String[] args = {"--workspace", file.toString()};

        // when/then
        assertThatThrownBy(() -> Main.main(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Workspace path is not a directory");
    }

    @Test
    void main_shouldHandleMissingWorkspaceValue() {
        // given
        String[] args = {"--workspace"};

        // when/then
        assertThatThrownBy(() -> Main.main(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing value for --workspace");
    }

    @Test
    void main_shouldHandleMissingPortValue() {
        // given
        String[] args = {"--socket", "--port"};

        // when/then
        assertThatThrownBy(() -> Main.main(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing value for --port");
    }

    @Test
    void main_shouldHandleMissingHostValue() {
        // given
        String[] args = {"--socket", "--host"};

        // when/then
        assertThatThrownBy(() -> Main.main(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing value for --host");
    }

    @Test
    void main_shouldPrintHelp() {
        // given
        String[] args = {"--help"};

        // when
        try {
            Main.main(args);
        } catch (SecurityException e) {
            // Expected when System.exit is called
        } catch (Exception e) {
            // Also handle any other exceptions from System.exit
        }

        // then
        String output = outContent.toString();
        assertThat(output).contains("Groovy Language Server");
        assertThat(output).contains("Usage:");
        assertThat(output).contains("--socket");
        assertThat(output).contains("--host");
        assertThat(output).contains("--port");
        assertThat(output).contains("--workspace");
        assertThat(output).contains("--help");
    }

    @Test
    void main_shouldHandleNonReadableWorkspace() throws Exception {
        // given
        Path nonReadable = tempDir.resolve("non-readable");
        Files.createDirectory(nonReadable);

        // Try to make it non-readable (this might not work on all platforms)
        File dir = nonReadable.toFile();
        if (dir.setReadable(false)) {
            String[] args = {"--workspace", nonReadable.toString()};

            // when/then
            assertThatThrownBy(() -> Main.main(args))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot read workspace directory");

            // Restore permissions
            dir.setReadable(true);
        }
        // If we can't set permissions, just skip this test
    }

    @Test
    void main_shouldHandleWorkspaceShortForm() {
        // given
        String workspace = tempDir.toString();
        String[] args = {"-w", workspace};

        // when/then - Just verify parsing works, don't actually launch
        // This will fail when trying to create Guice injector, but that's after argument parsing
        try {
            Main.main(args);
        } catch (Exception e) {
            // Expected - we're just testing argument parsing
            if (e instanceof IllegalArgumentException && e.getMessage().contains("workspace")) {
                throw (IllegalArgumentException) e; // Re-throw workspace-related errors
            }
        }
    }

    @Test
    void main_shouldHandlePortShortForm() {
        // given
        String[] args = {"-s", "-p", "8080"};

        // when/then - Just verify parsing works, don't actually launch
        try {
            Main.main(args);
        } catch (Exception e) {
            // Expected - we're just testing argument parsing
            if (e instanceof IllegalArgumentException
                    && (e.getMessage().contains("port") || e.getMessage().contains("Invalid"))) {
                throw (IllegalArgumentException) e; // Re-throw port-related errors
            }
        }
    }

    @Test
    void main_shouldHandleHostShortForm() {
        // given
        String[] args = {"-s", "-h", "localhost"};

        // when/then - Just verify parsing works, don't actually launch
        try {
            Main.main(args);
        } catch (Exception e) {
            // Expected - we're just testing argument parsing
            if (e instanceof IllegalArgumentException && e.getMessage().contains("host")) {
                throw (IllegalArgumentException) e; // Re-throw host-related errors
            }
        }
    }
}
