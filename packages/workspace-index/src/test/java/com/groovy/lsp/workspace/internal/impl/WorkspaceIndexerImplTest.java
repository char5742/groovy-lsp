package com.groovy.lsp.workspace.internal.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.groovy.lsp.shared.event.EventBus;
import com.groovy.lsp.shared.event.EventBusFactory;
import com.groovy.lsp.test.annotations.UnitTest;
import com.groovy.lsp.workspace.api.events.FileIndexedEvent;
import com.groovy.lsp.workspace.api.events.WorkspaceIndexedEvent;
import com.groovy.lsp.workspace.internal.index.SymbolIndex;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceIndexerImplTest {

    @TempDir @Nullable Path tempDir;
    private WorkspaceIndexerImpl indexer;
    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        indexer =
                new WorkspaceIndexerImpl(
                        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit"));
        eventBus = EventBusFactory.getInstance();
    }

    @AfterEach
    void tearDown() {
        indexer.close();
    }

    @UnitTest
    void initialize_shouldIndexGroovyFiles() throws Exception {
        // Given
        Path srcDir =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("src");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("Test.groovy"), "class Test {}");
        Files.writeString(srcDir.resolve("Example.groovy"), "class Example {}");

        CountDownLatch eventLatch = new CountDownLatch(1);
        AtomicReference<WorkspaceIndexedEvent> capturedEvent = new AtomicReference<>();
        eventBus.subscribe(
                WorkspaceIndexedEvent.class,
                event -> {
                    capturedEvent.set(event);
                    eventLatch.countDown();
                });

        // When
        CompletableFuture<Void> future = indexer.initialize();
        future.get(5, TimeUnit.SECONDS);
        eventLatch.await(1, TimeUnit.SECONDS);

        // Then
        assertThat(
                        Files.exists(
                                Objects.requireNonNull(
                                                tempDir, "tempDir should be initialized by JUnit")
                                        .resolve(".groovy-lsp/index")))
                .isTrue();
        WorkspaceIndexedEvent event = capturedEvent.get();
        assertThat(event).isNotNull();
        assertThat(Objects.requireNonNull(event).getTotalFiles()).isEqualTo(2);
        assertThat(event.getWorkspacePath()).isEqualTo(tempDir);
    }

    @UnitTest
    void initialize_shouldIndexJavaFiles() throws Exception {
        // Given
        Path srcDir =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("src");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("Test.java"), "public class Test {}");

        CountDownLatch eventLatch = new CountDownLatch(1);
        eventBus.subscribe(WorkspaceIndexedEvent.class, event -> eventLatch.countDown());

        // When
        CompletableFuture<Void> future = indexer.initialize();
        future.get(5, TimeUnit.SECONDS);
        eventLatch.await(1, TimeUnit.SECONDS);

        // Then
        assertThat(
                        Files.exists(
                                Objects.requireNonNull(
                                                tempDir, "tempDir should be initialized by JUnit")
                                        .resolve(".groovy-lsp/index")))
                .isTrue();
    }

    @UnitTest
    void initialize_shouldIndexGradleFiles() throws Exception {
        // Given
        Files.writeString(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("build.gradle"),
                "apply plugin: 'java'");
        Files.writeString(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("settings.gradle.kts"),
                "rootProject.name = 'test'");

        CountDownLatch eventLatch = new CountDownLatch(1);
        AtomicReference<WorkspaceIndexedEvent> capturedEvent = new AtomicReference<>();
        eventBus.subscribe(
                WorkspaceIndexedEvent.class,
                event -> {
                    capturedEvent.set(event);
                    eventLatch.countDown();
                });

        // When
        CompletableFuture<Void> future = indexer.initialize();
        future.get(10, TimeUnit.SECONDS);
        eventLatch.await(2, TimeUnit.SECONDS);

        // Then
        WorkspaceIndexedEvent event = capturedEvent.get();
        assertThat(event).isNotNull();
        assertThat(Objects.requireNonNull(event).getTotalFiles()).isEqualTo(2);
    }

    @UnitTest
    void initialize_shouldHandleEmptyWorkspace() throws Exception {
        // Given - empty workspace

        CountDownLatch eventLatch = new CountDownLatch(1);
        AtomicReference<WorkspaceIndexedEvent> capturedEvent = new AtomicReference<>();
        eventBus.subscribe(
                WorkspaceIndexedEvent.class,
                event -> {
                    capturedEvent.set(event);
                    eventLatch.countDown();
                });

        // When
        CompletableFuture<Void> future = indexer.initialize();
        future.get(5, TimeUnit.SECONDS);
        eventLatch.await(1, TimeUnit.SECONDS);

        // Then
        WorkspaceIndexedEvent event = capturedEvent.get();
        assertThat(event).isNotNull();
        assertThat(Objects.requireNonNull(event).getTotalFiles()).isEqualTo(0);
        assertThat(Objects.requireNonNull(event).getTotalSymbols()).isEqualTo(0);
    }

    @UnitTest
    void initialize_shouldSkipNonIndexableFiles() throws Exception {
        // Given
        Files.writeString(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("readme.txt"),
                "This is a text file");
        Files.writeString(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("image.png"),
                "fake image data");
        Files.createDirectories(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("empty-dir"));

        CountDownLatch eventLatch = new CountDownLatch(1);
        AtomicReference<WorkspaceIndexedEvent> capturedEvent = new AtomicReference<>();
        eventBus.subscribe(
                WorkspaceIndexedEvent.class,
                event -> {
                    capturedEvent.set(event);
                    eventLatch.countDown();
                });

        // When
        CompletableFuture<Void> future = indexer.initialize();
        future.get(5, TimeUnit.SECONDS);
        eventLatch.await(1, TimeUnit.SECONDS);

        // Then
        WorkspaceIndexedEvent event = capturedEvent.get();
        assertThat(event).isNotNull();
        assertThat(Objects.requireNonNull(event).getTotalFiles())
                .isEqualTo(0); // No indexable files
    }

    @UnitTest
    void updateFile_shouldIndexNewFile() throws Exception {
        // Given
        indexer.initialize().get(5, TimeUnit.SECONDS);
        Path newFile =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("New.groovy");
        Files.writeString(newFile, "class New {}");

        CountDownLatch eventLatch = new CountDownLatch(1);
        AtomicReference<FileIndexedEvent> capturedEvent = new AtomicReference<>();
        eventBus.subscribe(
                FileIndexedEvent.class,
                event -> {
                    capturedEvent.set(event);
                    eventLatch.countDown();
                });

        // When
        CompletableFuture<Void> future = indexer.updateFile(newFile);
        future.get(5, TimeUnit.SECONDS);
        eventLatch.await(1, TimeUnit.SECONDS);

        // Then
        FileIndexedEvent event = capturedEvent.get();
        assertThat(event).isNotNull();
        assertThat(Objects.requireNonNull(event).getFilePath()).isEqualTo(newFile);
        assertThat(event.isSuccess()).isTrue();
    }

    @UnitTest
    void updateFile_shouldRemoveDeletedFile() throws Exception {
        // Given
        Path file =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("ToDelete.groovy");
        Files.writeString(file, "class ToDelete {}");
        indexer.initialize().get(5, TimeUnit.SECONDS);

        // Delete the file
        Files.delete(file);

        // When
        CompletableFuture<Void> future = indexer.updateFile(file);
        future.get(5, TimeUnit.SECONDS);

        // Then
        // File should be removed from index (no event for removal)
        SymbolIndex symbolIndex = indexer.getSymbolIndex();
        assertThat(symbolIndex.getFileSymbols(file)).isEmpty();
    }

    @UnitTest
    void searchSymbols_shouldReturnEmptyStreamForEmptyIndex() throws Exception {
        // Given
        indexer.initialize().get(5, TimeUnit.SECONDS);

        // When
        var results = indexer.searchSymbols("Test").get(5, TimeUnit.SECONDS);

        // Then
        assertThat(results.count()).isEqualTo(0);
    }

    @UnitTest
    void getSymbolIndex_shouldReturnNonNullIndex() {
        // When
        SymbolIndex symbolIndex = indexer.getSymbolIndex();

        // Then
        assertThat(symbolIndex).isNotNull();
    }

    @UnitTest
    void shutdown_shouldCloseResources() throws Exception {
        // Given
        indexer.initialize().get(5, TimeUnit.SECONDS);

        // When
        indexer.shutdown();

        // Then - shutdown should be idempotent
        indexer.shutdown(); // Should not throw
    }

    @UnitTest
    void close_shouldBeIdempotent() {
        // When - close multiple times
        indexer.close();
        indexer.close();

        // Then - should not throw
    }

    @UnitTest
    void initialize_shouldHandleIOException() throws Exception {
        // Given - Create a file where the index directory should be
        Path indexPath =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve(".groovy-lsp/index");
        Files.createDirectories(indexPath.getParent());
        Files.writeString(indexPath, "This is a file, not a directory");

        // When
        CompletableFuture<Void> future = indexer.initialize();

        // Then
        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @UnitTest
    void shouldHandleNestedDirectoryStructure() throws Exception {
        // Given
        Path deepPath =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("src/main/groovy/com/example/deep");
        Files.createDirectories(deepPath);
        Files.writeString(deepPath.resolve("DeepClass.groovy"), "class DeepClass {}");
        Files.writeString(deepPath.resolve("DeepInterface.java"), "interface DeepInterface {}");

        CountDownLatch eventLatch = new CountDownLatch(1);
        AtomicReference<WorkspaceIndexedEvent> capturedEvent = new AtomicReference<>();
        eventBus.subscribe(
                WorkspaceIndexedEvent.class,
                event -> {
                    capturedEvent.set(event);
                    eventLatch.countDown();
                });

        // When
        CompletableFuture<Void> future = indexer.initialize();
        future.get(5, TimeUnit.SECONDS);
        eventLatch.await(1, TimeUnit.SECONDS);

        // Then
        WorkspaceIndexedEvent event = capturedEvent.get();
        assertThat(event).isNotNull();
        assertThat(Objects.requireNonNull(event).getTotalFiles()).isEqualTo(2);
    }

    @UnitTest
    void shouldHandleSymbolicLinks() throws Exception {
        // Given
        Path targetDir =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("target");
        Files.createDirectories(targetDir);
        Files.writeString(targetDir.resolve("Target.groovy"), "class Target {}");

        Path linkDir =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("link");
        try {
            Files.createSymbolicLink(linkDir, targetDir);
        } catch (IOException e) {
            // Skip test if symbolic links are not supported
            return;
        }

        CountDownLatch eventLatch = new CountDownLatch(1);
        eventBus.subscribe(WorkspaceIndexedEvent.class, event -> eventLatch.countDown());

        // When
        CompletableFuture<Void> future = indexer.initialize();
        future.get(5, TimeUnit.SECONDS);
        eventLatch.await(1, TimeUnit.SECONDS);

        // Then - should index files from both paths
        assertThat(
                        Files.exists(
                                Objects.requireNonNull(
                                                tempDir, "tempDir should be initialized by JUnit")
                                        .resolve(".groovy-lsp/index")))
                .isTrue();
    }
}
