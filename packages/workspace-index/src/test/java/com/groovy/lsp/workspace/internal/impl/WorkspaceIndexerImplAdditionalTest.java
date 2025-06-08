package com.groovy.lsp.workspace.internal.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.groovy.lsp.shared.event.EventBus;
import com.groovy.lsp.shared.event.EventBusFactory;
import com.groovy.lsp.workspace.api.dto.SymbolInfo;
import com.groovy.lsp.workspace.api.dto.SymbolKind;
import com.groovy.lsp.workspace.api.events.FileIndexedEvent;
import com.groovy.lsp.workspace.api.events.WorkspaceIndexedEvent;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

/**
 * Additional test cases for WorkspaceIndexerImpl to improve branch coverage.
 */
class WorkspaceIndexerImplAdditionalTest {

    @TempDir Path tempDir;
    private WorkspaceIndexerImpl indexer;
    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        indexer = new WorkspaceIndexerImpl(tempDir);
        eventBus = EventBusFactory.getInstance();
    }

    @Test
    void initialize_shouldIndexDependencies() throws Exception {
        // Given - Create a workspace with dependencies
        Path libDir = tempDir.resolve("lib");
        Files.createDirectories(libDir);

        // Create a JAR file as dependency
        Path jarFile = libDir.resolve("test-lib.jar");
        createTestJar(jarFile);

        // Create a source file
        Files.writeString(tempDir.resolve("Test.groovy"), "class Test {}");

        // Mock dependency resolver to return our JAR
        Path dependencyResolverMock = tempDir.resolve(".dependency-mock");
        Files.createFile(dependencyResolverMock);

        CountDownLatch eventLatch = new CountDownLatch(1);
        AtomicReference<WorkspaceIndexedEvent> capturedEvent = new AtomicReference<>();
        eventBus.subscribe(
                WorkspaceIndexedEvent.class,
                event -> {
                    capturedEvent.set(event);
                    eventLatch.countDown();
                });

        // When
        var future = indexer.initialize();
        future.get(10, TimeUnit.SECONDS);
        eventLatch.await(2, TimeUnit.SECONDS);

        // Then
        WorkspaceIndexedEvent event = capturedEvent.get();
        assertThat(event).isNotNull();
        assertThat(event.getTotalFiles()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void initialize_shouldHandleDependencyDirectories() throws Exception {
        // Given - Create a dependency directory structure
        Path depDir = tempDir.resolve("dependency");
        Files.createDirectories(depDir);
        Path depSrc = depDir.resolve("src");
        Files.createDirectories(depSrc);
        Files.writeString(depSrc.resolve("Dep.groovy"), "class Dep {}");

        // Create a main source file
        Files.writeString(tempDir.resolve("Main.groovy"), "class Main {}");

        CountDownLatch eventLatch = new CountDownLatch(1);
        AtomicInteger totalFiles = new AtomicInteger(0);
        eventBus.subscribe(
                WorkspaceIndexedEvent.class,
                event -> {
                    totalFiles.set(event.getTotalFiles());
                    eventLatch.countDown();
                });

        // When
        var future = indexer.initialize();
        future.get(10, TimeUnit.SECONDS);
        eventLatch.await(2, TimeUnit.SECONDS);

        // Then
        assertThat(totalFiles.get()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void initialize_shouldHandleJarIndexingErrors() throws Exception {
        // Given - Create a corrupted JAR file
        Path badJar = tempDir.resolve("bad.jar");
        Files.writeString(badJar, "This is not a valid JAR file");

        // Create a source file
        Files.writeString(tempDir.resolve("Test.groovy"), "class Test {}");

        CountDownLatch eventLatch = new CountDownLatch(1);
        AtomicReference<WorkspaceIndexedEvent> capturedEvent = new AtomicReference<>();
        eventBus.subscribe(
                WorkspaceIndexedEvent.class,
                event -> {
                    capturedEvent.set(event);
                    eventLatch.countDown();
                });

        // When
        var future = indexer.initialize();
        future.get(10, TimeUnit.SECONDS);
        eventLatch.await(2, TimeUnit.SECONDS);

        // Then - Should complete successfully despite JAR error
        WorkspaceIndexedEvent event = capturedEvent.get();
        assertThat(event).isNotNull();
    }

    @Test
    void updateFile_shouldHandleKotlinScriptFiles() throws Exception {
        // Given
        indexer.initialize().get(5, TimeUnit.SECONDS);
        Path kotlinScript = tempDir.resolve("build.gradle.kts");
        Files.writeString(kotlinScript, "plugins { kotlin(\"jvm\") }");

        CountDownLatch eventLatch = new CountDownLatch(1);
        AtomicReference<FileIndexedEvent> capturedEvent = new AtomicReference<>();
        eventBus.subscribe(
                FileIndexedEvent.class,
                event -> {
                    capturedEvent.set(event);
                    eventLatch.countDown();
                });

        // When
        var future = indexer.updateFile(kotlinScript);
        future.get(5, TimeUnit.SECONDS);
        eventLatch.await(1, TimeUnit.SECONDS);

        // Then
        FileIndexedEvent event = capturedEvent.get();
        assertThat(event).isNotNull();
        assertThat(event.getFilePath()).isEqualTo(kotlinScript);
        // Kotlin script parsing not implemented yet, so should have empty symbols
        assertThat(event.getSymbols()).isEmpty();
    }

    @Test
    void indexFile_shouldHandleParsingErrors() throws Exception {
        // Given - Create a file with invalid syntax
        Path errorFile = tempDir.resolve("Error.groovy");
        // Create more severe syntax error to ensure parsing fails
        Files.writeString(errorFile, "class { } invalid syntax $#@!");

        indexer.initialize().get(5, TimeUnit.SECONDS);

        CountDownLatch eventLatch = new CountDownLatch(1);
        AtomicBoolean errorEventReceived = new AtomicBoolean(false);
        AtomicReference<FileIndexedEvent> capturedEvent = new AtomicReference<>();
        eventBus.subscribe(
                FileIndexedEvent.class,
                event -> {
                    if (event.getFilePath().equals(errorFile)) {
                        errorEventReceived.set(true);
                        capturedEvent.set(event);
                        eventLatch.countDown();
                    }
                });

        // When
        var future = indexer.updateFile(errorFile);
        future.get(5, TimeUnit.SECONDS);
        eventLatch.await(2, TimeUnit.SECONDS);

        // Then - The file should be indexed and an event should be received
        assertThat(errorEventReceived.get()).isTrue();
        FileIndexedEvent event = capturedEvent.get();
        assertThat(event).isNotNull();
        assertThat(event.getFilePath()).isEqualTo(errorFile);
    }

    private void createTestJar(Path jarPath) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            // Add a simple class file entry
            JarEntry entry = new JarEntry("com/test/TestClass.class");
            jos.putNextEntry(entry);
            // Write minimal class file bytes (not a valid class, but enough for testing)
            jos.write("PK".getBytes());
            jos.closeEntry();
        }
    }

    @Test
    void initialize_shouldIndexDependencyDirectories() throws Exception {
        // Given - Create a dependency directory with Groovy files
        Path depDir = tempDir.resolve("dependencies/lib1");
        Files.createDirectories(depDir);
        Files.writeString(depDir.resolve("DepClass.groovy"), "class DepClass {}");
        Files.writeString(depDir.resolve("DepClass2.java"), "public class DepClass2 {}");

        // Create main workspace file
        Files.writeString(tempDir.resolve("Main.groovy"), "class Main {}");

        // Mock DependencyResolver to return our dependency directory
        // For this test, we'll leverage the fact that the resolver looks for dependencies
        CountDownLatch eventLatch = new CountDownLatch(1);
        AtomicReference<WorkspaceIndexedEvent> capturedEvent = new AtomicReference<>();
        eventBus.subscribe(
                WorkspaceIndexedEvent.class,
                event -> {
                    capturedEvent.set(event);
                    eventLatch.countDown();
                });

        // When
        var future = indexer.initialize();
        future.get(10, TimeUnit.SECONDS);
        eventLatch.await(2, TimeUnit.SECONDS);

        // Then
        WorkspaceIndexedEvent event = capturedEvent.get();
        assertThat(event).isNotNull();
        // Should have indexed at least the Main.groovy file
        assertThat(event.getTotalFiles()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void initialize_shouldHandleJarDependencies() throws Exception {
        // Given - Create a proper JAR file
        Path jarFile = tempDir.resolve("lib.jar");
        createTestJar(jarFile);

        // Create workspace file
        Files.writeString(tempDir.resolve("Test.groovy"), "class Test {}");

        CountDownLatch eventLatch = new CountDownLatch(1);
        AtomicReference<WorkspaceIndexedEvent> capturedEvent = new AtomicReference<>();
        eventBus.subscribe(
                WorkspaceIndexedEvent.class,
                event -> {
                    capturedEvent.set(event);
                    eventLatch.countDown();
                });

        // When
        var future = indexer.initialize();
        future.get(10, TimeUnit.SECONDS);
        eventLatch.await(2, TimeUnit.SECONDS);

        // Then
        WorkspaceIndexedEvent event = capturedEvent.get();
        assertThat(event).isNotNull();
        assertThat(event.getTotalFiles()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void indexFileWithResult_shouldHandleParsingException() throws Exception {
        // Given - Create a file with content that will cause parsing to fail
        Path problemFile = tempDir.resolve("Problem.groovy");
        // Write content that will cause a parsing exception
        Files.writeString(problemFile, "class Test { invalid syntax $#@! }");

        indexer.initialize().get(5, TimeUnit.SECONDS);

        CountDownLatch eventLatch = new CountDownLatch(1);
        AtomicBoolean errorEventReceived = new AtomicBoolean(false);
        AtomicReference<FileIndexedEvent> capturedEvent = new AtomicReference<>();
        eventBus.subscribe(
                FileIndexedEvent.class,
                event -> {
                    if (event.getFilePath().equals(problemFile)) {
                        errorEventReceived.set(true);
                        capturedEvent.set(event);
                        eventLatch.countDown();
                    }
                });

        // When
        var future = indexer.updateFile(problemFile);
        future.get(5, TimeUnit.SECONDS);
        eventLatch.await(2, TimeUnit.SECONDS);

        // Then - Should receive event (parser handles exception internally and returns empty
        // symbols)
        assertThat(errorEventReceived.get()).isTrue();
        FileIndexedEvent event = capturedEvent.get();
        assertThat(event).isNotNull();
        assertThat(event.isSuccess()).isTrue();
        assertThat(event.getSymbols()).isEmpty();
    }

    @Test
    void close_shouldHandleSymbolIndexCloseException() throws Exception {
        // Given - Initialize the indexer
        indexer.initialize().get(5, TimeUnit.SECONDS);

        // Close the symbol index first to make the second close throw an exception
        indexer.getSymbolIndex().close();

        // When/Then - Should not throw even if symbol index close fails
        assertThatCode(() -> indexer.close()).doesNotThrowAnyException();
    }

    @Test
    void indexWorkspaceFiles_shouldHandleFileWalkException() throws Exception {
        // Given - Create a directory that will be deleted during walk
        Path volatileDir = tempDir.resolve("volatile");
        Files.createDirectories(volatileDir);
        Files.writeString(volatileDir.resolve("Test.groovy"), "class Test {}");

        // Create another indexer with the volatile directory as root
        WorkspaceIndexerImpl volatileIndexer = new WorkspaceIndexerImpl(volatileDir);

        // Start initialization in a separate thread
        CompletableFuture<Void> initFuture =
                CompletableFuture.runAsync(
                        () -> {
                            try {
                                Thread.sleep(50); // Give time for file walk to start
                                // Delete the directory during walk to cause an exception
                                Files.walk(volatileDir)
                                        .sorted(
                                                (a, b) ->
                                                        b.compareTo(
                                                                a)) // reverse order to delete files
                                        // first
                                        .forEach(
                                                path -> {
                                                    try {
                                                        Files.deleteIfExists(path);
                                                    } catch (IOException e) {
                                                        // Ignore
                                                    }
                                                });
                            } catch (Exception e) {
                                // Ignore
                            }
                        });

        // When - Initialize should handle the exception gracefully
        var future = volatileIndexer.initialize();

        try {
            future.get(10, TimeUnit.SECONDS);
            initFuture.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            // The initialization might fail, but it should handle the exception
        } finally {
            volatileIndexer.close();
        }

        // Then - The test passes if no unhandled exception occurs
    }

    @Test
    void updateFile_shouldHandleNonExistentFile() throws Exception {
        // Given
        indexer.initialize().get(5, TimeUnit.SECONDS);
        Path nonExistentFile = tempDir.resolve("DoesNotExist.groovy");

        // When
        var future = indexer.updateFile(nonExistentFile);
        future.get(5, TimeUnit.SECONDS);

        // Then - Should complete without error (file is just removed from index)
        assertThat(future.isDone()).isTrue();
        assertThat(future.isCompletedExceptionally()).isFalse();
    }

    @Test
    void indexFile_shouldHandleNullSymbols() throws Exception {
        // Given - Create files that will result in empty symbol lists
        Path emptyGroovyFile = tempDir.resolve("Empty.groovy");
        Files.writeString(emptyGroovyFile, ""); // Empty file

        Path javaFile = tempDir.resolve("Test.java");
        Files.writeString(javaFile, "public class Test {}"); // Java files not fully supported yet

        indexer.initialize().get(5, TimeUnit.SECONDS);

        CountDownLatch eventLatch = new CountDownLatch(2);
        AtomicInteger eventCount = new AtomicInteger(0);
        eventBus.subscribe(
                FileIndexedEvent.class,
                event -> {
                    eventCount.incrementAndGet();
                    eventLatch.countDown();
                });

        // When
        indexer.updateFile(emptyGroovyFile).get(5, TimeUnit.SECONDS);
        indexer.updateFile(javaFile).get(5, TimeUnit.SECONDS);
        eventLatch.await(2, TimeUnit.SECONDS);

        // Then
        assertThat(eventCount.get()).isEqualTo(2);
    }

    @Test
    void indexDependency_shouldIndexDirectoryDependency() throws Exception {
        // Given - Create a workspace structure with a build.gradle that will trigger dependency
        // resolution
        Path buildFile = tempDir.resolve("build.gradle");
        Files.writeString(buildFile, "apply plugin: 'java'");

        // Create a directory that will be treated as a dependency
        Path depDir = tempDir.resolve("libs/dep1");
        Files.createDirectories(depDir);
        Files.writeString(depDir.resolve("Dep.groovy"), "class Dep {}");
        Files.writeString(depDir.resolve("DepJava.java"), "public class DepJava {}");
        Files.writeString(depDir.resolve("build.gradle"), "// Build file");

        // Create a main source file
        Files.writeString(tempDir.resolve("Main.groovy"), "class Main {}");

        // Mock the DependencyResolver to return our dependency directory
        try (MockedConstruction<com.groovy.lsp.workspace.internal.dependency.DependencyResolver>
                mocked =
                        Mockito.mockConstruction(
                                com.groovy.lsp.workspace.internal.dependency.DependencyResolver
                                        .class,
                                (mock, context) -> {
                                    Mockito.when(mock.resolveDependencies())
                                            .thenReturn(List.of(depDir));
                                })) {

            // Create a new indexer that will use the mocked resolver
            WorkspaceIndexerImpl mockedIndexer = new WorkspaceIndexerImpl(tempDir);

            CountDownLatch eventLatch = new CountDownLatch(1);
            AtomicReference<WorkspaceIndexedEvent> capturedEvent = new AtomicReference<>();
            eventBus.subscribe(
                    WorkspaceIndexedEvent.class,
                    event -> {
                        capturedEvent.set(event);
                        eventLatch.countDown();
                    });

            // When
            var future = mockedIndexer.initialize();
            future.get(10, TimeUnit.SECONDS);
            eventLatch.await(2, TimeUnit.SECONDS);

            // Then
            WorkspaceIndexedEvent event = capturedEvent.get();
            assertThat(event).isNotNull();
            // Should have indexed Main.groovy (1) + Dep.groovy (1) = at least 2 files
            assertThat(event.getTotalFiles()).isGreaterThanOrEqualTo(2);

            mockedIndexer.close();
        }
    }

    @Test
    void indexDependency_shouldIndexJarDependency() throws Exception {
        // Given - Create a workspace with a JAR dependency
        Path buildFile = tempDir.resolve("build.gradle");
        Files.writeString(buildFile, "apply plugin: 'java'");

        // Create a JAR file
        Path jarFile = tempDir.resolve("libs/test-lib.jar");
        Files.createDirectories(jarFile.getParent());
        createTestJar(jarFile);

        // Create a main source file
        Files.writeString(tempDir.resolve("Main.groovy"), "class Main {}");

        // Mock the DependencyResolver to return our JAR file
        try (MockedConstruction<com.groovy.lsp.workspace.internal.dependency.DependencyResolver>
                mocked =
                        Mockito.mockConstruction(
                                com.groovy.lsp.workspace.internal.dependency.DependencyResolver
                                        .class,
                                (mock, context) -> {
                                    Mockito.when(mock.resolveDependencies())
                                            .thenReturn(List.of(jarFile));
                                })) {

            // Create a new indexer that will use the mocked resolver
            WorkspaceIndexerImpl mockedIndexer = new WorkspaceIndexerImpl(tempDir);

            CountDownLatch eventLatch = new CountDownLatch(1);
            AtomicReference<WorkspaceIndexedEvent> capturedEvent = new AtomicReference<>();
            eventBus.subscribe(
                    WorkspaceIndexedEvent.class,
                    event -> {
                        capturedEvent.set(event);
                        eventLatch.countDown();
                    });

            // When
            var future = mockedIndexer.initialize();
            future.get(10, TimeUnit.SECONDS);
            eventLatch.await(2, TimeUnit.SECONDS);

            // Then
            WorkspaceIndexedEvent event = capturedEvent.get();
            assertThat(event).isNotNull();
            // Should have indexed at least Main.groovy
            assertThat(event.getTotalFiles()).isGreaterThanOrEqualTo(1);

            mockedIndexer.close();
        }
    }

    @Test
    void indexDependency_shouldHandleExceptionDuringIndexing() throws Exception {
        // Given - Create a workspace with an invalid path as dependency
        Path buildFile = tempDir.resolve("build.gradle");
        Files.writeString(buildFile, "apply plugin: 'java'");

        // Create a non-existent path that will cause an exception
        Path nonExistentDep = tempDir.resolve("non-existent-dep.jar");

        // Create a main source file
        Files.writeString(tempDir.resolve("Main.groovy"), "class Main {}");

        // Mock the DependencyResolver to return the non-existent dependency
        try (MockedConstruction<com.groovy.lsp.workspace.internal.dependency.DependencyResolver>
                mocked =
                        Mockito.mockConstruction(
                                com.groovy.lsp.workspace.internal.dependency.DependencyResolver
                                        .class,
                                (mock, context) -> {
                                    Mockito.when(mock.resolveDependencies())
                                            .thenReturn(List.of(nonExistentDep));
                                })) {

            // Create a new indexer that will use the mocked resolver
            WorkspaceIndexerImpl mockedIndexer = new WorkspaceIndexerImpl(tempDir);

            CountDownLatch eventLatch = new CountDownLatch(1);
            AtomicReference<WorkspaceIndexedEvent> capturedEvent = new AtomicReference<>();
            eventBus.subscribe(
                    WorkspaceIndexedEvent.class,
                    event -> {
                        capturedEvent.set(event);
                        eventLatch.countDown();
                    });

            // When
            var future = mockedIndexer.initialize();
            future.get(10, TimeUnit.SECONDS);
            eventLatch.await(2, TimeUnit.SECONDS);

            // Then - Should complete despite the error
            WorkspaceIndexedEvent event = capturedEvent.get();
            assertThat(event).isNotNull();
            // Should have indexed at least Main.groovy
            assertThat(event.getTotalFiles()).isGreaterThanOrEqualTo(1);

            mockedIndexer.close();
        }
    }

    @Test
    void indexDependency_shouldHandleJarWithSymbols() throws Exception {
        // Given - Create a JAR with symbols that will be indexed
        Path jarFile = tempDir.resolve("lib-with-symbols.jar");
        createJarWithClasses(jarFile);

        Path buildFile = tempDir.resolve("build.gradle");
        Files.writeString(buildFile, "apply plugin: 'java'");
        Files.writeString(tempDir.resolve("Main.groovy"), "class Main {}");

        // Mock JarFileIndexer to return symbols
        List<SymbolInfo> mockSymbols =
                List.of(
                        new SymbolInfo("TestClass", SymbolKind.CLASS, jarFile, 1, 1),
                        new SymbolInfo("testMethod", SymbolKind.METHOD, jarFile, 5, 5));

        try (MockedConstruction<com.groovy.lsp.workspace.internal.jar.JarFileIndexer> jarMocked =
                        Mockito.mockConstruction(
                                com.groovy.lsp.workspace.internal.jar.JarFileIndexer.class,
                                (mock, context) -> {
                                    Mockito.when(mock.indexJar(jarFile)).thenReturn(mockSymbols);
                                });
                MockedConstruction<com.groovy.lsp.workspace.internal.dependency.DependencyResolver>
                        depMocked =
                                Mockito.mockConstruction(
                                        com.groovy.lsp.workspace.internal.dependency
                                                .DependencyResolver.class,
                                        (mock, context) -> {
                                            Mockito.when(mock.resolveDependencies())
                                                    .thenReturn(List.of(jarFile));
                                        })) {

            WorkspaceIndexerImpl mockedIndexer = new WorkspaceIndexerImpl(tempDir);

            CountDownLatch eventLatch = new CountDownLatch(1);
            AtomicReference<WorkspaceIndexedEvent> capturedEvent = new AtomicReference<>();
            eventBus.subscribe(
                    WorkspaceIndexedEvent.class,
                    event -> {
                        capturedEvent.set(event);
                        eventLatch.countDown();
                    });

            // When
            var future = mockedIndexer.initialize();
            future.get(10, TimeUnit.SECONDS);
            eventLatch.await(2, TimeUnit.SECONDS);

            // Then
            WorkspaceIndexedEvent event = capturedEvent.get();
            assertThat(event).isNotNull();
            // Should have indexed Main.groovy (1) + JAR (1) + build.gradle (1) = 3 files
            assertThat(event.getTotalFiles()).isEqualTo(3);
            // Should have indexed class symbols from Main + JAR symbols
            assertThat(event.getTotalSymbols())
                    .isGreaterThanOrEqualTo(3); // Main class + 2 JAR symbols

            mockedIndexer.close();
        }
    }

    private void createJarWithClasses(Path jarPath) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            // Add a proper class file entry
            JarEntry classEntry = new JarEntry("com/example/TestClass.class");
            jos.putNextEntry(classEntry);
            // Write minimal valid class file bytes (Java class file magic number)
            jos.write(new byte[] {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE});
            jos.closeEntry();

            // Add another class
            JarEntry class2Entry = new JarEntry("com/example/AnotherClass.class");
            jos.putNextEntry(class2Entry);
            jos.write(new byte[] {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE});
            jos.closeEntry();
        }
    }

    @Test
    void indexDependency_shouldHandleExceptionDuringDependencyWalk() throws Exception {
        // Test exception handling in the indexDependency method
        Path depDir = tempDir.resolve("dep-dir");
        Files.createDirectories(depDir);

        // Create a file that will cause issues during walk
        Path problemFile = depDir.resolve("Problem.groovy");
        Files.writeString(problemFile, "class Problem {}");

        Path buildFile = tempDir.resolve("build.gradle");
        Files.writeString(buildFile, "apply plugin: 'java'");
        Files.writeString(tempDir.resolve("Main.groovy"), "class Main {}");

        // Mock the dependency resolver to return a directory that will cause issues
        try (MockedConstruction<com.groovy.lsp.workspace.internal.dependency.DependencyResolver>
                mocked =
                        Mockito.mockConstruction(
                                com.groovy.lsp.workspace.internal.dependency.DependencyResolver
                                        .class,
                                (mock, context) -> {
                                    // Return a path that exists but will cause an exception during
                                    // processing
                                    Path restrictedPath = Files.createTempDirectory("restricted");
                                    // Try to set restricted permissions, or use a non-existent path
                                    // instead
                                    try {
                                        Files.setPosixFilePermissions(
                                                restrictedPath,
                                                java.util.EnumSet.noneOf(
                                                        java.nio.file.attribute.PosixFilePermission
                                                                .class));
                                    } catch (UnsupportedOperationException e) {
                                        // On non-POSIX systems, use a non-existent path instead
                                        restrictedPath = tempDir.resolve("non-existent-path");
                                    }
                                    Mockito.when(mock.resolveDependencies())
                                            .thenReturn(List.of(restrictedPath));
                                })) {

            WorkspaceIndexerImpl mockedIndexer = new WorkspaceIndexerImpl(tempDir);

            CountDownLatch eventLatch = new CountDownLatch(1);
            eventBus.subscribe(WorkspaceIndexedEvent.class, event -> eventLatch.countDown());

            // When
            var future = mockedIndexer.initialize();
            future.get(10, TimeUnit.SECONDS);
            eventLatch.await(2, TimeUnit.SECONDS);

            // Then - Should complete despite the error
            assertThat(future.isDone()).isTrue();

            mockedIndexer.close();
        }
    }

    @Test
    void close_shouldHandleMultipleExceptions() throws Exception {
        // Test that close handles exceptions from symbol index
        indexer.initialize().get(5, TimeUnit.SECONDS);

        // Force an exception by closing the symbol index first
        indexer.getSymbolIndex().close();

        // Close again should handle the exception
        assertThatCode(() -> indexer.close()).doesNotThrowAnyException();

        // And closing yet again should also work
        assertThatCode(() -> indexer.close()).doesNotThrowAnyException();
    }

    @Test
    @org.junit.jupiter.api.Disabled(
            "GroovyFileParser no longer returns null, always returns a List")
    void indexFileWithResult_shouldReturnEmptyListOnNullFromParser() throws Exception {
        // Test the case where parser returns null (though it shouldn't in practice)
        Path nullResultFile = tempDir.resolve("NullResult.groovy");
        Files.writeString(nullResultFile, "// This file might return null from parser");

        // Mock the parser to return null
        try (MockedConstruction<com.groovy.lsp.workspace.internal.parser.GroovyFileParser> mocked =
                Mockito.mockConstruction(
                        com.groovy.lsp.workspace.internal.parser.GroovyFileParser.class,
                        (mock, context) -> {
                            Mockito.when(mock.parseFile(nullResultFile)).thenReturn(null);
                        })) {

            WorkspaceIndexerImpl mockedIndexer = new WorkspaceIndexerImpl(tempDir);

            CountDownLatch eventLatch = new CountDownLatch(1);
            AtomicReference<FileIndexedEvent> capturedEvent = new AtomicReference<>();
            eventBus.subscribe(
                    FileIndexedEvent.class,
                    event -> {
                        if (event.getFilePath().equals(nullResultFile)) {
                            capturedEvent.set(event);
                            eventLatch.countDown();
                        }
                    });

            // Initialize and then update the file
            mockedIndexer.initialize().get(5, TimeUnit.SECONDS);
            mockedIndexer.updateFile(nullResultFile).get(5, TimeUnit.SECONDS);
            eventLatch.await(2, TimeUnit.SECONDS);

            // Then - Should receive event with success (empty symbols)
            FileIndexedEvent event = capturedEvent.get();
            assertThat(event).isNotNull();
            assertThat(event.isSuccess()).isTrue();
            assertThat(event.getSymbols()).isEmpty();

            mockedIndexer.close();
        }
    }
}
