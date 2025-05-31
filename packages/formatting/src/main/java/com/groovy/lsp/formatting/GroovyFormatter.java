package com.groovy.lsp.formatting;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.google.googlejavaformat.java.JavaFormatterOptions;
import com.groovy.lsp.formatting.options.FormatOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jspecify.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * Main formatter for Groovy source code.
 * Extends Google Java Format with Groovy-specific formatting rules.
 */
public class GroovyFormatter {
    
    private static final Logger logger = LoggerFactory.getLogger(GroovyFormatter.class);
    
    /**
     * Pattern to identify Groovy-specific syntax elements
     */
    private static final Pattern CLOSURE_PATTERN = Pattern.compile("\\{[^}]*\\}");
    private static final Pattern TRIPLE_QUOTE_PATTERN = Pattern.compile("'''.*?'''|\"\"\".*?\"\"\"", Pattern.DOTALL);
    private static final Pattern GSTRING_PATTERN = Pattern.compile("\"[^\"]*\\$\\{[^}]*\\}[^\"]*\"");
    
    private final Formatter javaFormatter;
    private final FormatOptions options;
    
    /**
     * Creates a new GroovyFormatter with default options
     */
    public GroovyFormatter() {
        this(FormatOptions.DEFAULT);
    }
    
    /**
     * Creates a new GroovyFormatter with the specified options
     */
    public GroovyFormatter(FormatOptions options) {
        this.options = options;
        this.javaFormatter = createJavaFormatter(options);
    }
    
    /**
     * Formats the given Groovy source code
     * 
     * @param source the source code to format
     * @return the formatted source code
     * @throws FormatterException if the source code cannot be formatted
     */
    public String format(@Nullable String source) throws FormatterException {
        if (source == null || source.trim().isEmpty()) {
            return source != null ? source : "";
        }
        
        logger.debug("Formatting Groovy source code");
        
        try {
            // Pre-process Groovy-specific syntax
            String preprocessed = preprocessGroovySyntax(source);
            
            // Format using Google Java Format
            String formatted = javaFormatter.formatSource(preprocessed);
            
            // Post-process to restore Groovy-specific formatting
            String postprocessed = postprocessGroovySyntax(formatted);
            
            // Apply additional Groovy-specific formatting rules
            return applyGroovyFormattingRules(postprocessed);
            
        } catch (FormatterException e) {
            logger.error("Failed to format Groovy source code", e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during formatting", e);
            throw new FormatterException("Failed to format Groovy code: " + e.getMessage());
        }
    }
    
    /**
     * Formats a specific range within the source code
     * 
     * @param source the source code
     * @param offset the starting offset of the range to format
     * @param length the length of the range to format
     * @return the formatted source code
     * @throws FormatterException if the source code cannot be formatted
     */
    public String formatRange(@Nullable String source, int offset, int length) throws FormatterException {
        if (source == null || offset < 0 || length < 0 || offset + length > source.length()) {
            throw new IllegalArgumentException("Invalid range parameters");
        }
        
        logger.debug("Formatting range: offset={}, length={}", offset, length);
        
        // For now, format the entire source
        // TODO: Implement proper range formatting
        return format(source);
    }
    
    /**
     * Pre-processes Groovy-specific syntax before Java formatting
     */
    private String preprocessGroovySyntax(String source) {
        // Protect triple-quoted strings from formatting
        source = protectTripleQuotedStrings(source);
        
        // Protect GStrings with interpolations
        source = protectGStrings(source);
        
        // Handle Groovy-specific keywords and operators
        source = handleGroovyKeywords(source);
        
        return source;
    }
    
    /**
     * Post-processes the formatted code to restore Groovy-specific syntax
     */
    private String postprocessGroovySyntax(String formatted) {
        // Restore triple-quoted strings
        formatted = restoreTripleQuotedStrings(formatted);
        
        // Restore GStrings
        formatted = restoreGStrings(formatted);
        
        return formatted;
    }
    
    /**
     * Applies additional Groovy-specific formatting rules
     */
    private String applyGroovyFormattingRules(String source) {
        // Format closures according to options
        if (options.isCompactClosures()) {
            source = formatCompactClosures(source);
        }
        
        // Handle Groovy method calls without parentheses
        source = formatMethodCallsWithoutParentheses(source);
        
        // Format Groovy properties and field access
        source = formatPropertyAccess(source);
        
        return source;
    }
    
    /**
     * Creates a Java formatter with the appropriate options
     */
    private static Formatter createJavaFormatter(FormatOptions options) {
        JavaFormatterOptions.Builder builder = JavaFormatterOptions.builder()
            .style(options.getStyle());
        
        // Apply custom settings if available
        JavaFormatterOptions javaOptions = builder.build();
        
        return new Formatter(javaOptions);
    }
    
    // Placeholder methods for Groovy-specific formatting
    // These would contain the actual implementation
    
    private String protectTripleQuotedStrings(String source) {
        // Simple protection: check if triple-quoted strings exist
        if (TRIPLE_QUOTE_PATTERN.matcher(source).find()) {
            logger.debug("Found triple-quoted strings in source");
        }
        return source;
    }
    
    private String restoreTripleQuotedStrings(String source) {
        // TODO: Implement restoration of triple-quoted strings
        return source;
    }
    
    private String protectGStrings(String source) {
        // Simple protection: check if GStrings exist
        if (GSTRING_PATTERN.matcher(source).find()) {
            logger.debug("Found GString interpolations in source");
        }
        return source;
    }
    
    private String restoreGStrings(String source) {
        // TODO: Implement restoration of GStrings
        return source;
    }
    
    private String handleGroovyKeywords(String source) {
        // TODO: Handle def, in, as, etc.
        return source;
    }
    
    private String formatCompactClosures(String source) {
        // Simple closure detection for logging
        if (CLOSURE_PATTERN.matcher(source).find()) {
            logger.debug("Found closures in source for compact formatting");
        }
        return source;
    }
    
    private String formatMethodCallsWithoutParentheses(String source) {
        // TODO: Implement formatting for method calls without parentheses
        return source;
    }
    
    private String formatPropertyAccess(String source) {
        // TODO: Implement property access formatting
        return source;
    }
}