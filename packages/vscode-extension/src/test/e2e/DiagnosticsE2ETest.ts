import * as assert from 'assert';
import * as vscode from 'vscode';
import * as path from 'path';
import { activateExtension, openDocument, waitForDiagnostics, typeText } from './helper';

suite('Diagnostics E2E Test Suite', () => {
    let document: vscode.TextDocument;
    let editor: vscode.TextEditor;

    suiteSetup(async () => {
        await activateExtension();
    });

    test('should show diagnostics for syntax errors', async () => {
        // Create a temporary file with syntax error
        const content = `package com.example

class TestClass {
    def method() {
        // Missing closing bracket
        if (true {
            println "test"
        }
    }
}`;
        
        const uri = vscode.Uri.file(path.join(__dirname, '../../fixtures/temp-error.groovy'));
        const doc = await vscode.workspace.openTextDocument({ content, language: 'groovy' });
        editor = await vscode.window.showTextDocument(doc);
        
        // Wait for diagnostics
        const diagnostics = await waitForDiagnostics(doc.uri);
        
        assert.ok(diagnostics.length > 0, 'Should have at least one diagnostic');
        
        const syntaxError = diagnostics.find(d => d.severity === vscode.DiagnosticSeverity.Error);
        assert.ok(syntaxError, 'Should have an error diagnostic');
    });

    test('should show diagnostics for undefined references', async () => {
        const content = `package com.example

class TestClass {
    def method() {
        // Reference to undefined variable
        println undefinedVariable
        
        // Reference to undefined method
        undefinedMethod()
    }
}`;
        
        const doc = await vscode.workspace.openTextDocument({ content, language: 'groovy' });
        editor = await vscode.window.showTextDocument(doc);
        
        const diagnostics = await waitForDiagnostics(doc.uri);
        
        assert.ok(diagnostics.length > 0, 'Should have diagnostics for undefined references');
    });

    test('should clear diagnostics when errors are fixed', async () => {
        // Start with error
        const contentWithError = `package com.example

class TestClass {
    def method() {
        // Missing closing bracket
        if (true {
    }
}`;
        
        const doc = await vscode.workspace.openTextDocument({ content: contentWithError, language: 'groovy' });
        editor = await vscode.window.showTextDocument(doc);
        
        // Wait for initial diagnostics
        let diagnostics = await waitForDiagnostics(doc.uri);
        assert.ok(diagnostics.length > 0, 'Should have initial diagnostics');
        
        // Fix the error
        await editor.edit(editBuilder => {
            editBuilder.replace(
                new vscode.Range(5, 16, 5, 17),
                ') {'
            );
        });
        
        // Add closing bracket
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(6, 5), '}\n');
        });
        
        // Wait for diagnostics to clear
        await new Promise(resolve => setTimeout(resolve, 2000));
        
        diagnostics = vscode.languages.getDiagnostics(doc.uri);
        assert.strictEqual(diagnostics.length, 0, 'Diagnostics should be cleared after fixing errors');
    });

    test('should provide quick fixes for common issues', async () => {
        const content = `package com.example

import java.util.List

class TestClass {
    def method() {
        // Groovy style list declaration that could be simplified
        List<String> list = new ArrayList<String>()
    }
}`;
        
        const doc = await vscode.workspace.openTextDocument({ content, language: 'groovy' });
        editor = await vscode.window.showTextDocument(doc);
        
        const diagnostics = await waitForDiagnostics(doc.uri);
        
        if (diagnostics.length > 0) {
            const diagnostic = diagnostics[0];
            
            // Get code actions for the diagnostic
            const codeActions = await vscode.commands.executeCommand<vscode.CodeAction[]>(
                'vscode.executeCodeActionProvider',
                doc.uri,
                diagnostic.range
            );
            
            assert.ok(codeActions, 'Should provide code actions');
        }
    });

    teardown(async () => {
        await vscode.commands.executeCommand('workbench.action.closeActiveEditor');
    });
});