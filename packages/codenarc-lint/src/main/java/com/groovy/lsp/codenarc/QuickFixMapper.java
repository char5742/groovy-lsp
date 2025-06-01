package com.groovy.lsp.codenarc;

import org.eclipse.lsp4j.*;
import org.codenarc.rule.Rule;
import org.codenarc.rule.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Maps CodeNarc violations to LSP QuickFixes (Code Actions).
 * This class provides automatic fixes for common violations
 * that can be resolved programmatically.
 */
public class QuickFixMapper {
    private static final Logger logger = LoggerFactory.getLogger(QuickFixMapper.class);
    
    private final Map<String, QuickFixProvider> quickFixProviders;
    private @Nullable String currentFilePath;  // Thread-local storage for current file path
    
    public QuickFixMapper() {
        this.quickFixProviders = new HashMap<>();
        registerDefaultProviders();
    }
    
    /**
     * Register default quick fix providers for common violations.
     */
    private void registerDefaultProviders() {
        // Imports-related quick fixes
        registerProvider("UnusedImport", this::createUnusedImportFix);
        registerProvider("DuplicateImport", this::createDuplicateImportFix);
        registerProvider("ImportFromSamePackage", this::createImportFromSamePackageFix);
        registerProvider("UnnecessaryGroovyImport", this::createUnnecessaryGroovyImportFix);
        
        // Basic rule quick fixes
        registerProvider("EmptyCatchBlock", this::createEmptyCatchBlockFix);
        registerProvider("EmptyElseBlock", this::createEmptyElseBlockFix);
        registerProvider("EmptyFinallyBlock", this::createEmptyFinallyBlockFix);
        registerProvider("EmptyIfStatement", this::createEmptyIfStatementFix);
        registerProvider("EmptyWhileStatement", this::createEmptyWhileStatementFix);
        
        // Convention quick fixes
        registerProvider("FieldName", this::createFieldNameFix);
        registerProvider("MethodName", this::createMethodNameFix);
        registerProvider("ClassName", this::createClassNameFix);
        registerProvider("VariableName", this::createVariableNameFix);
        
        // Groovyism quick fixes
        registerProvider("ExplicitArrayListInstantiation", this::createExplicitArrayListFix);
        registerProvider("ExplicitHashMapInstantiation", this::createExplicitHashMapFix);
        registerProvider("ExplicitLinkedListInstantiation", this::createExplicitLinkedListFix);
        registerProvider("GStringAsMapKey", this::createGStringAsMapKeyFix);
        
        // Design quick fixes
        registerProvider("SimpleDateFormatMissingLocale", this::createSimpleDateFormatFix);
        registerProvider("BooleanMethodReturnsNull", this::createBooleanMethodReturnsNullFix);
    }
    
    /**
     * Register a custom quick fix provider for a specific rule.
     * 
     * @param ruleName The name of the CodeNarc rule
     * @param provider The quick fix provider function
     */
    public void registerProvider(String ruleName, QuickFixProvider provider) {
        quickFixProviders.put(ruleName, provider);
    }
    
    /**
     * Get quick fixes for a given violation.
     * 
     * @param violation The CodeNarc violation
     * @return List of code actions that can fix the violation
     */
    public List<CodeAction> getQuickFixesForViolation(Violation violation) {
        return getQuickFixesForViolation(violation, (@Nullable String) null);
    }
    
    public List<CodeAction> getQuickFixesForViolation(Violation violation, @Nullable String filePath) {
        String ruleName = violation.getRule().getName();
        QuickFixProvider provider = quickFixProviders.get(ruleName);
        
        if (provider != null) {
            try {
                // Store file path in thread local for use by providers
                this.currentFilePath = filePath;
                return provider.createQuickFix(violation);
            } catch (Exception e) {
                logger.error("Error creating quick fix for rule: " + ruleName, e);
            } finally {
                this.currentFilePath = (@Nullable String) null;
            }
        }
        
        return Collections.emptyList();
    }
    
    // Quick fix implementations
    
    private List<CodeAction> createUnusedImportFix(Violation violation) {
        CodeAction action = new CodeAction();
        action.setTitle("Remove unused import");
        action.setKind(CodeActionKind.QuickFix);
        
        // Create text edit to remove the import line
        TextEdit edit = new TextEdit();
        edit.setRange(createRangeForLine(violation.getLineNumber() - 1));
        edit.setNewText(""); // Empty string to delete the line
        
        WorkspaceEdit workspaceEdit = new WorkspaceEdit();
        // Use the file path provided through the analysis context
        String filePath = this.currentFilePath != null ? this.currentFilePath : "unknown";
        workspaceEdit.setChanges(Map.of(filePath, List.of(edit)));
        action.setEdit(workspaceEdit);
        
        return List.of(action);
    }
    
    private List<CodeAction> createDuplicateImportFix(Violation violation) {
        CodeAction action = new CodeAction();
        action.setTitle("Remove duplicate import");
        action.setKind(CodeActionKind.QuickFix);
        
        TextEdit edit = new TextEdit();
        edit.setRange(createRangeForLine(violation.getLineNumber() - 1));
        edit.setNewText("");
        
        WorkspaceEdit workspaceEdit = new WorkspaceEdit();
        // Use the file path provided through the analysis context
        String filePath = this.currentFilePath != null ? this.currentFilePath : "unknown";
        workspaceEdit.setChanges(Map.of(filePath, List.of(edit)));
        action.setEdit(workspaceEdit);
        
        return List.of(action);
    }
    
    private List<CodeAction> createImportFromSamePackageFix(Violation violation) {
        CodeAction action = new CodeAction();
        action.setTitle("Remove import from same package");
        action.setKind(CodeActionKind.QuickFix);
        
        TextEdit edit = new TextEdit();
        edit.setRange(createRangeForLine(violation.getLineNumber() - 1));
        edit.setNewText("");
        
        WorkspaceEdit workspaceEdit = new WorkspaceEdit();
        // Use the file path provided through the analysis context
        String filePath = this.currentFilePath != null ? this.currentFilePath : "unknown";
        workspaceEdit.setChanges(Map.of(filePath, List.of(edit)));
        action.setEdit(workspaceEdit);
        
        return List.of(action);
    }
    
    private List<CodeAction> createUnnecessaryGroovyImportFix(Violation violation) {
        CodeAction action = new CodeAction();
        action.setTitle("Remove unnecessary Groovy import");
        action.setKind(CodeActionKind.QuickFix);
        
        TextEdit edit = new TextEdit();
        edit.setRange(createRangeForLine(violation.getLineNumber() - 1));
        edit.setNewText("");
        
        WorkspaceEdit workspaceEdit = new WorkspaceEdit();
        // Use the file path provided through the analysis context
        String filePath = this.currentFilePath != null ? this.currentFilePath : "unknown";
        workspaceEdit.setChanges(Map.of(filePath, List.of(edit)));
        action.setEdit(workspaceEdit);
        
        return List.of(action);
    }
    
    private List<CodeAction> createEmptyCatchBlockFix(Violation violation) {
        CodeAction action = new CodeAction();
        action.setTitle("Add log statement to catch block");
        action.setKind(CodeActionKind.QuickFix);
        
        // This would need more context to properly implement
        // For now, return a placeholder action
        return List.of(action);
    }
    
    private List<CodeAction> createEmptyElseBlockFix(Violation violation) {
        CodeAction action = new CodeAction();
        action.setTitle("Remove empty else block");
        action.setKind(CodeActionKind.QuickFix);
        
        return List.of(action);
    }
    
    private List<CodeAction> createEmptyFinallyBlockFix(Violation violation) {
        CodeAction action = new CodeAction();
        action.setTitle("Remove empty finally block");
        action.setKind(CodeActionKind.QuickFix);
        
        return List.of(action);
    }
    
    private List<CodeAction> createEmptyIfStatementFix(Violation violation) {
        CodeAction action = new CodeAction();
        action.setTitle("Remove empty if statement");
        action.setKind(CodeActionKind.QuickFix);
        
        return List.of(action);
    }
    
    private List<CodeAction> createEmptyWhileStatementFix(Violation violation) {
        CodeAction action = new CodeAction();
        action.setTitle("Remove empty while statement");
        action.setKind(CodeActionKind.QuickFix);
        
        return List.of(action);
    }
    
    private List<CodeAction> createFieldNameFix(Violation violation) {
        // Would need to parse the field name and suggest proper casing
        CodeAction action = new CodeAction();
        action.setTitle("Fix field name to follow conventions");
        action.setKind(CodeActionKind.QuickFix);
        
        return List.of(action);
    }
    
    private List<CodeAction> createMethodNameFix(Violation violation) {
        CodeAction action = new CodeAction();
        action.setTitle("Fix method name to follow conventions");
        action.setKind(CodeActionKind.QuickFix);
        
        return List.of(action);
    }
    
    private List<CodeAction> createClassNameFix(Violation violation) {
        CodeAction action = new CodeAction();
        action.setTitle("Fix class name to follow conventions");
        action.setKind(CodeActionKind.QuickFix);
        
        return List.of(action);
    }
    
    private List<CodeAction> createVariableNameFix(Violation violation) {
        CodeAction action = new CodeAction();
        action.setTitle("Fix variable name to follow conventions");
        action.setKind(CodeActionKind.QuickFix);
        
        return List.of(action);
    }
    
    private List<CodeAction> createExplicitArrayListFix(Violation violation) {
        CodeAction action = new CodeAction();
        action.setTitle("Use Groovy list literal []");
        action.setKind(CodeActionKind.QuickFix);
        
        // Would need to parse and replace new ArrayList() with []
        return List.of(action);
    }
    
    private List<CodeAction> createExplicitHashMapFix(Violation violation) {
        CodeAction action = new CodeAction();
        action.setTitle("Use Groovy map literal [:]");
        action.setKind(CodeActionKind.QuickFix);
        
        // Would need to parse and replace new HashMap() with [:]
        return List.of(action);
    }
    
    private List<CodeAction> createExplicitLinkedListFix(Violation violation) {
        CodeAction action = new CodeAction();
        action.setTitle("Use Groovy list literal [] as LinkedList");
        action.setKind(CodeActionKind.QuickFix);
        
        return List.of(action);
    }
    
    private List<CodeAction> createGStringAsMapKeyFix(Violation violation) {
        CodeAction action = new CodeAction();
        action.setTitle("Convert GString to String");
        action.setKind(CodeActionKind.QuickFix);
        
        return List.of(action);
    }
    
    private List<CodeAction> createSimpleDateFormatFix(Violation violation) {
        CodeAction action = new CodeAction();
        action.setTitle("Add Locale parameter to SimpleDateFormat");
        action.setKind(CodeActionKind.QuickFix);
        
        return List.of(action);
    }
    
    private List<CodeAction> createBooleanMethodReturnsNullFix(Violation violation) {
        CodeAction action = new CodeAction();
        action.setTitle("Return false instead of null");
        action.setKind(CodeActionKind.QuickFix);
        
        return List.of(action);
    }
    
    /**
     * Create a range for a complete line.
     */
    private Range createRangeForLine(int lineNumber) {
        return new Range(
            new Position(lineNumber, 0),
            new Position(lineNumber + 1, 0)
        );
    }
    
    /**
     * Functional interface for quick fix providers.
     */
    @FunctionalInterface
    public interface QuickFixProvider {
        List<CodeAction> createQuickFix(Violation violation);
    }
    
    /**
     * Get all registered rule names that have quick fixes.
     * 
     * @return Set of rule names with quick fix support
     */
    public Set<String> getSupportedRules() {
        return new HashSet<>(quickFixProviders.keySet());
    }
}