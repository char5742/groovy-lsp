package com.groovy.lsp.protocol.internal.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.groovy.lsp.groovy.core.api.CompilationResult.CompilationError;
import com.groovy.lsp.test.annotations.UnitTest;
import org.junit.jupiter.api.BeforeEach;

class DiagnosticCodeMapperTest {

    private DiagnosticCodeMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new DiagnosticCodeMapper();
    }

    @UnitTest
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

        // Missing parenthesis - right
        error =
                new CompilationError(
                        "missing ')'", 1, 10, "test.groovy", CompilationError.ErrorType.SYNTAX);
        assertEquals(DiagnosticCodes.SYNTAX_MISSING_PARENTHESIS, mapper.mapErrorToCode(error));

        // Missing parenthesis - left
        error =
                new CompilationError(
                        "missing '('", 1, 10, "test.groovy", CompilationError.ErrorType.SYNTAX);
        assertEquals(DiagnosticCodes.SYNTAX_MISSING_PARENTHESIS, mapper.mapErrorToCode(error));

        // Invalid identifier
        error =
                new CompilationError(
                        "invalid identifier used in expression",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.SYNTAX);
        assertEquals(DiagnosticCodes.SYNTAX_INVALID_IDENTIFIER, mapper.mapErrorToCode(error));

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

    @UnitTest
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

        // Invalid import
        error =
                new CompilationError(
                        "unable to resolve class in import statement",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.SEMANTIC);
        assertEquals(DiagnosticCodes.SEMANTIC_INVALID_IMPORT, mapper.mapErrorToCode(error));

        // Unreachable statement
        error =
                new CompilationError(
                        "unreachable statement detected",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.SEMANTIC);
        assertEquals(DiagnosticCodes.SEMANTIC_UNREACHABLE_CODE, mapper.mapErrorToCode(error));
    }

    @UnitTest
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

        // Incompatible cast
        error =
                new CompilationError(
                        "cannot cast String to Integer",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.TYPE);
        assertEquals(DiagnosticCodes.TYPE_INCOMPATIBLE_CAST, mapper.mapErrorToCode(error));

        // Another cast error
        error =
                new CompilationError(
                        "incompatible cast from List to Map",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.TYPE);
        assertEquals(DiagnosticCodes.TYPE_INCOMPATIBLE_CAST, mapper.mapErrorToCode(error));

        // Invalid assignment
        error =
                new CompilationError(
                        "cannot assign null to primitive type int",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.TYPE);
        assertEquals(DiagnosticCodes.TYPE_INVALID_ASSIGNMENT, mapper.mapErrorToCode(error));

        // Another assignment error
        error =
                new CompilationError(
                        "invalid assignment to final variable",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.TYPE);
        assertEquals(DiagnosticCodes.TYPE_INVALID_ASSIGNMENT, mapper.mapErrorToCode(error));

        // Undefined method
        error =
                new CompilationError(
                        "No signature of method: String.unknownMethod()",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.TYPE);
        assertEquals(DiagnosticCodes.TYPE_UNDEFINED_METHOD, mapper.mapErrorToCode(error));

        // Another method not found error
        error =
                new CompilationError(
                        "cannot find method doSomething() in class MyClass",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.TYPE);
        assertEquals(DiagnosticCodes.TYPE_UNDEFINED_METHOD, mapper.mapErrorToCode(error));
    }

    @UnitTest
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

        // Dead code - with "dead code" text
        error =
                new CompilationError(
                        "Unreachable statement - dead code",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.WARNING);
        assertEquals(DiagnosticCodes.WARNING_DEAD_CODE, mapper.mapErrorToCode(error));

        // Dead code - with "unreachable" text
        error =
                new CompilationError(
                        "This code is unreachable",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.WARNING);
        assertEquals(DiagnosticCodes.WARNING_DEAD_CODE, mapper.mapErrorToCode(error));

        // Unnecessary cast
        error =
                new CompilationError(
                        "unnecessary cast from String to String",
                        1,
                        10,
                        "test.groovy",
                        CompilationError.ErrorType.WARNING);
        assertEquals(DiagnosticCodes.WARNING_UNNECESSARY_CAST, mapper.mapErrorToCode(error));
    }

    @UnitTest
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

    @UnitTest
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
