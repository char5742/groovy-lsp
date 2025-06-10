package com.groovy.lsp.workspace.internal.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.groovy.lsp.shared.event.EventBus;
import com.groovy.lsp.shared.event.EventBusFactory;
import com.groovy.lsp.shared.workspace.api.dto.SymbolInfo;
import com.groovy.lsp.shared.workspace.api.events.FileIndexedEvent;
import com.groovy.lsp.shared.workspace.api.events.WorkspaceIndexedEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CachedWorkspaceIndexerImplTest {

    @TempDir Path tempDir;
    private CachedWorkspaceIndexerImpl indexer;
    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        indexer = new CachedWorkspaceIndexerImpl(tempDir);
        eventBus = EventBusFactory.getInstance();
    }

    @AfterEach
    void tearDown() throws Exception {
        indexer.close();
    }

    @Test
    void initialize_shouldIndexGroovyFilesWithCaching() throws Exception {
        // Given
        Path srcDir = tempDir.resolve("src");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("Test.groovy"), "class Test { def method() {} }");
        Files.writeString(srcDir.resolve("Example.groovy"), "class Example { String field }");

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
        assertThat(Files.exists(tempDir.resolve(".groovy-lsp/index"))).isTrue();
        WorkspaceIndexedEvent event = capturedEvent.get();
        assertThat(event).isNotNull();
        assertThat(event.getTotalFiles()).isGreaterThanOrEqualTo(2);
        assertThat(event.getWorkspacePath()).isEqualTo(tempDir);
    }

    @Test
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
        future.get(10, TimeUnit.SECONDS);
        eventLatch.await(2, TimeUnit.SECONDS);

        // Then
        WorkspaceIndexedEvent event = capturedEvent.get();
        assertThat(event).isNotNull();
        assertThat(event.getTotalFiles()).isEqualTo(0);
        assertThat(event.getTotalSymbols()).isEqualTo(0);
    }

    @Test
    void updateFile_shouldReinitializeOnBuildFileChange() throws Exception {
        // Given
        Path buildFile = tempDir.resolve("build.gradle");
        Files.writeString(buildFile, "apply plugin: 'java'");
        indexer.initialize().get(10, TimeUnit.SECONDS);

        CountDownLatch eventLatch = new CountDownLatch(1);
        eventBus.subscribe(WorkspaceIndexedEvent.class, event -> eventLatch.countDown());

        // When - Update build file
        Files.writeString(buildFile, "apply plugin: 'groovy'");
        CompletableFuture<Void> future = indexer.updateFile(buildFile);
        future.get(10, TimeUnit.SECONDS);
        eventLatch.await(2, TimeUnit.SECONDS);

        // Then - Should trigger re-initialization
        assertThat(eventLatch.getCount()).isEqualTo(0);
    }

    @Test
    void updateFile_shouldIndexNewGroovyFile() throws Exception {
        // Given
        indexer.initialize().get(10, TimeUnit.SECONDS);
        Path newFile = tempDir.resolve("New.groovy");
        Files.writeString(newFile, "class New { void method() {} }");

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
        future.get(10, TimeUnit.SECONDS);
        eventLatch.await(2, TimeUnit.SECONDS);

        // Then
        FileIndexedEvent event = capturedEvent.get();
        assertThat(event).isNotNull();
        assertThat(event.getFilePath()).isEqualTo(newFile);
        assertThat(event.isSuccess()).isTrue();
        assertThat(event.getSymbols()).isNotEmpty();
    }

    @Test
    void updateFile_shouldHandleDeletedGroovyFile() throws Exception {
        // Given
        Path file = tempDir.resolve("ToDelete.groovy");
        Files.writeString(file, "class ToDelete {}");
        indexer.initialize().get(10, TimeUnit.SECONDS);

        // Delete the file
        Files.delete(file);

        // When
        CompletableFuture<Void> future = indexer.updateFile(file);
        future.get(10, TimeUnit.SECONDS);

        // Then - Should not throw exception
        assertThat(future.isDone()).isTrue();
    }

    @Test
    void searchSymbols_shouldReturnMatchingSymbols() throws Exception {
        // Given
        Path srcDir = tempDir.resolve("src");
        Files.createDirectories(srcDir);
        Files.writeString(
                srcDir.resolve("TestClass.groovy"), "class TestClass { void testMethod() {} }");
        indexer.initialize().get(10, TimeUnit.SECONDS);

        // When
        Stream<SymbolInfo> results = indexer.searchSymbols("Test").get(10, TimeUnit.SECONDS);

        // Then
        List<SymbolInfo> symbolList = results.toList();
        assertThat(symbolList).isNotEmpty();
        assertThat(symbolList).anyMatch(s -> s.name().contains("Test"));
    }

    @Test
    void searchSymbols_shouldReturnEmptyStreamForNoMatches() throws Exception {
        // Given
        indexer.initialize().get(10, TimeUnit.SECONDS);

        // When
        Stream<SymbolInfo> results = indexer.searchSymbols("NonExistent").get(10, TimeUnit.SECONDS);

        // Then
        assertThat(results.count()).isEqualTo(0);
    }

    @Test
    void shutdown_shouldLogCacheStatistics() {
        // Given
        indexer.initialize().join();

        // When
        indexer.shutdown();

        // Then - Should not throw exception
        // Cache statistics should be logged (check logs if needed)
    }

    @Test
    void close_shouldCallShutdown() throws Exception {
        // Given
        indexer.initialize().get(10, TimeUnit.SECONDS);

        // When
        indexer.close();

        // Then - Should not throw exception
        // Resources should be cleaned up
    }

    @Test
    void updateFile_shouldHandleParsingError() throws Exception {
        // Given
        indexer.initialize().get(10, TimeUnit.SECONDS);
        Path invalidFile = tempDir.resolve("Invalid.groovy");
        Files.writeString(invalidFile, "class { // Invalid syntax");

        // When
        CompletableFuture<Void> future = indexer.updateFile(invalidFile);

        // Then - Should complete without throwing (error is logged but not propagated)
        assertThat(future.get(10, TimeUnit.SECONDS)).isNull();
    }

    @Test
    void initialize_shouldHandleMixedFileTypes() throws Exception {
        // Given
        Files.writeString(tempDir.resolve("build.gradle"), "apply plugin: 'java'");
        Files.writeString(tempDir.resolve("Test.groovy"), "class Test {}");
        Files.writeString(tempDir.resolve("Example.java"), "public class Example {}");
        Files.writeString(tempDir.resolve("readme.txt"), "This is a text file");

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
        assertThat(event.getTotalFiles())
                .isGreaterThanOrEqualTo(2); // At least groovy and gradle files
    }

    @Test
    void updateFile_shouldHandlePomXmlChange() throws Exception {
        // Given
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, "<project><modelVersion>4.0.0</modelVersion></project>");
        indexer.initialize().get(10, TimeUnit.SECONDS);

        CountDownLatch eventLatch = new CountDownLatch(1);
        eventBus.subscribe(WorkspaceIndexedEvent.class, event -> eventLatch.countDown());

        // When - Update pom.xml
        Files.writeString(
                pomFile,
                "<project><modelVersion>4.0.0</modelVersion><dependencies></dependencies></project>");
        CompletableFuture<Void> future = indexer.updateFile(pomFile);
        future.get(10, TimeUnit.SECONDS);
        eventLatch.await(2, TimeUnit.SECONDS);

        // Then - Should trigger re-initialization
        assertThat(eventLatch.getCount()).isEqualTo(0);
    }

    @Test
    void updateFile_shouldHandleSettingsGradleChange() throws Exception {
        // Given
        Path settingsFile = tempDir.resolve("settings.gradle");
        Files.writeString(settingsFile, "rootProject.name = 'test'");
        indexer.initialize().get(10, TimeUnit.SECONDS);

        CountDownLatch eventLatch = new CountDownLatch(1);
        eventBus.subscribe(WorkspaceIndexedEvent.class, event -> eventLatch.countDown());

        // When - Update settings.gradle
        Files.writeString(settingsFile, "rootProject.name = 'updated-test'");
        CompletableFuture<Void> future = indexer.updateFile(settingsFile);
        future.get(10, TimeUnit.SECONDS);
        eventLatch.await(2, TimeUnit.SECONDS);

        // Then - Should trigger re-initialization
        assertThat(eventLatch.getCount()).isEqualTo(0);
    }

    @Test
    void initialize_shouldHandleNestedDirectories() throws Exception {
        // Given
        Path deepPath = tempDir.resolve("src/main/groovy/com/example");
        Files.createDirectories(deepPath);
        Files.writeString(deepPath.resolve("Deep.groovy"), "package com.example\nclass Deep {}");
        Files.writeString(
                deepPath.resolve("Nested.groovy"), "package com.example\nclass Nested {}");

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
        assertThat(event.getTotalFiles()).isEqualTo(2);
    }

    @Test
    void updateFile_shouldHandleNonGroovyNonBuildFile() throws Exception {
        // Given
        indexer.initialize().get(10, TimeUnit.SECONDS);
        Path textFile = tempDir.resolve("readme.txt");
        Files.writeString(textFile, "This is a readme file");

        // When
        CompletableFuture<Void> future = indexer.updateFile(textFile);
        future.get(10, TimeUnit.SECONDS);

        // Then - Should complete without error
        assertThat(future.isDone()).isTrue();
    }

    @Test
    void initialize_shouldHandleJarDependencies() throws Exception {
        // Given - Create a minimal JAR file with valid structure
        Path libDir = tempDir.resolve("lib");
        Files.createDirectories(libDir);
        Path jarFile = libDir.resolve("test.jar");

        // Create a minimal valid JAR file
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarFile))) {
            // Add a dummy class entry
            JarEntry entry = new JarEntry("com/example/Test.class");
            jos.putNextEntry(entry);
            jos.write(
                    new byte[] {
                        (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE
                    }); // Minimal class file header
            jos.closeEntry();
        }

        // Create a Maven project structure to trigger dependency resolution
        Files.writeString(
                tempDir.resolve("pom.xml"),
                "<project><modelVersion>4.0.0</modelVersion>"
                    + "<groupId>test</groupId><artifactId>test</artifactId><version>1.0</version>"
                    + "</project>");

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
        // Should complete without error, even if no dependencies are resolved
    }

    @Test
    void updateFile_shouldHandleBuildGradleKtsChange() throws Exception {
        // Given
        Path buildFile = tempDir.resolve("build.gradle.kts");
        Files.writeString(buildFile, "plugins { java }");
        indexer.initialize().get(10, TimeUnit.SECONDS);

        CountDownLatch eventLatch = new CountDownLatch(1);
        eventBus.subscribe(WorkspaceIndexedEvent.class, event -> eventLatch.countDown());

        // When - Update build.gradle.kts
        Files.writeString(buildFile, "plugins { java\ngroovy }");
        CompletableFuture<Void> future = indexer.updateFile(buildFile);
        future.get(10, TimeUnit.SECONDS);
        eventLatch.await(2, TimeUnit.SECONDS);

        // Then - Should trigger re-initialization
        assertThat(eventLatch.getCount()).isEqualTo(0);
    }

    @Test
    void updateFile_shouldHandleSettingsGradleKtsChange() throws Exception {
        // Given
        Path settingsFile = tempDir.resolve("settings.gradle.kts");
        Files.writeString(settingsFile, "rootProject.name = \"test\"");
        indexer.initialize().get(10, TimeUnit.SECONDS);

        CountDownLatch eventLatch = new CountDownLatch(1);
        eventBus.subscribe(WorkspaceIndexedEvent.class, event -> eventLatch.countDown());

        // When - Update settings.gradle.kts
        Files.writeString(settingsFile, "rootProject.name = \"updated-test\"");
        CompletableFuture<Void> future = indexer.updateFile(settingsFile);
        future.get(10, TimeUnit.SECONDS);
        eventLatch.await(2, TimeUnit.SECONDS);

        // Then - Should trigger re-initialization
        assertThat(eventLatch.getCount()).isEqualTo(0);
    }
}
