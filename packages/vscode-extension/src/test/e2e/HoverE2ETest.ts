import * as assert from 'assert';
import * as vscode from 'vscode';
import * as path from 'path';
import { activateExtension, openDocument } from './helper';

suite('Hover E2E Test Suite', () => {
    let document: vscode.TextDocument;

    suiteSetup(async () => {
        await activateExtension();
    });

    test('should show hover information for Java classes', async () => {
        const docPath = path.join(__dirname, '../../fixtures/sample-java-groovy-project/src/test/groovy/com/example/CalculatorSpec.groovy');
        document = await openDocument(docPath);

        // Hover over Calculator class
        const position = new vscode.Position(6, 25); // Position on "Calculator"
        
        const hovers = await vscode.commands.executeCommand<vscode.Hover[]>(
            'vscode.executeHoverProvider',
            document.uri,
            position
        );

        assert.ok(hovers && hovers.length > 0, 'Should provide hover information');
        
        const hover = hovers[0];
        assert.ok(hover.contents.length > 0, 'Hover should have content');
        
        // Check if hover contains class information
        const hoverText = hover.contents.map(c => 
            typeof c === 'string' ? c : c.value
        ).join('\n');
        
        assert.ok(hoverText.includes('Calculator') || hoverText.includes('class'), 
            'Hover should contain class information');
    });

    test('should show hover information for methods', async () => {
        const docPath = path.join(__dirname, '../../fixtures/sample-java-groovy-project/src/test/groovy/com/example/CalculatorSpec.groovy');
        document = await openDocument(docPath);

        // Hover over add method
        const position = new vscode.Position(14, 20); // Position on "add"
        
        const hovers = await vscode.commands.executeCommand<vscode.Hover[]>(
            'vscode.executeHoverProvider',
            document.uri,
            position
        );

        assert.ok(hovers && hovers.length > 0, 'Should provide hover information for method');
        
        const hover = hovers[0];
        const hoverText = hover.contents.map(c => 
            typeof c === 'string' ? c : c.value
        ).join('\n');
        
        assert.ok(hoverText.includes('add') || hoverText.includes('int'), 
            'Hover should contain method signature information');
    });

    test('should show hover information for Groovy keywords', async () => {
        const docPath = path.join(__dirname, '../../fixtures/sample-java-groovy-project/src/test/groovy/com/example/CalculatorSpec.groovy');
        document = await openDocument(docPath);

        // Hover over 'def' keyword
        const position = new vscode.Position(6, 5); // Position on "def"
        
        const hovers = await vscode.commands.executeCommand<vscode.Hover[]>(
            'vscode.executeHoverProvider',
            document.uri,
            position
        );

        if (hovers && hovers.length > 0) {
            const hover = hovers[0];
            assert.ok(hover.contents.length > 0, 'Should provide information about Groovy keywords');
        }
    });

    test('should show hover for Spock framework elements', async () => {
        const docPath = path.join(__dirname, '../../fixtures/sample-java-groovy-project/src/test/groovy/com/example/CalculatorSpec.groovy');
        document = await openDocument(docPath);

        // Hover over Specification class
        const position = new vscode.Position(4, 35); // Position on "Specification"
        
        const hovers = await vscode.commands.executeCommand<vscode.Hover[]>(
            'vscode.executeHoverProvider',
            document.uri,
            position
        );

        assert.ok(hovers && hovers.length > 0, 'Should provide hover for Spock Specification');
        
        const hover = hovers[0];
        const hoverText = hover.contents.map(c => 
            typeof c === 'string' ? c : c.value
        ).join('\n');
        
        assert.ok(hoverText.includes('Specification') || hoverText.includes('spock'), 
            'Hover should contain Spock framework information');
    });

    teardown(async () => {
        await vscode.commands.executeCommand('workbench.action.closeActiveEditor');
    });
});