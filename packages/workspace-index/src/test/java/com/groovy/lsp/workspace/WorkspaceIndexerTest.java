package com.groovy.lsp.workspace;

import static org.junit.jupiter.api.Assertions.*;

import com.groovy.lsp.workspace.api.WorkspaceIndexFactory;
import com.groovy.lsp.workspace.api.WorkspaceIndexService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for WorkspaceIndexService.
 */
class WorkspaceIndexerTest {

    @SuppressWarnings("NullAway") // @TempDir is guaranteed to be initialized by JUnit
    @TempDir
    Path tempDir;

    private WorkspaceIndexService indexer;

    @BeforeEach
    void setUp() {
        // @TempDir is guaranteed to be initialized before @BeforeEach
        indexer = WorkspaceIndexFactory.createWorkspaceIndexService(tempDir);
    }

    @AfterEach
    void tearDown() {
        indexer.shutdown();
    }

    @Test
    void testInitialize() throws Exception {
        // Create some test files
        Path srcDir = tempDir.resolve("src/main/groovy");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("Example.groovy"), "class Example { String name }");

        // Initialize indexer
        CompletableFuture<Void> future = indexer.initialize();
        assertNotNull(future);

        // Wait for completion
        future.get();

        // Verify index was created
        assertTrue(Files.exists(tempDir.resolve(".groovy-lsp/index")));
    }

    @Test
    void testUpdateFile() throws Exception {
        // Initialize first
        indexer.initialize().get();

        // Create a new file
        Path newFile = tempDir.resolve("Test.groovy");
        Files.writeString(newFile, "class Test {}");

        // Update index
        CompletableFuture<Void> future = indexer.updateFile(newFile);
        assertNotNull(future);
        future.get();

        // File should be indexed
        // TODO: Add verification once symbol extraction is implemented
    }

    @Test
    void testSearchSymbols() throws Exception {
        // Initialize first
        indexer.initialize().get();

        // Search for symbols
        var results = indexer.searchSymbols("Test").get();
        assertNotNull(results);

        // Initially should be empty
        assertEquals(0, results.count());
    }
}
