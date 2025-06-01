package com.groovy.lsp.protocol.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Consumer;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.jspecify.annotations.NonNull;

/**
 * Assertion utilities for LSP protocol testing.
 * Provides fluent assertions for common LSP protocol elements.
 */
public final class ProtocolAssertions {

    private ProtocolAssertions() {
        // Utility class
    }

    /**
     * Assert diagnostics for a specific URI.
     */
    public static DiagnosticsAssertion assertDiagnostics(
            @NonNull TestLanguageClient client, @NonNull String uri) {
        List<PublishDiagnosticsParams> allDiagnostics = client.getDiagnostics();
        PublishDiagnosticsParams diagnostics =
                allDiagnostics.stream()
                        .filter(d -> d.getUri().equals(uri))
                        .findFirst()
                        .orElse(null);

        return new DiagnosticsAssertion(diagnostics);
    }

    /**
     * Assert log messages.
     */
    public static MessagesAssertion assertLogMessages(@NonNull TestLanguageClient client) {
        return new MessagesAssertion(client.getLogMessages());
    }

    /**
     * Assert show messages.
     */
    public static MessagesAssertion assertShowMessages(@NonNull TestLanguageClient client) {
        return new MessagesAssertion(client.getShowMessages());
    }

    /**
     * Fluent assertions for diagnostics.
     */
    public static class DiagnosticsAssertion {
        private final PublishDiagnosticsParams diagnostics;

        DiagnosticsAssertion(PublishDiagnosticsParams diagnostics) {
            this.diagnostics = diagnostics;
        }

        public DiagnosticsAssertion hasCount(int expectedCount) {
            assertThat(diagnostics).isNotNull();
            assertThat(diagnostics.getDiagnostics()).hasSize(expectedCount);
            return this;
        }

        public DiagnosticsAssertion isEmpty() {
            if (diagnostics == null) {
                return this;
            }
            assertThat(diagnostics.getDiagnostics()).isEmpty();
            return this;
        }

        public DiagnosticsAssertion hasError(@NonNull String message) {
            return hasDiagnostic(DiagnosticSeverity.Error, message);
        }

        public DiagnosticsAssertion hasWarning(@NonNull String message) {
            return hasDiagnostic(DiagnosticSeverity.Warning, message);
        }

        public DiagnosticsAssertion hasInfo(@NonNull String message) {
            return hasDiagnostic(DiagnosticSeverity.Information, message);
        }

        public DiagnosticsAssertion hasHint(@NonNull String message) {
            return hasDiagnostic(DiagnosticSeverity.Hint, message);
        }

        public DiagnosticsAssertion hasDiagnostic(
                @NonNull DiagnosticSeverity severity, @NonNull String message) {
            assertThat(diagnostics).isNotNull();
            assertThat(diagnostics.getDiagnostics())
                    .anySatisfy(
                            d -> {
                                assertThat(d.getSeverity()).isEqualTo(severity);
                                assertThat(d.getMessage()).contains(message);
                            });
            return this;
        }

        public DiagnosticsAssertion satisfies(@NonNull Consumer<List<Diagnostic>> requirements) {
            assertThat(diagnostics).isNotNull();
            assertThat(diagnostics.getDiagnostics()).satisfies(requirements);
            return this;
        }
    }

    /**
     * Fluent assertions for messages.
     */
    public static class MessagesAssertion {
        private final List<MessageParams> messages;

        MessagesAssertion(List<MessageParams> messages) {
            this.messages = messages;
        }

        public MessagesAssertion hasCount(int expectedCount) {
            assertThat(messages).hasSize(expectedCount);
            return this;
        }

        public MessagesAssertion isEmpty() {
            assertThat(messages).isEmpty();
            return this;
        }

        public MessagesAssertion hasMessage(@NonNull MessageType type, @NonNull String message) {
            assertThat(messages)
                    .anySatisfy(
                            m -> {
                                assertThat(m.getType()).isEqualTo(type);
                                assertThat(m.getMessage()).contains(message);
                            });
            return this;
        }

        public MessagesAssertion hasError(@NonNull String message) {
            return hasMessage(MessageType.Error, message);
        }

        public MessagesAssertion hasWarning(@NonNull String message) {
            return hasMessage(MessageType.Warning, message);
        }

        public MessagesAssertion hasInfo(@NonNull String message) {
            return hasMessage(MessageType.Info, message);
        }

        public MessagesAssertion hasLog(@NonNull String message) {
            return hasMessage(MessageType.Log, message);
        }

        public MessagesAssertion satisfies(@NonNull Consumer<List<MessageParams>> requirements) {
            assertThat(messages).satisfies(requirements);
            return this;
        }
    }
}
