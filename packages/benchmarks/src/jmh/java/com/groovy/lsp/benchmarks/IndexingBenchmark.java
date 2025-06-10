package com.groovy.lsp.benchmarks;

import com.groovy.lsp.shared.workspace.api.WorkspaceIndexService;
import com.groovy.lsp.workspace.api.WorkspaceIndexFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmark for workspace indexing performance.
 * Measures the time to index Groovy files and search symbols.
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(
        value = 2,
        jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class IndexingBenchmark {

    private WorkspaceIndexService indexService;
    private Path workspaceRoot;
    private Path[] groovyFiles;

    @Param({"10", "50", "100"})
    private int fileCount;

    @Setup
    public void setup() throws Exception {
        // Create workspace directory
        workspaceRoot = Files.createTempDirectory("groovy-index-benchmark");

        // Initialize index service
        indexService =
                WorkspaceIndexFactory.getInstance().createIndexService(workspaceRoot.toString());

        // Create Groovy files
        groovyFiles = new Path[fileCount];
        for (int i = 0; i < fileCount; i++) {
            Path file = workspaceRoot.resolve("Class" + i + ".groovy");
            Files.writeString(file, generateClassFile(i));
            groovyFiles[i] = file;
        }
    }

    @Benchmark
    public void indexWorkspace(Blackhole bh) throws Exception {
        // Clear index before each run
        indexService.clearIndex().get();

        // Index all files
        var future = indexService.indexWorkspace();
        var result = future.get();

        bh.consume(result);
    }

    @Benchmark
    public void indexSingleFile(Blackhole bh) throws Exception {
        // Index a single file
        var future = indexService.indexFile(groovyFiles[0].toString());
        var result = future.get();

        bh.consume(result);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void searchSymbols(Blackhole bh) throws Exception {
        // Ensure workspace is indexed
        if (!isIndexed()) {
            indexService.indexWorkspace().get();
        }

        // Search for symbols
        var results = indexService.searchSymbols("method").get();
        bh.consume(results);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void getSymbolByName(Blackhole bh) throws Exception {
        // Ensure workspace is indexed
        if (!isIndexed()) {
            indexService.indexWorkspace().get();
        }

        // Get specific symbol
        var symbol = indexService.getSymbol("Class0").get();
        bh.consume(symbol);
    }

    private boolean isIndexed() {
        try {
            return !indexService.searchSymbols("Class0").get().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private String generateClassFile(int index) {
        return """
        package com.benchmark

        import groovy.transform.CompileStatic

        @CompileStatic
        class Class%d {
            private String field1 = "value1"
            private int field2 = %d

            String method1(String param) {
                return param + field1
            }

            int method2(int param) {
                return param + field2
            }

            void method3() {
                println "Class %d method"
            }

            static void staticMethod() {
                println "Static method"
            }
        }
        """
                .formatted(index, index, index);
    }

    @TearDown
    public void tearDown() throws Exception {
        // Clean up index
        indexService.clearIndex().get();

        // Delete files
        for (Path file : groovyFiles) {
            Files.deleteIfExists(file);
        }
        Files.deleteIfExists(workspaceRoot);
    }
}
