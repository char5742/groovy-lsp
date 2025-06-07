package com.groovy.lsp.protocol.internal.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.groovy.lsp.groovy.core.api.CompilationResult.CompilationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DiagnosticCodeMapperTest {

    private DiagnosticCodeMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new DiagnosticCodeMapper();
    }

    @Test
    void testMapSyntaxErrors() {
        // Unexpected token
        CompilationError error =
                new CompilationError(
                        "unexpected token: {",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.SYNTAX);
        assertEquals(DiagnosticCodes.SYNTAX_UNEXPECTED_TOKEN, mapper.mapErrorToCode(error));

        // Missing parenthesis
        error =
                new CompilationError(
                        "expecting ')', found '{'",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.SYNTAX);
        assertEquals(DiagnosticCodes.SYNTAX_GENERAL, mapper.mapErrorToCode(error));

        // Unclosed string
        error =
                new CompilationError(
                        "Unclosed string literal",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.SYNTAX);
        assertEquals(DiagnosticCodes.SYNTAX_UNCLOSED_STRING, mapper.mapErrorToCode(error));

        // Unexpected EOF
        error =
                new CompilationError(
                        "unexpected end of file",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.SYNTAX);
        assertEquals(DiagnosticCodes.SYNTAX_UNEXPECTED_EOF, mapper.mapErrorToCode(error));
    }

    @Test
    void testMapSemanticErrors() {
        // Undefined variable
        CompilationError error =
                new CompilationError(
                        "The variable x is undeclared",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.SEMANTIC);
        assertEquals(DiagnosticCodes.SEMANTIC_UNDEFINED_VARIABLE, mapper.mapErrorToCode(error));

        // Duplicate method
        error =
                new CompilationError(
                        "Method doSomething() already defined",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.SEMANTIC);
        assertEquals(DiagnosticCodes.SEMANTIC_DUPLICATE_METHOD, mapper.mapErrorToCode(error));

        // Missing return
        error =
                new CompilationError(
                        "Missing return statement",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.SEMANTIC);
        assertEquals(DiagnosticCodes.SEMANTIC_MISSING_RETURN, mapper.mapErrorToCode(error));
    }

    @Test
    void testMapTypeErrors() {
        // Type mismatch
        CompilationError error =
                new CompilationError(
                        "Cannot assign value of type ArrayList to variable of type String",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.TYPE);
        assertEquals(DiagnosticCodes.TYPE_MISMATCH, mapper.mapErrorToCode(error));

        // Cannot resolve class
        error =
                new CompilationError(
                        "unable to resolve class UnknownClass",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.TYPE);
        assertEquals(DiagnosticCodes.TYPE_CANNOT_RESOLVE, mapper.mapErrorToCode(error));

        // Undefined method
        error =
                new CompilationError(
                        "No signature of method: String.unknownMethod()",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.TYPE);
        assertEquals(DiagnosticCodes.TYPE_UNDEFINED_METHOD, mapper.mapErrorToCode(error));
    }

    @Test
    void testMapWarnings() {
        // Unused variable
        CompilationError error =
                new CompilationError(
                        "Unused variable 'x'",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.WARNING);
        assertEquals(DiagnosticCodes.WARNING_UNUSED_VARIABLE, mapper.mapErrorToCode(error));

        // Deprecated method
        error =
                new CompilationError(
                        "Method doSomething() is deprecated",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.WARNING);
        assertEquals(DiagnosticCodes.WARNING_DEPRECATED_METHOD, mapper.mapErrorToCode(error));

        // Dead code
        error =
                new CompilationError(
                        "Unreachable statement - dead code",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.WARNING);
        assertEquals(DiagnosticCodes.WARNING_DEAD_CODE, mapper.mapErrorToCode(error));
    }

    @Test
    void testGeneralCodes() {
        // Unknown syntax error
        CompilationError error =
                new CompilationError(
                        "Some unknown syntax error",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.SYNTAX);
        assertEquals(DiagnosticCodes.SYNTAX_GENERAL, mapper.mapErrorToCode(error));

        // Unknown semantic error
        error =
                new CompilationError(
                        "Some unknown semantic error",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.SEMANTIC);
        assertEquals(DiagnosticCodes.SEMANTIC_GENERAL, mapper.mapErrorToCode(error));

        // Unknown type error
        error =
                new CompilationError(
                        "Some unknown type error",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.TYPE);
        assertEquals(DiagnosticCodes.TYPE_GENERAL, mapper.mapErrorToCode(error));

        // Unknown warning
        error =
                new CompilationError(
                        "Some unknown warning",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.WARNING);
        assertEquals(DiagnosticCodes.WARNING_GENERAL, mapper.mapErrorToCode(error));
    }

    @Test
    void testCaseInsensitiveMatching() {
        // Test that matching works regardless of case
        CompilationError error =
                new CompilationError(
                        "UNEXPECTED TOKEN: {",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.SYNTAX);
        assertEquals(DiagnosticCodes.SYNTAX_UNEXPECTED_TOKEN, mapper.mapErrorToCode(error));
    }
}
