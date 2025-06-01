package com.groovy.lsp.test.builders;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Builder utilities for creating test data objects.
 * Provides fluent APIs for constructing LSP protocol objects for testing.
 */
public final class TestDataBuilder {

    private TestDataBuilder() {
        // Utility class
    }

    /**
     * Create a diagnostic builder.
     */
    @NonNull
    public static DiagnosticBuilder diagnostic() {
        return new DiagnosticBuilder();
    }

    /**
     * Create a completion item builder.
     */
    @NonNull
    public static CompletionItemBuilder completionItem() {
        return new CompletionItemBuilder();
    }

    /**
     * Create a range from line/column positions.
     */
    @NonNull
    public static Range range(int startLine, int startChar, int endLine, int endChar) {
        return new Range(new Position(startLine, startChar), new Position(endLine, endChar));
    }

    /**
     * Create a position.
     */
    @NonNull
    public static Position position(int line, int character) {
        return new Position(line, character);
    }

    /**
     * Builder for Diagnostic objects.
     */
    public static class DiagnosticBuilder {
        private Range range;
        private DiagnosticSeverity severity = DiagnosticSeverity.Error;
        private String code;
        private String source;
        private String message = "";
        private List<Object> relatedInformation;

        public DiagnosticBuilder range(int startLine, int startChar, int endLine, int endChar) {
            this.range = TestDataBuilder.range(startLine, startChar, endLine, endChar);
            return this;
        }

        public DiagnosticBuilder range(@NonNull Range range) {
            this.range = range;
            return this;
        }

        public DiagnosticBuilder error() {
            this.severity = DiagnosticSeverity.Error;
            return this;
        }

        public DiagnosticBuilder warning() {
            this.severity = DiagnosticSeverity.Warning;
            return this;
        }

        public DiagnosticBuilder info() {
            this.severity = DiagnosticSeverity.Information;
            return this;
        }

        public DiagnosticBuilder hint() {
            this.severity = DiagnosticSeverity.Hint;
            return this;
        }

        public DiagnosticBuilder severity(@NonNull DiagnosticSeverity severity) {
            this.severity = severity;
            return this;
        }

        public DiagnosticBuilder code(@Nullable String code) {
            this.code = code;
            return this;
        }

        public DiagnosticBuilder source(@Nullable String source) {
            this.source = source;
            return this;
        }

        public DiagnosticBuilder message(@NonNull String message) {
            this.message = message;
            return this;
        }

        @NonNull
        public Diagnostic build() {
            if (range == null) {
                range = TestDataBuilder.range(0, 0, 0, 0);
            }

            Diagnostic diagnostic = new Diagnostic();
            diagnostic.setRange(range);
            diagnostic.setSeverity(severity);
            diagnostic.setCode(code);
            diagnostic.setSource(source);
            diagnostic.setMessage(message);
            // relatedInformation would need proper typing if used

            return diagnostic;
        }
    }

    /**
     * Builder for CompletionItem objects.
     */
    public static class CompletionItemBuilder {
        private String label = "";
        private CompletionItemKind kind;
        private String detail;
        private String documentation;
        private Boolean deprecated;
        private Boolean preselect;
        private String sortText;
        private String filterText;
        private String insertText;
        private TextEdit textEdit;
        private List<TextEdit> additionalTextEdits;

        public CompletionItemBuilder label(@NonNull String label) {
            this.label = label;
            return this;
        }

        public CompletionItemBuilder kind(@NonNull CompletionItemKind kind) {
            this.kind = kind;
            return this;
        }

        public CompletionItemBuilder method() {
            return kind(CompletionItemKind.Method);
        }

        public CompletionItemBuilder field() {
            return kind(CompletionItemKind.Field);
        }

        public CompletionItemBuilder clazz() {
            return kind(CompletionItemKind.Class);
        }

        public CompletionItemBuilder keyword() {
            return kind(CompletionItemKind.Keyword);
        }

        public CompletionItemBuilder snippet() {
            return kind(CompletionItemKind.Snippet);
        }

        public CompletionItemBuilder detail(@Nullable String detail) {
            this.detail = detail;
            return this;
        }

        public CompletionItemBuilder documentation(@Nullable String documentation) {
            this.documentation = documentation;
            return this;
        }

        public CompletionItemBuilder deprecated(boolean deprecated) {
            this.deprecated = deprecated;
            return this;
        }

        public CompletionItemBuilder preselect(boolean preselect) {
            this.preselect = preselect;
            return this;
        }

        public CompletionItemBuilder sortText(@Nullable String sortText) {
            this.sortText = sortText;
            return this;
        }

        public CompletionItemBuilder filterText(@Nullable String filterText) {
            this.filterText = filterText;
            return this;
        }

        public CompletionItemBuilder insertText(@Nullable String insertText) {
            this.insertText = insertText;
            return this;
        }

        public CompletionItemBuilder textEdit(@Nullable TextEdit textEdit) {
            this.textEdit = textEdit;
            return this;
        }

        public CompletionItemBuilder additionalTextEdit(@NonNull TextEdit edit) {
            if (additionalTextEdits == null) {
                additionalTextEdits = new ArrayList<>();
            }
            additionalTextEdits.add(edit);
            return this;
        }

        @NonNull
        public CompletionItem build() {
            CompletionItem item = new CompletionItem();
            item.setLabel(label);
            item.setKind(kind);
            item.setDetail(detail);
            item.setDocumentation(documentation);
            item.setDeprecated(deprecated);
            item.setPreselect(preselect);
            item.setSortText(sortText);
            item.setFilterText(filterText);
            item.setInsertText(insertText);
            if (textEdit != null) {
                item.setTextEdit(Either.forLeft(textEdit));
            }
            item.setAdditionalTextEdits(additionalTextEdits);

            return item;
        }
    }
}
