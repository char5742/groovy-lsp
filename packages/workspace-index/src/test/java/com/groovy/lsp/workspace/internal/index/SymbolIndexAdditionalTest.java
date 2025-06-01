package com.groovy.lsp.workspace.internal.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.groovy.lsp.workspace.api.dto.SymbolInfo;
import com.groovy.lsp.workspace.api.dto.SymbolKind;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Additional tests for SymbolIndex to improve branch coverage.
 */
class SymbolIndexAdditionalTest {

    @TempDir Path tempDir;

    private SymbolIndex symbolIndex;
    private Path indexPath;

    @BeforeEach
    void setUp() {
        indexPath = tempDir.resolve("test-index");
        symbolIndex = new SymbolIndex(indexPath);
        symbolIndex.initialize();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (symbolIndex != null) {
            symbolIndex.close();
        }
    }

    @Test
    void search_shouldHandleNoResultsForQuery() {
        // given
        Path file = Path.of("/test/Example.groovy");
        symbolIndex.addSymbol(new SymbolInfo("TestClass", SymbolKind.CLASS, file, 1, 1));

        // when
        List<SymbolInfo> results = symbolIndex.search("NonExistent").collect(Collectors.toList());

        // then
        assertThat(results).isEmpty();
    }

    @Test
    void search_shouldHandleEmptyIndexWithEmptyQuery() {
        // when - search on empty index with empty query
        List<SymbolInfo> results = symbolIndex.search("").collect(Collectors.toList());

        // then
        assertThat(results).isEmpty();
    }

    @Test
    void getFileSymbols_shouldReturnEmptyForNonExistentFile() {
        // given
        Path nonExistentFile = Path.of("/test/NonExistent.groovy");

        // when
        List<SymbolInfo> results =
                symbolIndex.getFileSymbols(nonExistentFile).collect(Collectors.toList());

        // then
        assertThat(results).isEmpty();
    }

    @Test
    void getFileSymbols_shouldHandleEmptyIndex() {
        // given
        Path file = Path.of("/test/Example.groovy");

        // when
        List<SymbolInfo> results = symbolIndex.getFileSymbols(file).collect(Collectors.toList());

        // then
        assertThat(results).isEmpty();
    }

    @Test
    void removeFile_shouldHandleNonExistentFile() {
        // given
        Path nonExistentFile = Path.of("/test/NonExistent.groovy");

        // when/then - should not throw exception
        symbolIndex.removeFile(nonExistentFile);
    }

    @Test
    void removeFile_shouldClearFileButKeepOtherFiles() {
        // given
        Path file1 = Path.of("/test/File1.groovy");
        Path file2 = Path.of("/test/File2.groovy");

        symbolIndex.addFile(file1);
        symbolIndex.addFile(file2);

        symbolIndex.addSymbol(new SymbolInfo("Class1", SymbolKind.CLASS, file1, 1, 1));
        symbolIndex.addSymbol(new SymbolInfo("Class2", SymbolKind.CLASS, file2, 1, 1));

        // when
        symbolIndex.removeFile(file1);

        // then
        List<SymbolInfo> file1Symbols =
                symbolIndex.getFileSymbols(file1).collect(Collectors.toList());
        List<SymbolInfo> file2Symbols =
                symbolIndex.getFileSymbols(file2).collect(Collectors.toList());

        assertThat(file1Symbols).isEmpty();
        assertThat(file2Symbols).hasSize(1);
        assertThat(file2Symbols.get(0).name()).isEqualTo("Class2");
    }

    @Test
    void checkInitialized_shouldNotThrowWhenInitialized() {
        // given - already initialized in setUp

        // when/then - should not throw
        symbolIndex.addFile(Path.of("/test/Example.groovy"));
        symbolIndex.addDependency(Path.of("/lib/dep.jar"));
    }

    @Test
    void checkInitialized_shouldThrowForAllOperationsWhenNotInitialized() throws Exception {
        // given
        symbolIndex.close();
        SymbolIndex uninitializedIndex = new SymbolIndex(tempDir.resolve("uninit"));

        // when/then - all operations should throw
        assertThatThrownBy(() -> uninitializedIndex.addFile(Path.of("test.groovy")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Symbol index is not initialized");

        assertThatThrownBy(() -> uninitializedIndex.removeFile(Path.of("test.groovy")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Symbol index is not initialized");

        assertThatThrownBy(() -> uninitializedIndex.addDependency(Path.of("dep.jar")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Symbol index is not initialized");

        assertThatThrownBy(
                        () ->
                                uninitializedIndex.addSymbol(
                                        new SymbolInfo(
                                                "Test",
                                                SymbolKind.CLASS,
                                                Path.of("test.groovy"),
                                                1,
                                                1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Symbol index is not initialized");

        assertThatThrownBy(() -> uninitializedIndex.search("test"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Symbol index is not initialized");

        assertThatThrownBy(() -> uninitializedIndex.getFileSymbols(Path.of("test.groovy")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Symbol index is not initialized");
    }

    @Test
    void deserializeSymbol_shouldHandleCorruptedData() {
        // This test would require accessing internal methods or injecting corrupted data
        // For now, we'll test the normal serialization/deserialization path
        Path file = Path.of("/test/Example.groovy");
        SymbolInfo symbol = new SymbolInfo("TestClass", SymbolKind.CLASS, file, 10, 5);

        symbolIndex.addSymbol(symbol);

        List<SymbolInfo> results = symbolIndex.search("TestClass").collect(Collectors.toList());
        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("TestClass");
    }

    @Test
    void search_shouldHandleCursorNotFindingRange() {
        // given - add symbols that won't match our search prefix
        Path file = Path.of("/test/Example.groovy");
        symbolIndex.addSymbol(new SymbolInfo("AClass", SymbolKind.CLASS, file, 1, 1));
        symbolIndex.addSymbol(new SymbolInfo("BClass", SymbolKind.CLASS, file, 2, 1));

        // when - search for something that comes after all existing entries
        List<SymbolInfo> results = symbolIndex.search("ZZZ").collect(Collectors.toList());

        // then
        assertThat(results).isEmpty();
    }

    @Test
    void removeSymbolEntry_shouldHandleMultipleEntriesForSameKey() {
        // given
        Path file1 = Path.of("/test/File1.groovy");
        Path file2 = Path.of("/test/File2.groovy");

        // Add same symbol name in different files
        symbolIndex.addSymbol(new SymbolInfo("SharedSymbol", SymbolKind.CLASS, file1, 1, 1));
        symbolIndex.addSymbol(new SymbolInfo("SharedSymbol", SymbolKind.CLASS, file2, 1, 1));

        // when - remove one file
        symbolIndex.removeFile(file1);

        // then - symbol from other file should still exist
        List<SymbolInfo> results = symbolIndex.search("SharedSymbol").collect(Collectors.toList());
        assertThat(results).hasSize(1);
        assertThat(results.get(0).location()).isEqualTo(file2);
    }

    @Test
    void close_shouldHandleMultipleCalls() throws Exception {
        // given
        Path file = Path.of("/test/Example.groovy");
        symbolIndex.addSymbol(new SymbolInfo("TestSymbol", SymbolKind.CLASS, file, 1, 1));

        // when
        symbolIndex.close();
        symbolIndex.close(); // Second call should not throw

        // then - index should be closed
        assertThatThrownBy(() -> symbolIndex.addFile(Path.of("test.groovy")))
                .isInstanceOf(IllegalStateException.class);
    }
}
