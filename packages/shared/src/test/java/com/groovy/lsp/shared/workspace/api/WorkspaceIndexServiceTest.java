package com.groovy.lsp.shared.workspace.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.groovy.lsp.shared.workspace.api.dto.SymbolInfo;
import com.groovy.lsp.shared.workspace.api.dto.SymbolKind;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WorkspaceIndexServiceTest {

    private TestWorkspaceIndexService workspaceIndexService;
    private Path testPath;
    private SymbolInfo testSymbol;

    @BeforeEach
    void setUp() {
        workspaceIndexService = new TestWorkspaceIndexService();
        testPath = Paths.get("/test/path/file.groovy");
        testSymbol = new SymbolInfo("TestClass", SymbolKind.CLASS, testPath, 10, 5);
    }

    @Test
    @DisplayName("Should initialize workspace index")
    void testInitialize() throws ExecutionException, InterruptedException {
        // Act
        CompletableFuture<Void> result = workspaceIndexService.initialize();

        // Assert
        assertNotNull(result);
        result.get(); // Wait for completion
        assertTrue(workspaceIndexService.isInitialized());
        assertEquals(1, workspaceIndexService.getInitializeCallCount());
    }

    @Test
    @DisplayName("Should update file in index")
    void testUpdateFile() throws ExecutionException, InterruptedException {
        // Act
        CompletableFuture<Void> result = workspaceIndexService.updateFile(testPath);

        // Assert
        assertNotNull(result);
        result.get(); // Wait for completion
        assertTrue(workspaceIndexService.getUpdatedFiles().contains(testPath));
        assertEquals(1, workspaceIndexService.getUpdateFileCallCount());
    }

    @Test
    @DisplayName("Should search symbols by query")
    void testSearchSymbols() throws ExecutionException, InterruptedException {
        // Arrange
        String query = "Test";
        workspaceIndexService.addSymbol(testSymbol);

        // Act
        CompletableFuture<Stream<SymbolInfo>> result = workspaceIndexService.searchSymbols(query);

        // Assert
        assertNotNull(result);
        Stream<SymbolInfo> symbols = result.get();
        assertEquals(1, symbols.count());
        assertEquals(1, workspaceIndexService.getSearchSymbolsCallCount());
    }

    @Test
    @DisplayName("Should handle empty search results")
    void testSearchSymbolsEmpty() throws ExecutionException, InterruptedException {
        // Arrange
        String query = "NonExistent";

        // Act
        CompletableFuture<Stream<SymbolInfo>> result = workspaceIndexService.searchSymbols(query);

        // Assert
        assertNotNull(result);
        Stream<SymbolInfo> symbols = result.get();
        assertEquals(0, symbols.count());
    }

    @Test
    @DisplayName("Should shutdown service")
    void testShutdown() {
        // Act
        workspaceIndexService.shutdown();

        // Assert
        assertTrue(workspaceIndexService.isShutdown());
        assertEquals(1, workspaceIndexService.getShutdownCallCount());
    }

    @Test
    @DisplayName("Should handle multiple symbol search results")
    void testSearchMultipleSymbols() throws ExecutionException, InterruptedException {
        // Arrange
        String query = "test";
        SymbolInfo symbol1 = new SymbolInfo("TestClass", SymbolKind.CLASS, testPath, 10, 5);
        SymbolInfo symbol2 = new SymbolInfo("testMethod", SymbolKind.METHOD, testPath, 20, 10);
        SymbolInfo symbol3 = new SymbolInfo("testField", SymbolKind.FIELD, testPath, 30, 15);

        workspaceIndexService.addSymbol(symbol1);
        workspaceIndexService.addSymbol(symbol2);
        workspaceIndexService.addSymbol(symbol3);

        // Act
        CompletableFuture<Stream<SymbolInfo>> result = workspaceIndexService.searchSymbols(query);

        // Assert
        assertNotNull(result);
        Stream<SymbolInfo> symbols = result.get();
        assertEquals(3, symbols.count());
    }

    @Test
    @DisplayName("Should handle concurrent operations")
    void testConcurrentOperations() throws ExecutionException, InterruptedException {
        // Arrange
        Path file1 = Paths.get("/test/file1.groovy");
        Path file2 = Paths.get("/test/file2.groovy");

        // Act
        CompletableFuture<Void> future1 = workspaceIndexService.updateFile(file1);
        CompletableFuture<Void> future2 = workspaceIndexService.updateFile(file2);

        // Assert
        CompletableFuture.allOf(future1, future2).get();
        assertTrue(workspaceIndexService.getUpdatedFiles().contains(file1));
        assertTrue(workspaceIndexService.getUpdatedFiles().contains(file2));
        assertEquals(2, workspaceIndexService.getUpdateFileCallCount());
    }

    @Test
    @DisplayName("Should test interface contract")
    void testInterfaceContract() {
        // This test verifies that the interface has all required methods
        // by creating an anonymous implementation
        WorkspaceIndexService service =
                new WorkspaceIndexService() {
                    @Override
                    public CompletableFuture<Void> initialize() {
                        return CompletableFuture.completedFuture(null);
                    }

                    @Override
                    public CompletableFuture<Void> updateFile(Path file) {
                        return CompletableFuture.completedFuture(null);
                    }

                    @Override
                    public CompletableFuture<Stream<SymbolInfo>> searchSymbols(String query) {
                        return CompletableFuture.completedFuture(Stream.empty());
                    }

                    @Override
                    public void shutdown() {
                        // No-op
                    }
                };

        // Act & Assert
        assertNotNull(service.initialize());
        assertNotNull(service.updateFile(testPath));
        assertNotNull(service.searchSymbols("test"));
        service.shutdown(); // Should not throw
    }

    @Test
    @DisplayName("Should handle failed futures")
    void testFailedFutures() {
        // Arrange
        workspaceIndexService.setFailOnUpdate(true);

        // Act
        CompletableFuture<Void> result = workspaceIndexService.updateFile(testPath);

        // Assert
        assertNotNull(result);
        assertTrue(result.isCompletedExceptionally());
    }

    @Test
    @DisplayName("Should handle case-insensitive search")
    void testCaseInsensitiveSearch() throws ExecutionException, InterruptedException {
        // Arrange
        SymbolInfo symbol = new SymbolInfo("TestClass", SymbolKind.CLASS, testPath, 10, 5);
        workspaceIndexService.addSymbol(symbol);

        // Act
        CompletableFuture<Stream<SymbolInfo>> result1 = workspaceIndexService.searchSymbols("test");
        CompletableFuture<Stream<SymbolInfo>> result2 = workspaceIndexService.searchSymbols("TEST");
        CompletableFuture<Stream<SymbolInfo>> result3 = workspaceIndexService.searchSymbols("Test");

        // Assert
        assertEquals(1, result1.get().count());
        assertEquals(1, result2.get().count());
        assertEquals(1, result3.get().count());
    }

    /**
     * Test implementation of WorkspaceIndexService for testing purposes.
     */
    private static class TestWorkspaceIndexService implements WorkspaceIndexService {
        private final AtomicBoolean initialized = new AtomicBoolean(false);
        private final AtomicBoolean shutdown = new AtomicBoolean(false);
        private final AtomicInteger initializeCallCount = new AtomicInteger(0);
        private final AtomicInteger updateFileCallCount = new AtomicInteger(0);
        private final AtomicInteger searchSymbolsCallCount = new AtomicInteger(0);
        private final AtomicInteger shutdownCallCount = new AtomicInteger(0);
        private final ConcurrentHashMap<Path, List<SymbolInfo>> symbols = new ConcurrentHashMap<>();
        private final List<Path> updatedFiles = new ArrayList<>();
        private boolean failOnUpdate = false;

        @Override
        public CompletableFuture<Void> initialize() {
            initializeCallCount.incrementAndGet();
            initialized.set(true);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> updateFile(Path file) {
            updateFileCallCount.incrementAndGet();
            if (failOnUpdate) {
                CompletableFuture<Void> future = new CompletableFuture<>();
                future.completeExceptionally(new RuntimeException("Update failed"));
                return future;
            }
            updatedFiles.add(file);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Stream<SymbolInfo>> searchSymbols(String query) {
            searchSymbolsCallCount.incrementAndGet();
            List<SymbolInfo> results = new ArrayList<>();
            for (List<SymbolInfo> fileSymbols : symbols.values()) {
                for (SymbolInfo symbol : fileSymbols) {
                    if (symbol.name()
                            .toLowerCase(Locale.ROOT)
                            .contains(query.toLowerCase(Locale.ROOT))) {
                        results.add(symbol);
                    }
                }
            }
            return CompletableFuture.completedFuture(results.stream());
        }

        @Override
        public void shutdown() {
            shutdownCallCount.incrementAndGet();
            shutdown.set(true);
        }

        // Test helper methods
        public void addSymbol(SymbolInfo symbol) {
            symbols.computeIfAbsent(symbol.location(), k -> new ArrayList<>()).add(symbol);
        }

        public boolean isInitialized() {
            return initialized.get();
        }

        public boolean isShutdown() {
            return shutdown.get();
        }

        public int getInitializeCallCount() {
            return initializeCallCount.get();
        }

        public int getUpdateFileCallCount() {
            return updateFileCallCount.get();
        }

        public int getSearchSymbolsCallCount() {
            return searchSymbolsCallCount.get();
        }

        public int getShutdownCallCount() {
            return shutdownCallCount.get();
        }

        public List<Path> getUpdatedFiles() {
            return new ArrayList<>(updatedFiles);
        }

        public void setFailOnUpdate(boolean fail) {
            this.failOnUpdate = fail;
        }
    }
}
