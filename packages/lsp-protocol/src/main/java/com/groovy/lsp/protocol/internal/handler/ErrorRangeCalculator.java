package com.groovy.lsp.protocol.internal.handler;

import com.groovy.lsp.groovy.core.api.CompilationResult.CompilationError;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

/**
 * Calculates more accurate error ranges for diagnostics.
 * This class analyzes error messages and source code to determine the actual span of errors.
 */
public class ErrorRangeCalculator {

    // Common patterns in Groovy error messages that indicate what token caused the error
    private static final Pattern TOKEN_PATTERN = Pattern.compile("unexpected token: (\\S+)");
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("'([^']+)'");
    private static final Pattern TYPE_PATTERN = Pattern.compile("\\[([^\\]]+)\\]");

    /**
     * Calculates the error range based on the error information and source code.
     *
     * @param error the compilation error
     * @param sourceCode the source code
     * @return the calculated range
     */
    public Range calculateRange(CompilationError error, String sourceCode) {
        // LSP uses 0-based indexing
        int startLine = error.getLine() - 1;
        int startColumn = error.getColumn() - 1;

        // Try to extract the actual token from the error message
        String errorToken = extractErrorToken(error.getMessage());

        // Get the line content
        String[] lines = sourceCode.split("\n", -1);
        if (startLine >= 0 && startLine < lines.length) {
            String line = lines[startLine];

            // Calculate end column based on the token or context
            int endColumn = calculateEndColumn(line, startColumn, errorToken);

            return new Range(
                    new Position(startLine, startColumn), new Position(startLine, endColumn));
        }

        // Fallback to default range
        return new Range(
                new Position(startLine, startColumn),
                new Position(startLine, Math.max(startColumn + 1, startColumn + 10)));
    }

    /**
     * Extracts the error token from the error message.
     *
     * @param errorMessage the error message
     * @return the extracted token, or null if not found
     */
    private String extractErrorToken(String errorMessage) {
        // Try to match "unexpected token: X"
        Matcher tokenMatcher = TOKEN_PATTERN.matcher(errorMessage);
        if (tokenMatcher.find()) {
            return tokenMatcher.group(1);
        }

        // Try to match quoted identifiers
        Matcher identifierMatcher = IDENTIFIER_PATTERN.matcher(errorMessage);
        if (identifierMatcher.find()) {
            return identifierMatcher.group(1);
        }

        // Try to match type references
        Matcher typeMatcher = TYPE_PATTERN.matcher(errorMessage);
        if (typeMatcher.find()) {
            return typeMatcher.group(1);
        }

        return null;
    }

    /**
     * Calculates the end column based on the line content and error token.
     *
     * @param line the line content
     * @param startColumn the start column (0-based)
     * @param errorToken the error token (may be null)
     * @return the calculated end column
     */
    private int calculateEndColumn(String line, int startColumn, String errorToken) {
        if (startColumn >= line.length()) {
            return line.length();
        }

        // If we have a specific token, try to find it in the line
        if (errorToken != null && !errorToken.isEmpty()) {
            int tokenIndex = line.indexOf(errorToken, startColumn);
            if (tokenIndex >= 0) {
                return tokenIndex + errorToken.length();
            }
        }

        // Try to find the end of the current token/word
        int endColumn = startColumn;

        // Skip any whitespace at the start
        while (endColumn < line.length() && Character.isWhitespace(line.charAt(endColumn))) {
            endColumn++;
        }

        // Find the end of the current token
        if (endColumn < line.length()) {
            char firstChar = line.charAt(endColumn);

            if (Character.isLetterOrDigit(firstChar) || firstChar == '_' || firstChar == '$') {
                // Identifier or keyword
                while (endColumn < line.length()) {
                    char ch = line.charAt(endColumn);
                    if (!Character.isLetterOrDigit(ch) && ch != '_' && ch != '$') {
                        break;
                    }
                    endColumn++;
                }
            } else if (isOperatorChar(firstChar)) {
                // Operator
                while (endColumn < line.length() && isOperatorChar(line.charAt(endColumn))) {
                    endColumn++;
                }
            } else {
                // Single character token
                endColumn++;
            }
        }

        // Ensure we have at least some range
        return Math.max(endColumn, startColumn + 1);
    }

    /**
     * Checks if a character is part of an operator.
     *
     * @param ch the character to check
     * @return true if it's an operator character
     */
    private boolean isOperatorChar(char ch) {
        return "+-*/%<>=!&|^~?:.".indexOf(ch) >= 0;
    }
}
