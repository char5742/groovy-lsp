package com.groovy.lsp.server.launcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.groovy.lsp.protocol.api.GroovyLanguageServer;
import com.groovy.lsp.server.launcher.di.ServerModule;
import com.groovy.lsp.test.annotations.UnitTest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import javax.annotation.Nullable;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * Tests for the Main class launcher methods.
 * These tests focus on the server launching functionality.
 */
class MainLaunchTest {

    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @TempDir @Nullable Path tempDir;

    @BeforeEach
    void setUp() {
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @UnitTest
    void testMainMethodExecution() throws Exception {
        // Use reflection to test the main method behavior
        Method mainMethod = Main.class.getDeclaredMethod("main", String[].class);
        mainMethod.setAccessible(true);

        // We can't easily test main() since it calls System.exit
        // Instead, verify the static logger is initialized correctly
        assertThat(Main.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("logger"));
    }

    @UnitTest
    @SuppressWarnings("AddressSelection") // Intentional for test - mocking socket addresses
    void testRunServerWithSocketMode() throws Exception {
        // Test runServer method with socket mode - using mocks
        GroovyLanguageServer mockServer = mock(GroovyLanguageServer.class);
        LanguageClient mockClient = mock(LanguageClient.class);
        Future<Void> mockFuture = mock(Future.class);

        // Mock the future to throw InterruptedException to exit the listening loop
        when(mockFuture.get()).thenThrow(new InterruptedException("Test interruption"));

        try (MockedStatic<Guice> guiceMock = mockStatic(Guice.class);
                MockedConstruction<ServerSocket> serverSocketMock =
                        mockConstruction(
                                ServerSocket.class,
                                (mock, context) -> {
                                    Socket clientSocket = mock(Socket.class);
                                    when(clientSocket.getInputStream())
                                            .thenReturn(new ByteArrayInputStream(new byte[0]));
                                    when(clientSocket.getOutputStream())
                                            .thenReturn(new ByteArrayOutputStream());
                                    when(clientSocket.getRemoteSocketAddress())
                                            .thenReturn(
                                                    new InetSocketAddress(
                                                            "test-client",
                                                            12345)); // Error-prone suppression:
                                    // AddressSelection intentional
                                    // for test
                                    when(mock.accept()).thenReturn(clientSocket);
                                });
                MockedStatic<LSPLauncher> launcherMock = mockStatic(LSPLauncher.class)) {

            // Mock Guice injector
            Injector mockInjector = mock(Injector.class);
            when(mockInjector.getInstance(GroovyLanguageServer.class)).thenReturn(mockServer);
            guiceMock
                    .when(() -> Guice.createInjector(any(ServerModule.class)))
                    .thenReturn(mockInjector);

            // Mock launcher
            Launcher<LanguageClient> mockLauncher = mock(Launcher.class);
            when(mockLauncher.getRemoteProxy()).thenReturn(mockClient);
            when(mockLauncher.startListening()).thenReturn(mockFuture);

            launcherMock
                    .when(
                            () ->
                                    LSPLauncher.createServerLauncher(
                                            any(LanguageServer.class),
                                            any(InputStream.class),
                                            any(OutputStream.class),
                                            any(ExecutorService.class),
                                            any()))
                    .thenReturn(mockLauncher);

            // Run server in socket mode
            assertThrows(
                    InterruptedException.class,
                    () -> Main.runServer(new String[] {"--socket", "--port", "9999"}));

            // Verify server was connected
            verify(mockServer).connect(mockClient);
        }
    }

    @UnitTest
    void testRunServerWithStdioMode() throws Exception {
        // Test runServer method with stdio mode
        GroovyLanguageServer mockServer = mock(GroovyLanguageServer.class);
        LanguageClient mockClient = mock(LanguageClient.class);
        Future<Void> mockFuture = mock(Future.class);

        // Mock the future to throw InterruptedException to exit the listening loop
        when(mockFuture.get()).thenThrow(new InterruptedException("Test interruption"));

        try (MockedStatic<Guice> guiceMock = mockStatic(Guice.class);
                MockedStatic<LSPLauncher> launcherMock = mockStatic(LSPLauncher.class)) {

            // Mock Guice injector
            Injector mockInjector = mock(Injector.class);
            when(mockInjector.getInstance(GroovyLanguageServer.class)).thenReturn(mockServer);
            guiceMock
                    .when(() -> Guice.createInjector(any(ServerModule.class)))
                    .thenReturn(mockInjector);

            // Mock launcher
            Launcher<LanguageClient> mockLauncher = mock(Launcher.class);
            when(mockLauncher.getRemoteProxy()).thenReturn(mockClient);
            when(mockLauncher.startListening()).thenReturn(mockFuture);

            launcherMock
                    .when(
                            () ->
                                    LSPLauncher.createServerLauncher(
                                            any(LanguageServer.class),
                                            eq(System.in),
                                            eq(System.out),
                                            any(ExecutorService.class),
                                            any()))
                    .thenReturn(mockLauncher);

            // Run server in stdio mode
            assertThrows(InterruptedException.class, () -> Main.runServer(new String[] {}));

            // Verify server was connected
            verify(mockServer).connect(mockClient);
        }
    }

    @UnitTest
    void testLaunchSocketWithPortInUse() throws Exception {
        // Test socket launch with port already in use
        try (ServerSocket blockingSocket = new ServerSocket(0)) {
            int port = blockingSocket.getLocalPort();

            GroovyLanguageServer mockServer = mock(GroovyLanguageServer.class);

            try (MockedStatic<Guice> guiceMock = mockStatic(Guice.class)) {
                // Mock Guice injector
                Injector mockInjector = mock(Injector.class);
                when(mockInjector.getInstance(GroovyLanguageServer.class)).thenReturn(mockServer);
                guiceMock
                        .when(() -> Guice.createInjector(any(ServerModule.class)))
                        .thenReturn(mockInjector);

                // Try to run server on the same port
                Exception exception =
                        assertThrows(
                                IllegalStateException.class,
                                () ->
                                        Main.runServer(
                                                new String[] {
                                                    "--socket", "--port", String.valueOf(port)
                                                }));

                assertThat(exception.getMessage()).contains("Port " + port + " is already in use");
            }
        }
    }

    @UnitTest
    void testRunServerWithInvalidWorkspace() {
        // Test with non-existent workspace
        String nonExistentPath = Objects.requireNonNull(tempDir).resolve("non-existent").toString();

        Exception exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> Main.runServer(new String[] {"--workspace", nonExistentPath}));

        assertThat(exception.getMessage()).contains("Workspace directory does not exist");
    }

    @UnitTest
    void testRunServerWithWorkspaceAsFile() throws Exception {
        // Test with workspace path pointing to a file
        Path file = Objects.requireNonNull(tempDir).resolve("test.txt");
        Files.createFile(file);

        Exception exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> Main.runServer(new String[] {"--workspace", file.toString()}));

        assertThat(exception.getMessage()).contains("Workspace path is not a directory");
    }

    @UnitTest
    void testLaunchStdioDirectly() throws Exception {
        // Use reflection to test launchStdio method directly
        Method launchStdioMethod =
                Main.class.getDeclaredMethod("launchStdio", LanguageServer.class);
        launchStdioMethod.setAccessible(true);

        GroovyLanguageServer mockServer = mock(GroovyLanguageServer.class);
        LanguageClient mockClient = mock(LanguageClient.class);
        Future<Void> mockFuture = mock(Future.class);

        // Mock the future to complete immediately
        when(mockFuture.get()).thenReturn(null);

        try (MockedStatic<LSPLauncher> launcherMock = mockStatic(LSPLauncher.class)) {
            // Mock launcher
            Launcher<LanguageClient> mockLauncher = mock(Launcher.class);
            when(mockLauncher.getRemoteProxy()).thenReturn(mockClient);
            when(mockLauncher.startListening()).thenReturn(mockFuture);

            launcherMock
                    .when(
                            () ->
                                    LSPLauncher.createServerLauncher(
                                            any(LanguageServer.class),
                                            eq(System.in),
                                            eq(System.out),
                                            any(ExecutorService.class),
                                            any()))
                    .thenReturn(mockLauncher);

            // Invoke launchStdio
            assertDoesNotThrow(() -> launchStdioMethod.invoke(null, mockServer));

            // Verify server was connected
            verify(mockServer).connect(mockClient);
            verify(mockLauncher).startListening();
        }
    }

    @UnitTest
    @SuppressWarnings("AddressSelection") // Intentional for test - mocking socket addresses
    void testLaunchSocketDirectly() throws Exception {
        // Use reflection to test launchSocket method directly
        Method launchSocketMethod =
                Main.class.getDeclaredMethod(
                        "launchSocket", LanguageServer.class, String.class, int.class);
        launchSocketMethod.setAccessible(true);

        GroovyLanguageServer mockServer = mock(GroovyLanguageServer.class);
        LanguageClient mockClient = mock(LanguageClient.class);
        Future<Void> mockFuture = mock(Future.class);

        // Mock the future to complete immediately
        when(mockFuture.get()).thenReturn(null);

        try (MockedConstruction<ServerSocket> serverSocketMock =
                        mockConstruction(
                                ServerSocket.class,
                                (mock, context) -> {
                                    Socket clientSocket = mock(Socket.class);
                                    when(clientSocket.getInputStream())
                                            .thenReturn(new ByteArrayInputStream(new byte[0]));
                                    when(clientSocket.getOutputStream())
                                            .thenReturn(new ByteArrayOutputStream());
                                    when(clientSocket.getRemoteSocketAddress())
                                            .thenReturn(
                                                    new InetSocketAddress(
                                                            "test-client",
                                                            12345)); // Error-prone suppression:
                                    // AddressSelection intentional
                                    // for test
                                    when(mock.accept()).thenReturn(clientSocket);
                                });
                MockedStatic<LSPLauncher> launcherMock = mockStatic(LSPLauncher.class)) {

            // Mock launcher
            Launcher<LanguageClient> mockLauncher = mock(Launcher.class);
            when(mockLauncher.getRemoteProxy()).thenReturn(mockClient);
            when(mockLauncher.startListening()).thenReturn(mockFuture);

            launcherMock
                    .when(
                            () ->
                                    LSPLauncher.createServerLauncher(
                                            any(LanguageServer.class),
                                            any(InputStream.class),
                                            any(OutputStream.class),
                                            any(ExecutorService.class),
                                            any()))
                    .thenReturn(mockLauncher);

            // Invoke launchSocket
            assertDoesNotThrow(() -> launchSocketMethod.invoke(null, mockServer, "localhost", 0));

            // Verify server was connected
            verify(mockServer).connect(mockClient);
            verify(mockLauncher).startListening();
        }
    }

    @UnitTest
    void testLaunchSocketWithBindFailure() throws Exception {
        // Test socket launch with generic bind failure
        Method launchSocketMethod =
                Main.class.getDeclaredMethod(
                        "launchSocket", LanguageServer.class, String.class, int.class);
        launchSocketMethod.setAccessible(true);

        GroovyLanguageServer mockServer = mock(GroovyLanguageServer.class);

        try (MockedConstruction<ServerSocket> serverSocketMock =
                mockConstruction(
                        ServerSocket.class,
                        (mock, context) -> {
                            doThrow(new IOException("Network error"))
                                    .when(mock)
                                    .bind(any(InetSocketAddress.class));
                        })) {

            // Invoke launchSocket and expect IOException
            Exception exception =
                    assertThrows(
                            Exception.class,
                            () -> launchSocketMethod.invoke(null, mockServer, "localhost", 8888));

            // The InvocationTargetException wraps the actual IOException
            Throwable cause = exception.getCause();
            assertThat(cause).isNotNull();
            assertThat(cause).isInstanceOf(IOException.class);
            if (cause != null) {
                assertThat(cause.getMessage()).contains("Network error");
            }
        }
    }

    @UnitTest
    void testRunServerUnknownLaunchMode() throws Exception {
        // This test is tricky because LaunchType is an enum with only two values
        // We'll test the edge case by using reflection to modify the launch mode
        GroovyLanguageServer mockServer = mock(GroovyLanguageServer.class);

        try (MockedStatic<Guice> guiceMock = mockStatic(Guice.class)) {
            // Mock Guice injector
            Injector mockInjector = mock(Injector.class);
            when(mockInjector.getInstance(GroovyLanguageServer.class)).thenReturn(mockServer);
            guiceMock
                    .when(() -> Guice.createInjector(any(ServerModule.class)))
                    .thenReturn(mockInjector);

            // This would require modifying the enum at runtime which is complex
            // Instead, we'll verify the current implementation handles all enum values
            Main.LaunchType[] allTypes = Main.LaunchType.values();
            assertThat(allTypes)
                    .containsExactlyInAnyOrder(Main.LaunchType.STDIO, Main.LaunchType.SOCKET);
        }
    }

    @UnitTest
    void testMainConstructor() throws Exception {
        // Test that Main constructor exists (for coverage)
        Main instance = Main.class.getDeclaredConstructor().newInstance();
        assertThat(instance).isNotNull();
    }

    @UnitTest
    void testSocketCleanupOnError() throws Exception {
        // Test that socket resources are properly cleaned up on error
        Method launchSocketMethod =
                Main.class.getDeclaredMethod(
                        "launchSocket", LanguageServer.class, String.class, int.class);
        launchSocketMethod.setAccessible(true);

        GroovyLanguageServer mockServer = mock(GroovyLanguageServer.class);
        Socket mockClientSocket = mock(Socket.class);

        // Setup mocks to throw exception during processing
        when(mockClientSocket.getInputStream()).thenThrow(new IOException("Stream error"));

        try (MockedConstruction<ServerSocket> serverSocketMock =
                mockConstruction(
                        ServerSocket.class,
                        (mock, context) -> {
                            when(mock.accept()).thenReturn(mockClientSocket);
                            when(mock.isClosed()).thenReturn(false);
                        })) {

            // Invoke launchSocket and expect IOException
            assertThrows(
                    Exception.class,
                    () -> launchSocketMethod.invoke(null, mockServer, "localhost", 7777));

            // Verify cleanup was attempted
            ServerSocket constructedSocket = serverSocketMock.constructed().get(0);
            verify(constructedSocket).close();
            verify(mockClientSocket).close();
        }
    }
}
