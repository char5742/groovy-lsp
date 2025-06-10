package com.groovy.lsp.formatting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.googlejavaformat.java.FormatterException;
import com.groovy.lsp.formatting.options.FormatOptions;
import com.groovy.lsp.test.annotations.UnitTest;
import org.junit.jupiter.api.BeforeEach;

/**
 * Unit tests for GroovyFormatter
 */
class GroovyFormatterTest {

    private GroovyFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new GroovyFormatter();
    }

    @UnitTest
    void testFormatEmptyString() throws FormatterException {
        String result = formatter.format("");
        assertThat(result).isEmpty();
    }

    @UnitTest
    void testFormatNull() throws FormatterException {
        String result = formatter.format(null);
        assertThat(result).isNull();
    }

    @UnitTest
    void testFormatSimpleClass() throws FormatterException {
        String input = "class HelloWorld{def sayHello(){println 'Hello, World!'}}";
        String result = formatter.format(input);

        assertThat(result).isNotNull();
        assertThat(result).contains("class HelloWorld");
        assertThat(result).contains("def sayHello()");
    }

    @UnitTest
    void testFormatWithCustomOptions() throws FormatterException {
        FormatOptions options = FormatOptions.builder().indentSize(2).maxLineLength(80).build();

        GroovyFormatter customFormatter = new GroovyFormatter(options);

        String input =
                "def veryLongMethodNameThatShouldBeSplitAcrossMultipleLines(param1, param2, param3)"
                        + " { return param1 + param2 + param3 }";
        String result = customFormatter.format(input);

        assertThat(result).isNotNull();
    }

    @UnitTest
    void testFormatRangeWithInvalidParameters() {
        assertThatThrownBy(() -> formatter.formatRange("test", -1, 10))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> formatter.formatRange("test", 0, -1))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> formatter.formatRange("test", 10, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @UnitTest
    void testFormatGroovyClosure() throws FormatterException {
        String input = "def numbers = [1,2,3,4,5]; numbers.each{println it}";
        String result = formatter.format(input);

        assertThat(result).isNotNull();
        assertThat(result).contains("numbers.each");
    }

    @UnitTest
    void testFormatGroovyGString() throws FormatterException {
        String input = "def name='World';def greeting=\"Hello, ${name}!\"";
        String result = formatter.format(input);

        assertThat(result).isNotNull();
        assertThat(result).contains("${name}");
    }

    @UnitTest
    void testFormatTripleQuotedString() throws FormatterException {
        String input = "def text='''This is a\nmulti-line\nstring'''";
        String result = formatter.format(input);

        assertThat(result).isNotNull();
        assertThat(result).contains("'''");
    }
}
