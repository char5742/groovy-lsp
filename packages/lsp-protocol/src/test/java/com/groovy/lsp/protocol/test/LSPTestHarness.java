package com.groovy.lsp.protocol.test;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Test harness for LSP4J protocol testing.
 * Provides utilities for testing language server implementations.
 */
public class LSPTestHarness implements AutoCloseable {
    private final LanguageServer server;
    private final LanguageClient client;
    private final TestLanguageClient testClient;
    private final ExecutorService executorService;
    private final Launcher<LanguageClient> serverLauncher;
    private final Launcher<LanguageServer> clientLauncher;

    private LSPTestHarness(Builder builder) throws IOException {
        // NullAway requires explicit null check even though Builder.build() validates
        if (builder.server == null) {
            throw new IllegalStateException("Server must be provided");
        }
        this.server = builder.server;
        this.testClient = builder.client != null ? builder.client : new TestLanguageClient();
        this.executorService = Executors.newCachedThreadPool();

        // Create bidirectional streams
        PipedInputStream serverInput = new PipedInputStream();
        PipedOutputStream clientOutput = new PipedOutputStream(serverInput);
        PipedInputStream clientInput = new PipedInputStream();
        PipedOutputStream serverOutput = new PipedOutputStream(clientInput);

        // Create server launcher
        this.serverLauncher =
                LSPLauncher.createServerLauncher(
                        server, serverInput, serverOutput, executorService, builder.wrapper);

        // Create client launcher
        this.clientLauncher =
                LSPLauncher.createClientLauncher(
                        testClient, clientInput, clientOutput, executorService, builder.wrapper);

        this.client = serverLauncher.getRemoteProxy();
        if (server instanceof LanguageClientAware languageClientAware) {
            languageClientAware.connect(serverLauncher.getRemoteProxy());
        }

        // Start listening
        CompletableFuture<?> serverFuture =
                CompletableFuture.runAsync(serverLauncher::startListening, executorService);
        CompletableFuture<?> clientFuture =
                CompletableFuture.runAsync(clientLauncher::startListening, executorService);
        CompletableFuture.allOf(serverFuture, clientFuture).join();
    }

    @NonNull
    public LanguageServer getServer() {
        return server;
    }

    @NonNull
    public LanguageClient getClient() {
        return client;
    }

    @NonNull
    public TestLanguageClient getTestClient() {
        return testClient;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> request(String method, Object params) {
        return (CompletableFuture<T>) serverLauncher.getRemoteEndpoint().request(method, params);
    }

    public void notify(String method, Object params) {
        serverLauncher.getRemoteEndpoint().notify(method, params);
    }

    @Override
    public void close() throws Exception {
        server.shutdown().get(5, TimeUnit.SECONDS);
        executorService.shutdown();
        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private @Nullable LanguageServer server;
        private @Nullable TestLanguageClient client;
        private @Nullable Function<MessageConsumer, MessageConsumer> wrapper;

        public Builder server(@NonNull LanguageServer server) {
            this.server = server;
            return this;
        }

        public Builder client(@NonNull TestLanguageClient client) {
            this.client = client;
            return this;
        }

        public Builder messageWrapper(
                @Nullable Function<MessageConsumer, MessageConsumer> wrapper) {
            this.wrapper = wrapper;
            return this;
        }

        public LSPTestHarness build() throws IOException {
            if (server == null) {
                throw new IllegalStateException("Server must be provided");
            }
            return new LSPTestHarness(this);
        }
    }
}
