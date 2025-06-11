package com.groovy.lsp.protocol.internal.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.groovy.lsp.groovy.core.api.CompilationResult;
import com.groovy.lsp.groovy.core.api.CompilationResult.CompilationError;
import com.groovy.lsp.groovy.core.api.IncrementalCompilationService;
import com.groovy.lsp.protocol.api.IServiceRouter;
import com.groovy.lsp.protocol.internal.document.DocumentManager;
import com.groovy.lsp.test.annotations.UnitTest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class DiagnosticsHandlerTest {

    @Mock private IServiceRouter serviceRouter;

    @Mock private DocumentManager documentManager;

    @Mock private IncrementalCompilationService compilationService;

    @Mock private LanguageClient languageClient;

    @Mock private CompilationUnit compilationUnit;

    private DiagnosticsHandler diagnosticsHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(serviceRouter.getIncrementalCompilationService()).thenReturn(compilationService);
        diagnosticsHandler = new DiagnosticsHandler(serviceRouter, documentManager);
    }

    @AfterEach
    void tearDown() {
        diagnosticsHandler.shutdown();
    }

    @UnitTest
    void testHandleDiagnosticsImmediate_Success() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "def hello() { return 'Hello' }";

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);
        when(compilationService.createCompilationUnit(any(CompilerConfiguration.class)))
                .thenReturn(compilationUnit);

        ModuleNode moduleNode = mock(ModuleNode.class);
        CompilationResult result = CompilationResult.success(moduleNode);
        when(compilationService.compileToPhaseWithResult(
                        eq(compilationUnit),
                        eq(sourceCode),
                        eq(uri),
                        eq(IncrementalCompilationService.CompilationPhase.SEMANTIC_ANALYSIS)))
                .thenReturn(result);

        // When
        CompletableFuture<Void> future =
                diagnosticsHandler.handleDiagnosticsImmediate(uri, languageClient);
        future.get(5, TimeUnit.SECONDS);

        // Then
        ArgumentCaptor<PublishDiagnosticsParams> captor =
                ArgumentCaptor.forClass(PublishDiagnosticsParams.class);
        verify(languageClient).publishDiagnostics(captor.capture());

        PublishDiagnosticsParams params = captor.getValue();
        assertEquals(uri, params.getUri());
        assertTrue(params.getDiagnostics().isEmpty());
    }

    @UnitTest
    void testHandleDiagnosticsImmediate_WithSyntaxError() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "def hello( { return 'Hello' }"; // Missing closing parenthesis

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);
        when(compilationService.createCompilationUnit(any(CompilerConfiguration.class)))
                .thenReturn(compilationUnit);

        CompilationError error =
                new CompilationError(
                        "unexpected token: {", 1, 11, uri, CompilationError.ErrorType.SYNTAX);
        CompilationResult result = CompilationResult.failure(Collections.singletonList(error));
        when(compilationService.compileToPhaseWithResult(
                        eq(compilationUnit),
                        eq(sourceCode),
                        eq(uri),
                        eq(IncrementalCompilationService.CompilationPhase.SEMANTIC_ANALYSIS)))
                .thenReturn(result);

        // When
        CompletableFuture<Void> future =
                diagnosticsHandler.handleDiagnosticsImmediate(uri, languageClient);
        future.get(5, TimeUnit.SECONDS);

        // Then
        ArgumentCaptor<PublishDiagnosticsParams> captor =
                ArgumentCaptor.forClass(PublishDiagnosticsParams.class);
        verify(languageClient).publishDiagnostics(captor.capture());

        PublishDiagnosticsParams params = captor.getValue();
        assertEquals(uri, params.getUri());
        assertEquals(1, params.getDiagnostics().size());

        Diagnostic diagnostic = params.getDiagnostics().get(0);
        assertEquals("unexpected token: {", diagnostic.getMessage());
        assertEquals(DiagnosticSeverity.Error, diagnostic.getSeverity());
        assertEquals(0, diagnostic.getRange().getStart().getLine());
        assertEquals(10, diagnostic.getRange().getStart().getCharacter());
        assertEquals("groovy", diagnostic.getSource());
        assertNotNull(diagnostic.getCode());
        assertEquals("groovy-1001", diagnostic.getCode().getLeft());
    }

    @UnitTest
    void testHandleDiagnosticsImmediate_WithMultipleErrors() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "def hello( { \n def x = }"; // Multiple syntax errors

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);
        when(compilationService.createCompilationUnit(any(CompilerConfiguration.class)))
                .thenReturn(compilationUnit);

        List<CompilationError> errors =
                Arrays.asList(
                        new CompilationError(
                                "unexpected token: {",
                                1,
                                11,
                                uri,
                                CompilationError.ErrorType.SYNTAX),
                        new CompilationError(
                                "unexpected token: }",
                                2,
                                10,
                                uri,
                                CompilationError.ErrorType.SYNTAX));
        CompilationResult result = CompilationResult.failure(errors);
        when(compilationService.compileToPhaseWithResult(
                        eq(compilationUnit),
                        eq(sourceCode),
                        eq(uri),
                        eq(IncrementalCompilationService.CompilationPhase.SEMANTIC_ANALYSIS)))
                .thenReturn(result);

        // When
        CompletableFuture<Void> future =
                diagnosticsHandler.handleDiagnosticsImmediate(uri, languageClient);
        future.get(5, TimeUnit.SECONDS);

        // Then
        ArgumentCaptor<PublishDiagnosticsParams> captor =
                ArgumentCaptor.forClass(PublishDiagnosticsParams.class);
        verify(languageClient).publishDiagnostics(captor.capture());

        PublishDiagnosticsParams params = captor.getValue();
        assertEquals(uri, params.getUri());
        assertEquals(2, params.getDiagnostics().size());
    }

    @UnitTest
    void testHandleDiagnosticsImmediate_WithWarning() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "def hello() { return 'Hello' }";

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);
        when(compilationService.createCompilationUnit(any(CompilerConfiguration.class)))
                .thenReturn(compilationUnit);

        CompilationError warning =
                new CompilationError(
                        "Unused variable 'x'", 1, 5, uri, CompilationError.ErrorType.WARNING);
        ModuleNode moduleNode = mock(ModuleNode.class);
        CompilationResult result =
                CompilationResult.partial(moduleNode, Collections.singletonList(warning));
        when(compilationService.compileToPhaseWithResult(
                        eq(compilationUnit),
                        eq(sourceCode),
                        eq(uri),
                        eq(IncrementalCompilationService.CompilationPhase.SEMANTIC_ANALYSIS)))
                .thenReturn(result);

        // When
        CompletableFuture<Void> future =
                diagnosticsHandler.handleDiagnosticsImmediate(uri, languageClient);
        future.get(5, TimeUnit.SECONDS);

        // Then
        ArgumentCaptor<PublishDiagnosticsParams> captor =
                ArgumentCaptor.forClass(PublishDiagnosticsParams.class);
        verify(languageClient).publishDiagnostics(captor.capture());

        PublishDiagnosticsParams params = captor.getValue();
        assertEquals(uri, params.getUri());
        assertEquals(1, params.getDiagnostics().size());

        Diagnostic diagnostic = params.getDiagnostics().get(0);
        assertEquals(DiagnosticSeverity.Warning, diagnostic.getSeverity());
        assertNotNull(diagnostic.getCode());
        assertEquals("groovy-4001", diagnostic.getCode().getLeft()); // Unused variable warning
    }

    @UnitTest
    void testHandleDiagnosticsDebounced() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "def hello() { return 'Hello' }";

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);
        when(compilationService.createCompilationUnit(any(CompilerConfiguration.class)))
                .thenReturn(compilationUnit);
        when(compilationService.compileToPhaseWithResult(any(), any(), any(), any()))
                .thenReturn(CompilationResult.success(mock(ModuleNode.class)));

        // When - call multiple times rapidly
        var future1 = diagnosticsHandler.handleDiagnosticsDebounced(uri, languageClient);
        Thread.sleep(100);
        var future2 = diagnosticsHandler.handleDiagnosticsDebounced(uri, languageClient);
        Thread.sleep(100);
        var future3 = diagnosticsHandler.handleDiagnosticsDebounced(uri, languageClient);

        // Wait for debounce delay
        Thread.sleep(300);

        // Ensure futures are not null
        assertNotNull(future1);
        assertNotNull(future2);
        assertNotNull(future3);

        // Then - should only publish once due to debouncing
        verify(languageClient, times(1)).publishDiagnostics(any());

        // And scheduledTasks should be empty after execution
        Thread.sleep(100); // Wait for cleanup
        assertTrue(
                diagnosticsHandler.getScheduledTasksSize() == 0,
                "Scheduled tasks should be empty after execution");
    }

    @UnitTest
    void testHandleDiagnosticsImmediate_DocumentNotFound() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        when(documentManager.getDocumentContent(uri)).thenReturn(null);

        // When
        CompletableFuture<Void> future =
                diagnosticsHandler.handleDiagnosticsImmediate(uri, languageClient);
        future.get(5, TimeUnit.SECONDS);

        // Then
        verify(languageClient, never()).publishDiagnostics(any());
    }

    @UnitTest
    void testHandleDiagnosticsImmediate_CompilationException() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "def hello() { return 'Hello' }";

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);
        when(compilationService.createCompilationUnit(any(CompilerConfiguration.class)))
                .thenReturn(compilationUnit);
        when(compilationService.compileToPhaseWithResult(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Compilation failed"));

        // When
        CompletableFuture<Void> future =
                diagnosticsHandler.handleDiagnosticsImmediate(uri, languageClient);
        future.get(5, TimeUnit.SECONDS);

        // Then - should not throw, error should be logged
        verify(languageClient, never()).publishDiagnostics(any());
    }

    @UnitTest
    void testClearDiagnostics() throws Exception {
        // Given
        String uri = "file:///test.groovy";

        // When
        diagnosticsHandler.clearDiagnostics(uri, languageClient);

        // Then
        ArgumentCaptor<PublishDiagnosticsParams> captor =
                ArgumentCaptor.forClass(PublishDiagnosticsParams.class);
        verify(languageClient).publishDiagnostics(captor.capture());

        PublishDiagnosticsParams params = captor.getValue();
        assertEquals(uri, params.getUri());
        assertTrue(params.getDiagnostics().isEmpty());
    }

    @UnitTest
    void testClearDiagnostics_WithPendingTask() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        when(documentManager.getDocumentContent(uri)).thenReturn("test code");
        when(compilationService.createCompilationUnit(any())).thenReturn(compilationUnit);
        when(compilationService.compileToPhaseWithResult(any(), any(), any(), any()))
                .thenReturn(CompilationResult.success(mock(ModuleNode.class)));

        // Schedule a debounced task
        var future = diagnosticsHandler.handleDiagnosticsDebounced(uri, languageClient);
        assertNotNull(future);

        // When - clear diagnostics before the task executes
        diagnosticsHandler.clearDiagnostics(uri, languageClient);

        // Wait to ensure the task would have executed if not cancelled
        Thread.sleep(300);

        // Then - should only publish empty diagnostics once
        verify(languageClient, times(1)).publishDiagnostics(any());
        assertEquals(0, diagnosticsHandler.getScheduledTasksSize());
    }

    @UnitTest
    void testShutdown_CancelsAllTasks() throws Exception {
        // Given
        String uri1 = "file:///test1.groovy";
        String uri2 = "file:///test2.groovy";

        // Schedule multiple tasks
        var future1 = diagnosticsHandler.handleDiagnosticsDebounced(uri1, languageClient);
        var future2 = diagnosticsHandler.handleDiagnosticsDebounced(uri2, languageClient);
        assertNotNull(future1);
        assertNotNull(future2);

        // When
        diagnosticsHandler.shutdown();

        // Then
        assertEquals(0, diagnosticsHandler.getScheduledTasksSize());
    }
}
