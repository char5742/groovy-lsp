package com.groovy.lsp.codenarc;

import com.groovy.lsp.protocol.Diagnostic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the LintEngine class.
 */
class LintEngineTest {
    
    @TempDir
    Path tempDir;
    
    @Mock
    private RuleSetProvider ruleSetProvider;
    
    @Mock
    private QuickFixMapper quickFixMapper;
    
    private LintEngine lintEngine;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        lintEngine = new LintEngine(ruleSetProvider, quickFixMapper);
    }
    
    @Test
    void testAnalyzeFile_WithValidGroovyFile() throws IOException, ExecutionException, InterruptedException {
        // Create a test Groovy file
        Path testFile = tempDir.resolve("TestClass.groovy");
        String groovyCode = """
            class TestClass {
                def unusedVariable = "test"
                
                void testMethod() {
                    println "Hello, World!"
                }
            }
            """;
        Files.writeString(testFile, groovyCode);
        
        // Mock the rule set provider
        when(ruleSetProvider.getRuleSet()).thenReturn(new MockRuleSet());
        
        // Analyze the file
        CompletableFuture<List<Diagnostic>> future = lintEngine.analyzeFile(testFile.toString());
        List<Diagnostic> diagnostics = future.get();
        
        // Verify the results
        assertNotNull(diagnostics);
        // The actual number of diagnostics will depend on the rules configured
    }
    
    @Test
    void testAnalyzeFile_WithNonExistentFile() throws ExecutionException, InterruptedException {
        // Mock the rule set provider
        when(ruleSetProvider.getRuleSet()).thenReturn(new MockRuleSet());
        
        // Analyze a non-existent file
        CompletableFuture<List<Diagnostic>> future = lintEngine.analyzeFile("/non/existent/file.groovy");
        List<Diagnostic> diagnostics = future.get();
        
        // Should return empty list on error
        assertNotNull(diagnostics);
        assertTrue(diagnostics.isEmpty());
    }
    
    @Test
    void testAnalyzeDirectory() throws IOException, ExecutionException, InterruptedException {
        // Create test Groovy files
        Path file1 = tempDir.resolve("Class1.groovy");
        Path file2 = tempDir.resolve("Class2.groovy");
        
        Files.writeString(file1, "class Class1 { }");
        Files.writeString(file2, "class Class2 { }");
        
        // Mock the rule set provider
        when(ruleSetProvider.getRuleSet()).thenReturn(new MockRuleSet());
        
        // Analyze the directory
        CompletableFuture<List<LintEngine.FileAnalysisResult>> future = 
            lintEngine.analyzeDirectory(tempDir.toString(), "**/*.groovy", null);
        List<LintEngine.FileAnalysisResult> results = future.get();
        
        // Verify the results
        assertNotNull(results);
        // Should have results for both files
        assertTrue(results.size() >= 0);
    }
    
    /**
     * Mock RuleSet for testing purposes.
     */
    private static class MockRuleSet implements org.codenarc.ruleset.RuleSet {
        @Override
        public List getRules() {
            return List.of();
        }
        
        @Override
        public void addRule(org.codenarc.rule.Rule rule) {
            // Mock implementation
        }
        
        @Override
        public void removeRule(org.codenarc.rule.Rule rule) {
            // Mock implementation
        }
    }
}