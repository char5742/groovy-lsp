package com.groovy.lsp.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import com.groovy.lsp.workspace.api.dto.SymbolInfo;
import com.groovy.lsp.workspace.api.dto.SymbolKind;
import com.groovy.lsp.workspace.internal.index.SymbolIndex;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

/**
 * Performance tests for the workspace index module.
 * Tests that search operations complete within 50ms for 100,000 lines of code.
 */
public class PerformanceTest {

    private static final int TOTAL_LINES = 100_000;
    private static final int CLASSES_PER_FILE = 10;
    private static final int METHODS_PER_CLASS = 20;
    private static final int FIELDS_PER_CLASS = 10;
    private static final int LINES_PER_METHOD = 5;
    private static final int LINES_PER_FIELD = 1;
    private static final int LINES_PER_CLASS_HEADER = 3;

    // Calculate required files based on lines per file
    private static final int LINES_PER_FILE =
            CLASSES_PER_FILE
                    * (LINES_PER_CLASS_HEADER
                            + METHODS_PER_CLASS * LINES_PER_METHOD
                            + FIELDS_PER_CLASS * LINES_PER_FIELD);
    private static final int TOTAL_FILES = TOTAL_LINES / LINES_PER_FILE;

    // Performance thresholds
    private static final long MAX_SEARCH_TIME_MS = 50;
    private static final int WARMUP_ITERATIONS = 5;
    private static final int TEST_ITERATIONS = 10;

    @TempDir private Path tempDir;

    private SymbolIndex symbolIndex;
    private List<SymbolInfo> testSymbols;
    private Random random;

    @BeforeEach
    void setUp() throws Exception {
        Path indexPath = tempDir.resolve("index");
        Files.createDirectories(indexPath);

        symbolIndex = new SymbolIndex(indexPath);
        symbolIndex.initialize();

        testSymbols = new ArrayList<>();
        random = new Random(42); // Fixed seed for reproducible tests

        // Generate test data
        generateTestData();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (symbolIndex != null) {
            symbolIndex.close();
        }
    }

    /**
     * Tests that symbol search completes within 50ms for 100,000 lines of code.
     * Disabled in CI environment due to resource constraints.
     */
    @Test
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
    void testSearchPerformance() {
        // Warmup - perform searches to warm up caches
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            performRandomSearches(10);
        }

        // Actual performance test
        List<Long> searchTimes = new ArrayList<>();

        for (int i = 0; i < TEST_ITERATIONS; i++) {
            // Test different types of searches
            searchTimes.add(measureSearchTime("Class")); // Common prefix
            searchTimes.add(measureSearchTime("method")); // Common substring
            searchTimes.add(measureSearchTime("field")); // Another common substring
            searchTimes.add(measureSearchTime(getRandomSymbolName())); // Random symbol
            searchTimes.add(measureSearchTime("")); // Empty query (all symbols)
        }

        // Calculate statistics
        double averageTime = searchTimes.stream().mapToLong(Long::longValue).average().orElse(0);

        long maxTime = searchTimes.stream().mapToLong(Long::longValue).max().orElse(0);

        // Log results
        System.out.println("Performance Test Results:");
        System.out.println("  Total symbols indexed: " + testSymbols.size());
        System.out.println("  Average search time: " + String.format("%.2f", averageTime) + " ms");
        System.out.println("  Max search time: " + maxTime + " ms");
        System.out.println("  All search times: " + searchTimes);

        // Assert performance requirements
        assertThat(maxTime)
                .as("Maximum search time should be under %d ms", MAX_SEARCH_TIME_MS)
                .isLessThanOrEqualTo(MAX_SEARCH_TIME_MS);
    }

    /**
     * Tests index creation performance.
     * Disabled in CI environment due to resource constraints.
     */
    @Test
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
    void testIndexCreationPerformance() {
        // Create a new index
        Path newIndexPath = tempDir.resolve("new-index");
        SymbolIndex newIndex = new SymbolIndex(newIndexPath);
        newIndex.initialize();

        Instant start = Instant.now();

        // Add all symbols
        for (SymbolInfo symbol : testSymbols) {
            newIndex.addSymbol(symbol);
        }

        Duration indexingTime = Duration.between(start, Instant.now());

        System.out.println("Index Creation Performance:");
        System.out.println("  Total symbols: " + testSymbols.size());
        System.out.println("  Indexing time: " + indexingTime.toMillis() + " ms");
        System.out.println(
                "  Symbols per second: "
                        + String.format(
                                "%.0f", testSymbols.size() * 1000.0 / indexingTime.toMillis()));

        // Clean up
        try {
            newIndex.close();
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Tests concurrent search performance.
     * Disabled in CI environment due to resource constraints.
     */
    @Test
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
    void testConcurrentSearchPerformance() throws InterruptedException {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            performRandomSearches(10);
        }

        int threadCount = 4;
        int searchesPerThread = 25;
        List<Thread> threads = new ArrayList<>();
        List<Long> allSearchTimes = new ArrayList<>();
        Object lock = new Object();

        Instant start = Instant.now();

        // Create and start threads
        for (int t = 0; t < threadCount; t++) {
            Thread thread =
                    new Thread(
                            () -> {
                                List<Long> threadSearchTimes = new ArrayList<>();
                                for (int i = 0; i < searchesPerThread; i++) {
                                    threadSearchTimes.add(measureSearchTime(getRandomSymbolName()));
                                }
                                synchronized (lock) {
                                    allSearchTimes.addAll(threadSearchTimes);
                                }
                            });
            threads.add(thread);
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        Duration totalTime = Duration.between(start, Instant.now());

        // Calculate statistics
        double averageTime = allSearchTimes.stream().mapToLong(Long::longValue).average().orElse(0);

        long maxTime = allSearchTimes.stream().mapToLong(Long::longValue).max().orElse(0);

        System.out.println("Concurrent Search Performance:");
        System.out.println("  Threads: " + threadCount);
        System.out.println("  Total searches: " + allSearchTimes.size());
        System.out.println("  Total time: " + totalTime.toMillis() + " ms");
        System.out.println("  Average search time: " + String.format("%.2f", averageTime) + " ms");
        System.out.println("  Max search time: " + maxTime + " ms");

        // Assert performance requirements
        assertThat(maxTime)
                .as("Maximum concurrent search time should be under %d ms", MAX_SEARCH_TIME_MS)
                .isLessThanOrEqualTo(MAX_SEARCH_TIME_MS);
    }

    private void generateTestData() {
        System.out.println("Generating test data...");
        System.out.println("  Target lines: " + TOTAL_LINES);
        System.out.println("  Files to generate: " + TOTAL_FILES);
        System.out.println("  Classes per file: " + CLASSES_PER_FILE);
        System.out.println("  Methods per class: " + METHODS_PER_CLASS);
        System.out.println("  Fields per class: " + FIELDS_PER_CLASS);

        int currentLine = 1;

        for (int fileNum = 0; fileNum < TOTAL_FILES; fileNum++) {
            Path filePath =
                    Path.of("/virtual/src/main/groovy/com/example/File" + fileNum + ".groovy");

            for (int classNum = 0; classNum < CLASSES_PER_FILE; classNum++) {
                String className = "Class" + fileNum + "_" + classNum;

                // Add class symbol
                SymbolInfo classSymbol =
                        new SymbolInfo(className, SymbolKind.CLASS, filePath, currentLine, 1);
                testSymbols.add(classSymbol);
                symbolIndex.addSymbol(classSymbol);
                currentLine += LINES_PER_CLASS_HEADER;

                // Add fields
                for (int fieldNum = 0; fieldNum < FIELDS_PER_CLASS; fieldNum++) {
                    String fieldName = "field" + fieldNum;
                    SymbolInfo fieldSymbol =
                            new SymbolInfo(fieldName, SymbolKind.FIELD, filePath, currentLine, 5);
                    testSymbols.add(fieldSymbol);
                    symbolIndex.addSymbol(fieldSymbol);
                    currentLine += LINES_PER_FIELD;
                }

                // Add methods
                for (int methodNum = 0; methodNum < METHODS_PER_CLASS; methodNum++) {
                    String methodName = "method" + methodNum;
                    SymbolInfo methodSymbol =
                            new SymbolInfo(methodName, SymbolKind.METHOD, filePath, currentLine, 5);
                    testSymbols.add(methodSymbol);
                    symbolIndex.addSymbol(methodSymbol);
                    currentLine += LINES_PER_METHOD;
                }
            }
        }

        System.out.println("  Total symbols generated: " + testSymbols.size());
    }

    private long measureSearchTime(String query) {
        long start = System.nanoTime();
        var results = symbolIndex.search(query).collect(Collectors.toList());
        long end = System.nanoTime();

        return TimeUnit.NANOSECONDS.toMillis(end - start);
    }

    private void performRandomSearches(int count) {
        for (int i = 0; i < count; i++) {
            String query = getRandomSymbolName();
            var unused = symbolIndex.search(query).collect(Collectors.toList());
        }
    }

    private String getRandomSymbolName() {
        if (testSymbols.isEmpty()) {
            return "test";
        }
        return testSymbols.get(random.nextInt(testSymbols.size())).name();
    }
}
