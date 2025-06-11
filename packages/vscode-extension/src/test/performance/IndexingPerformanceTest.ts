import * as assert from 'assert';
import * as vscode from 'vscode';
import * as path from 'path';
import { activateExtension, sleep } from '../e2e/helper';

suite('Indexing Performance Test Suite', () => {
    const performanceResults: { [key: string]: number } = {};

    suiteSetup(async () => {
        await activateExtension();
    });

    test('should index a medium-sized project within acceptable time', async function() {
        this.timeout(60000); // 1 minute timeout

        const startTime = Date.now();
        
        // Open workspace
        const workspaceFolder = path.join(__dirname, '../../fixtures/sample-java-groovy-project');
        await vscode.commands.executeCommand('vscode.openFolder', vscode.Uri.file(workspaceFolder));
        
        // Wait for indexing to complete (check for diagnostics as indicator)
        let indexed = false;
        const maxWaitTime = 30000; // 30 seconds max
        const checkInterval = 500;
        let waitedTime = 0;
        
        while (!indexed && waitedTime < maxWaitTime) {
            // Check if any Groovy file has been indexed by trying to get diagnostics
            const groovyFiles = await vscode.workspace.findFiles('**/*.groovy', null, 10);
            if (groovyFiles.length > 0) {
                const diagnostics = vscode.languages.getDiagnostics(groovyFiles[0]);
                if (diagnostics !== undefined) {
                    indexed = true;
                    break;
                }
            }
            await sleep(checkInterval);
            waitedTime += checkInterval;
        }
        
        const indexingTime = Date.now() - startTime;
        performanceResults['indexingTime'] = indexingTime;
        
        console.log(`Indexing completed in ${indexingTime}ms`);
        assert.ok(indexingTime < 30000, `Indexing should complete within 30 seconds, took ${indexingTime}ms`);
    });

    test('should provide completions quickly', async () => {
        const docPath = path.join(__dirname, '../../fixtures/sample-java-groovy-project/src/test/groovy/com/example/CalculatorSpec.groovy');
        const document = await vscode.workspace.openTextDocument(docPath);
        await vscode.window.showTextDocument(document);
        
        const position = new vscode.Position(6, 28);
        
        const startTime = Date.now();
        const completions = await vscode.commands.executeCommand<vscode.CompletionList>(
            'vscode.executeCompletionItemProvider',
            document.uri,
            position
        );
        const completionTime = Date.now() - startTime;
        
        performanceResults['completionTime'] = completionTime;
        
        console.log(`Completion took ${completionTime}ms`);
        assert.ok(completions);
        assert.ok(completionTime < 1000, `Completion should respond within 1 second, took ${completionTime}ms`);
    });

    test('should provide hover information quickly', async () => {
        const docPath = path.join(__dirname, '../../fixtures/sample-java-groovy-project/src/test/groovy/com/example/CalculatorSpec.groovy');
        const document = await vscode.workspace.openTextDocument(docPath);
        await vscode.window.showTextDocument(document);
        
        const position = new vscode.Position(6, 25);
        
        const startTime = Date.now();
        const hovers = await vscode.commands.executeCommand<vscode.Hover[]>(
            'vscode.executeHoverProvider',
            document.uri,
            position
        );
        const hoverTime = Date.now() - startTime;
        
        performanceResults['hoverTime'] = hoverTime;
        
        console.log(`Hover took ${hoverTime}ms`);
        assert.ok(hovers);
        assert.ok(hoverTime < 500, `Hover should respond within 500ms, took ${hoverTime}ms`);
    });

    test('should format document quickly', async () => {
        const content = `package com.example
class TestClass {
def method() {
println "Hello"
if (true) {
println "World"
}
}
}`;
        
        const document = await vscode.workspace.openTextDocument({ 
            content, 
            language: 'groovy' 
        });
        await vscode.window.showTextDocument(document);
        
        const startTime = Date.now();
        await vscode.commands.executeCommand('editor.action.formatDocument');
        const formatTime = Date.now() - startTime;
        
        performanceResults['formatTime'] = formatTime;
        
        console.log(`Formatting took ${formatTime}ms`);
        assert.ok(formatTime < 2000, `Formatting should complete within 2 seconds, took ${formatTime}ms`);
    });

    suiteTeardown(() => {
        // Log all performance results
        console.log('\n=== Performance Test Results ===');
        Object.entries(performanceResults).forEach(([metric, time]) => {
            console.log(`${metric}: ${time}ms`);
        });
        console.log('================================\n');
    });
});