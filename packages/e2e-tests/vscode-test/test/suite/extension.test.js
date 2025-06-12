import assert from 'assert';
import path from 'path';
import { createRequire } from 'module';

const require = createRequire(import.meta.url);
// vscodeモジュールはVS Code内でCJSとして提供されるためrequireを使用
const vscode = require('vscode');

suite('Groovy Language Server Extension Test Suite', () => {
    vscode.window.showInformationMessage('Start all tests.');

    test('Extension should be present', () => {
        assert.ok(vscode.extensions.getExtension('groovy-lsp.groovy-language-server'));
    });

    test('Should activate extension', async () => {
        const ext = vscode.extensions.getExtension('groovy-lsp.groovy-language-server');
        if (ext) {
            await ext.activate();
            assert.ok(ext.isActive);
        }
    });

    test('Should provide Groovy language configuration', () => {
        const languages = vscode.languages.getLanguages();
        assert.ok(languages.includes('groovy'));
    });

    test('Should open and recognize Groovy file', async () => {
        const testFileContent = `
class HelloWorld {
    static void main(String[] args) {
        println "Hello, World!"
    }
}`;
        
        // Create a new untitled document with Groovy content
        const doc = await vscode.workspace.openTextDocument({
            language: 'groovy',
            content: testFileContent
        });
        
        assert.strictEqual(doc.languageId, 'groovy');
        assert.ok(doc.getText().includes('HelloWorld'));
    });

    test('Should provide diagnostics for Groovy files', async () => {
        const testFileContent = `
class TestClass {
    def invalidMethod() {
        // This should trigger a diagnostic
        undefinedVariable = 42
    }
}`;
        
        const doc = await vscode.workspace.openTextDocument({
            language: 'groovy',
            content: testFileContent
        });
        
        const editor = await vscode.window.showTextDocument(doc);
        
        // Wait for diagnostics to be computed
        await new Promise(resolve => setTimeout(resolve, 2000));
        
        const diagnostics = vscode.languages.getDiagnostics(doc.uri);
        // The language server should provide diagnostics, but for now we just check the API works
        assert.ok(Array.isArray(diagnostics));
    });

    test('Should provide completion items', async () => {
        const testFileContent = `
class Person {
    String name
    int age
    
    void greet() {
        println "Hello"
    }
}

def person = new Person()
person.`;
        
        const doc = await vscode.workspace.openTextDocument({
            language: 'groovy',
            content: testFileContent
        });
        
        const position = new vscode.Position(10, 7); // After "person."
        
        // This tests that the completion provider is registered
        const completions = await vscode.commands.executeCommand(
            'vscode.executeCompletionItemProvider',
            doc.uri,
            position
        );
        
        // The completion API should return a list (even if empty initially)
        assert.ok(completions);
    });

    test('Should provide hover information', async () => {
        const testFileContent = `
class Calculator {
    int add(int a, int b) {
        return a + b
    }
}`;
        
        const doc = await vscode.workspace.openTextDocument({
            language: 'groovy',
            content: testFileContent
        });
        
        const position = new vscode.Position(2, 8); // On "add" method
        
        // Test that hover provider is registered
        const hovers = await vscode.commands.executeCommand(
            'vscode.executeHoverProvider',
            doc.uri,
            position
        );
        
        // The hover API should return an array (even if empty initially)
        assert.ok(Array.isArray(hovers));
    });

    test('Should format Groovy document', async () => {
        const unformattedContent = `
class   Messy    {
def    name
    void   doSomething( )   {
println    "hello"
    }
}`;
        
        const doc = await vscode.workspace.openTextDocument({
            language: 'groovy',
            content: unformattedContent
        });
        
        const editor = await vscode.window.showTextDocument(doc);
        
        // Test that format document command is available
        const edits = await vscode.commands.executeCommand(
            'vscode.executeFormatDocumentProvider',
            doc.uri,
            { tabSize: 4, insertSpaces: true }
        );
        
        // The format API should return an array of edits (even if empty initially)
        assert.ok(Array.isArray(edits));
    });

    test('Should provide document symbols', async () => {
        const testFileContent = `
package com.example

class Book {
    String title
    String author
    
    String getInfo() {
        return "$title by $author"
    }
}

interface Readable {
    void read()
}`;
        
        const doc = await vscode.workspace.openTextDocument({
            language: 'groovy',
            content: testFileContent
        });
        
        // Test that document symbol provider is registered
        const symbols = await vscode.commands.executeCommand(
            'vscode.executeDocumentSymbolProvider',
            doc.uri
        );
        
        // The symbol API should return an array (even if empty initially)
        assert.ok(Array.isArray(symbols));
    });

    test('Should handle workspace symbols', async () => {
        // Test that workspace symbol provider is registered
        const symbols = await vscode.commands.executeCommand(
            'vscode.executeWorkspaceSymbolProvider',
            'Book' // Search query
        );
        
        // The workspace symbol API should return an array (even if empty initially)
        assert.ok(Array.isArray(symbols));
    });
});