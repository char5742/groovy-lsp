import * as assert from 'assert';
import * as vscode from 'vscode';
import * as path from 'path';
import { activateExtension, openDocument, sleep } from './helper';

suite('Completion E2E Test Suite', () => {
    let document: vscode.TextDocument;
    let editor: vscode.TextEditor;

    suiteSetup(async () => {
        await activateExtension();
    });

    test('should provide completion for Java classes in Groovy file', async () => {
        const docPath = path.join(__dirname, '../../fixtures/sample-java-groovy-project/src/test/groovy/com/example/CalculatorSpec.groovy');
        document = await openDocument(docPath);
        editor = await vscode.window.showTextDocument(document);

        // Position after "new Calc" to test completion
        const position = new vscode.Position(6, 28);
        
        // Trigger completion
        const completions = await vscode.commands.executeCommand<vscode.CompletionList>(
            'vscode.executeCompletionItemProvider',
            document.uri,
            position
        );

        assert.ok(completions);
        assert.ok(completions.items.length > 0);
        
        // Check if Calculator class is in the completion list
        const calculatorCompletion = completions.items.find(item => 
            item.label === 'Calculator' || 
            (typeof item.label === 'object' && item.label.label === 'Calculator')
        );
        
        assert.ok(calculatorCompletion, 'Calculator class should be in completion list');
    });

    test('should provide method completion for Java objects', async () => {
        const docPath = path.join(__dirname, '../../fixtures/sample-java-groovy-project/src/test/groovy/com/example/CalculatorSpec.groovy');
        document = await openDocument(docPath);
        editor = await vscode.window.showTextDocument(document);

        // Position after "calculator." to test method completion
        const position = new vscode.Position(14, 24);
        
        // Trigger completion
        const completions = await vscode.commands.executeCommand<vscode.CompletionList>(
            'vscode.executeCompletionItemProvider',
            document.uri,
            position
        );

        assert.ok(completions);
        assert.ok(completions.items.length > 0);
        
        // Check for Calculator methods
        const methodNames = ['add', 'subtract', 'multiply', 'divide'];
        for (const methodName of methodNames) {
            const methodCompletion = completions.items.find(item => 
                item.label === methodName || 
                (typeof item.label === 'object' && item.label.label === methodName)
            );
            assert.ok(methodCompletion, `Method ${methodName} should be in completion list`);
        }
    });

    test('should provide Spock framework completions', async () => {
        const docPath = path.join(__dirname, '../../fixtures/sample-java-groovy-project/src/test/groovy/com/example/CalculatorSpec.groovy');
        document = await openDocument(docPath);
        editor = await vscode.window.showTextDocument(document);

        // Test for Spock keywords
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(40, 0), '\n    def "new test"() {\n        ');
        });

        const position = new vscode.Position(42, 8);
        
        const completions = await vscode.commands.executeCommand<vscode.CompletionList>(
            'vscode.executeCompletionItemProvider',
            document.uri,
            position
        );

        assert.ok(completions);
        
        // Check for Spock blocks
        const spockBlocks = ['given:', 'when:', 'then:', 'expect:', 'where:'];
        for (const block of spockBlocks) {
            const blockCompletion = completions.items.find(item => 
                item.label === block || 
                (typeof item.label === 'object' && item.label.label === block)
            );
            assert.ok(blockCompletion, `Spock block ${block} should be in completion list`);
        }
    });

    teardown(async () => {
        await vscode.commands.executeCommand('workbench.action.closeActiveEditor');
    });
});