package com.groovy.lsp.workspace.internal.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.groovy.lsp.workspace.api.dto.SymbolInfo;
import com.groovy.lsp.workspace.api.dto.SymbolKind;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GroovyFileParserTest {

    @TempDir Path tempDir;

    private GroovyFileParser parser;

    @BeforeEach
    void setUp() {
        parser = new GroovyFileParser();
    }

    @Test
    void testParseSimpleClass() throws IOException {
        String groovyCode =
                """
                package com.example

                class SimpleClass {
                    String name

                    SimpleClass(String name) {
                        this.name = name
                    }

                    def sayHello() {
                        println "Hello, $name"
                    }
                }
                """;

        Path file = tempDir.resolve("SimpleClass.groovy");
        Files.writeString(file, groovyCode);

        List<SymbolInfo> symbols = parser.parseFile(file);

        assertThat(symbols).hasSize(4);

        SymbolInfo classSymbol =
                symbols.stream()
                        .filter(s -> s.name().equals("com.example.SimpleClass"))
                        .findFirst()
                        .orElseThrow();
        assertThat(classSymbol.kind()).isEqualTo(SymbolKind.CLASS);

        SymbolInfo propertySymbol =
                symbols.stream().filter(s -> s.name().endsWith(".name")).findFirst().orElseThrow();
        assertThat(propertySymbol.kind()).isEqualTo(SymbolKind.PROPERTY);

        SymbolInfo constructorSymbol =
                symbols.stream()
                        .filter(s -> s.name().endsWith(".<init>"))
                        .findFirst()
                        .orElseThrow();
        assertThat(constructorSymbol.kind()).isEqualTo(SymbolKind.CONSTRUCTOR);

        SymbolInfo methodSymbol =
                symbols.stream()
                        .filter(s -> s.name().endsWith(".sayHello"))
                        .findFirst()
                        .orElseThrow();
        assertThat(methodSymbol.kind()).isEqualTo(SymbolKind.METHOD);
    }

    @Test
    void testParseInterface() throws IOException {
        String groovyCode =
                """
                interface MyInterface {
                    void doSomething()
                }
                """;

        Path file = tempDir.resolve("MyInterface.groovy");
        Files.writeString(file, groovyCode);

        List<SymbolInfo> symbols = parser.parseFile(file);

        assertThat(symbols).hasSize(2);

        SymbolInfo interfaceSymbol =
                symbols.stream()
                        .filter(s -> s.name().equals("MyInterface"))
                        .findFirst()
                        .orElseThrow();
        assertThat(interfaceSymbol.kind()).isEqualTo(SymbolKind.INTERFACE);
    }

    @Test
    void testParseTrait() throws IOException {
        String groovyCode =
                """
                trait MyTrait {
                    String property
                    abstract void abstractMethod()
                    void concreteMethod() {
                        println "Concrete"
                    }
                }
                """;

        Path file = tempDir.resolve("MyTrait.groovy");
        Files.writeString(file, groovyCode);

        List<SymbolInfo> symbols = parser.parseFile(file);

        System.out.println("Trait parsing - Parsed symbols: " + symbols);
        symbols.forEach(s -> System.out.println("  - " + s.name() + " (" + s.kind() + ")"));

        assertThat(symbols).isNotEmpty();

        SymbolInfo traitSymbol =
                symbols.stream().filter(s -> s.name().equals("MyTrait")).findFirst().orElseThrow();
        assertThat(traitSymbol.kind()).isEqualTo(SymbolKind.TRAIT);
    }

    @Test
    void testParseEnum() throws IOException {
        String groovyCode =
                """
                enum Color {
                    RED, GREEN, BLUE
                }
                """;

        Path file = tempDir.resolve("Color.groovy");
        Files.writeString(file, groovyCode);

        List<SymbolInfo> symbols = parser.parseFile(file);

        assertThat(symbols).isNotEmpty();

        SymbolInfo enumSymbol =
                symbols.stream().filter(s -> s.name().equals("Color")).findFirst().orElseThrow();
        assertThat(enumSymbol.kind()).isEqualTo(SymbolKind.ENUM);
    }

    @Test
    void testParseClosures() throws IOException {
        String groovyCode =
                """
                class ClosureExample {
                    def closure = { String name ->
                        println name
                    }

                    def method() {
                        [1, 2, 3].each { num ->
                            println num
                        }
                    }
                }
                """;

        Path file = tempDir.resolve("ClosureExample.groovy");
        Files.writeString(file, groovyCode);

        List<SymbolInfo> symbols = parser.parseFile(file);

        long closureCount = symbols.stream().filter(s -> s.kind() == SymbolKind.CLOSURE).count();
        assertThat(closureCount).isGreaterThanOrEqualTo(2);
    }

    @Test
    void testParseInvalidFile() throws IOException {
        String invalidCode =
                """
                class {  // Missing class name
                    def method() {
                }  // Missing closing brace
                """;

        Path file = tempDir.resolve("Invalid.groovy");
        Files.writeString(file, invalidCode);

        List<SymbolInfo> symbols = assertDoesNotThrow(() -> parser.parseFile(file));
        assertThat(symbols).isEmpty();
    }

    @Test
    void testParseProperties() throws IOException {
        String groovyCode =
                """
                class PropertyExample {
                    String simpleProperty
                    private int privateField = 10
                    static final String CONSTANT = "CONST"

                    def getComputedProperty() {
                        return "computed"
                    }
                }
                """;

        Path file = tempDir.resolve("PropertyExample.groovy");
        Files.writeString(file, groovyCode);

        List<SymbolInfo> symbols = parser.parseFile(file);

        long propertyCount =
                symbols.stream()
                        .filter(
                                s ->
                                        s.kind() == SymbolKind.PROPERTY
                                                || s.kind() == SymbolKind.FIELD)
                        .count();
        assertThat(propertyCount).isGreaterThanOrEqualTo(3);
    }

    @Test
    void testParseAnnotation() throws IOException {
        String groovyCode =
                """
                import java.lang.annotation.*

                @Target(ElementType.TYPE)
                @Retention(RetentionPolicy.RUNTIME)
                @interface MyAnnotation {
                    String value() default ""
                }
                """;

        Path file = tempDir.resolve("MyAnnotation.groovy");
        Files.writeString(file, groovyCode);

        List<SymbolInfo> symbols = parser.parseFile(file);

        assertThat(symbols).isNotEmpty();

        SymbolInfo annotationSymbol =
                symbols.stream()
                        .filter(s -> s.name().equals("MyAnnotation"))
                        .findFirst()
                        .orElseThrow();
        assertThat(annotationSymbol.kind()).isEqualTo(SymbolKind.ANNOTATION);
    }

    @Test
    void testParseNonExistentFile() throws IOException {
        Path nonExistentFile = tempDir.resolve("NonExistent.groovy");

        List<SymbolInfo> symbols = parser.parseFile(nonExistentFile);

        assertThat(symbols).isEmpty();
    }

    @Test
    void testParseLargeFile() throws IOException {
        // Create a file larger than MAX_FILE_SIZE (10MB)
        StringBuilder largeContent = new StringBuilder();
        String line = "class Test { def method() { println 'x'.repeat(1000) } }\n";

        // Each line is about 60 bytes, need ~175,000 lines for > 10MB
        for (int i = 0; i < 180_000; i++) {
            largeContent.append(line);
        }

        Path largeFile = tempDir.resolve("LargeFile.groovy");
        Files.writeString(largeFile, largeContent.toString());

        List<SymbolInfo> symbols = parser.parseFile(largeFile);

        assertThat(symbols).isEmpty();
    }
}
