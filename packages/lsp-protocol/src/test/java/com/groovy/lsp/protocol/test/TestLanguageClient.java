package com.groovy.lsp.protocol.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.eclipse.lsp4j.ApplyWorkspaceEditParams;
import org.eclipse.lsp4j.ApplyWorkspaceEditResponse;
import org.eclipse.lsp4j.ConfigurationParams;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.RegistrationParams;
import org.eclipse.lsp4j.ShowDocumentParams;
import org.eclipse.lsp4j.ShowDocumentResult;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.UnregistrationParams;
import org.eclipse.lsp4j.WorkDoneProgressCreateParams;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.jspecify.annotations.NonNull;

/**
 * Test implementation of LanguageClient for protocol testing.
 * Captures all client notifications and requests for verification.
 */
public class TestLanguageClient implements LanguageClient {
    private final ConcurrentLinkedQueue<MessageParams> logMessages = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<MessageParams> showMessages = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<PublishDiagnosticsParams> diagnostics =
            new ConcurrentLinkedQueue<>();
    private final List<WorkspaceFolder> workspaceFolders = new ArrayList<>();

    @Override
    public void telemetryEvent(Object object) {
        // Capture telemetry events if needed
    }

    @Override
    public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
        this.diagnostics.add(diagnostics);
    }

    @Override
    public void showMessage(MessageParams messageParams) {
        this.showMessages.add(messageParams);
    }

    @Override
    public CompletableFuture<MessageActionItem> showMessageRequest(
            ShowMessageRequestParams requestParams) {
        // Return the first action item by default
        if (requestParams.getActions() != null && !requestParams.getActions().isEmpty()) {
            return CompletableFuture.completedFuture(requestParams.getActions().get(0));
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<ShowDocumentResult> showDocument(ShowDocumentParams params) {
        ShowDocumentResult result = new ShowDocumentResult(true);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public void logMessage(MessageParams message) {
        this.logMessages.add(message);
    }

    @Override
    public CompletableFuture<ApplyWorkspaceEditResponse> applyEdit(
            ApplyWorkspaceEditParams params) {
        ApplyWorkspaceEditResponse response = new ApplyWorkspaceEditResponse();
        response.setApplied(true);
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<Void> registerCapability(RegistrationParams params) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> unregisterCapability(UnregistrationParams params) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<List<WorkspaceFolder>> workspaceFolders() {
        return CompletableFuture.completedFuture(Collections.unmodifiableList(workspaceFolders));
    }

    @Override
    public CompletableFuture<List<Object>> configuration(ConfigurationParams configurationParams) {
        List<Object> result = new ArrayList<>();
        for (int i = 0; i < configurationParams.getItems().size(); i++) {
            result.add(null); // Return null for all configuration requests
        }
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public CompletableFuture<Void> createProgress(WorkDoneProgressCreateParams params) {
        return CompletableFuture.completedFuture(null);
    }

    // Test helper methods
    @NonNull
    public List<MessageParams> getLogMessages() {
        return new ArrayList<>(logMessages);
    }

    @NonNull
    public List<MessageParams> getShowMessages() {
        return new ArrayList<>(showMessages);
    }

    @NonNull
    public List<PublishDiagnosticsParams> getDiagnostics() {
        return new ArrayList<>(diagnostics);
    }

    public void clearMessages() {
        logMessages.clear();
        showMessages.clear();
        diagnostics.clear();
    }

    public void addWorkspaceFolder(@NonNull WorkspaceFolder folder) {
        workspaceFolders.add(folder);
    }

    public void clearWorkspaceFolders() {
        workspaceFolders.clear();
    }
}
