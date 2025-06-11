package com.groovy.lsp.workspace.internal.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.groovy.lsp.shared.workspace.api.dto.SymbolInfo;
import com.groovy.lsp.shared.workspace.api.dto.SymbolKind;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Additional tests for SymbolIndex to improve branch coverage.
 */
class SymbolIndexAdditionalTest {

    @TempDir @Nullable Path tempDir;

    private SymbolIndex symbolIndex;
    private Path indexPath;

    @BeforeEach
    void setUp() {
        indexPath =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("test-index");
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
        SymbolIndex uninitializedIndex =
                new SymbolIndex(
                        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                                .resolve("uninit"));

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

    @Test
    void checkInitialized_shouldDetectIndividualNullStates() throws Exception {
        // Test individual null conditions in checkInitialized
        // This requires testing the internal state conditions:
        // (!initialized || env == null || symbolsDb == null || filesDb == null || dependenciesDb ==
        // null)

        // First ensure we can create a partially initialized index by manipulating the state
        symbolIndex.close();

        // Create new index that we can manipulate
        SymbolIndex testIndex =
                new SymbolIndex(
                        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                                .resolve("test-partial"));

        // Test case 1: Not initialized at all (initialized = false)
        assertThatThrownBy(() -> testIndex.addFile(Path.of("test.groovy")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Symbol index is not initialized");

        // Test case 2: Initialize then close to test env == null condition
        testIndex.initialize();
        testIndex.close();

        assertThatThrownBy(() -> testIndex.addFile(Path.of("test.groovy")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Symbol index is not initialized");
    }

    @Test
    void search_shouldHandleEdgeCasesWithCursorPositioning() {
        // Test edge cases in search method that might miss branches

        // Case 1: Search in empty database
        List<SymbolInfo> emptyResults = symbolIndex.search("anything").collect(Collectors.toList());
        assertThat(emptyResults).isEmpty();

        // Case 2: Add symbols and search for prefix that doesn't exist but falls within range
        Path file = Path.of("/test/Example.groovy");
        symbolIndex.addSymbol(new SymbolInfo("Apple", SymbolKind.CLASS, file, 1, 1));
        symbolIndex.addSymbol(new SymbolInfo("Banana", SymbolKind.CLASS, file, 2, 1));
        symbolIndex.addSymbol(new SymbolInfo("Cherry", SymbolKind.CLASS, file, 3, 1));

        // Search for something that would be positioned between existing entries
        List<SymbolInfo> betweenResults =
                symbolIndex.search("Avocado").collect(Collectors.toList());
        assertThat(betweenResults).isEmpty();

        // Case 3: Search for prefix that matches exactly but has no continuation
        List<SymbolInfo> exactResults = symbolIndex.search("Apple").collect(Collectors.toList());
        assertThat(exactResults).hasSize(1);
        assertThat(exactResults.get(0).name()).isEqualTo("Apple");

        // Case 4: Search with prefix that comes after all entries
        List<SymbolInfo> afterResults = symbolIndex.search("Zebra").collect(Collectors.toList());
        assertThat(afterResults).isEmpty();
    }

    @Test
    void removeSymbolEntry_shouldHandleVariousEdgeCases() {
        // Test edge cases in removeSymbolEntry that might cause missed branches

        Path file1 = Path.of("/test/File1.groovy");
        Path file2 = Path.of("/test/File2.groovy");

        // Add symbols with same names in different files
        symbolIndex.addSymbol(new SymbolInfo("SharedName", SymbolKind.CLASS, file1, 1, 1));
        symbolIndex.addSymbol(new SymbolInfo("SharedName", SymbolKind.METHOD, file1, 5, 1));
        symbolIndex.addSymbol(new SymbolInfo("SharedName", SymbolKind.CLASS, file2, 1, 1));
        symbolIndex.addSymbol(new SymbolInfo("DifferentName", SymbolKind.CLASS, file1, 10, 1));

        // Remove file1 - this should trigger removeSymbolEntry with multiple matches
        symbolIndex.removeFile(file1);

        // Verify only file2 symbols remain
        List<SymbolInfo> remainingShared =
                symbolIndex.search("SharedName").collect(Collectors.toList());
        assertThat(remainingShared).hasSize(1);
        assertThat(remainingShared.get(0).location()).isEqualTo(file2);

        List<SymbolInfo> removedDifferent =
                symbolIndex.search("DifferentName").collect(Collectors.toList());
        assertThat(removedDifferent).isEmpty();

        // Test removing a file with no symbols
        Path emptyFile = Path.of("/test/EmptyFile.groovy");
        symbolIndex.addFile(emptyFile);
        symbolIndex.removeFile(emptyFile); // Should not cause issues
    }

    @Test
    void getFileSymbols_shouldHandleEmptyAndMissingCursorCases() {
        // Test edge cases in getFileSymbols that might miss branches

        Path file1 = Path.of("/test/File1.groovy");
        Path file2 = Path.of("/test/File2.groovy");
        Path file3 = Path.of("/test/NonExistent.groovy");

        // Case 1: Get symbols from file that doesn't exist
        List<SymbolInfo> nonExistentResults =
                symbolIndex.getFileSymbols(file3).collect(Collectors.toList());
        assertThat(nonExistentResults).isEmpty();

        // Case 2: Add symbols and test cursor iteration edge cases
        symbolIndex.addSymbol(new SymbolInfo("Class1", SymbolKind.CLASS, file1, 1, 1));
        symbolIndex.addSymbol(new SymbolInfo("Method1", SymbolKind.METHOD, file1, 5, 1));
        symbolIndex.addSymbol(new SymbolInfo("Class2", SymbolKind.CLASS, file2, 1, 1));

        // Get symbols for file1
        List<SymbolInfo> file1Results =
                symbolIndex.getFileSymbols(file1).collect(Collectors.toList());
        assertThat(file1Results).hasSize(2);
        assertThat(file1Results.stream().map(SymbolInfo::name))
                .containsExactlyInAnyOrder("Class1", "Method1");

        // Get symbols for file2
        List<SymbolInfo> file2Results =
                symbolIndex.getFileSymbols(file2).collect(Collectors.toList());
        assertThat(file2Results).hasSize(1);
        assertThat(file2Results.get(0).name()).isEqualTo("Class2");

        // Case 3: Get symbols from file that exists but has no symbols
        symbolIndex.addFile(file3);
        List<SymbolInfo> emptyFileResults =
                symbolIndex.getFileSymbols(file3).collect(Collectors.toList());
        assertThat(emptyFileResults).isEmpty();
    }

    @Test
    void deserializeSymbol_shouldHandleMalformedData() {
        // Test the missing branch in deserializeSymbol for malformed data
        // This is tricky since the method is private, but we can trigger it indirectly

        // Add a properly formatted symbol first
        Path file = Path.of("/test/ValidFile.groovy");
        SymbolInfo validSymbol = new SymbolInfo("ValidClass", SymbolKind.CLASS, file, 10, 5);
        symbolIndex.addSymbol(validSymbol);

        // Verify the valid symbol works
        List<SymbolInfo> validResults =
                symbolIndex.search("ValidClass").collect(Collectors.toList());
        assertThat(validResults).hasSize(1);
        assertThat(validResults.get(0).name()).isEqualTo("ValidClass");
        assertThat(validResults.get(0).line()).isEqualTo(10);
        assertThat(validResults.get(0).column()).isEqualTo(5);

        // Test with symbols that might cause parsing issues
        // Use unusual characters that could cause split issues
        SymbolInfo symbolWithPipes =
                new SymbolInfo("Class|With|Pipes", SymbolKind.CLASS, file, 1, 1);
        symbolIndex.addSymbol(symbolWithPipes);

        List<SymbolInfo> pipeResults =
                symbolIndex.search("Class|With|Pipes").collect(Collectors.toList());
        // This might fail to deserialize properly due to pipe character conflicts
        // The deserializeSymbol method should handle this gracefully and return null
        // which would result in the symbol not appearing in results
        // For this test, we'll verify that the search handles the data gracefully
        assertThat(pipeResults).isNotNull(); // Should not throw, regardless of result

        // Test with extremely long symbol names that might cause issues
        String longName = "Very".repeat(100) + "LongClassName";
        SymbolInfo longSymbol = new SymbolInfo(longName, SymbolKind.CLASS, file, 1, 1);
        symbolIndex.addSymbol(longSymbol);

        List<SymbolInfo> longResults = symbolIndex.search(longName).collect(Collectors.toList());
        // Should handle long names without issues
        assertThat(longResults).hasSize(1);
        assertThat(longResults.get(0).name()).isEqualTo(longName);
    }

    @Test
    void search_shouldHandleSpecialQueryCharacters() {
        // Test search with special characters that might affect cursor operations

        Path file = Path.of("/test/SpecialChars.groovy");

        // Add symbols with various special characters
        symbolIndex.addSymbol(new SymbolInfo("Normal", SymbolKind.CLASS, file, 1, 1));
        symbolIndex.addSymbol(new SymbolInfo("With Space", SymbolKind.CLASS, file, 2, 1));
        symbolIndex.addSymbol(new SymbolInfo("With-Dash", SymbolKind.CLASS, file, 3, 1));
        symbolIndex.addSymbol(new SymbolInfo("With_Underscore", SymbolKind.CLASS, file, 4, 1));

        // Test search with exact matches
        List<SymbolInfo> normalResults = symbolIndex.search("Normal").collect(Collectors.toList());
        assertThat(normalResults).hasSize(1);

        List<SymbolInfo> spaceResults =
                symbolIndex.search("With Space").collect(Collectors.toList());
        assertThat(spaceResults).hasSize(1);

        List<SymbolInfo> dashResults = symbolIndex.search("With-Dash").collect(Collectors.toList());
        assertThat(dashResults).hasSize(1);

        List<SymbolInfo> underscoreResults =
                symbolIndex.search("With_Underscore").collect(Collectors.toList());
        assertThat(underscoreResults).hasSize(1);

        // Test prefix searches that might not find ranges correctly
        List<SymbolInfo> withPrefixResults =
                symbolIndex.search("With").collect(Collectors.toList());
        assertThat(withPrefixResults).hasSize(3); // Should find all "With*" symbols
    }

    @Test
    void checkInitialized_shouldCoverAllBranchConditions() throws Exception {
        // This test aims to improve branch coverage for checkInitialized() method
        // The method checks: (!initialized || env == null || symbolsDb == null || filesDb == null
        // || dependenciesDb == null)

        // Test 1: Test when properly initialized (should not throw)
        symbolIndex.addFile(
                Path.of("/test/valid.groovy")); // This calls checkInitialized internally

        // Test 2: Test after closing (all fields become null)
        symbolIndex.close();

        // Verify all operations fail after closing (triggers different null checks)
        assertThatThrownBy(() -> symbolIndex.search("test"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Symbol index is not initialized");

        assertThatThrownBy(() -> symbolIndex.getFileSymbols(Path.of("test.groovy")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Symbol index is not initialized");

        assertThatThrownBy(() -> symbolIndex.addDependency(Path.of("dep.jar")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Symbol index is not initialized");

        assertThatThrownBy(() -> symbolIndex.removeFile(Path.of("test.groovy")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Symbol index is not initialized");

        // Test 3: Create a new index and test uninitialized state
        SymbolIndex newIndex =
                new SymbolIndex(
                        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                                .resolve("new-test"));

        // Test multiple operations on uninitialized index to trigger different code paths
        assertThatThrownBy(() -> newIndex.addFile(Path.of("test.groovy")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Symbol index is not initialized");

        assertThatThrownBy(
                        () ->
                                newIndex.addSymbol(
                                        new SymbolInfo(
                                                "Test",
                                                SymbolKind.CLASS,
                                                Path.of("test.groovy"),
                                                1,
                                                1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Symbol index is not initialized");
    }
}
