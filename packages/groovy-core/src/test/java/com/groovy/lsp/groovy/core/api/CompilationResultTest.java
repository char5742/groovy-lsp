package com.groovy.lsp.groovy.core.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.groovy.lsp.test.annotations.UnitTest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.control.messages.SimpleMessage;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CompilationResultのテストクラス。
 */
@ExtendWith(MockitoExtension.class)
class CompilationResultTest {

    @UnitTest
    void success_shouldCreateSuccessResult() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);

        // when
        CompilationResult result = CompilationResult.success(moduleNode);

        // then
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getModuleNode()).isSameAs(moduleNode);
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.hasErrors()).isFalse();
    }

    @UnitTest
    void failure_shouldCreateFailureResult() {
        // given
        List<CompilationResult.CompilationError> errors =
                Arrays.asList(
                        new CompilationResult.CompilationError(
                                "Syntax error",
                                10,
                                5,
                                "Test.groovy",
                                CompilationResult.CompilationError.ErrorType.SYNTAX),
                        new CompilationResult.CompilationError(
                                "Type error",
                                20,
                                15,
                                "Test.groovy",
                                CompilationResult.CompilationError.ErrorType.TYPE));

        // when
        CompilationResult result = CompilationResult.failure(errors);

        // then
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getModuleNode()).isNull();
        assertThat(result.getErrors()).hasSize(2);
        assertThat(result.hasErrors()).isTrue();
    }

    @UnitTest
    void partial_shouldCreatePartialResult() {
        // given
        ModuleNode moduleNode = new ModuleNode((SourceUnit) null);
        List<CompilationResult.CompilationError> errors =
                Collections.singletonList(
                        new CompilationResult.CompilationError(
                                "Warning",
                                5,
                                10,
                                "Test.groovy",
                                CompilationResult.CompilationError.ErrorType.WARNING));

        // when
        CompilationResult result = CompilationResult.partial(moduleNode, errors);

        // then
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getModuleNode()).isSameAs(moduleNode);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.hasErrors()).isTrue();
    }

    @UnitTest
    void getErrors_shouldReturnImmutableList() {
        // given
        List<CompilationResult.CompilationError> errors =
                Arrays.asList(
                        new CompilationResult.CompilationError(
                                "Error 1",
                                1,
                                1,
                                "Test.groovy",
                                CompilationResult.CompilationError.ErrorType.SYNTAX));
        CompilationResult result = CompilationResult.failure(errors);

        // when/then
        assertThat(result.getErrors()).isUnmodifiable().hasSize(1);
    }

    @UnitTest
    void CompilationError_shouldSetPropertiesCorrectly() {
        // given
        String message = "Test error message";
        int line = 42;
        int column = 10;
        String sourceName = "TestFile.groovy";
        CompilationResult.CompilationError.ErrorType type =
                CompilationResult.CompilationError.ErrorType.SEMANTIC;

        // when
        CompilationResult.CompilationError error =
                new CompilationResult.CompilationError(message, line, column, sourceName, type);

        // then
        assertThat(error.getMessage()).isEqualTo(message);
        assertThat(error.getLine()).isEqualTo(line);
        assertThat(error.getColumn()).isEqualTo(column);
        assertThat(error.getSourceName()).isEqualTo(sourceName);
        assertThat(error.getType()).isEqualTo(type);
    }

    @UnitTest
    void CompilationError_fromGroovyMessage_shouldHandleSyntaxErrorMessage() {
        // given
        SyntaxException syntaxException = new SyntaxException("Unexpected token", 15, 25);
        SourceUnit sourceUnit = mock(SourceUnit.class);
        when(sourceUnit.getName()).thenReturn("SyntaxTest.groovy");
        SyntaxErrorMessage syntaxErrorMessage = new SyntaxErrorMessage(syntaxException, sourceUnit);
        String sourceName = "SyntaxTest.groovy";

        // when
        CompilationResult.CompilationError error =
                CompilationResult.CompilationError.fromGroovyMessage(
                        syntaxErrorMessage, sourceName);

        // then
        assertThat(error.getMessage()).contains("Unexpected token");
        assertThat(error.getLine()).isEqualTo(15);
        assertThat(error.getColumn()).isEqualTo(25);
        assertThat(error.getSourceName()).isEqualTo(sourceName);
        assertThat(error.getType()).isEqualTo(CompilationResult.CompilationError.ErrorType.SYNTAX);
    }

    @UnitTest
    void CompilationError_fromGroovyMessage_shouldHandleNullSyntaxException() {
        // given
        // Create a mock SyntaxErrorMessage that returns null for getCause()
        SyntaxErrorMessage syntaxErrorMessage = mock(SyntaxErrorMessage.class);
        when(syntaxErrorMessage.getCause()).thenReturn(null);
        when(syntaxErrorMessage.toString()).thenReturn("SyntaxErrorMessage: null exception");
        String sourceName = "NullTest.groovy";

        // when
        CompilationResult.CompilationError error =
                CompilationResult.CompilationError.fromGroovyMessage(
                        syntaxErrorMessage, sourceName);

        // then
        assertThat(error.getMessage()).contains("SyntaxErrorMessage");
        assertThat(error.getLine()).isEqualTo(1);
        assertThat(error.getColumn()).isEqualTo(1);
        assertThat(error.getSourceName()).isEqualTo(sourceName);
        assertThat(error.getType()).isEqualTo(CompilationResult.CompilationError.ErrorType.SYNTAX);
    }

    @UnitTest
    void CompilationError_fromGroovyMessage_shouldHandleSimpleMessage() {
        // given
        SimpleMessage simpleMessage = new SimpleMessage("Simple error message", null);
        String sourceName = "SimpleTest.groovy";

        // when
        CompilationResult.CompilationError error =
                CompilationResult.CompilationError.fromGroovyMessage(simpleMessage, sourceName);

        // then
        assertThat(error.getMessage()).contains("Simple error message");
        assertThat(error.getLine()).isEqualTo(1);
        assertThat(error.getColumn()).isEqualTo(1);
        assertThat(error.getSourceName()).isEqualTo(sourceName);
        assertThat(error.getType()).isEqualTo(CompilationResult.CompilationError.ErrorType.SYNTAX);
    }

    @UnitTest
    void CompilationError_fromGroovyMessage_shouldParseMessageWithPosition() {
        // given
        Message customMessage =
                new Message() {
                    @Override
                    public void write(
                            java.io.PrintWriter writer,
                            org.codehaus.groovy.control.Janitor janitor) {
                        writer.write(toString());
                    }

                    @Override
                    public String toString() {
                        return "Error occurred @ line 30, column 45.";
                    }
                };
        String sourceName = "ParseTest.groovy";

        // when
        CompilationResult.CompilationError error =
                CompilationResult.CompilationError.fromGroovyMessage(customMessage, sourceName);

        // then
        assertThat(error.getMessage()).contains("Error occurred @ line 30, column 45.");
        assertThat(error.getLine()).isEqualTo(30);
        assertThat(error.getColumn()).isEqualTo(45);
        assertThat(error.getSourceName()).isEqualTo(sourceName);
    }

    @UnitTest
    void CompilationError_fromGroovyMessage_shouldHandleInvalidPosition() {
        // given
        Message customMessage =
                new Message() {
                    @Override
                    public void write(
                            java.io.PrintWriter writer,
                            org.codehaus.groovy.control.Janitor janitor) {
                        writer.write(toString());
                    }

                    @Override
                    public String toString() {
                        return "Error occurred @ line abc, column xyz.";
                    }
                };
        String sourceName = "InvalidParse.groovy";

        // when
        CompilationResult.CompilationError error =
                CompilationResult.CompilationError.fromGroovyMessage(customMessage, sourceName);

        // then
        assertThat(error.getMessage()).contains("Error occurred @ line abc, column xyz.");
        assertThat(error.getLine()).isEqualTo(1); // デフォルト値
        assertThat(error.getColumn()).isEqualTo(1); // デフォルト値
        assertThat(error.getSourceName()).isEqualTo(sourceName);
    }

    @UnitTest
    void CompilationError_ErrorType_shouldDefineAllTypes() {
        // when
        CompilationResult.CompilationError.ErrorType[] types =
                CompilationResult.CompilationError.ErrorType.values();

        // then
        assertThat(types)
                .containsExactlyInAnyOrder(
                        CompilationResult.CompilationError.ErrorType.SYNTAX,
                        CompilationResult.CompilationError.ErrorType.SEMANTIC,
                        CompilationResult.CompilationError.ErrorType.TYPE,
                        CompilationResult.CompilationError.ErrorType.WARNING);
    }
}
