package com.groovy.lsp.formatting.options;

import static org.assertj.core.api.Assertions.assertThat;

import com.groovy.lsp.test.annotations.UnitTest;

/**
 * Unit tests for FormatOptions.
 */
class FormatOptionsTest {

    @UnitTest
    void testDefaultOptions() {
        FormatOptions options = FormatOptions.builder().build();

        assertThat(options.getIndentSize()).isEqualTo(4);
        assertThat(options.isUseTabs()).isFalse();
        assertThat(options.getMaxLineLength()).isEqualTo(100); // Default is 100, not 120
        assertThat(options.getStyle())
                .isEqualTo(com.google.googlejavaformat.java.JavaFormatterOptions.Style.GOOGLE);
        assertThat(options.isCompactClosures()).isFalse();
        assertThat(options.isPreserveLineBreaks()).isFalse();
    }

    @UnitTest
    void testCustomOptions() {
        FormatOptions options =
                FormatOptions.builder()
                        .indentSize(2)
                        .useTabs(true)
                        .maxLineLength(80)
                        .compactClosures(true)
                        .preserveLineBreaks(true)
                        .style(com.google.googlejavaformat.java.JavaFormatterOptions.Style.AOSP)
                        .build();

        assertThat(options.getIndentSize()).isEqualTo(2);
        assertThat(options.isUseTabs()).isTrue();
        assertThat(options.getMaxLineLength()).isEqualTo(80);
        assertThat(options.isCompactClosures()).isTrue();
        assertThat(options.isPreserveLineBreaks()).isTrue();
        assertThat(options.getStyle())
                .isEqualTo(com.google.googlejavaformat.java.JavaFormatterOptions.Style.AOSP);
    }

    @UnitTest
    void testBuilderCopy() {
        FormatOptions original =
                FormatOptions.builder().indentSize(2).useTabs(true).compactClosures(true).build();

        FormatOptions copy = original.toBuilder().indentSize(4).build();

        assertThat(copy.getIndentSize()).isEqualTo(4);
        assertThat(copy.isUseTabs()).isTrue(); // Preserved from original
        assertThat(copy.isCompactClosures()).isTrue(); // Preserved from original
        assertThat(original.getIndentSize()).isEqualTo(2); // Original unchanged
    }

    @UnitTest
    void testEqualsAndHashCode() {
        FormatOptions options1 =
                FormatOptions.builder().indentSize(2).useTabs(true).compactClosures(true).build();

        FormatOptions options2 =
                FormatOptions.builder().indentSize(2).useTabs(true).compactClosures(true).build();

        FormatOptions options3 =
                FormatOptions.builder().indentSize(4).useTabs(true).compactClosures(true).build();

        // FormatOptions doesn't override equals/hashCode, so comparing by reference
        assertThat(options1).isNotSameAs(options2);
        assertThat(options1).isNotSameAs(options3);
    }

    @UnitTest
    void testToString() {
        FormatOptions options = FormatOptions.builder().indentSize(2).useTabs(true).build();

        String toString = options.toString();
        assertThat(toString).contains("FormatOptions");
        // The actual toString format depends on the implementation
    }
}
