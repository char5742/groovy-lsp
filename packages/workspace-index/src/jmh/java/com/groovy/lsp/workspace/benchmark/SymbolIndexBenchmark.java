package com.groovy.lsp.workspace.benchmark;

import com.groovy.lsp.workspace.api.dto.SymbolInfo;
import com.groovy.lsp.workspace.api.dto.SymbolKind;
import com.groovy.lsp.workspace.internal.index.SymbolIndex;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH benchmarks for SymbolIndex performance.
 * Measures search performance with 100,000 lines of code worth of symbols.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(
        value = 1,
        jvmArgs = {
            "--add-opens", "java.base/java.nio=ALL-UNNAMED",
            "--add-opens", "java.base/java.lang=ALL-UNNAMED",
            "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED"
        })
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class SymbolIndexBenchmark {

    private static final int TOTAL_SYMBOLS = 100_000;
    private static final int SYMBOLS_PER_FILE = 100;
    private static final int TOTAL_FILES = TOTAL_SYMBOLS / SYMBOLS_PER_FILE;

    private SymbolIndex symbolIndex;
    private List<String> searchQueries;
    private Path tempDir;
    private Random random;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        // Create temporary directory for index
        tempDir = Files.createTempDirectory("symbol-index-benchmark");
        Path indexPath = tempDir.resolve("index");
        Files.createDirectories(indexPath);

        // Initialize index
        symbolIndex = new SymbolIndex(indexPath);
        symbolIndex.initialize();

        // Generate test data
        random = new Random(42);
        generateTestSymbols();

        // Prepare search queries
        prepareSearchQueries();
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        if (symbolIndex != null) {
            symbolIndex.close();
        }
        // Clean up temp directory
        deleteRecursively(tempDir);
    }

    /**
     * Benchmark empty query (returns all symbols).
     */
    @Benchmark
    public void benchmarkSearchAll(Blackhole blackhole) {
        List<SymbolInfo> results = symbolIndex.search("").collect(Collectors.toList());
        blackhole.consume(results);
    }

    /**
     * Benchmark prefix search with common prefix.
     */
    @Benchmark
    public void benchmarkSearchCommonPrefix(Blackhole blackhole) {
        List<SymbolInfo> results = symbolIndex.search("Class").collect(Collectors.toList());
        blackhole.consume(results);
    }

    /**
     * Benchmark search for specific symbol.
     */
    @Benchmark
    public void benchmarkSearchSpecific(Blackhole blackhole) {
        List<SymbolInfo> results =
                symbolIndex.search("Class500_method10").collect(Collectors.toList());
        blackhole.consume(results);
    }

    /**
     * Benchmark random searches.
     */
    @Benchmark
    public void benchmarkSearchRandom(Blackhole blackhole) {
        String query = searchQueries.get(random.nextInt(searchQueries.size()));
        List<SymbolInfo> results = symbolIndex.search(query).collect(Collectors.toList());
        blackhole.consume(results);
    }

    /**
     * Benchmark multiple searches in sequence.
     */
    @Benchmark
    @OperationsPerInvocation(10)
    public void benchmarkSearchMultiple(Blackhole blackhole) {
        for (int i = 0; i < 10; i++) {
            String query = searchQueries.get(i % searchQueries.size());
            List<SymbolInfo> results = symbolIndex.search(query).collect(Collectors.toList());
            blackhole.consume(results);
        }
    }

    /**
     * Benchmark adding new symbols.
     */
    @Benchmark
    public void benchmarkAddSymbol(Blackhole blackhole) {
        Path path = Path.of("/virtual/Benchmark.groovy");
        SymbolInfo symbol =
                new SymbolInfo("BenchmarkSymbol" + System.nanoTime(), SymbolKind.CLASS, path, 1, 1);
        symbolIndex.addSymbol(symbol);
        blackhole.consume(symbol);
    }

    private void generateTestSymbols() {
        int symbolCount = 0;

        for (int fileNum = 0; fileNum < TOTAL_FILES; fileNum++) {
            Path filePath =
                    Path.of("/virtual/src/main/groovy/com/example/File" + fileNum + ".groovy");
            int line = 1;

            // Generate classes
            for (int i = 0; i < 10 && symbolCount < TOTAL_SYMBOLS; i++) {
                String className = "Class" + fileNum + "_" + i;
                symbolIndex.addSymbol(
                        new SymbolInfo(className, SymbolKind.CLASS, filePath, line++, 1));
                symbolCount++;

                // Generate methods
                for (int j = 0; j < 5 && symbolCount < TOTAL_SYMBOLS; j++) {
                    String methodName = className + "_method" + j;
                    symbolIndex.addSymbol(
                            new SymbolInfo(methodName, SymbolKind.METHOD, filePath, line++, 5));
                    symbolCount++;
                }

                // Generate fields
                for (int j = 0; j < 3 && symbolCount < TOTAL_SYMBOLS; j++) {
                    String fieldName = className + "_field" + j;
                    symbolIndex.addSymbol(
                            new SymbolInfo(fieldName, SymbolKind.FIELD, filePath, line++, 5));
                    symbolCount++;
                }
            }
        }
    }

    private void prepareSearchQueries() {
        searchQueries = new ArrayList<>();

        // Common prefixes
        searchQueries.add("Class");
        searchQueries.add("method");
        searchQueries.add("field");

        // Specific symbols
        searchQueries.add("Class100_method0");
        searchQueries.add("Class500_field2");
        searchQueries.add("Class999_method4");

        // Partial matches
        searchQueries.add("Class1");
        searchQueries.add("method2");
        searchQueries.add("field1");

        // Non-existing
        searchQueries.add("NonExisting");
    }

    private void deleteRecursively(Path path) throws Exception {
        if (Files.isDirectory(path)) {
            Files.list(path)
                    .forEach(
                            child -> {
                                try {
                                    deleteRecursively(child);
                                } catch (Exception e) {
                                    // Ignore
                                }
                            });
        }
        Files.deleteIfExists(path);
    }
}
