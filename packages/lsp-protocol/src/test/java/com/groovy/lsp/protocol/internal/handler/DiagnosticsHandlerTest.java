package com.groovy.lsp.protocol.internal.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.groovy.lsp.groovy.core.api.CompilationResult;
import com.groovy.lsp.groovy.core.api.CompilationResult.CompilationError;
import com.groovy.lsp.groovy.core.api.IncrementalCompilationService;
import com.groovy.lsp.protocol.api.IServiceRouter;
import com.groovy.lsp.protocol.internal.document.DocumentManager;
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
import org.junit.jupiter.api.Test;
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

    @Test
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

    @Test
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
    }

    @Test
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

    @Test
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
    }

    @Test
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
        diagnosticsHandler.handleDiagnosticsDebounced(uri, languageClient);
        Thread.sleep(100);
        diagnosticsHandler.handleDiagnosticsDebounced(uri, languageClient);
        Thread.sleep(100);
        diagnosticsHandler.handleDiagnosticsDebounced(uri, languageClient);

        // Wait for debounce delay
        Thread.sleep(300);

        // Then - should only publish once due to debouncing
        verify(languageClient, times(1)).publishDiagnostics(any());
    }

    @Test
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

    @Test
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
}
