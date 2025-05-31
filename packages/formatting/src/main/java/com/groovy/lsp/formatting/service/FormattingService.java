package com.groovy.lsp.formatting.service;

import com.google.googlejavaformat.java.FormatterException;
import com.groovy.lsp.formatting.GroovyFormatter;
import com.groovy.lsp.formatting.options.FormatOptions;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * LSP formatting service implementation for Groovy code.
 * Handles document formatting and range formatting requests.
 */
public class FormattingService {
    
    private static final Logger logger = LoggerFactory.getLogger(FormattingService.class);
    
    
    /**
     * Creates a new FormattingService with default options
     */
    public FormattingService() {
        this(FormatOptions.DEFAULT);
    }
    
    /**
     * Creates a new FormattingService with the specified options
     */
    public FormattingService(FormatOptions options) {
        // Options will be used when creating formatters for specific requests
    }
    
    /**
     * Formats an entire document
     * 
     * @param params the formatting parameters including document URI and options
     * @param documentContent the current content of the document
     * @return a future containing the list of text edits to apply
     */
    public CompletableFuture<List<TextEdit>> formatDocument(
            DocumentFormattingParams params, 
            String documentContent) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Formatting document: {}", params.getTextDocument().getUri());
                
                // Convert LSP formatting options to our format options
                FormatOptions formatOptions = convertFormattingOptions(params.getOptions());
                
                // Create formatter with the specified options
                GroovyFormatter customFormatter = new GroovyFormatter(formatOptions);
                
                // Format the entire document
                String formatted = customFormatter.format(documentContent);
                
                // If formatting returns null, return empty list
                if (formatted == null) {
                    logger.warn("Formatter returned null for document");
                    return Collections.emptyList();
                }
                
                // Create a text edit that replaces the entire document
                TextEdit edit = createFullDocumentEdit(documentContent, formatted);
                
                return Collections.singletonList(edit);
                
            } catch (FormatterException e) {
                logger.error("Failed to format document", e);
                return Collections.emptyList();
            } catch (Exception e) {
                logger.error("Unexpected error during formatting", e);
                return Collections.emptyList();
            }
        });
    }
    
    /**
     * Formats a specific range within a document
     * 
     * @param params the range formatting parameters
     * @param documentContent the current content of the document
     * @return a future containing the list of text edits to apply
     */
    public CompletableFuture<List<TextEdit>> formatRange(
            DocumentRangeFormattingParams params,
            String documentContent) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Formatting range in document: {}", params.getTextDocument().getUri());
                
                Range range = params.getRange();
                logger.debug("Range: start={}:{}, end={}:{}", 
                    range.getStart().getLine(), range.getStart().getCharacter(),
                    range.getEnd().getLine(), range.getEnd().getCharacter());
                
                // Convert LSP formatting options
                FormatOptions formatOptions = convertFormattingOptions(params.getOptions());
                GroovyFormatter customFormatter = new GroovyFormatter(formatOptions);
                
                // Calculate offset and length from range
                int offset = calculateOffset(documentContent, range.getStart());
                int endOffset = calculateOffset(documentContent, range.getEnd());
                int length = endOffset - offset;
                
                // Format the range
                String formatted = customFormatter.formatRange(documentContent, offset, length);
                
                // If formatting returns null, return empty list
                if (formatted == null) {
                    logger.warn("Formatter returned null for range");
                    return Collections.emptyList();
                }
                
                // Create text edit for the formatted range
                TextEdit edit = createRangeEdit(documentContent, formatted, range);
                
                return Collections.singletonList(edit);
                
            } catch (Exception e) {
                logger.error("Failed to format range", e);
                return Collections.emptyList();
            }
        });
    }
    
    /**
     * Converts LSP FormattingOptions to our FormatOptions
     */
    private FormatOptions convertFormattingOptions(FormattingOptions lspOptions) {
        FormatOptions.Builder builder = FormatOptions.builder();
        
        // Tab size and spaces/tabs preference
        builder.indentSize(lspOptions.getTabSize());
        builder.useTabs(!lspOptions.isInsertSpaces());
        
        // Additional options can be extracted from custom properties
        // For example, style preference, line length, etc.
        
        return builder.build();
    }
    
    /**
     * Creates a text edit that replaces the entire document
     */
    private TextEdit createFullDocumentEdit(String original, String formatted) {
        // Calculate the range of the entire document
        int lines = countLines(original);
        int lastLineLength = getLastLineLength(original);
        
        Range fullRange = new Range(
            new Position(0, 0),
            new Position(lines - 1, lastLineLength)
        );
        
        return new TextEdit(fullRange, formatted);
    }
    
    /**
     * Creates a text edit for a specific range
     */
    private TextEdit createRangeEdit(String original, String formatted, Range range) {
        // Log range information for debugging
        logger.debug("Creating range edit for range: start={}:{}, end={}:{}", 
            range.getStart().getLine(), range.getStart().getCharacter(),
            range.getEnd().getLine(), range.getEnd().getCharacter());
        
        // For now, return a full document edit
        // TODO: Implement proper range extraction based on range parameter
        return createFullDocumentEdit(original, formatted);
    }
    
    /**
     * Calculates the character offset for a given position
     */
    private int calculateOffset(String content, Position position) {
        String[] lines = content.split("\n", -1);
        int offset = 0;
        
        for (int i = 0; i < position.getLine() && i < lines.length; i++) {
            offset += lines[i].length() + 1; // +1 for newline
        }
        
        if (position.getLine() < lines.length) {
            offset += Math.min(position.getCharacter(), lines[position.getLine()].length());
        }
        
        return offset;
    }
    
    /**
     * Counts the number of lines in the content
     */
    private int countLines(String content) {
        if (content.isEmpty()) {
            return 1;
        }
        
        int lines = 1;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                lines++;
            }
        }
        
        return lines;
    }
    
    /**
     * Gets the length of the last line
     */
    private int getLastLineLength(String content) {
        int lastNewline = content.lastIndexOf('\n');
        if (lastNewline == -1) {
            return content.length();
        }
        return content.length() - lastNewline - 1;
    }
}