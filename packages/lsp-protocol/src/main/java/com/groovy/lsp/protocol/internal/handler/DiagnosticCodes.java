package com.groovy.lsp.protocol.internal.handler;

/**
 * Diagnostic codes for Groovy language errors and warnings.
 * The codes are structured as "groovy-XXXX" where XXXX is a numeric code:
 * - 1000-1999: Syntax errors
 * - 2000-2999: Semantic errors
 * - 3000-3999: Type errors
 * - 4000-4999: Warnings
 */
public final class DiagnosticCodes {

    private DiagnosticCodes() {
        // Private constructor to prevent instantiation
    }

    // Syntax Errors (1000-1999)
    public static final String SYNTAX_UNEXPECTED_TOKEN = "groovy-1001";
    public static final String SYNTAX_MISSING_PARENTHESIS = "groovy-1002";
    public static final String SYNTAX_UNCLOSED_STRING = "groovy-1003";
    public static final String SYNTAX_INVALID_IDENTIFIER = "groovy-1004";
    public static final String SYNTAX_UNEXPECTED_EOF = "groovy-1005";
    public static final String SYNTAX_INVALID_EXPRESSION = "groovy-1006";
    public static final String SYNTAX_GENERAL = "groovy-1000";

    // Semantic Errors (2000-2999)
    public static final String SEMANTIC_UNDEFINED_VARIABLE = "groovy-2001";
    public static final String SEMANTIC_DUPLICATE_METHOD = "groovy-2002";
    public static final String SEMANTIC_INVALID_IMPORT = "groovy-2003";
    public static final String SEMANTIC_MISSING_RETURN = "groovy-2004";
    public static final String SEMANTIC_UNREACHABLE_CODE = "groovy-2005";
    public static final String SEMANTIC_GENERAL = "groovy-2000";

    // Type Errors (3000-3999)
    public static final String TYPE_MISMATCH = "groovy-3001";
    public static final String TYPE_CANNOT_RESOLVE = "groovy-3002";
    public static final String TYPE_INCOMPATIBLE_CAST = "groovy-3003";
    public static final String TYPE_INVALID_ASSIGNMENT = "groovy-3004";
    public static final String TYPE_UNDEFINED_METHOD = "groovy-3005";
    public static final String TYPE_GENERAL = "groovy-3000";

    // Warnings (4000-4999)
    public static final String WARNING_UNUSED_VARIABLE = "groovy-4001";
    public static final String WARNING_DEPRECATED_METHOD = "groovy-4002";
    public static final String WARNING_DEAD_CODE = "groovy-4003";
    public static final String WARNING_UNNECESSARY_CAST = "groovy-4004";
    public static final String WARNING_GENERAL = "groovy-4000";
}
