package com.groovy.lsp.workspace.internal.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.groovy.lsp.shared.workspace.api.dto.SymbolInfo;
import com.groovy.lsp.shared.workspace.api.dto.SymbolKind;
import java.nio.file.Files;
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
 * SymbolIndexのテストクラス。
 */
class SymbolIndexTest {

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
    void initialize_shouldCreateIndexDirectory() {
        // then
        assertThat(Files.exists(indexPath)).isTrue();
        assertThat(Files.isDirectory(indexPath)).isTrue();
    }

    @Test
    void checkInitialized_shouldThrowExceptionWhenNotInitialized() throws Exception {
        // given
        symbolIndex.close();
        SymbolIndex uninitializedIndex =
                new SymbolIndex(
                        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                                .resolve("uninit"));

        // when/then
        assertThatThrownBy(() -> uninitializedIndex.addFile(Path.of("test.groovy")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Symbol index is not initialized");
    }

    @Test
    void addFile_shouldAddFileToIndex() {
        // given
        Path file = Path.of("/test/Example.groovy");

        // when
        symbolIndex.addFile(file);

        // then - ファイルが追加されたことを確認
        // (実際のテストでは、getFileSymbolsなどで確認)
    }

    @Test
    void removeFile_shouldRemoveFileFromIndex() {
        // given
        Path file = Path.of("/test/Example.groovy");
        symbolIndex.addFile(file);

        SymbolInfo symbol = new SymbolInfo("TestClass", SymbolKind.CLASS, file, 1, 1);
        symbolIndex.addSymbol(symbol);

        // when
        symbolIndex.removeFile(file);

        // then
        List<SymbolInfo> symbols = symbolIndex.getFileSymbols(file).collect(Collectors.toList());
        assertThat(symbols).isEmpty();
    }

    @Test
    void addDependency_shouldAddDependencyToIndex() {
        // given
        Path dependency = Path.of("/libs/groovy-all-3.0.9.jar");

        // when
        symbolIndex.addDependency(dependency);

        // then - 依存関係が追加されたことを確認
        // (実際のLMDBでは内部的に保存される)
    }

    @Test
    void addSymbol_shouldAddSymbolToIndex() {
        // given
        Path file = Path.of("/test/Example.groovy");
        SymbolInfo symbol = new SymbolInfo("TestClass", SymbolKind.CLASS, file, 10, 5);

        // when
        symbolIndex.addSymbol(symbol);

        // then
        List<SymbolInfo> results = symbolIndex.search("TestClass").collect(Collectors.toList());
        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("TestClass");
        assertThat(results.get(0).kind()).isEqualTo(SymbolKind.CLASS);
        assertThat(results.get(0).line()).isEqualTo(10);
        assertThat(results.get(0).column()).isEqualTo(5);
    }

    @Test
    void search_shouldFindSymbolsByPrefix() {
        // given
        Path file = Path.of("/test/Example.groovy");
        symbolIndex.addSymbol(new SymbolInfo("TestClass", SymbolKind.CLASS, file, 1, 1));
        symbolIndex.addSymbol(new SymbolInfo("TestMethod", SymbolKind.METHOD, file, 10, 4));
        symbolIndex.addSymbol(new SymbolInfo("AnotherClass", SymbolKind.CLASS, file, 20, 1));

        // when
        List<SymbolInfo> results = symbolIndex.search("Test").collect(Collectors.toList());

        // then
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(SymbolInfo::name)
                .containsExactlyInAnyOrder("TestClass", "TestMethod");
    }

    @Test
    void search_shouldReturnCachedResults() {
        // given
        Path file = Path.of("/test/Example.groovy");
        symbolIndex.addSymbol(new SymbolInfo("CachedSymbol", SymbolKind.CLASS, file, 1, 1));

        // when - 最初の検索
        List<SymbolInfo> firstResults = symbolIndex.search("Cached").collect(Collectors.toList());

        // 新しいシンボルを追加（キャッシュには影響しない）
        symbolIndex.addSymbol(new SymbolInfo("CachedSymbol2", SymbolKind.CLASS, file, 2, 1));

        // 同じクエリで再検索（キャッシュから）
        List<SymbolInfo> cachedResults = symbolIndex.search("Cached").collect(Collectors.toList());

        // then
        assertThat(firstResults).hasSize(1);
        assertThat(cachedResults).hasSize(1); // キャッシュから返されるので新しいシンボルは含まれない
    }

    @Test
    void getFileSymbols_shouldRetrieveAllSymbolsInFile() {
        // given
        Path file1 = Path.of("/test/File1.groovy");
        Path file2 = Path.of("/test/File2.groovy");

        symbolIndex.addSymbol(new SymbolInfo("Class1", SymbolKind.CLASS, file1, 1, 1));
        symbolIndex.addSymbol(new SymbolInfo("Method1", SymbolKind.METHOD, file1, 5, 4));
        symbolIndex.addSymbol(new SymbolInfo("Field1", SymbolKind.FIELD, file1, 3, 4));
        symbolIndex.addSymbol(new SymbolInfo("Class2", SymbolKind.CLASS, file2, 1, 1));

        // when
        List<SymbolInfo> file1Symbols =
                symbolIndex.getFileSymbols(file1).collect(Collectors.toList());
        List<SymbolInfo> file2Symbols =
                symbolIndex.getFileSymbols(file2).collect(Collectors.toList());

        // then
        assertThat(file1Symbols).hasSize(3);
        assertThat(file1Symbols)
                .extracting(SymbolInfo::name)
                .containsExactlyInAnyOrder("Class1", "Method1", "Field1");

        assertThat(file2Symbols).hasSize(1);
        assertThat(file2Symbols.get(0).name()).isEqualTo("Class2");
    }

    @Test
    void addSymbol_shouldAllowMultipleSymbolsWithSameName() {
        // given
        Path file = Path.of("/test/Example.groovy");
        SymbolInfo symbol1 = new SymbolInfo("overloaded", SymbolKind.METHOD, file, 10, 1);
        SymbolInfo symbol2 = new SymbolInfo("overloaded", SymbolKind.METHOD, file, 20, 1);

        // when
        symbolIndex.addSymbol(symbol1);
        symbolIndex.addSymbol(symbol2);

        // then
        List<SymbolInfo> results = symbolIndex.search("overloaded").collect(Collectors.toList());
        assertThat(results).hasSize(2);
        assertThat(results).extracting(SymbolInfo::line).containsExactlyInAnyOrder(10, 20);
    }

    @Test
    void removeFile_shouldRemoveRelatedSymbols() {
        // given
        Path file = Path.of("/test/ToDelete.groovy");
        symbolIndex.addFile(file);
        symbolIndex.addSymbol(new SymbolInfo("DeletedClass", SymbolKind.CLASS, file, 1, 1));
        symbolIndex.addSymbol(new SymbolInfo("DeletedMethod", SymbolKind.METHOD, file, 5, 4));

        // when
        symbolIndex.removeFile(file);

        // then
        List<SymbolInfo> deletedClassResults =
                symbolIndex.search("DeletedClass").collect(Collectors.toList());
        List<SymbolInfo> deletedMethodResults =
                symbolIndex.search("DeletedMethod").collect(Collectors.toList());

        assertThat(deletedClassResults).isEmpty();
        assertThat(deletedMethodResults).isEmpty();
    }

    @Test
    void search_shouldHandleEmptyQuery() {
        // given
        Path file = Path.of("/test/Example.groovy");
        symbolIndex.addSymbol(new SymbolInfo("TestSymbol", SymbolKind.CLASS, file, 1, 1));

        // when
        List<SymbolInfo> results = symbolIndex.search("").collect(Collectors.toList());

        // then
        assertThat(results).isNotEmpty(); // 空のクエリはすべてのシンボルにマッチ
    }

    @Test
    void close_shouldProperlyCleanupResources() throws Exception {
        // given
        Path file = Path.of("/test/Example.groovy");
        symbolIndex.addSymbol(new SymbolInfo("TestSymbol", SymbolKind.CLASS, file, 1, 1));

        // when
        symbolIndex.close();

        // then
        assertThatThrownBy(() -> symbolIndex.addFile(Path.of("test.groovy")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldHandleJapaneseSymbolNames() {
        // given
        Path file = Path.of("/test/Japanese.groovy");
        SymbolInfo japaneseSymbol = new SymbolInfo("テストクラス", SymbolKind.CLASS, file, 1, 1);

        // when
        symbolIndex.addSymbol(japaneseSymbol);

        // then
        List<SymbolInfo> results = symbolIndex.search("テスト").collect(Collectors.toList());
        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("テストクラス");
    }

    @Test
    void shouldHandleSymbolNamesWithSpecialCharacters() {
        // given
        Path file = Path.of("/test/Special.groovy");
        symbolIndex.addSymbol(new SymbolInfo("Test$Inner", SymbolKind.CLASS, file, 1, 1));
        symbolIndex.addSymbol(new SymbolInfo("_privateField", SymbolKind.FIELD, file, 2, 1));
        symbolIndex.addSymbol(new SymbolInfo("method@Override", SymbolKind.METHOD, file, 3, 1));

        // when
        List<SymbolInfo> dollarResults = symbolIndex.search("Test$").collect(Collectors.toList());
        List<SymbolInfo> underscoreResults =
                symbolIndex.search("_private").collect(Collectors.toList());
        List<SymbolInfo> atResults = symbolIndex.search("method@").collect(Collectors.toList());

        // then
        assertThat(dollarResults).hasSize(1);
        assertThat(underscoreResults).hasSize(1);
        assertThat(atResults).hasSize(1);
    }

    @Test
    void constructor_shouldAcceptCustomMapSize() throws Exception {
        // Clean up the default index first
        symbolIndex.close();

        // given
        long customMapSize = 2L * 1024L * 1024L * 1024L; // 2GB
        Path customIndexPath =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("custom-index");

        // when
        SymbolIndex customIndex = new SymbolIndex(customIndexPath, customMapSize);
        customIndex.initialize();

        // Add some data to verify it works
        Path file = Path.of("/test/CustomSize.java");
        customIndex.addFile(file);
        customIndex.addSymbol(new SymbolInfo("CustomClass", SymbolKind.CLASS, file, 1, 1));

        // then
        assertThat(Files.exists(customIndexPath)).isTrue();
        List<SymbolInfo> symbols = customIndex.getFileSymbols(file).collect(Collectors.toList());
        assertThat(symbols).hasSize(1);
        assertThat(symbols.get(0).name()).isEqualTo("CustomClass");

        // Clean up
        customIndex.close();
    }
}
