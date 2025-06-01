package com.groovy.lsp.groovy.core.api;

import java.util.Collections;
import java.util.List;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;
import org.jspecify.annotations.Nullable;

/**
 * Result of a compilation operation, including the AST and any error messages.
 * This allows for proper error propagation and handling in the LSP.
 */
public class CompilationResult {

    @Nullable private final ModuleNode moduleNode;

    private final List<CompilationError> errors;

    private final boolean successful;

    private CompilationResult(
            @Nullable ModuleNode moduleNode, List<CompilationError> errors, boolean successful) {
        this.moduleNode = moduleNode;
        this.errors = Collections.unmodifiableList(errors);
        this.successful = successful;
    }

    /**
     * Creates a successful compilation result.
     *
     * @param moduleNode the compiled AST
     * @return a successful compilation result
     */
    public static CompilationResult success(ModuleNode moduleNode) {
        return new CompilationResult(moduleNode, Collections.emptyList(), true);
    }

    /**
     * Creates a failed compilation result.
     *
     * @param errors the compilation errors
     * @return a failed compilation result
     */
    public static CompilationResult failure(List<CompilationError> errors) {
        return new CompilationResult(null, errors, false);
    }

    /**
     * Creates a partial compilation result (has AST but also errors).
     *
     * @param moduleNode the partial AST
     * @param errors the compilation errors
     * @return a partial compilation result
     */
    public static CompilationResult partial(ModuleNode moduleNode, List<CompilationError> errors) {
        return new CompilationResult(moduleNode, errors, false);
    }

    /**
     * Gets the compiled module node.
     *
     * @return the module node, or null if compilation failed
     */
    @Nullable
    public ModuleNode getModuleNode() {
        return moduleNode;
    }

    /**
     * Gets the compilation errors.
     *
     * @return the list of compilation errors (never null)
     */
    public List<CompilationError> getErrors() {
        return errors;
    }

    /**
     * Checks if the compilation was successful.
     *
     * @return true if compilation was successful, false otherwise
     */
    public boolean isSuccessful() {
        return successful;
    }

    /**
     * Checks if there are any errors.
     *
     * @return true if there are errors, false otherwise
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Represents a compilation error with detailed information.
     */
    public static class CompilationError {
        private final String message;
        private final int line;
        private final int column;
        private final String sourceName;
        private final ErrorType type;

        public CompilationError(
                String message, int line, int column, String sourceName, ErrorType type) {
            this.message = message;
            this.line = line;
            this.column = column;
            this.sourceName = sourceName;
            this.type = type;
        }

        public String getMessage() {
            return message;
        }

        public int getLine() {
            return line;
        }

        public int getColumn() {
            return column;
        }

        public String getSourceName() {
            return sourceName;
        }

        public ErrorType getType() {
            return type;
        }

        /**
         * Creates a CompilationError from a Groovy Message.
         *
         * @param message the Groovy message
         * @param sourceName the source file name
         * @return a CompilationError
         */
        public static CompilationError fromGroovyMessage(Message message, String sourceName) {
            String msgText;
            int line = 1;
            int column = 1;

            // Handle different message types
            if (message instanceof SyntaxErrorMessage syntaxError) {
                SyntaxException cause = syntaxError.getCause();
                if (cause != null) {
                    msgText = cause.getMessage();
                    line = cause.getLine();
                    column = cause.getStartColumn();
                } else {
                    msgText = message.toString();
                }
            } else if (message
                    instanceof org.codehaus.groovy.control.messages.SimpleMessage simpleMsg) {
                // SimpleMessage has a message field but no public getter,
                // so we need to use reflection or toString
                msgText = simpleMsg.getMessage();
            } else {
                // For other message types, extract from toString
                msgText = message.toString();

                // Parse line and column from message if possible
                // Format usually includes "@ line X, column Y"
                if (msgText.contains("@ line")) {
                    try {
                        int lineIndex = msgText.indexOf("@ line") + 7;
                        int commaIndex = msgText.indexOf(",", lineIndex);
                        if (commaIndex > 0) {
                            line =
                                    Integer.parseInt(
                                            msgText.substring(lineIndex, commaIndex).trim());

                            int columnIndex = msgText.indexOf("column", commaIndex) + 7;
                            int endIndex = msgText.indexOf(".", columnIndex);
                            if (endIndex < 0) endIndex = msgText.indexOf(" ", columnIndex);
                            if (endIndex > 0) {
                                column =
                                        Integer.parseInt(
                                                msgText.substring(columnIndex, endIndex).trim());
                            }
                        }
                    } catch (Exception e) {
                        // Ignore parsing errors, use defaults
                    }
                }
            }

            return new CompilationError(
                    msgText != null ? msgText : "Unknown error",
                    line,
                    column,
                    sourceName,
                    ErrorType.SYNTAX);
        }

        public enum ErrorType {
            SYNTAX,
            SEMANTIC,
            TYPE,
            WARNING
        }
    }
}
