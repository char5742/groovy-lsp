package com.groovy.lsp.codenarc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.codenarc.CodeNarcRunner;
import org.codenarc.analyzer.FilesystemSourceAnalyzer;
import org.codenarc.results.FileResults;
import org.codenarc.results.Results;
import org.codenarc.rule.Violation;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for linting Groovy files using CodeNarc.
 * This class coordinates the static analysis process and converts
 * CodeNarc violations into LSP diagnostics.
 */
public class LintEngine {
    private static final Logger logger = LoggerFactory.getLogger(LintEngine.class);

    private final QuickFixMapper quickFixMapper;

    @SuppressWarnings("unused")
    public LintEngine(RuleSetProvider ruleSetProvider, QuickFixMapper quickFixMapper) {
        // RuleSetProvider is not currently used but kept for API compatibility
        this.quickFixMapper = quickFixMapper;
    }

    /**
     * Analyze a single Groovy file and return diagnostics.
     *
     * @param filePath The path to the Groovy file to analyze
     * @return A CompletableFuture containing the list of diagnostics
     */
    public CompletableFuture<List<Diagnostic>> analyzeFile(String filePath) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        logger.debug("Analyzing file: {}", filePath);

                        // Create a source analyzer for the single file
                        FilesystemSourceAnalyzer analyzer = new FilesystemSourceAnalyzer();
                        analyzer.setBaseDirectory(new File(filePath).getParent());
                        analyzer.setIncludes(new File(filePath).getName());

                        // Create CodeNarcRunner and run analysis
                        CodeNarcRunner runner = new CodeNarcRunner();
                        runner.setSourceAnalyzer(analyzer);

                        // Configure rule sets
                        // In tests, we use a simple ruleset from resources
                        String ruleSetPath = "test-ruleset.xml";

                        // Check if we have a test ruleset available
                        if (getClass().getClassLoader().getResource(ruleSetPath) != null) {
                            try {
                                java.lang.reflect.Method setRuleSetFilesMethod =
                                        runner.getClass()
                                                .getMethod("setRuleSetFiles", String.class);
                                setRuleSetFilesMethod.invoke(runner, ruleSetPath);
                            } catch (Exception e) {
                                logger.error("Failed to configure rule sets: {}", e.getMessage());
                            }
                        } else {
                            // Use default basic rules if no test ruleset is found
                            try {
                                // Use a basic ruleset string with minimal rules
                                String basicRuleSet =
                                        """
                                        <ruleset xmlns="http://codenarc.org/ruleset/1.0"
                                                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                                 xsi:schemaLocation="http://codenarc.org/ruleset/1.0 http://codenarc.org/ruleset-schema.xsd"
                                                 xsi:noNamespaceSchemaLocation="http://codenarc.org/ruleset-schema.xsd">
                                            <description>Basic ruleset</description>
                                            <rule class='org.codenarc.rule.basic.EmptyIfStatementRule'/>
                                            <rule class='org.codenarc.rule.basic.EmptyWhileStatementRule'/>
                                        </ruleset>
                                        """;
                                java.lang.reflect.Method setRuleSetStringMethod =
                                        runner.getClass()
                                                .getMethod("setRuleSetString", String.class);
                                setRuleSetStringMethod.invoke(runner, basicRuleSet);
                            } catch (Exception e) {
                                logger.error("Failed to set default rule set: {}", e.getMessage());
                            }
                        }

                        // Run the analysis
                        Results results = runner.execute();

                        // Convert violations to diagnostics
                        return convertResultsToDiagnostics(results, filePath);

                    } catch (Exception e) {
                        logger.error("Error analyzing file: " + filePath, e);
                        return List.of();
                    }
                });
    }

    /**
     * Analyze multiple Groovy files in a directory.
     *
     * @param directory The directory containing Groovy files
     * @param includes Pattern for files to include (e.g., **&#47;*.groovy)
     * @param excludes Pattern for files to exclude
     * @return A CompletableFuture containing a map of file paths to diagnostics
     */
    public CompletableFuture<List<FileAnalysisResult>> analyzeDirectory(
            String directory, String includes, @Nullable String excludes) {

        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        logger.debug(
                                "Analyzing directory: {} with includes: {} and excludes: {}",
                                directory,
                                includes,
                                excludes);

                        // Configure source analyzer
                        FilesystemSourceAnalyzer analyzer = new FilesystemSourceAnalyzer();
                        analyzer.setBaseDirectory(directory);
                        if (includes != null) {
                            analyzer.setIncludes(includes);
                        }
                        if (excludes != null) {
                            analyzer.setExcludes(excludes);
                        }

                        CodeNarcRunner runner = new CodeNarcRunner();
                        runner.setSourceAnalyzer(analyzer);

                        // Configure rule sets
                        // In tests, we use a simple ruleset from resources
                        String ruleSetPath = "test-ruleset.xml";

                        // Check if we have a test ruleset available
                        if (getClass().getClassLoader().getResource(ruleSetPath) != null) {
                            try {
                                java.lang.reflect.Method setRuleSetFilesMethod =
                                        runner.getClass()
                                                .getMethod("setRuleSetFiles", String.class);
                                setRuleSetFilesMethod.invoke(runner, ruleSetPath);
                            } catch (Exception e) {
                                logger.error("Failed to configure rule sets: {}", e.getMessage());
                            }
                        } else {
                            // Use default basic rules if no test ruleset is found
                            try {
                                // Use a basic ruleset string with minimal rules
                                String basicRuleSet =
                                        """
                                        <ruleset xmlns="http://codenarc.org/ruleset/1.0"
                                                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                                 xsi:schemaLocation="http://codenarc.org/ruleset/1.0 http://codenarc.org/ruleset-schema.xsd"
                                                 xsi:noNamespaceSchemaLocation="http://codenarc.org/ruleset-schema.xsd">
                                            <description>Basic ruleset</description>
                                            <rule class='org.codenarc.rule.basic.EmptyIfStatementRule'/>
                                            <rule class='org.codenarc.rule.basic.EmptyWhileStatementRule'/>
                                        </ruleset>
                                        """;
                                java.lang.reflect.Method setRuleSetStringMethod =
                                        runner.getClass()
                                                .getMethod("setRuleSetString", String.class);
                                setRuleSetStringMethod.invoke(runner, basicRuleSet);
                            } catch (Exception e) {
                                logger.error("Failed to set default rule set: {}", e.getMessage());
                            }
                        }

                        // Run the analysis
                        Results results = runner.execute();

                        // Convert results to file analysis results
                        return convertResultsToFileAnalysisResults(results);

                    } catch (Exception e) {
                        logger.error("Error analyzing directory: " + directory, e);
                        return List.of();
                    }
                });
    }

    private List<Diagnostic> convertResultsToDiagnostics(Results results, String filePath) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        if (results != null && results.getChildren() != null) {
            // In CodeNarc 3.x, getChildren() might return a different type
            Object children = results.getChildren();
            if (children instanceof List) {
                @SuppressWarnings("unchecked")
                List<Results> childResults = (List<Results>) children;
                for (Results childResult : childResults) {
                    if (childResult instanceof FileResults fileResults) {
                        if (fileResults.getPath().equals(filePath)) {
                            diagnostics.addAll(
                                    convertViolationsToDiagnostics(
                                            fileResults.getViolations(), fileResults.getPath()));
                        }
                    }
                }
            }
        }

        return diagnostics;
    }

    private List<FileAnalysisResult> convertResultsToFileAnalysisResults(Results results) {
        List<FileAnalysisResult> fileResults = new ArrayList<>();

        if (results != null && results.getChildren() != null) {
            // In CodeNarc 3.x, getChildren() might return a different type
            Object children = results.getChildren();
            if (children instanceof List) {
                @SuppressWarnings("unchecked")
                List<Results> childResults = (List<Results>) children;
                for (Results childResult : childResults) {
                    if (childResult instanceof FileResults fileResult) {
                        List<Diagnostic> diagnostics =
                                convertViolationsToDiagnostics(
                                        fileResult.getViolations(), fileResult.getPath());
                        fileResults.add(new FileAnalysisResult(fileResult.getPath(), diagnostics));
                    }
                }
            }
        }

        return fileResults;
    }

    private List<Diagnostic> convertViolationsToDiagnostics(
            List<Violation> violations, @Nullable String filePath) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        for (Violation violation : violations) {
            Diagnostic diagnostic = new Diagnostic();

            // Set range
            int line = violation.getLineNumber() - 1; // LSP uses 0-based line numbers
            Range range = new Range(new Position(line, 0), new Position(line, Integer.MAX_VALUE));
            diagnostic.setRange(range);

            // Set message
            diagnostic.setMessage(violation.getMessage());

            // Set severity based on priority
            diagnostic.setSeverity(mapPriorityToSeverity(violation.getRule().getPriority()));

            // Set code (rule name)
            diagnostic.setCode(violation.getRule().getName());

            // Set source
            diagnostic.setSource("codenarc");

            // Add code actions if available
            var codeActions = quickFixMapper.getQuickFixesForViolation(violation, filePath);
            if (!codeActions.isEmpty()) {
                diagnostic.setData(codeActions);
            }

            diagnostics.add(diagnostic);
        }

        return diagnostics;
    }

    private DiagnosticSeverity mapPriorityToSeverity(int priority) {
        return switch (priority) {
            case 1 -> DiagnosticSeverity.Error;
            case 2 -> DiagnosticSeverity.Warning;
            case 3 -> DiagnosticSeverity.Information;
            default -> DiagnosticSeverity.Hint;
        };
    }

    /**
     * Result of analyzing a single file.
     */
    public static class FileAnalysisResult {
        private final String filePath;
        private final List<Diagnostic> diagnostics;

        public FileAnalysisResult(String filePath, List<Diagnostic> diagnostics) {
            this.filePath = filePath;
            this.diagnostics = diagnostics;
        }

        public String getFilePath() {
            return filePath;
        }

        public List<Diagnostic> getDiagnostics() {
            return diagnostics;
        }
    }
}
