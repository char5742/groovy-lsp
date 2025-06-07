package com.groovy.lsp.protocol.internal.handler;

import com.groovy.lsp.groovy.core.api.CompilationResult;
import com.groovy.lsp.groovy.core.api.IncrementalCompilationService;
import com.groovy.lsp.protocol.api.IServiceRouter;
import com.groovy.lsp.protocol.internal.document.DocumentManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.services.LanguageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles diagnostics for Groovy documents.
 *
 * This handler provides real-time diagnostics including:
 * - Syntax errors (using Parrot parser)
 * - Type errors
 * - Semantic errors
 * With debounce functionality for better performance.
 */
public class DiagnosticsHandler {

    private static final Logger logger = LoggerFactory.getLogger(DiagnosticsHandler.class);

    // Debounce delay in milliseconds
    private static final long DEBOUNCE_DELAY_MS = 250;

    private final IServiceRouter serviceRouter;
    private final DocumentManager documentManager;
    private final ErrorRangeCalculator errorRangeCalculator = new ErrorRangeCalculator();
    private final DiagnosticCodeMapper diagnosticCodeMapper = new DiagnosticCodeMapper();
    private final ScheduledExecutorService debounceExecutor =
            Executors.newSingleThreadScheduledExecutor(
                    r -> {
                        Thread thread = new Thread(r);
                        thread.setName("diagnostics-debounce");
                        thread.setDaemon(true);
                        return thread;
                    });

    // Map to track scheduled tasks for debouncing
    private final java.util.concurrent.ConcurrentHashMap<String, ScheduledFuture<?>>
            scheduledTasks = new java.util.concurrent.ConcurrentHashMap<>();

    public DiagnosticsHandler(IServiceRouter serviceRouter, DocumentManager documentManager) {
        this.serviceRouter = serviceRouter;
        this.documentManager = documentManager;
    }

    /**
     * Handles diagnostics for a document immediately (e.g., on open).
     */
    public CompletableFuture<Void> handleDiagnosticsImmediate(String uri, LanguageClient client) {
        return CompletableFuture.runAsync(
                () -> {
                    try {
                        publishDiagnostics(uri, client);
                    } catch (Exception e) {
                        logger.error("Error handling immediate diagnostics for URI: {}", uri, e);
                    }
                });
    }

    /**
     * Handles diagnostics for a document with debouncing (e.g., on change).
     */
    public CompletableFuture<Void> handleDiagnosticsDebounced(String uri, LanguageClient client) {
        // Cancel any existing scheduled task for this URI
        ScheduledFuture<?> existingTask = scheduledTasks.get(uri);
        if (existingTask != null && !existingTask.isDone()) {
            existingTask.cancel(false);
        }

        // Schedule new task with debounce delay
        ScheduledFuture<?> newTask =
                debounceExecutor.schedule(
                        () -> {
                            try {
                                publishDiagnostics(uri, client);
                            } finally {
                                // タスク完了後にマップから削除
                                scheduledTasks.remove(uri);
                            }
                        },
                        DEBOUNCE_DELAY_MS,
                        TimeUnit.MILLISECONDS);

        scheduledTasks.put(uri, newTask);

        return CompletableFuture.completedFuture(null);
    }

    private void publishDiagnostics(String uri, LanguageClient client) {
        try {
            logger.debug("Computing diagnostics for: {}", uri);

            // Get document content
            String sourceCode = documentManager.getDocumentContent(uri);
            if (sourceCode == null) {
                logger.debug("Document not found in document manager: {}", uri);
                return;
            }

            // Get incremental compilation service
            IncrementalCompilationService compilationService =
                    serviceRouter.getIncrementalCompilationService();

            // Create compilation unit with default configuration
            CompilerConfiguration config = new CompilerConfiguration();
            config.setTargetBytecode(CompilerConfiguration.JDK8);
            CompilationUnit unit = compilationService.createCompilationUnit(config);

            // Compile to SEMANTIC_ANALYSIS phase to get both syntax and type errors
            CompilationResult result =
                    compilationService.compileToPhaseWithResult(
                            unit,
                            sourceCode,
                            uri,
                            IncrementalCompilationService.CompilationPhase.SEMANTIC_ANALYSIS);

            // Convert compilation errors to diagnostics
            List<Diagnostic> diagnostics = new ArrayList<>();
            for (CompilationResult.CompilationError error : result.getErrors()) {
                Diagnostic diagnostic = convertToDiagnostic(error, sourceCode);
                diagnostics.add(diagnostic);
            }

            // Publish diagnostics
            PublishDiagnosticsParams params = new PublishDiagnosticsParams();
            params.setUri(uri);
            params.setDiagnostics(diagnostics);

            client.publishDiagnostics(params);

            logger.debug("Published {} diagnostics for: {}", diagnostics.size(), uri);

        } catch (Exception e) {
            logger.error("Error computing diagnostics for URI: {}", uri, e);
        }
    }

    private Diagnostic convertToDiagnostic(
            CompilationResult.CompilationError error, String sourceCode) {
        Diagnostic diagnostic = new Diagnostic();

        // Set range using ErrorRangeCalculator for better accuracy
        Range range = errorRangeCalculator.calculateRange(error, sourceCode);
        diagnostic.setRange(range);

        // Set message
        diagnostic.setMessage(error.getMessage());

        // Set severity based on error type
        DiagnosticSeverity severity =
                switch (error.getType()) {
                    case WARNING -> DiagnosticSeverity.Warning;
                    case SYNTAX, SEMANTIC, TYPE -> DiagnosticSeverity.Error;
                };
        diagnostic.setSeverity(severity);

        // Set source
        diagnostic.setSource("groovy");

        // Set diagnostic code
        String code = diagnosticCodeMapper.mapErrorToCode(error);
        if (code != null) {
            diagnostic.setCode(code);
        }

        return diagnostic;
    }

    /**
     * Clears diagnostics for a specific document and cancels any pending tasks.
     */
    public void clearDiagnostics(String uri, LanguageClient client) {
        // Cancel any pending task for this URI
        ScheduledFuture<?> task = scheduledTasks.remove(uri);
        if (task != null && !task.isDone()) {
            task.cancel(false);
        }

        // Clear diagnostics by publishing empty list
        PublishDiagnosticsParams params = new PublishDiagnosticsParams();
        params.setUri(uri);
        params.setDiagnostics(new ArrayList<>());
        client.publishDiagnostics(params);
    }

    /**
     * Shuts down the debounce executor.
     */
    public void shutdown() {
        // Cancel all pending tasks
        scheduledTasks
                .values()
                .forEach(
                        task -> {
                            if (task != null && !task.isDone()) {
                                task.cancel(false);
                            }
                        });
        scheduledTasks.clear();

        // Shutdown executor
        debounceExecutor.shutdown();
        try {
            if (!debounceExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                debounceExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            debounceExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Test helper method
    int getScheduledTasksSize() {
        return scheduledTasks.size();
    }
}
