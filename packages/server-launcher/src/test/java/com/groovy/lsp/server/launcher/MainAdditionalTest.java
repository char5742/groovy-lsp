package com.groovy.lsp.server.launcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
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

    @Test
    void main_shouldHandleInvalidPortFormat() {
        // given
        String[] args = {"--socket", "--port", "abc123"};

        // when/then
        assertThatThrownBy(() -> Main.parseArguments(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid port number");
    }

    @Test
    void main_shouldHandleNonExistentWorkspace() {
        // given
        String[] args = {"--workspace", "/non/existent/path"};

        // when/then
        assertThatThrownBy(() -> Main.parseArguments(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Workspace directory does not exist");
    }

    @Test
    void main_shouldHandleFileAsWorkspace() throws Exception {
        // given
        Path file =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("file.txt");
        Files.createFile(file);
        String[] args = {"--workspace", file.toString()};

        // when/then
        assertThatThrownBy(() -> Main.parseArguments(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Workspace path is not a directory");
    }

    @Test
    void main_shouldHandleMissingWorkspaceValue() {
        // given
        String[] args = {"--workspace"};

        // when/then
        assertThatThrownBy(() -> Main.parseArguments(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing value for --workspace");
    }

    @Test
    void main_shouldHandleMissingPortValue() {
        // given
        String[] args = {"--socket", "--port"};

        // when/then
        assertThatThrownBy(() -> Main.parseArguments(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing value for --port");
    }

    @Test
    void main_shouldHandleMissingHostValue() {
        // given
        String[] args = {"--socket", "--host"};

        // when/then
        assertThatThrownBy(() -> Main.parseArguments(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing value for --host");
    }

    @Test
    void main_shouldPrintHelp() throws Exception {
        // given
        String[] args = {"--help"};

        // when/then
        assertThatThrownBy(() -> Main.parseArguments(args))
                .isInstanceOf(Main.HelpRequestedException.class);
    }

    @Test
    void main_shouldHandleNonReadableWorkspace() throws Exception {
        // given
        Path nonReadable =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("non-readable");
        Files.createDirectory(nonReadable);

        // Try to make it non-readable (this might not work on all platforms)
        File dir = nonReadable.toFile();
        if (dir.setReadable(false)) {
            String[] args = {"--workspace", nonReadable.toString()};

            // when/then
            assertThatThrownBy(() -> Main.parseArguments(args))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot read workspace directory");

            // Restore permissions
            dir.setReadable(true);
        }
        // If we can't set permissions, just skip this test
    }

    @Test
    void main_shouldHandleWorkspaceShortForm() throws Exception {
        // given
        String workspace =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .toString();
        String[] args = {"-w", workspace};

        // when
        Main.LaunchMode mode = Main.parseArguments(args);

        // then
        assertThat(mode.workspaceRoot).isEqualTo(workspace);
    }

    @Test
    void main_shouldHandlePortShortForm() throws Exception {
        // given
        String[] args = {"-s", "-p", "8080"};

        // when
        Main.LaunchMode mode = Main.parseArguments(args);

        // then
        assertThat(mode.type).isEqualTo(Main.LaunchType.SOCKET);
        assertThat(mode.port).isEqualTo(8080);
    }

    @Test
    void main_shouldHandleHostShortForm() throws Exception {
        // given
        String[] args = {"-s", "-h", "localhost"};

        // when
        Main.LaunchMode mode = Main.parseArguments(args);

        // then
        assertThat(mode.type).isEqualTo(Main.LaunchType.SOCKET);
        assertThat(mode.host).isEqualTo("localhost");
    }

    @Test
    void main_shouldHandleSocketShortForm() throws Exception {
        // given
        String[] args = {"-s"};

        // when
        Main.LaunchMode mode = Main.parseArguments(args);

        // then
        assertThat(mode.type).isEqualTo(Main.LaunchType.SOCKET);
    }

    @Test
    void main_shouldHandleUnknownArgument() throws Exception {
        // given
        String[] args = {"--unknown-argument"};

        // when - Unknown arguments are just warned about, not thrown as errors
        Main.LaunchMode mode = Main.parseArguments(args);

        // then - Should use defaults
        assertThat(mode.type).isEqualTo(Main.LaunchType.STDIO);
    }

    @Test
    void main_shouldHandlePortOutOfRange() {
        // given
        String[] args = {"--socket", "--port", "70000"}; // Port > 65535

        // when/then
        assertThatThrownBy(() -> Main.parseArguments(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Port number must be between 1 and 65535");
    }

    @Test
    void main_shouldHandleNegativePort() {
        // given
        String[] args = {"--socket", "--port", "-1"};

        // when/then
        assertThatThrownBy(() -> Main.parseArguments(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Port number must be between 1 and 65535");
    }

    @Test
    void main_shouldHandleValidWorkspaceDirectory() throws Exception {
        // given
        String workspace =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .toString();
        String[] args = {"--workspace", workspace};

        // when
        Main.LaunchMode mode = Main.parseArguments(args);

        // then
        assertThat(mode.workspaceRoot).isEqualTo(workspace);
    }

    @Test
    void main_shouldHandleDryRunFlag() throws Exception {
        // given
        String[] args = {"--dry-run", "--socket", "--port", "8080"};

        // when
        Main.LaunchMode mode = Main.parseArguments(args);

        // then
        assertThat(mode.dryRun).isTrue();
        assertThat(mode.type).isEqualTo(Main.LaunchType.SOCKET);
        assertThat(mode.port).isEqualTo(8080);
    }

    @Test
    void main_shouldNotHangWithDryRun() throws Exception {
        // given
        String[] args = {"--dry-run"};

        // when/then - Should complete quickly without hanging
        assertThatCode(() -> Main.runServer(args)).doesNotThrowAnyException();
    }

    @Test
    void main_shouldHandleAllArgumentCombinations() throws Exception {
        // given
        String workspace =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .toString();
        String[] args = {"--dry-run", "-s", "-h", "0.0.0.0", "-p", "12345", "-w", workspace};

        // when
        assertThatCode(() -> Main.runServer(args)).doesNotThrowAnyException();
    }

    @Test
    void main_shouldHandleRepeatedArguments() throws Exception {
        // given - repeated workspace arguments
        String workspace1 =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("ws1")
                        .toString();
        String workspace2 =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("ws2")
                        .toString();
        Files.createDirectory(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("ws1"));
        Files.createDirectory(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("ws2"));

        String[] args = {"--workspace", workspace1, "--workspace", workspace2};

        // when
        Main.LaunchMode mode = Main.parseArguments(args);

        // then - last one wins
        assertThat(mode.workspaceRoot).isEqualTo(workspace2);
    }

    @Test
    void main_shouldHandleHostOnlyWithoutSocket() throws Exception {
        // given
        String[] args = {"--host", "example.com"};

        // when
        Main.LaunchMode mode = Main.parseArguments(args);

        // then - should remain in stdio mode
        assertThat(mode.type).isEqualTo(Main.LaunchType.STDIO);
        assertThat(mode.host).isEqualTo("example.com");
    }

    @Test
    void main_shouldHandlePortOnlyWithoutSocket() throws Exception {
        // given
        String[] args = {"--port", "8888"};

        // when
        Main.LaunchMode mode = Main.parseArguments(args);

        // then - should remain in stdio mode
        assertThat(mode.type).isEqualTo(Main.LaunchType.STDIO);
        assertThat(mode.port).isEqualTo(8888);
    }

    @Test
    void main_shouldHandleMixedValidAndInvalidArgs() throws Exception {
        // given - valid args mixed with unknown args
        String[] args = {"--socket", "--unknown", "--port", "5555", "--another-unknown"};

        // when
        Main.LaunchMode mode = Main.parseArguments(args);

        // then - should parse valid args and ignore unknown
        assertThat(mode.type).isEqualTo(Main.LaunchType.SOCKET);
        assertThat(mode.port).isEqualTo(5555);
    }

    @Test
    void main_shouldHandleArgumentsWithEquals() throws Exception {
        // given - some systems pass args like --port=8080
        String[] args = {"--port=8080"};

        // when - this should be treated as unknown argument
        Main.LaunchMode mode = Main.parseArguments(args);

        // then - defaults should be used
        assertThat(mode.type).isEqualTo(Main.LaunchType.STDIO);
    }

    @Test
    void main_shouldValidateWorkspaceExists() throws Exception {
        // given
        Path nonExistent =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("does-not-exist");
        String[] args = {"--workspace", nonExistent.toString()};

        // when/then
        assertThatThrownBy(() -> Main.parseArguments(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void main_shouldHandleSocketModeWithDefaults() throws Exception {
        // given
        String[] args = {"-s"};

        // when
        Main.LaunchMode mode = Main.parseArguments(args);

        // then
        assertThat(mode.type).isEqualTo(Main.LaunchType.SOCKET);
        assertThat(mode.host).isEqualTo("localhost");
        assertThat(mode.port).isEqualTo(4389);
    }

    @Test
    void main_shouldHandleDryRunWithSocket() throws Exception {
        // given
        String[] args = {"--dry-run", "--socket", "--host", "test.com", "--port", "9999"};

        // when/then
        assertThatCode(() -> Main.runServer(args)).doesNotThrowAnyException();
    }

    @Test
    void main_shouldHandleEmptyHost() throws Exception {
        // given
        String[] args = {"--socket", "--host", ""};

        // when
        Main.LaunchMode mode = Main.parseArguments(args);

        // then - empty host is allowed
        assertThat(mode.host).isEmpty();
    }

    @Test
    void main_shouldHandleMaxPortBoundary() throws Exception {
        // given
        String[] args = {"--socket", "--port", "65535"};

        // when
        Main.LaunchMode mode = Main.parseArguments(args);

        // then
        assertThat(mode.port).isEqualTo(65535);
    }

    @Test
    void main_shouldHandleMinPortBoundary() throws Exception {
        // given
        String[] args = {"--socket", "--port", "1"};

        // when
        Main.LaunchMode mode = Main.parseArguments(args);

        // then
        assertThat(mode.port).isEqualTo(1);
    }

    @Test
    void main_shouldHandleMissingWorkspaceValueShortForm() {
        // given
        String[] args = {"-w"};

        // when/then
        assertThatThrownBy(() -> Main.parseArguments(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing value for -w");
    }
}
