package com.groovy.lsp.protocol.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.assertj.core.api.AbstractAssert;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Custom assertions for JSON validation in LSP protocol tests.
 * Provides fluent API for validating JSON responses.
 */
public class JsonAssertions extends AbstractAssert<JsonAssertions, String> {
    private final JsonElement json;

    private JsonAssertions(@NonNull String jsonString) {
        super(jsonString, JsonAssertions.class);
        this.json = JsonParser.parseString(jsonString);
    }

    /**
     * Create assertion for JSON string.
     */
    @NonNull
    public static JsonAssertions assertThatJson(@NonNull String jsonString) {
        return new JsonAssertions(jsonString);
    }

    /**
     * Assert that a JSON path exists and has the expected value.
     */
    @NonNull
    public JsonAssertions hasJsonPath(@NonNull String path, @Nullable Object expectedValue) {
        JsonElement element = getJsonElement(path);
        assertThat(element).as("JSON path '%s' should exist", path).isNotNull();

        if (expectedValue != null) {
            Object actualValue = extractValue(element);
            assertThat(actualValue)
                    .as("JSON path '%s' should have value '%s'", path, expectedValue)
                    .isEqualTo(expectedValue);
        }

        return this;
    }

    /**
     * Assert that a JSON path exists.
     */
    @NonNull
    public JsonAssertions hasJsonPath(@NonNull String path) {
        return hasJsonPath(path, null);
    }

    /**
     * Assert that a JSON path does not exist.
     */
    @NonNull
    public JsonAssertions doesNotHaveJsonPath(@NonNull String path) {
        JsonElement element = getJsonElement(path);
        assertThat(element).as("JSON path '%s' should not exist", path).isNull();
        return this;
    }

    /**
     * Assert that the JSON matches LSP JSON-RPC format.
     */
    @NonNull
    public JsonAssertions isValidJsonRpc() {
        assertThat(json.isJsonObject()).as("JSON should be an object").isTrue();

        JsonObject obj = json.getAsJsonObject();
        assertThat(obj.has("jsonrpc")).as("JSON-RPC should have 'jsonrpc' field").isTrue();
        assertThat(obj.get("jsonrpc").getAsString())
                .as("JSON-RPC version should be '2.0'")
                .isEqualTo("2.0");

        return this;
    }

    /**
     * Assert that the JSON is a valid LSP response.
     */
    @NonNull
    public JsonAssertions isValidLspResponse() {
        isValidJsonRpc();

        JsonObject obj = json.getAsJsonObject();
        assertThat(obj.has("id")).as("LSP response should have 'id' field").isTrue();
        assertThat(obj.has("result") || obj.has("error"))
                .as("LSP response should have either 'result' or 'error' field")
                .isTrue();

        return this;
    }

    /**
     * Assert that the JSON is a valid LSP error response.
     */
    @NonNull
    public JsonAssertions isLspError(int expectedCode) {
        isValidLspResponse();

        JsonObject obj = json.getAsJsonObject();
        assertThat(obj.has("error")).as("LSP error response should have 'error' field").isTrue();

        JsonObject error = obj.getAsJsonObject("error");
        assertThat(error.has("code")).as("LSP error should have 'code' field").isTrue();
        assertThat(error.get("code").getAsInt()).as("LSP error code").isEqualTo(expectedCode);

        return this;
    }

    /**
     * Get JSON element by path (simplified JSONPath).
     */
    @Nullable
    private JsonElement getJsonElement(@NonNull String path) {
        String[] parts = path.split("\\.");
        JsonElement current = json;

        for (String part : parts) {
            if (current == null || current.isJsonNull()) {
                return null;
            }

            // Remove leading '$' if present
            if (part.startsWith("$")) {
                part = part.substring(1);
                if (part.isEmpty()) {
                    continue;
                }
            }

            // Handle array index
            if (part.contains("[") && part.endsWith("]")) {
                int bracketIndex = part.indexOf('[');
                String fieldName = part.substring(0, bracketIndex);
                String indexStr = part.substring(bracketIndex + 1, part.length() - 1);
                int index;
                try {
                    index = Integer.parseInt(indexStr);
                } catch (NumberFormatException e) {
                    // Invalid array index format
                    return null;
                }

                if (!fieldName.isEmpty() && current.isJsonObject()) {
                    current = current.getAsJsonObject().get(fieldName);
                }

                if (current != null && current.isJsonArray()) {
                    JsonArray array = current.getAsJsonArray();
                    if (index < array.size()) {
                        current = array.get(index);
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                // Regular field access
                if (current.isJsonObject()) {
                    current = current.getAsJsonObject().get(part);
                } else {
                    return null;
                }
            }
        }

        return current;
    }

    /**
     * Extract value from JSON element.
     * @param element The JSON element to extract value from (can contain JsonNull)
     * @return The extracted value, or null for JsonNull elements
     */
    @Nullable
    private Object extractValue(@Nullable JsonElement element) {
        if (element == null) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            } else if (primitive.isNumber()) {
                // Try to return the most appropriate number type
                Number num = primitive.getAsNumber();
                if (num.doubleValue() == num.intValue()) {
                    return num.intValue();
                }
                return num.doubleValue();
            } else {
                return primitive.getAsString();
            }
        } else if (element.isJsonNull()) {
            return null;
        } else if (element.isJsonObject()) {
            return element.getAsJsonObject().toString();
        } else if (element.isJsonArray()) {
            return element.getAsJsonArray().toString();
        }
        return element.toString();
    }
}
