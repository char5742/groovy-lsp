package com.groovy.lsp.benchmarks;

import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.groovy.core.api.GroovyCoreFactory;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Groovyファイルのパースパフォーマンスを測定
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class ParsingBenchmark {
    
    private ASTService astService;
    private Path smallFile;
    private Path mediumFile;
    private Path largeFile;
    
    @Setup
    public void setup() throws Exception {
        astService = GroovyCoreFactory.getInstance().createASTService();
        
        // テスト用ファイルの準備
        Path tempDir = Files.createTempDirectory("groovy-benchmark");
        
        // Small file (< 100 lines)
        smallFile = tempDir.resolve("Small.groovy");
        Files.writeString(smallFile, generateGroovyCode(50));
        
        // Medium file (~ 500 lines)
        mediumFile = tempDir.resolve("Medium.groovy");
        Files.writeString(mediumFile, generateGroovyCode(500));
        
        // Large file (> 1000 lines)
        largeFile = tempDir.resolve("Large.groovy");
        Files.writeString(largeFile, generateGroovyCode(1500));
    }
    
    @Benchmark
    public void parseSmallFile(Blackhole bh) throws Exception {
        Object ast = astService.parseFile(smallFile.toString()).get();
        bh.consume(ast);
    }
    
    @Benchmark
    public void parseMediumFile(Blackhole bh) throws Exception {
        Object ast = astService.parseFile(mediumFile.toString()).get();
        bh.consume(ast);
    }
    
    @Benchmark
    public void parseLargeFile(Blackhole bh) throws Exception {
        Object ast = astService.parseFile(largeFile.toString()).get();
        bh.consume(ast);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void parseString(Blackhole bh) throws Exception {
        String code = """
            class BenchmarkClass {
                def method(param) {
                    return param * 2
                }
            }
            """;
        Object ast = astService.parseString(code).get();
        bh.consume(ast);
    }
    
    private String generateGroovyCode(int lines) {
        StringBuilder sb = new StringBuilder();
        sb.append("package com.benchmark\n\n");
        
        int classCount = lines / 50;
        int methodsPerClass = 10;
        
        for (int i = 0; i < classCount; i++) {
            sb.append("class GeneratedClass").append(i).append(" {\n");
            
            // Fields
            for (int j = 0; j < 5; j++) {
                sb.append("    private def field").append(j).append(" = ").append(j).append("\n");
            }
            
            // Methods
            for (int j = 0; j < methodsPerClass; j++) {
                sb.append("    def method").append(j).append("(param").append(j).append(") {\n");
                sb.append("        def result = param").append(j).append(" * field0\n");
                sb.append("        println \"Result: ${result}\"\n");
                sb.append("        return result\n");
                sb.append("    }\n\n");
            }
            
            sb.append("}\n\n");
        }
        
        return sb.toString();
    }
    
    @TearDown
    public void tearDown() throws Exception {
        Files.deleteIfExists(smallFile);
        Files.deleteIfExists(mediumFile);
        Files.deleteIfExists(largeFile);
        Files.deleteIfExists(smallFile.getParent());
    }
}