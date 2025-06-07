package com.groovy.lsp.protocol.internal.handler;

import com.groovy.lsp.groovy.core.api.CompilationResult.CompilationError;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Maps compilation errors to diagnostic codes based on error type and message patterns.
 */
public class DiagnosticCodeMapper {

    // Common patterns in Groovy error messages (case-insensitive)
    private static final Pattern UNEXPECTED_TOKEN_PATTERN =
            Pattern.compile("unexpected token", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNABLE_TO_RESOLVE_PATTERN =
            Pattern.compile("unable to resolve class|cannot resolve", Pattern.CASE_INSENSITIVE);
    private static final Pattern TYPE_MISMATCH_PATTERN =
            Pattern.compile(
                    "cannot assign value of type|incompatible types", Pattern.CASE_INSENSITIVE);
    private static final Pattern MISSING_PROPERTY_PATTERN =
            Pattern.compile("no such property", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNDEFINED_VARIABLE_PATTERN =
            Pattern.compile("the variable .* is undeclared", Pattern.CASE_INSENSITIVE);
    private static final Pattern DUPLICATE_METHOD_PATTERN =
            Pattern.compile("method .* already defined", Pattern.CASE_INSENSITIVE);
    private static final Pattern MISSING_RETURN_PATTERN =
            Pattern.compile("missing return statement", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNCLOSED_STRING_PATTERN =
            Pattern.compile("unclosed string", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNEXPECTED_EOF_PATTERN =
            Pattern.compile("unexpected end of file", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNUSED_VARIABLE_PATTERN =
            Pattern.compile("unused variable", Pattern.CASE_INSENSITIVE);
    private static final Pattern DEPRECATED_PATTERN =
            Pattern.compile("deprecated", Pattern.CASE_INSENSITIVE);

    /**
     * Maps a compilation error to a diagnostic code.
     *
     * @param error the compilation error
     * @return the diagnostic code, or null if no specific code applies
     */
    public String mapErrorToCode(CompilationError error) {
        String message = error.getMessage().toLowerCase(Locale.ROOT);

        return switch (error.getType()) {
            case SYNTAX -> mapSyntaxErrorCode(message);
            case SEMANTIC -> mapSemanticErrorCode(message);
            case TYPE -> mapTypeErrorCode(message);
            case WARNING -> mapWarningCode(message);
        };
    }

    private String mapSyntaxErrorCode(String message) {
        if (UNEXPECTED_TOKEN_PATTERN.matcher(message).find()) {
            return DiagnosticCodes.SYNTAX_UNEXPECTED_TOKEN;
        }
        if (UNCLOSED_STRING_PATTERN.matcher(message).find()) {
            return DiagnosticCodes.SYNTAX_UNCLOSED_STRING;
        }
        if (UNEXPECTED_EOF_PATTERN.matcher(message).find()) {
            return DiagnosticCodes.SYNTAX_UNEXPECTED_EOF;
        }
        if (message.contains("missing ')'") || message.contains("missing '('")) {
            return DiagnosticCodes.SYNTAX_MISSING_PARENTHESIS;
        }
        if (message.contains("invalid identifier")) {
            return DiagnosticCodes.SYNTAX_INVALID_IDENTIFIER;
        }

        return DiagnosticCodes.SYNTAX_GENERAL;
    }

    private String mapSemanticErrorCode(String message) {
        if (UNDEFINED_VARIABLE_PATTERN.matcher(message).find()) {
            return DiagnosticCodes.SEMANTIC_UNDEFINED_VARIABLE;
        }
        if (DUPLICATE_METHOD_PATTERN.matcher(message).find()) {
            return DiagnosticCodes.SEMANTIC_DUPLICATE_METHOD;
        }
        if (MISSING_RETURN_PATTERN.matcher(message).find()) {
            return DiagnosticCodes.SEMANTIC_MISSING_RETURN;
        }
        if (message.contains("unable to resolve") && message.contains("import")) {
            return DiagnosticCodes.SEMANTIC_INVALID_IMPORT;
        }
        if (message.contains("unreachable statement")) {
            return DiagnosticCodes.SEMANTIC_UNREACHABLE_CODE;
        }

        return DiagnosticCodes.SEMANTIC_GENERAL;
    }

    private String mapTypeErrorCode(String message) {
        if (TYPE_MISMATCH_PATTERN.matcher(message).find()) {
            return DiagnosticCodes.TYPE_MISMATCH;
        }
        if (UNABLE_TO_RESOLVE_PATTERN.matcher(message).find()) {
            return DiagnosticCodes.TYPE_CANNOT_RESOLVE;
        }
        if (message.contains("cannot cast") || message.contains("incompatible cast")) {
            return DiagnosticCodes.TYPE_INCOMPATIBLE_CAST;
        }
        if (message.contains("cannot assign") || message.contains("invalid assignment")) {
            return DiagnosticCodes.TYPE_INVALID_ASSIGNMENT;
        }
        if (message.contains("no signature of method") || message.contains("cannot find method")) {
            return DiagnosticCodes.TYPE_UNDEFINED_METHOD;
        }

        return DiagnosticCodes.TYPE_GENERAL;
    }

    private String mapWarningCode(String message) {
        if (UNUSED_VARIABLE_PATTERN.matcher(message).find()) {
            return DiagnosticCodes.WARNING_UNUSED_VARIABLE;
        }
        if (DEPRECATED_PATTERN.matcher(message).find()) {
            return DiagnosticCodes.WARNING_DEPRECATED_METHOD;
        }
        if (message.contains("dead code") || message.contains("unreachable")) {
            return DiagnosticCodes.WARNING_DEAD_CODE;
        }
        if (message.contains("unnecessary cast")) {
            return DiagnosticCodes.WARNING_UNNECESSARY_CAST;
        }

        return DiagnosticCodes.WARNING_GENERAL;
    }
}
