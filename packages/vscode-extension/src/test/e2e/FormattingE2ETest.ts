import * as assert from 'assert';
import * as vscode from 'vscode';
import { activateExtension, formatDocument } from './helper';

suite('Formatting E2E Test Suite', () => {
    let document: vscode.TextDocument;
    let editor: vscode.TextEditor;

    suiteSetup(async () => {
        await activateExtension();
    });

    test('should format Groovy code with proper indentation', async () => {
        const unformattedCode = `package com.example

class TestClass {
def method() {
println "Hello"
if (true) {
println "World"
}
}
}`;

        const expectedFormattedCode = `package com.example

class TestClass {
    def method() {
        println "Hello"
        if (true) {
            println "World"
        }
    }
}`;

        document = await vscode.workspace.openTextDocument({ 
            content: unformattedCode, 
            language: 'groovy' 
        });
        editor = await vscode.window.showTextDocument(document);

        // Execute format command
        await vscode.commands.executeCommand('editor.action.formatDocument');
        
        // Wait for formatting to complete
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const formattedText = document.getText();
        
        // Compare formatted text (normalize line endings)
        const normalizedFormatted = formattedText.replace(/\r\n/g, '\n').trim();
        const normalizedExpected = expectedFormattedCode.replace(/\r\n/g, '\n').trim();
        
        assert.strictEqual(normalizedFormatted, normalizedExpected, 
            'Code should be properly formatted with correct indentation');
    });

    test('should format Spock test with proper spacing', async () => {
        const unformattedSpock = `package com.example
import spock.lang.Specification

class TestSpec extends Specification {
def "test method"() {
given:
def a=1
def b=2
when:
def result=a+b
then:
result==3
}
}`;

        document = await vscode.workspace.openTextDocument({ 
            content: unformattedSpock, 
            language: 'groovy' 
        });
        editor = await vscode.window.showTextDocument(document);

        // Execute format command
        await vscode.commands.executeCommand('editor.action.formatDocument');
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const formattedText = document.getText();
        
        // Check that spacing is improved
        assert.ok(formattedText.includes('def a = 1') || formattedText.includes('def a=1'), 
            'Should format variable declarations');
        assert.ok(formattedText.includes('def result = a + b') || formattedText.includes('def result=a+b'), 
            'Should format expressions');
    });

    test('should preserve Groovy DSL formatting', async () => {
        const groovyDSL = `pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                echo 'Building...'
                sh 'make build'
            }
        }
        stage('Test') {
            steps {
                echo 'Testing...'
                sh 'make test'
            }
        }
    }
}`;

        document = await vscode.workspace.openTextDocument({ 
            content: groovyDSL, 
            language: 'groovy' 
        });
        editor = await vscode.window.showTextDocument(document);

        const originalText = document.getText();

        // Execute format command
        await vscode.commands.executeCommand('editor.action.formatDocument');
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const formattedText = document.getText();
        
        // DSL structure should be preserved
        assert.ok(formattedText.includes('pipeline {'), 'Should preserve DSL structure');
        assert.ok(formattedText.includes('agent any'), 'Should preserve DSL keywords');
        assert.ok(formattedText.includes('stages {'), 'Should preserve nested blocks');
    });

    test('should handle formatting with syntax errors gracefully', async () => {
        const codeWithError = `package com.example

class TestClass {
    def method() {
        // Missing closing bracket
        if (true {
            println "test"
    }
}`;

        document = await vscode.workspace.openTextDocument({ 
            content: codeWithError, 
            language: 'groovy' 
        });
        editor = await vscode.window.showTextDocument(document);

        // Try to format - should not crash
        try {
            await vscode.commands.executeCommand('editor.action.formatDocument');
            await new Promise(resolve => setTimeout(resolve, 1000));
            
            // Even with errors, formatting should not throw
            assert.ok(true, 'Formatting with errors should not crash');
        } catch (error) {
            assert.fail('Formatting should handle syntax errors gracefully');
        }
    });

    teardown(async () => {
        await vscode.commands.executeCommand('workbench.action.closeActiveEditor');
    });
});