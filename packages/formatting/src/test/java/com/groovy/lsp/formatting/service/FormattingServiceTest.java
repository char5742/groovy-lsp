package com.groovy.lsp.formatting.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.groovy.lsp.formatting.options.FormatOptions;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * FormattingServiceのテストクラス。
 */
class FormattingServiceTest {

    private FormattingService formattingService;

    @BeforeEach
    void setUp() {
        formattingService = new FormattingService();
    }

    @Test
    void constructor_shouldCreateWithDefaultOptions() {
        // when
        FormattingService service = new FormattingService();

        // then
        assertThat(service).isNotNull();
    }

    @Test
    void constructor_shouldCreateWithCustomOptions() {
        // given
        FormatOptions options = FormatOptions.builder().indentSize(2).useTabs(true).build();

        // when
        FormattingService service = new FormattingService(options);

        // then
        assertThat(service).isNotNull();
    }

    @Test
    void formatDocument_shouldFormatValidGroovyCode()
            throws ExecutionException, InterruptedException {
        // given
        String unformattedCode =
                """
                class Test{
                def name
                void method(){
                println "hello"
                }
                }
                """;

        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///test.groovy");
        FormattingOptions options = new FormattingOptions(4, true);
        DocumentFormattingParams params = new DocumentFormattingParams(textDocument, options);

        // when
        CompletableFuture<List<TextEdit>> future =
                formattingService.formatDocument(params, unformattedCode);
        List<TextEdit> edits = future.get();

        // then
        assertThat(edits).isNotNull();
        assertThat(edits).hasSize(1);
        TextEdit edit = edits.get(0);
        assertThat(edit.getNewText()).isNotEmpty();
        assertThat(edit.getRange()).isNotNull();
    }

    @Test
    void formatDocument_shouldHandleEmptyCode() throws ExecutionException, InterruptedException {
        // given
        String emptyCode = "";
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///empty.groovy");
        FormattingOptions options = new FormattingOptions(4, true);
        DocumentFormattingParams params = new DocumentFormattingParams(textDocument, options);

        // when
        CompletableFuture<List<TextEdit>> future =
                formattingService.formatDocument(params, emptyCode);
        List<TextEdit> edits = future.get();

        // then
        assertThat(edits).isNotNull();
        assertThat(edits).hasSize(1);
    }

    @Test
    void formatDocument_shouldApplyTabSettings() throws ExecutionException, InterruptedException {
        // given
        String code = "class Test {\n\tdef field\n}";
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///tabs.groovy");
        FormattingOptions options = new FormattingOptions(2, false); // Use tabs
        DocumentFormattingParams params = new DocumentFormattingParams(textDocument, options);

        // when
        CompletableFuture<List<TextEdit>> future = formattingService.formatDocument(params, code);
        List<TextEdit> edits = future.get();

        // then
        assertThat(edits).isNotNull();
        assertThat(edits).hasSize(1);
    }

    @Test
    void formatDocument_shouldFormatComplexGroovyCode()
            throws ExecutionException, InterruptedException {
        // given
        String complexCode =
                """
                package com.example
                import java.util.*
                @groovy.transform.CompileStatic
                class ComplexClass implements Serializable {
                private String name
                private int age
                ComplexClass(String name,int age){
                this.name=name
                this.age=age
                }
                String toString(){
                return"ComplexClass{name='$name',age=$age}"
                }
                static void main(String[]args){
                def instance=new ComplexClass("test",25)
                println instance
                }
                }
                """;

        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///complex.groovy");
        FormattingOptions options = new FormattingOptions(4, true);
        DocumentFormattingParams params = new DocumentFormattingParams(textDocument, options);

        // when
        CompletableFuture<List<TextEdit>> future =
                formattingService.formatDocument(params, complexCode);
        List<TextEdit> edits = future.get();

        // then
        assertThat(edits).isNotNull();
        assertThat(edits).hasSize(1);
        assertThat(edits.get(0).getNewText()).contains("ComplexClass");
    }

    @Test
    void formatRange_shouldFormatSpecifiedRange() throws ExecutionException, InterruptedException {
        // given
        String code =
                """
                class Test {
                    def method1() {
                println"unformatted"
                    }

                    def method2() {
                        println "already formatted"
                    }
                }
                """;

        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///range.groovy");
        FormattingOptions options = new FormattingOptions(4, true);
        Range range = new Range(new Position(2, 0), new Position(4, 0));
        DocumentRangeFormattingParams params = new DocumentRangeFormattingParams();
        params.setTextDocument(textDocument);
        params.setRange(range);
        params.setOptions(options);

        // when
        CompletableFuture<List<TextEdit>> future = formattingService.formatRange(params, code);
        List<TextEdit> edits = future.get();

        // then
        assertThat(edits).isNotNull();
        assertThat(edits).hasSize(1);
    }

    @Test
    void formatRange_shouldHandleSingleLineRange() throws ExecutionException, InterruptedException {
        // given
        String code = "def x=1;def y=2;def z=3";
        TextDocumentIdentifier textDocument =
                new TextDocumentIdentifier("file:///single-line.groovy");
        FormattingOptions options = new FormattingOptions(4, true);
        Range range = new Range(new Position(0, 8), new Position(0, 16));
        DocumentRangeFormattingParams params = new DocumentRangeFormattingParams();
        params.setTextDocument(textDocument);
        params.setRange(range);
        params.setOptions(options);

        // when
        CompletableFuture<List<TextEdit>> future = formattingService.formatRange(params, code);
        List<TextEdit> edits = future.get();

        // then
        assertThat(edits).isNotNull();
        assertThat(edits).hasSize(1);
    }

    @Test
    void formatRange_shouldHandleMultiLineRange() throws ExecutionException, InterruptedException {
        // given
        String code =
                """
                class Test {
                def field1
                def field2
                def field3
                }
                """;

        TextDocumentIdentifier textDocument =
                new TextDocumentIdentifier("file:///multi-line.groovy");
        FormattingOptions options = new FormattingOptions(4, true);
        Range range = new Range(new Position(1, 0), new Position(3, 10));
        DocumentRangeFormattingParams params = new DocumentRangeFormattingParams();
        params.setTextDocument(textDocument);
        params.setRange(range);
        params.setOptions(options);

        // when
        CompletableFuture<List<TextEdit>> future = formattingService.formatRange(params, code);
        List<TextEdit> edits = future.get();

        // then
        assertThat(edits).isNotNull();
        assertThat(edits).hasSize(1);
    }

    @Test
    void formatDocument_shouldReturnEmptyListForInvalidGroovyCode()
            throws ExecutionException, InterruptedException {
        // given
        String invalidCode = "class { invalid syntax";
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///invalid.groovy");
        FormattingOptions options = new FormattingOptions(4, true);
        DocumentFormattingParams params = new DocumentFormattingParams(textDocument, options);

        // when
        CompletableFuture<List<TextEdit>> future =
                formattingService.formatDocument(params, invalidCode);
        List<TextEdit> edits = future.get();

        // then
        assertThat(edits).isEmpty();
    }

    @Test
    void formatRange_shouldReturnEmptyListForInvalidGroovyCode()
            throws ExecutionException, InterruptedException {
        // given
        String invalidCode = "def { broken";
        TextDocumentIdentifier textDocument =
                new TextDocumentIdentifier("file:///invalid-range.groovy");
        FormattingOptions options = new FormattingOptions(4, true);
        Range range = new Range(new Position(0, 0), new Position(0, 10));
        DocumentRangeFormattingParams params = new DocumentRangeFormattingParams();
        params.setTextDocument(textDocument);
        params.setRange(range);
        params.setOptions(options);

        // when
        CompletableFuture<List<TextEdit>> future =
                formattingService.formatRange(params, invalidCode);
        List<TextEdit> edits = future.get();

        // then
        assertThat(edits).isEmpty();
    }

    @Test
    void formatDocument_shouldHandleCodeWithNewlines()
            throws ExecutionException, InterruptedException {
        // given
        String codeWithNewlines =
                "class Test {\n\n\n    def method() {\n        println 'test'\n    }\n\n}";
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///newlines.groovy");
        FormattingOptions options = new FormattingOptions(4, true);
        DocumentFormattingParams params = new DocumentFormattingParams(textDocument, options);

        // when
        CompletableFuture<List<TextEdit>> future =
                formattingService.formatDocument(params, codeWithNewlines);
        List<TextEdit> edits = future.get();

        // then
        assertThat(edits).isNotNull();
        assertThat(edits).hasSize(1);
        TextEdit edit = edits.get(0);
        assertThat(edit.getRange().getEnd().getLine()).isGreaterThanOrEqualTo(7);
    }

    @Test
    void formatRange_shouldHandleRangeExceedingLineEnd()
            throws ExecutionException, InterruptedException {
        // given
        String code = "def x = 1\ndef y = 2";
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///bounds.groovy");
        FormattingOptions options = new FormattingOptions(4, true);
        Range range =
                new Range(
                        new Position(0, 0), new Position(1, 100)); // Character exceeds line length
        DocumentRangeFormattingParams params = new DocumentRangeFormattingParams();
        params.setTextDocument(textDocument);
        params.setRange(range);
        params.setOptions(options);

        // when
        CompletableFuture<List<TextEdit>> future = formattingService.formatRange(params, code);
        List<TextEdit> edits = future.get();

        // then
        assertThat(edits).isNotNull();
        assertThat(edits).hasSize(1);
    }

    @Test
    void formatDocument_shouldApplyIndentSize() throws ExecutionException, InterruptedException {
        // given
        String code = "class Test {\ndef method() {\nprintln 'hello'\n}\n}";
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier("file:///indent.groovy");
        FormattingOptions options = new FormattingOptions(2, true); // 2 spaces
        DocumentFormattingParams params = new DocumentFormattingParams(textDocument, options);

        // when
        CompletableFuture<List<TextEdit>> future = formattingService.formatDocument(params, code);
        List<TextEdit> edits = future.get();

        // then
        assertThat(edits).isNotNull();
        assertThat(edits).hasSize(1);
    }
}
