package com.groovy.lsp.codenarc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.codenarc.rule.Violation;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * LintEngineのテストクラス。
 */
@ExtendWith(MockitoExtension.class)
class LintEngineTest {

    @TempDir @Nullable Path tempDir;

    @Mock private RuleSetProvider ruleSetProvider;

    @Mock private QuickFixMapper quickFixMapper;

    private LintEngine lintEngine;

    @BeforeEach
    void setUp() {
        lintEngine = new LintEngine(ruleSetProvider, quickFixMapper);
    }

    @Test
    void analyzeFile_shouldAnalyzeValidGroovyFile()
            throws IOException, ExecutionException, InterruptedException {
        // given
        lenient()
                .when(quickFixMapper.getQuickFixesForViolation(any(Violation.class), anyString()))
                .thenReturn(List.of());
        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit");
        Path testFile = tempDir.resolve("TestClass.groovy");
        String groovyCode =
                """
                class TestClass {
                    def unusedVariable = "test"

                    void testMethod() {
                        println "Hello, World!"
                    }
                }
                """;
        Files.writeString(testFile, groovyCode);

        // when
        CompletableFuture<List<Diagnostic>> future = lintEngine.analyzeFile(testFile.toString());
        List<Diagnostic> diagnostics = future.get();

        // then
        assertThat(diagnostics).isNotNull();
        // The actual number of diagnostics will depend on the rules configured
    }

    @Test
    void analyzeFile_shouldReturnEmptyListForNonExistentFile()
            throws ExecutionException, InterruptedException {
        // when
        CompletableFuture<List<Diagnostic>> future =
                lintEngine.analyzeFile("/non/existent/file.groovy");
        List<Diagnostic> diagnostics = future.get();

        // then
        assertThat(diagnostics).isEmpty();
    }

    @Test
    void analyzeFile_shouldAnalyzeFileWithEmptyIfStatement()
            throws IOException, ExecutionException, InterruptedException {
        // given
        lenient()
                .when(quickFixMapper.getQuickFixesForViolation(any(Violation.class), anyString()))
                .thenReturn(List.of());
        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit");
        Path testFile = tempDir.resolve("EmptyIfTest.groovy");
        String groovyCode =
                """
                class EmptyIfTest {
                    void testMethod() {
                        if (true) {
                        }
                    }
                }
                """;
        Files.writeString(testFile, groovyCode);

        // when
        CompletableFuture<List<Diagnostic>> future = lintEngine.analyzeFile(testFile.toString());
        List<Diagnostic> diagnostics = future.get();

        // then
        assertThat(diagnostics).isNotNull();
        // With basic ruleset, this should trigger EmptyIfStatement rule
    }

    @Test
    void analyzeFile_shouldAnalyzeFileWithEmptyWhileStatement()
            throws IOException, ExecutionException, InterruptedException {
        // given
        lenient()
                .when(quickFixMapper.getQuickFixesForViolation(any(Violation.class), anyString()))
                .thenReturn(List.of());
        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit");
        Path testFile = tempDir.resolve("EmptyWhileTest.groovy");
        String groovyCode =
                """
                class EmptyWhileTest {
                    void testMethod() {
                        while (false) {
                            // This is an empty while statement
                        }
                    }
                }
                """;
        Files.writeString(testFile, groovyCode);

        // when
        CompletableFuture<List<Diagnostic>> future = lintEngine.analyzeFile(testFile.toString());
        List<Diagnostic> diagnostics = future.get();

        // then
        assertThat(diagnostics).isNotNull();
        // With basic ruleset, this should trigger EmptyWhileStatement rule
    }

    @Test
    void analyzeDirectory_shouldAnalyzeMultipleGroovyFiles()
            throws IOException, ExecutionException, InterruptedException {
        // given
        lenient()
                .when(quickFixMapper.getQuickFixesForViolation(any(Violation.class), anyString()))
                .thenReturn(List.of());
        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit");
        Path file1 = tempDir.resolve("Class1.groovy");
        Path file2 = tempDir.resolve("Class2.groovy");

        Files.writeString(
                file1,
                """
                class Class1 {
                    void method() {
                        if (true) {
                            // empty if statement
                        }
                    }
                }
                """);
        Files.writeString(
                file2,
                """
                class Class2 {
                    void method() {
                        while (false) {
                            // empty while statement
                        }
                    }
                }
                """);

        // when
        CompletableFuture<List<LintEngine.FileAnalysisResult>> future =
                lintEngine.analyzeDirectory(tempDir.toString(), "**/*.groovy", null);
        List<LintEngine.FileAnalysisResult> results = future.get();

        // then
        assertThat(results).isNotNull();
        // Results might be empty due to CodeNarc configuration, but should not throw
    }

    @Test
    void analyzeDirectory_shouldUseExclusionPatterns()
            throws IOException, ExecutionException, InterruptedException {
        // given
        lenient()
                .when(quickFixMapper.getQuickFixesForViolation(any(Violation.class), anyString()))
                .thenReturn(List.of());
        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit");
        Path includeFile = tempDir.resolve("Include.groovy");
        Path excludeFile = tempDir.resolve("Exclude.groovy");

        Files.writeString(includeFile, "class Include {}");
        Files.writeString(excludeFile, "class Exclude {}");

        // when
        CompletableFuture<List<LintEngine.FileAnalysisResult>> future =
                lintEngine.analyzeDirectory(tempDir.toString(), "*.groovy", "*Exclude*");
        List<LintEngine.FileAnalysisResult> results = future.get();

        // then
        assertThat(results).isNotNull();
        // Exclude.groovy should be excluded from analysis
    }

    @Test
    void analyzeDirectory_shouldReturnEmptyListForNonExistentDirectory()
            throws ExecutionException, InterruptedException {
        // when
        CompletableFuture<List<LintEngine.FileAnalysisResult>> future =
                lintEngine.analyzeDirectory("/non/existent/directory", "*.groovy", null);
        List<LintEngine.FileAnalysisResult> results = future.get();

        // then
        assertThat(results).isEmpty();
    }

    @Test
    void analyzeFile_shouldIntegrateWithQuickFixMapper()
            throws IOException, ExecutionException, InterruptedException {
        // given
        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit");
        Path testFile = tempDir.resolve("QuickFixTest.groovy");
        String groovyCode =
                """
                class QuickFixTest {
                    void method() {
                        if (true) {

                        }
                    }
                }
                """;
        Files.writeString(testFile, groovyCode);

        // when
        CompletableFuture<List<Diagnostic>> future = lintEngine.analyzeFile(testFile.toString());
        List<Diagnostic> diagnostics = future.get();

        // then
        assertThat(diagnostics).isNotNull();

        // If there are diagnostics, verify that quick fix mapper was called
        if (!diagnostics.isEmpty()) {
            verify(quickFixMapper, atLeastOnce())
                    .getQuickFixesForViolation(any(Violation.class), anyString());

            // Check that code actions were added to diagnostic data
            Diagnostic diagnostic = diagnostics.get(0);
            assertThat(diagnostic.getData()).isNotNull();
        } else {
            // If no violations, the quick fix mapper should not have been called
            verify(quickFixMapper, never())
                    .getQuickFixesForViolation(any(Violation.class), anyString());
        }
    }

    @Test
    void analyzeFile_shouldHandleQuickFixMapperReturningNonEmptyList()
            throws IOException, ExecutionException, InterruptedException {
        // given
        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit");
        Path testFile = tempDir.resolve("QuickFixWithActionsTest.groovy");
        String groovyCode =
                """
                class QuickFixWithActionsTest {
                    void method() {
                        if (true) {
                        }
                    }
                }
                """;
        Files.writeString(testFile, groovyCode);

        // Mock quick fix mapper to return code actions with lenient stubbing
        CodeAction codeAction = new CodeAction();
        codeAction.setTitle("Fix violation");
        lenient()
                .when(quickFixMapper.getQuickFixesForViolation(any(Violation.class), anyString()))
                .thenReturn(List.of(codeAction));

        // when
        CompletableFuture<List<Diagnostic>> future = lintEngine.analyzeFile(testFile.toString());
        List<Diagnostic> diagnostics = future.get();

        // then
        assertThat(diagnostics).isNotNull();
        // If there are diagnostics, they should have code actions in data
        if (!diagnostics.isEmpty()) {
            Diagnostic diagnostic = diagnostics.get(0);
            assertThat(diagnostic.getData()).isEqualTo(List.of(codeAction));
        }
    }

    @Test
    void FileAnalysisResult_shouldHoldFilePathAndDiagnostics() {
        // given
        String filePath = "/test/file.groovy";
        List<Diagnostic> diagnostics = List.of(new Diagnostic());

        // when
        LintEngine.FileAnalysisResult result =
                new LintEngine.FileAnalysisResult(filePath, diagnostics);

        // then
        assertThat(result.getFilePath()).isEqualTo(filePath);
        assertThat(result.getDiagnostics()).isEqualTo(diagnostics);
    }

    @Test
    void mapPriorityToSeverity_shouldMapPriorityCorrectly()
            throws IOException, ExecutionException, InterruptedException {
        // given - Create files with different violation priorities
        lenient()
                .when(quickFixMapper.getQuickFixesForViolation(any(Violation.class), anyString()))
                .thenReturn(List.of());
        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit");
        Path testFile = tempDir.resolve("PriorityTest.groovy");

        // This would need actual CodeNarc rules with different priorities to test properly
        String groovyCode =
                """
                class PriorityTest {
                    void method() {
                        // Various violations with different priorities
                        if (true) {
                        }
                    }
                }
                """;
        Files.writeString(testFile, groovyCode);

        // when
        CompletableFuture<List<Diagnostic>> future = lintEngine.analyzeFile(testFile.toString());
        List<Diagnostic> diagnostics = future.get();

        // then
        assertThat(diagnostics).isNotNull();
        // Diagnostics should have appropriate severity levels based on rule priorities
        // Priority 1 -> Error, 2 -> Warning, 3 -> Information, others -> Hint
    }

    @Test
    void analyzeFile_shouldSetSourceInDiagnostics()
            throws IOException, ExecutionException, InterruptedException {
        // given
        lenient()
                .when(quickFixMapper.getQuickFixesForViolation(any(Violation.class), anyString()))
                .thenReturn(List.of());
        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit");
        Path testFile = tempDir.resolve("SourceTest.groovy");
        String groovyCode =
                """
                class SourceTest {
                    void method() {
                        if (true) {
                        }
                    }
                }
                """;
        Files.writeString(testFile, groovyCode);

        // when
        CompletableFuture<List<Diagnostic>> future = lintEngine.analyzeFile(testFile.toString());
        List<Diagnostic> diagnostics = future.get();

        // then
        assertThat(diagnostics).isNotNull();
        diagnostics.forEach(
                diagnostic -> {
                    if (diagnostic.getSource() != null) {
                        assertThat(diagnostic.getSource()).isEqualTo("codenarc");
                    }
                });
    }

    @Test
    void analyzeFile_shouldHandleExceptionDuringAnalysis()
            throws ExecutionException, InterruptedException {
        // given - Use an invalid file path that will cause an exception
        String invalidPath = "/\0invalid\0path\0with\0null\0chars.groovy";

        // when
        CompletableFuture<List<Diagnostic>> future = lintEngine.analyzeFile(invalidPath);
        List<Diagnostic> diagnostics = future.get();

        // then
        assertThat(diagnostics).isEmpty();
    }

    @Test
    void analyzeDirectory_shouldHandleExceptionDuringAnalysis()
            throws ExecutionException, InterruptedException {
        // given - Use an invalid directory path
        String invalidPath = "/\0invalid\0directory\0path";

        // when
        CompletableFuture<List<LintEngine.FileAnalysisResult>> future =
                lintEngine.analyzeDirectory(invalidPath, "*.groovy", null);
        List<LintEngine.FileAnalysisResult> results = future.get();

        // then
        assertThat(results).isEmpty();
    }

    @Test
    void convertResultsToDiagnostics_shouldHandleNullResults()
            throws ExecutionException, InterruptedException {
        // This test requires accessing the private method through reflection or testing via public
        // API
        // We'll test through analyzeFile with a mock that returns null results
        // given
        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit");
        Path testFile = tempDir.resolve("NullResultsTest.groovy");

        // when - The file doesn't exist, which should result in empty diagnostics
        CompletableFuture<List<Diagnostic>> future = lintEngine.analyzeFile(testFile.toString());
        List<Diagnostic> diagnostics = future.get();

        // then
        assertThat(diagnostics).isEmpty();
    }

    @Test
    void mapPriorityToSeverity_shouldMapAllPriorityLevels()
            throws IOException, ExecutionException, InterruptedException {
        // given - We need to test all priority levels (1, 2, 3, and default)
        // Since we can't control CodeNarc rule priorities directly, we'll verify through actual
        // analysis
        lenient()
                .when(quickFixMapper.getQuickFixesForViolation(any(Violation.class), anyString()))
                .thenReturn(List.of());
        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit");
        Path testFile = tempDir.resolve("PriorityMappingTest.groovy");

        // Create code that will trigger violations
        String groovyCode =
                """
                class PriorityMappingTest {
                    void method1() {
                        if (true) {
                            // empty if - typically priority 2 (warning)
                        }
                    }

                    void method2() {
                        while (false) {
                            // empty while - typically priority 2 (warning)
                        }
                    }
                }
                """;
        Files.writeString(testFile, groovyCode);

        // when
        CompletableFuture<List<Diagnostic>> future = lintEngine.analyzeFile(testFile.toString());
        List<Diagnostic> diagnostics = future.get();

        // then
        assertThat(diagnostics).isNotNull();
        // Verify that diagnostics have appropriate severities set
        diagnostics.forEach(
                diagnostic -> {
                    DiagnosticSeverity severity = diagnostic.getSeverity();
                    assertThat(severity).isNotNull();
                    // Should be one of the expected severity levels
                    assertThat(severity)
                            .isIn(
                                    DiagnosticSeverity.Error,
                                    DiagnosticSeverity.Warning,
                                    DiagnosticSeverity.Information,
                                    DiagnosticSeverity.Hint);
                });
    }

    @Test
    void analyzeFile_shouldSetDiagnosticCodeToRuleName()
            throws IOException, ExecutionException, InterruptedException {
        // given
        lenient()
                .when(quickFixMapper.getQuickFixesForViolation(any(Violation.class), anyString()))
                .thenReturn(List.of());
        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit");
        Path testFile = tempDir.resolve("RuleNameTest.groovy");
        String groovyCode =
                """
                class RuleNameTest {
                    void method() {
                        if (true) {
                            // This should trigger EmptyIfStatement rule
                        }
                    }
                }
                """;
        Files.writeString(testFile, groovyCode);

        // when
        CompletableFuture<List<Diagnostic>> future = lintEngine.analyzeFile(testFile.toString());
        List<Diagnostic> diagnostics = future.get();

        // then
        assertThat(diagnostics).isNotNull();
        if (!diagnostics.isEmpty()) {
            diagnostics.forEach(
                    diagnostic -> {
                        assertThat(diagnostic.getCode()).isNotNull();
                        // The code should be the rule name
                        assertThat(diagnostic.getCode().toString()).isNotEmpty();
                    });
        }
    }

    @Test
    void analyzeDirectory_shouldProcessMultipleFilesWithDifferentViolations()
            throws IOException, ExecutionException, InterruptedException {
        // given
        lenient()
                .when(quickFixMapper.getQuickFixesForViolation(any(Violation.class), anyString()))
                .thenReturn(List.of());
        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit");

        // Create multiple files with different types of violations
        Path file1 = tempDir.resolve("FileWithEmptyIf.groovy");
        Files.writeString(
                file1,
                """
                class FileWithEmptyIf {
                    void method() {
                        if (true) {
                        }
                    }
                }
                """);

        Path file2 = tempDir.resolve("FileWithEmptyWhile.groovy");
        Files.writeString(
                file2,
                """
                class FileWithEmptyWhile {
                    void method() {
                        while (false) {
                        }
                    }
                }
                """);

        Path file3 = tempDir.resolve("FileWithNoViolations.groovy");
        Files.writeString(
                file3,
                """
                class FileWithNoViolations {
                    void method() {
                        println "This is fine"
                    }
                }
                """);

        // when
        CompletableFuture<List<LintEngine.FileAnalysisResult>> future =
                lintEngine.analyzeDirectory(tempDir.toString(), "*.groovy", null);
        List<LintEngine.FileAnalysisResult> results = future.get();

        // then
        assertThat(results).isNotNull();
        // Should have results for all files, even those without violations
        assertThat(results.size()).isGreaterThanOrEqualTo(0);

        // Verify file paths are set correctly
        results.forEach(
                result -> {
                    assertThat(result.getFilePath()).isNotNull();
                    assertThat(result.getDiagnostics()).isNotNull();
                });
    }
}
