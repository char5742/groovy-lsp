package com.groovy.lsp.workspace.api.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class WorkspaceIndexedEventTest {

    @Test
    void constructor_shouldInitializeAllFields() {
        // Given
        Path workspacePath = Paths.get("/workspace");
        int totalFiles = 100;
        int totalSymbols = 500;
        long indexingDurationMs = 1500L;

        // When
        WorkspaceIndexedEvent event =
                new WorkspaceIndexedEvent(
                        workspacePath, totalFiles, totalSymbols, indexingDurationMs);

        // Then
        assertThat(event.getWorkspacePath()).isEqualTo(workspacePath);
        assertThat(event.getTotalFiles()).isEqualTo(totalFiles);
        assertThat(event.getTotalSymbols()).isEqualTo(totalSymbols);
        assertThat(event.getIndexingDurationMs()).isEqualTo(indexingDurationMs);
        assertThat(event.getAggregateId()).isEqualTo(workspacePath.toString());
    }

    @Test
    void toString_shouldReturnFormattedString() {
        // Given
        Path workspacePath = Paths.get("/workspace");
        WorkspaceIndexedEvent event = new WorkspaceIndexedEvent(workspacePath, 50, 250, 800L);

        // When
        String result = event.toString();

        // Then
        assertThat(result)
                .isEqualTo(
                        "WorkspaceIndexedEvent{workspacePath=/workspace, files=50, symbols=250,"
                                + " duration=800ms}");
    }

    @Test
    void toString_shouldHandleZeroValues() {
        // Given
        Path workspacePath = Paths.get("/empty");
        WorkspaceIndexedEvent event = new WorkspaceIndexedEvent(workspacePath, 0, 0, 0L);

        // When
        String result = event.toString();

        // Then
        assertThat(result)
                .isEqualTo(
                        "WorkspaceIndexedEvent{workspacePath=/empty, files=0, symbols=0,"
                                + " duration=0ms}");
    }

    @Test
    void shouldHandleWindowsPaths() {
        // Given
        Path windowsPath = Paths.get("C:\\Users\\workspace");
        WorkspaceIndexedEvent event = new WorkspaceIndexedEvent(windowsPath, 10, 20, 100L);

        // When & Then
        assertThat(event.getWorkspacePath()).isEqualTo(windowsPath);
        assertThat(event.getAggregateId()).isEqualTo(windowsPath.toString());
    }

    @Test
    void shouldHandleLargeNumbers() {
        // Given
        Path workspacePath = Paths.get("/large-project");
        int largeFileCount = Integer.MAX_VALUE / 2;
        int largeSymbolCount = Integer.MAX_VALUE / 2;
        long longDuration = Long.MAX_VALUE / 2;

        // When
        WorkspaceIndexedEvent event =
                new WorkspaceIndexedEvent(
                        workspacePath, largeFileCount, largeSymbolCount, longDuration);

        // Then
        assertThat(event.getTotalFiles()).isEqualTo(largeFileCount);
        assertThat(event.getTotalSymbols()).isEqualTo(largeSymbolCount);
        assertThat(event.getIndexingDurationMs()).isEqualTo(longDuration);
    }

    @Test
    void shouldHandleRelativePaths() {
        // Given
        Path relativePath = Paths.get("../relative/path");
        WorkspaceIndexedEvent event = new WorkspaceIndexedEvent(relativePath, 5, 10, 50L);

        // When & Then
        assertThat(event.getWorkspacePath()).isEqualTo(relativePath);
        assertThat(event.getAggregateId()).isEqualTo(relativePath.toString());
    }
}
