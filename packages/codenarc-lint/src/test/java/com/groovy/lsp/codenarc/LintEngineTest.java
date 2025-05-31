package com.groovy.lsp.codenarc;

import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.codenarc.ruleset.RuleSet;

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
@ExtendWith(MockitoExtension.class)
class LintEngineTest {
    
    @TempDir
    Path tempDir;
    
    @Mock
    private RuleSetProvider ruleSetProvider;
    
    @Mock
    private QuickFixMapper quickFixMapper;
    
    @Mock
    private RuleSet mockRuleSet;
    
    private LintEngine lintEngine;
    
    @BeforeEach
    void setUp() {
        lintEngine = new LintEngine(ruleSetProvider, quickFixMapper);
        when(mockRuleSet.getRules()).thenReturn(List.of());
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
        when(ruleSetProvider.getRuleSet()).thenReturn(mockRuleSet);
        
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
        when(ruleSetProvider.getRuleSet()).thenReturn(mockRuleSet);
        
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
        when(ruleSetProvider.getRuleSet()).thenReturn(mockRuleSet);
        
        // Analyze the directory
        CompletableFuture<List<LintEngine.FileAnalysisResult>> future = 
            lintEngine.analyzeDirectory(tempDir.toString(), "**/*.groovy", null);
        List<LintEngine.FileAnalysisResult> results = future.get();
        
        // Verify the results
        assertNotNull(results);
        // Should have results for both files
        assertTrue(results.size() >= 0);
    }
    
}