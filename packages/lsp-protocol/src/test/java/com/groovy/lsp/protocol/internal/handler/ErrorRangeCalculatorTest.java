package com.groovy.lsp.protocol.internal.handler;

import static org.junit.jupiter.api.Assertions.*;

import com.groovy.lsp.groovy.core.api.CompilationResult.CompilationError;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ErrorRangeCalculatorTest {

    private ErrorRangeCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ErrorRangeCalculator();
    }

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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
}
