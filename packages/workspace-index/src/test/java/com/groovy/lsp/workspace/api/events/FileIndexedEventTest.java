package com.groovy.lsp.workspace.api.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.groovy.lsp.shared.workspace.api.dto.SymbolInfo;
import com.groovy.lsp.shared.workspace.api.dto.SymbolKind;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class FileIndexedEventTest {

    @Test
    void successConstructor_shouldInitializeSuccessfully() {
        // Given
        Path filePath = Paths.get("/workspace/Test.groovy");
        List<SymbolInfo> symbols =
                Arrays.asList(
                        new SymbolInfo("TestClass", SymbolKind.CLASS, filePath, 10, 20),
                        new SymbolInfo("testMethod", SymbolKind.METHOD, filePath, 30, 40));

        // When
        FileIndexedEvent event = new FileIndexedEvent(filePath, symbols);

        // Then
        assertThat(event.getFilePath()).isEqualTo(filePath);
        assertThat(event.getSymbols()).hasSize(2);
        assertThat(event.isSuccess()).isTrue();
        assertThat(event.getErrorMessage()).isEmpty();
        assertThat(event.getAggregateId()).isEqualTo(filePath.toString());
    }

    @Test
    void failureConstructor_shouldInitializeWithError() {
        // Given
        Path filePath = Paths.get("/workspace/Error.groovy");
        String errorMessage = "Failed to parse file: syntax error";

        // When
        FileIndexedEvent event = new FileIndexedEvent(filePath, errorMessage);

        // Then
        assertThat(event.getFilePath()).isEqualTo(filePath);
        assertThat(event.getSymbols()).isEmpty();
        assertThat(event.isSuccess()).isFalse();
        assertThat(event.getErrorMessage()).isEqualTo(errorMessage);
        assertThat(event.getAggregateId()).isEqualTo(filePath.toString());
    }

    @Test
    void successConstructor_shouldMakeDefensiveCopyOfSymbols() {
        // Given
        Path filePath = Paths.get("/workspace/Test.groovy");
        List<SymbolInfo> originalSymbols =
                Arrays.asList(new SymbolInfo("TestClass", SymbolKind.CLASS, filePath, 10, 20));
        List<SymbolInfo> mutableList = new java.util.ArrayList<>(originalSymbols);

        // When
        FileIndexedEvent event = new FileIndexedEvent(filePath, mutableList);
        mutableList.clear(); // Modify original list

        // Then
        assertThat(event.getSymbols()).hasSize(1); // Event should still have the symbol
    }

    @Test
    void toString_shouldReturnFormattedStringForSuccess() {
        // Given
        Path filePath = Paths.get("/workspace/Success.groovy");
        List<SymbolInfo> symbols =
                Arrays.asList(
                        new SymbolInfo("Class1", SymbolKind.CLASS, filePath, 1, 10),
                        new SymbolInfo("method1", SymbolKind.METHOD, filePath, 20, 30),
                        new SymbolInfo("var1", SymbolKind.FIELD, filePath, 40, 50));
        FileIndexedEvent event = new FileIndexedEvent(filePath, symbols);

        // When
        String result = event.toString();

        // Then
        assertThat(result).isEqualTo("FileIndexedEvent{file=/workspace/Success.groovy, symbols=3}");
    }

    @Test
    void toString_shouldReturnFormattedStringForFailure() {
        // Given
        Path filePath = Paths.get("/workspace/Failed.groovy");
        FileIndexedEvent event = new FileIndexedEvent(filePath, "Parse error at line 10");

        // When
        String result = event.toString();

        // Then
        assertThat(result)
                .isEqualTo(
                        "FileIndexedEvent{file=/workspace/Failed.groovy, error=Parse error at line"
                                + " 10}");
    }

    @Test
    void toString_shouldHandleEmptyErrorMessage() {
        // Given
        Path filePath = Paths.get("/workspace/Failed.groovy");
        FileIndexedEvent event = new FileIndexedEvent(filePath, "");

        // When
        String result = event.toString();

        // Then
        assertThat(result)
                .isEqualTo("FileIndexedEvent{file=/workspace/Failed.groovy, error=Unknown error}");
    }

    @Test
    void successConstructor_shouldHandleEmptySymbolList() {
        // Given
        Path filePath = Paths.get("/workspace/Empty.groovy");
        List<SymbolInfo> emptySymbols = Collections.emptyList();

        // When
        FileIndexedEvent event = new FileIndexedEvent(filePath, emptySymbols);

        // Then
        assertThat(event.getSymbols()).isEmpty();
        assertThat(event.isSuccess()).isTrue();
        assertThat(event.toString())
                .isEqualTo("FileIndexedEvent{file=/workspace/Empty.groovy, symbols=0}");
    }

    @Test
    void shouldHandleWindowsPaths() {
        // Given
        Path windowsPath = Paths.get("C:\\Users\\workspace\\Test.groovy");
        FileIndexedEvent event = new FileIndexedEvent(windowsPath, Collections.emptyList());

        // When & Then
        assertThat(event.getFilePath()).isEqualTo(windowsPath);
        assertThat(event.getAggregateId()).isEqualTo(windowsPath.toString());
    }

    @Test
    void shouldHandleRelativePaths() {
        // Given
        Path relativePath = Paths.get("../src/Test.groovy");
        FileIndexedEvent event = new FileIndexedEvent(relativePath, "Error");

        // When & Then
        assertThat(event.getFilePath()).isEqualTo(relativePath);
        assertThat(event.getAggregateId()).isEqualTo(relativePath.toString());
    }

    @Test
    void failureConstructor_shouldHandleNullErrorMessage() {
        // Given
        Path filePath = Paths.get("/workspace/Null.groovy");
        String nullError = null;

        // When
        FileIndexedEvent event = new FileIndexedEvent(filePath, nullError);

        // Then
        assertThat(event.getErrorMessage()).isNull();
        assertThat(event.isSuccess()).isFalse();
    }
}
