package com.groovy.lsp.formatting.options;

import com.google.googlejavaformat.java.JavaFormatterOptions;

/**
 * Configuration options for Groovy code formatting.
 * Extends Google Java Format options with Groovy-specific settings.
 */
public class FormatOptions {

    /**
     * The underlying Google Java Format options
     */
    private final JavaFormatterOptions.Style style;

    /**
     * Whether to format Groovy closures on a single line when possible
     */
    private final boolean compactClosures;

    /**
     * Whether to preserve existing line breaks in Groovy code
     */
    private final boolean preserveLineBreaks;

    /**
     * Maximum line length for formatted code
     */
    private final int maxLineLength;

    /**
     * Indentation size in spaces
     */
    private final int indentSize;

    /**
     * Whether to use tabs for indentation
     */
    private final boolean useTabs;

    private FormatOptions(Builder builder) {
        this.style = builder.style;
        this.compactClosures = builder.compactClosures;
        this.preserveLineBreaks = builder.preserveLineBreaks;
        this.maxLineLength = builder.maxLineLength;
        this.indentSize = builder.indentSize;
        this.useTabs = builder.useTabs;
    }

    public JavaFormatterOptions.Style getStyle() {
        return style;
    }

    public boolean isCompactClosures() {
        return compactClosures;
    }

    public boolean isPreserveLineBreaks() {
        return preserveLineBreaks;
    }

    public int getMaxLineLength() {
        return maxLineLength;
    }

    public int getIndentSize() {
        return indentSize;
    }

    public boolean isUseTabs() {
        return useTabs;
    }

    /**
     * Creates a new builder with default options
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder initialized with the current options
     */
    public Builder toBuilder() {
        return new Builder()
                .style(style)
                .compactClosures(compactClosures)
                .preserveLineBreaks(preserveLineBreaks)
                .maxLineLength(maxLineLength)
                .indentSize(indentSize)
                .useTabs(useTabs);
    }

    /**
     * Builder for FormatOptions
     */
    public static class Builder {
        private JavaFormatterOptions.Style style = JavaFormatterOptions.Style.GOOGLE;
        private boolean compactClosures = false;
        private boolean preserveLineBreaks = false;
        private int maxLineLength = 100;
        private int indentSize = 4;
        private boolean useTabs = false;

        public Builder style(JavaFormatterOptions.Style style) {
            this.style = style;
            return this;
        }

        public Builder compactClosures(boolean compactClosures) {
            this.compactClosures = compactClosures;
            return this;
        }

        public Builder preserveLineBreaks(boolean preserveLineBreaks) {
            this.preserveLineBreaks = preserveLineBreaks;
            return this;
        }

        public Builder maxLineLength(int maxLineLength) {
            if (maxLineLength <= 0) {
                throw new IllegalArgumentException("Max line length must be positive");
            }
            this.maxLineLength = maxLineLength;
            return this;
        }

        public Builder indentSize(int indentSize) {
            if (indentSize <= 0) {
                throw new IllegalArgumentException("Indent size must be positive");
            }
            this.indentSize = indentSize;
            return this;
        }

        public Builder useTabs(boolean useTabs) {
            this.useTabs = useTabs;
            return this;
        }

        public FormatOptions build() {
            return new FormatOptions(this);
        }
    }

    /**
     * Default format options for Groovy code
     */
    public static final FormatOptions DEFAULT =
            builder()
                    .style(JavaFormatterOptions.Style.GOOGLE)
                    .compactClosures(false)
                    .preserveLineBreaks(false)
                    .maxLineLength(100)
                    .indentSize(4)
                    .useTabs(false)
                    .build();
}
