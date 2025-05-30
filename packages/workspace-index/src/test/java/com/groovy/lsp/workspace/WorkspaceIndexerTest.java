package com.groovy.lsp.workspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for WorkspaceIndexer.
 */
class WorkspaceIndexerTest {
    
    @TempDir
    Path tempDir;
    
    private WorkspaceIndexer indexer;
    
    @BeforeEach
    void setUp() {
        indexer = new WorkspaceIndexer(tempDir);
    }
    
    @AfterEach
    void tearDown() {
        indexer.close();
    }
    
    @Test
    void testInitialize() throws Exception {
        // Create some test files
        Path srcDir = tempDir.resolve("src/main/groovy");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("Example.groovy"), 
            "class Example { String name }");
        
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