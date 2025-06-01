package com.groovy.lsp.benchmarks;

import com.groovy.lsp.groovy.core.api.CompilerConfigurationService;
import com.groovy.lsp.groovy.core.api.GroovyCoreFactory;
import com.groovy.lsp.groovy.core.api.IncrementalCompilationService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmark for code completion performance.
 * Measures the time to generate completion suggestions.
 */
@BenchmarkMode({Mode.AverageTime, Mode.SampleTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(
        value = 2,
        jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class CompletionBenchmark {

    private IncrementalCompilationService compilationService;
    private CompilerConfigurationService configService;
    private Path workspaceRoot;

    // Test scenarios
    private String simpleCompletion;
    private String methodCompletion;
    private String importCompletion;
    private String chainedCompletion;

    @Setup
    public void setup() throws Exception {
        workspaceRoot = Files.createTempDirectory("groovy-completion-benchmark");

        var factory = GroovyCoreFactory.getInstance();
        compilationService = factory.createIncrementalCompilationService();
        configService = factory.createCompilerConfigurationService();

        // Initialize test scenarios
        simpleCompletion =
                """
                class Test {
                    String name

                    void test() {
                        na| // cursor position
                    }
                }
                """;

        methodCompletion =
                """
                class Test {
                    String getName() { return "test" }
                    int getAge() { return 25 }

                    void test() {
                        this.get| // cursor position
                    }
                }
                """;

        importCompletion =
                """
                import java.util.|  // cursor position

                class Test {
                    void test() {
                    }
                }
                """;

        chainedCompletion =
                """
                class Test {
                    void test() {
                        "hello".toUpper|  // cursor position
                    }
                }
                """;
    }

    @Benchmark
    public void completeSimpleField(Blackhole bh) throws Exception {
        var completions = performCompletion(simpleCompletion);
        bh.consume(completions);
    }

    @Benchmark
    public void completeMethod(Blackhole bh) throws Exception {
        var completions = performCompletion(methodCompletion);
        bh.consume(completions);
    }

    @Benchmark
    public void completeImport(Blackhole bh) throws Exception {
        var completions = performCompletion(importCompletion);
        bh.consume(completions);
    }

    @Benchmark
    public void completeChainedCall(Blackhole bh) throws Exception {
        var completions = performCompletion(chainedCompletion);
        bh.consume(completions);
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void completeInLargeFile(Blackhole bh) throws Exception {
        String largeFile = generateLargeFile();
        var completions = performCompletion(largeFile);
        bh.consume(completions);
    }

    private Object performCompletion(String content) throws Exception {
        // Find cursor position
        int cursorPos = content.indexOf('|');
        if (cursorPos == -1) {
            throw new IllegalArgumentException("No cursor position marked with |");
        }

        // Remove cursor marker
        String code = content.substring(0, cursorPos) + content.substring(cursorPos + 1);

        // Compile and get completion suggestions
        var result = compilationService.compile(code, configService.getConfiguration()).get();

        // TODO: Implement actual completion service call
        // In a real implementation, we would:
        // 1. Extract the AST node at the cursor position
        // 2. Call the completion service with the AST context
        // 3. Generate and return completion suggestions
        // For benchmarking purposes, we currently only compile the code
        return result;
    }

    private String generateLargeFile() {
        StringBuilder sb = new StringBuilder();
        sb.append("package com.benchmark\n\n");

        // Add imports
        sb.append("import java.util.*\n");
        sb.append("import groovy.transform.*\n\n");

        // Generate multiple classes
        for (int i = 0; i < 10; i++) {
            sb.append("@CompileStatic\n");
            sb.append("class LargeClass").append(i).append(" {\n");

            // Add fields
            for (int j = 0; j < 20; j++) {
                sb.append("    private String field")
                        .append(j)
                        .append(" = 'value")
                        .append(j)
                        .append("'\n");
            }

            // Add methods
            for (int j = 0; j < 30; j++) {
                sb.append("    def method").append(j).append("(param").append(j).append(") {\n");
                sb.append("        return field0 + param").append(j).append("\n");
                sb.append("    }\n");
            }

            sb.append("}\n\n");
        }

        // Add completion point
        sb.append("class CompletionTest {\n");
        sb.append("    void test() {\n");
        sb.append("        LargeClass0 obj = new LargeClass0()\n");
        sb.append("        obj.meth|  // cursor position\n");
        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }

    @TearDown
    public void tearDown() throws Exception {
        Files.deleteIfExists(workspaceRoot);
    }
}
