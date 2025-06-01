package com.groovy.lsp.benchmarks;

import com.groovy.lsp.formatting.GroovyFormatter;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Groovyコードフォーマッティングのパフォーマンスを測定
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Fork(
        value = 2,
        jvmArgs = {"-Xms1G", "-Xmx1G"})
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
public class FormattingBenchmark {

    private GroovyFormatter formatter;

    private String simpleCode;
    private String complexCode;
    private String badlyFormattedCode;

    @Setup
    public void setup() {
        formatter = new GroovyFormatter();

        simpleCode =
                """
                class SimpleClass {
                    def method() {
                        return "Hello World"
                    }
                }
                """;

        complexCode =
                """
                @CompileStatic
                class ComplexService implements ServiceInterface {
                    private final Repository repository
                    private final Logger logger = LoggerFactory.getLogger(ComplexService)

                    @Autowired
                    ComplexService(Repository repository) {
                        this.repository = repository
                    }

                    @Transactional
                    def processData(List<Map<String, Object>> data) {
                        logger.info("Processing ${data.size()} items")

                        data.each { item ->
                            try {
                                def entity = new Entity(
                                    name: item.name,
                                    value: item.value,
                                    timestamp: new Date()
                                )

                                if (validate(entity)) {
                                    repository.save(entity)
                                } else {
                                    logger.warn("Invalid entity: ${entity}")
                                }
                            } catch (Exception e) {
                                logger.error("Error processing item: ${item}", e)
                                throw new ProcessingException("Failed to process item", e)
                            }
                        }

                        return data.size()
                    }

                    private boolean validate(Entity entity) {
                        return entity.name && entity.value > 0
                    }
                }
                """;

        badlyFormattedCode =
                """
                class    BadlyFormatted    {
                       def    field1   =   123
                         def     field2='hello'


                    def    method1(   param1,param2,    param3   )   {
                        if(param1>0){
                            println    "Positive"
                        }else{
                            println   "Non-positive"
                        }


                        def    result=param1+param2   *   param3
                          return    result
                    }


                        def method2(){[1,2,3,4,5].collect{it*2}.findAll{it>5}}
                }
                """;
    }

    @Benchmark
    public void formatSimpleCode(Blackhole bh) throws Exception {
        String formatted = formatter.format(simpleCode).get();
        bh.consume(formatted);
    }

    @Benchmark
    public void formatComplexCode(Blackhole bh) throws Exception {
        String formatted = formatter.format(complexCode).get();
        bh.consume(formatted);
    }

    @Benchmark
    public void formatBadlyFormattedCode(Blackhole bh) throws Exception {
        String formatted = formatter.format(badlyFormattedCode).get();
        bh.consume(formatted);
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    public void formatWithCustomOptions(Blackhole bh) throws Exception {
        var options = new com.groovy.lsp.formatting.options.FormatOptions();
        options.setIndentSize(2);
        options.setMaxLineLength(80);

        String formatted = formatter.format(complexCode, options).get();
        bh.consume(formatted);
    }
}
