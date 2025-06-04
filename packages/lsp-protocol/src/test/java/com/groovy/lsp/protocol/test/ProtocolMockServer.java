package com.groovy.lsp.protocol.test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Mock server for testing LSP protocol interactions.
 * Uses WireMock to simulate LSP server responses.
 */
public class ProtocolMockServer implements AutoCloseable {
    private final WireMockServer wireMockServer;

    public ProtocolMockServer() {
        this(0); // Random port
    }

    public ProtocolMockServer(int port) {
        WireMockConfiguration config = WireMockConfiguration.wireMockConfig();
        if (port > 0) {
            config.port(port);
        } else {
            config.dynamicPort();
        }
        this.wireMockServer = new WireMockServer(config);
    }

    /**
     * Start the mock server.
     */
    public void start() {
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    /**
     * Stop the mock server.
     */
    public void stop() {
        wireMockServer.stop();
    }

    /**
     * Get the port the server is running on.
     */
    public int getPort() {
        return wireMockServer.port();
    }

    /**
     * Get the base URL of the mock server.
     */
    @NonNull
    public String getBaseUrl() {
        return "http://localhost:" + getPort();
    }

    /**
     * Reset all mappings and requests.
     */
    public void reset() {
        wireMockServer.resetAll();
    }

    /**
     * Stub a JSON-RPC request with a response.
     */
    public void stubJsonRpcRequest(
            @NonNull String method, @Nullable Object params, @NonNull Object result) {
        Map<String, Object> requestBody =
                Map.of(
                        "jsonrpc",
                        "2.0",
                        "method",
                        method,
                        "params",
                        params != null ? params : Map.of(),
                        "id",
                        1);

        Map<String, Object> responseBody = Map.of("jsonrpc", "2.0", "result", result, "id", 1);

        WireMock.stubFor(
                WireMock.post("/")
                        .withRequestBody(WireMock.equalToJson(toJson(requestBody)))
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(toJson(responseBody))));
    }

    /**
     * Stub a JSON-RPC notification (no response expected).
     */
    public void stubJsonRpcNotification(@NonNull String method, @Nullable Object params) {
        Map<String, Object> requestBody =
                Map.of(
                        "jsonrpc",
                        "2.0",
                        "method",
                        method,
                        "params",
                        params != null ? params : Map.of());

        WireMock.stubFor(
                WireMock.post("/")
                        .withRequestBody(WireMock.equalToJson(toJson(requestBody)))
                        .willReturn(WireMock.aResponse().withStatus(200)));
    }

    /**
     * Verify that a JSON-RPC request was made.
     */
    public void verifyJsonRpcRequest(@NonNull String method, int count) {
        RequestPatternBuilder pattern =
                WireMock.postRequestedFor(WireMock.urlEqualTo("/"))
                        .withRequestBody(
                                WireMock.matchingJsonPath("$.method", WireMock.equalTo(method)));

        WireMock.verify(count, pattern);
    }

    /**
     * Get all requests made to the server.
     */
    @NonNull
    public List<com.github.tomakehurst.wiremock.verification.LoggedRequest> getAllRequests() {
        return wireMockServer.findAll(WireMock.anyRequestedFor(WireMock.anyUrl()));
    }

    @Override
    public void close() {
        stop();
    }

    /**
     * Convert object to JSON string.
     * This is a simplified implementation - in production, use a proper JSON library.
     */
    private String toJson(Object obj) {
        // This would use a proper JSON serialization library in production
        if (obj instanceof Map) {
            return mapToJson((Map<?, ?>) obj);
        }
        return obj.toString();
    }

    private String mapToJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            if (entry.getValue() instanceof String) {
                sb.append("\"").append(entry.getValue()).append("\"");
            } else if (entry.getValue() instanceof Map) {
                sb.append(mapToJson((Map<?, ?>) entry.getValue()));
            } else {
                sb.append(entry.getValue());
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
