package com.groovy.lsp.protocol.internal.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.groovy.lsp.groovy.core.api.CompilationResult.CompilationError;
import com.groovy.lsp.test.annotations.UnitTest;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.BeforeEach;

class ErrorRangeCalculatorTest {

    private ErrorRangeCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ErrorRangeCalculator();
    }

    @UnitTest
    void testCalculateRange_UnexpectedToken() {
        // Given
        String sourceCode = "def hello( { return 'Hello' }";
        CompilationError error =
                new CompilationError(
                        "unexpected token: {",
                        1,
                        11,
                        "test.groovy",
                        CompilationError.ErrorType.SYNTAX);

        // When
        Range range = calculator.calculateRange(error, sourceCode);

        // Then
        assertEquals(0, range.getStart().getLine());
        assertEquals(10, range.getStart().getCharacter());
        assertEquals(0, range.getEnd().getLine());
        // The '{' token is at position 11, and it's a single character, so end should be 12
        assertEquals(12, range.getEnd().getCharacter()); // Single character token '{'
    }

    @UnitTest
    void testCalculateRange_IdentifierError() {
        // Given
        String sourceCode = "def myVariable = unknownFunction()";
        CompilationError error =
                new CompilationError(
                        "unable to resolve class 'unknownFunction'",
                        1,
                        18,
                        "test.groovy",
                        CompilationError.ErrorType.TYPE);

        // When
        Range range = calculator.calculateRange(error, sourceCode);

        // Then
        assertEquals(0, range.getStart().getLine());
        assertEquals(17, range.getStart().getCharacter());
        assertEquals(0, range.getEnd().getLine());
        assertEquals(32, range.getEnd().getCharacter()); // "unknownFunction"
    }

    @UnitTest
    void testCalculateRange_OperatorError() {
        // Given
        String sourceCode = "def x = 5 ++ 3";
        CompilationError error =
                new CompilationError(
                        "unexpected token: ++",
                        1,
                        11,
                        "test.groovy",
                        CompilationError.ErrorType.SYNTAX);

        // When
        Range range = calculator.calculateRange(error, sourceCode);

        // Then
        assertEquals(0, range.getStart().getLine());
        assertEquals(10, range.getStart().getCharacter());
        assertEquals(0, range.getEnd().getLine());
        assertEquals(12, range.getEnd().getCharacter()); // "++"
    }

    @UnitTest
    void testCalculateRange_EndOfLine() {
        // Given
        String sourceCode = "def x = ";
        CompilationError error =
                new CompilationError(
                        "unexpected end of file",
                        1,
                        9,
                        "test.groovy",
                        CompilationError.ErrorType.SYNTAX);

        // When
        Range range = calculator.calculateRange(error, sourceCode);

        // Then
        assertEquals(0, range.getStart().getLine());
        assertEquals(8, range.getStart().getCharacter());
        assertEquals(0, range.getEnd().getLine());
        assertEquals(8, range.getEnd().getCharacter()); // End of line
    }

    @UnitTest
    void testCalculateRange_MultilineError() {
        // Given
        String sourceCode = "def hello() {\n    return\n}";
        CompilationError error =
                new CompilationError(
                        "Missing return value",
                        2,
                        5,
                        "test.groovy",
                        CompilationError.ErrorType.SEMANTIC);

        // When
        Range range = calculator.calculateRange(error, sourceCode);

        // Then
        assertEquals(1, range.getStart().getLine());
        assertEquals(4, range.getStart().getCharacter());
        assertEquals(1, range.getEnd().getLine());
        assertEquals(10, range.getEnd().getCharacter()); // "return"
    }

    @UnitTest
    void testCalculateRange_InvalidLineNumber() {
        // Given
        String sourceCode = "def hello() { }";
        CompilationError error =
                new CompilationError(
                        "Some error",
                        100, // Invalid line number
                        1,
                        "test.groovy",
                        CompilationError.ErrorType.SYNTAX);

        // When
        Range range = calculator.calculateRange(error, sourceCode);

        // Then
        assertEquals(99, range.getStart().getLine());
        assertEquals(0, range.getStart().getCharacter());
        assertEquals(99, range.getEnd().getLine());
        assertTrue(range.getEnd().getCharacter() > 0); // Should have some range
    }

    @UnitTest
    void testCalculateRange_TypeReference() {
        // Given
        String sourceCode = "String x = new ArrayList()";
        CompilationError error =
                new CompilationError(
                        "Cannot assign value of type [ArrayList] to variable of type [String]",
                        1,
                        16,
                        "test.groovy",
                        CompilationError.ErrorType.TYPE);

        // When
        Range range = calculator.calculateRange(error, sourceCode);

        // Then
        assertEquals(0, range.getStart().getLine());
        assertEquals(15, range.getStart().getCharacter());
        assertEquals(0, range.getEnd().getLine());
        assertEquals(24, range.getEnd().getCharacter()); // "ArrayList"
    }

    @UnitTest
    void testCalculateRange_NegativeLineNumber() {
        // Given
        String sourceCode = "def hello() { }";
        CompilationError error =
                new CompilationError(
                        "Some error",
                        -1, // Negative line number
                        1,
                        "test.groovy",
                        CompilationError.ErrorType.SYNTAX);

        // When
        Range range = calculator.calculateRange(error, sourceCode);

        // Then
        assertEquals(-2, range.getStart().getLine()); // -1 - 1 = -2
        assertEquals(0, range.getStart().getCharacter());
        assertEquals(-2, range.getEnd().getLine());
        assertTrue(range.getEnd().getCharacter() > 0);
    }

    @UnitTest
    void testCalculateRange_StartColumnExceedsLineLength() {
        // Given
        String sourceCode = "def x = 5";
        CompilationError error =
                new CompilationError(
                        "Some error",
                        1,
                        50, // Column exceeds line length
                        "test.groovy",
                        CompilationError.ErrorType.SYNTAX);

        // When
        Range range = calculator.calculateRange(error, sourceCode);

        // Then
        assertEquals(0, range.getStart().getLine());
        assertEquals(49, range.getStart().getCharacter());
        assertEquals(0, range.getEnd().getLine());
        assertEquals(9, range.getEnd().getCharacter()); // End of line
    }

    @UnitTest
    void testCalculateRange_WhitespaceHandling() {
        // Given
        String sourceCode = "def x =     hello"; // Multiple spaces before 'hello'
        CompilationError error =
                new CompilationError(
                        "undefined variable",
                        1,
                        13, // Points to spaces before 'hello'
                        "test.groovy",
                        CompilationError.ErrorType.SEMANTIC);

        // When
        Range range = calculator.calculateRange(error, sourceCode);

        // Then
        assertEquals(0, range.getStart().getLine());
        assertEquals(12, range.getStart().getCharacter());
        assertEquals(0, range.getEnd().getLine());
        assertEquals(
                17, range.getEnd().getCharacter()); // Should skip whitespace and highlight 'hello'
    }

    @UnitTest
    void testCalculateRange_SingleCharacterToken() {
        // Given
        String sourceCode = "def x = 5 + a";
        CompilationError error =
                new CompilationError(
                        "undefined variable 'a'",
                        1,
                        13,
                        "test.groovy",
                        CompilationError.ErrorType.SEMANTIC);

        // When
        Range range = calculator.calculateRange(error, sourceCode);

        // Then
        assertEquals(0, range.getStart().getLine());
        assertEquals(12, range.getStart().getCharacter());
        assertEquals(0, range.getEnd().getLine());
        assertEquals(13, range.getEnd().getCharacter()); // Single character 'a'
    }

    @UnitTest
    void testCalculateRange_NoMatchingPattern() {
        // Given
        String sourceCode = "def hello() { return 42 }";
        CompilationError error =
                new CompilationError(
                        "Generic error with no pattern",
                        1,
                        5,
                        "test.groovy",
                        CompilationError.ErrorType.SEMANTIC);

        // When
        Range range = calculator.calculateRange(error, sourceCode);

        // Then
        assertEquals(0, range.getStart().getLine());
        assertEquals(4, range.getStart().getCharacter());
        assertEquals(0, range.getEnd().getLine());
        assertEquals(9, range.getEnd().getCharacter()); // Should highlight 'hello'
    }

    @UnitTest
    void testCalculateRange_MultiCharacterOperator() {
        // Given
        String sourceCode = "def x = 5 == 3";
        CompilationError error =
                new CompilationError(
                        "invalid operator",
                        1,
                        11, // Points to '=='
                        "test.groovy",
                        CompilationError.ErrorType.SYNTAX);

        // When
        Range range = calculator.calculateRange(error, sourceCode);

        // Then
        assertEquals(0, range.getStart().getLine());
        assertEquals(10, range.getStart().getCharacter());
        assertEquals(0, range.getEnd().getLine());
        assertEquals(12, range.getEnd().getCharacter()); // Both characters of '=='
    }

    @UnitTest
    void testCalculateRange_SingleCharacterBracket() {
        // Given
        String sourceCode = "def x = {";
        CompilationError error =
                new CompilationError(
                        "unclosed bracket", 1, 9, "test.groovy", CompilationError.ErrorType.SYNTAX);

        // When
        Range range = calculator.calculateRange(error, sourceCode);

        // Then
        assertEquals(0, range.getStart().getLine());
        assertEquals(8, range.getStart().getCharacter());
        assertEquals(0, range.getEnd().getLine());
        assertEquals(9, range.getEnd().getCharacter()); // Single character '{'
    }

    @UnitTest
    void testCalculateRange_EmptyLine() {
        // Given
        String sourceCode = "\n\ndef x = 5";
        CompilationError error =
                new CompilationError(
                        "unexpected empty line",
                        1,
                        1,
                        "test.groovy",
                        CompilationError.ErrorType.SYNTAX);

        // When
        Range range = calculator.calculateRange(error, sourceCode);

        // Then
        assertEquals(0, range.getStart().getLine());
        assertEquals(0, range.getStart().getCharacter());
        assertEquals(0, range.getEnd().getLine());
        assertEquals(0, range.getEnd().getCharacter()); // Empty line
    }
}
