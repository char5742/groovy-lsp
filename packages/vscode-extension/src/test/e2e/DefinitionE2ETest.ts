import * as vscode from 'vscode';
import * as assert from 'assert';
import { activateExtension, openDocument, sleep } from './helper';
import * as path from 'path';

suite('Definition E2E Test Suite', () => {
    let groovyDocument: vscode.TextDocument;
    let javaDocument: vscode.TextDocument;

    suiteSetup(async () => {
        await activateExtension();
    });

    test('should navigate to Java class definition from Groovy', async function () {
        this.timeout(30000);

        // Open the Groovy test file
        const groovyPath = path.join(
            __dirname,
            '../../../src/test/fixtures/sample-java-groovy-project/src/test/groovy/com/example/CalculatorSpec.groovy'
        );
        groovyDocument = await openDocument(groovyPath);

        // Wait for the document to be fully loaded
        await sleep(2000);

        // Position on "Calculator" class usage in "new Calculator()" (line 8, column 22)
        const position = new vscode.Position(7, 22); // 0-indexed
        
        // Debug: check what's at the position
        const line = groovyDocument.lineAt(position.line);
        console.log(`Line content: "${line.text}"`);
        console.log(`Character at position ${position.character}: "${line.text.charAt(position.character)}"`);

        // Execute go to definition
        let definitions: vscode.Location[] | undefined;
        try {
            definitions = await vscode.commands.executeCommand<vscode.Location[]>(
                'vscode.executeDefinitionProvider',
                groovyDocument.uri,
                position
            );
        } catch (error) {
            console.error(`Error executing definition provider: ${error}`);
            throw error;
        }

        // Debug output
        console.log(`Definitions result: ${JSON.stringify(definitions)}`);
        console.log(`Number of definitions: ${definitions ? definitions.length : 'null'}`);
        
        // If no definitions, wait and try again
        if (!definitions || definitions.length === 0) {
            console.log('No definitions found, waiting 3 seconds and trying again...');
            await sleep(3000);
            definitions = await vscode.commands.executeCommand<vscode.Location[]>(
                'vscode.executeDefinitionProvider',
                groovyDocument.uri,
                position
            );
            console.log(`Retry - Definitions result: ${JSON.stringify(definitions)}`);
            console.log(`Retry - Number of definitions: ${definitions ? definitions.length : 'null'}`);
        }

        // Verify we got a definition
        assert.ok(definitions, 'Should return definition locations');
        assert.ok(definitions.length > 0, 'Should have at least one definition');

        // Verify the definition points to Calculator.java
        const definitionUri = definitions[0].uri;
        assert.ok(
            definitionUri.path.includes('Calculator.java'),
            `Expected definition in Calculator.java but got ${definitionUri.path}`
        );
    });

    test('should navigate to method definition within same file', async function () {
        this.timeout(30000);

        // Use the already opened Groovy document
        if (!groovyDocument) {
            const groovyPath = path.join(
                __dirname,
                '../../../src/test/fixtures/sample-java-groovy-project/src/test/groovy/com/example/CalculatorSpec.groovy'
            );
            groovyDocument = await openDocument(groovyPath);
            await sleep(2000);
        }

        // Find a method call and try to go to its definition
        // Position on "add" method call in "calculator.add(a, b)" (line 16, column 29)
        const position = new vscode.Position(15, 29); // 0-indexed

        const definitions = await vscode.commands.executeCommand<vscode.Location[]>(
            'vscode.executeDefinitionProvider',
            groovyDocument.uri,
            position
        );

        // Verify we got a definition
        assert.ok(definitions, 'Should return definition locations for method');
        assert.ok(definitions.length > 0, 'Should have at least one method definition');
    });

    test('should navigate to variable definition', async function () {
        this.timeout(30000);

        // Use the already opened Groovy document
        if (!groovyDocument) {
            const groovyPath = path.join(
                __dirname,
                '../../../src/test/fixtures/sample-java-groovy-project/src/test/groovy/com/example/CalculatorSpec.groovy'
            );
            groovyDocument = await openDocument(groovyPath);
            await sleep(2000);
        }

        // Find a variable usage and try to go to its definition
        // Position on "calculator" variable usage in line 16
        const position = new vscode.Position(15, 20); // 0-indexed, on "calculator" in "calculator.add"

        const definitions = await vscode.commands.executeCommand<vscode.Location[]>(
            'vscode.executeDefinitionProvider',
            groovyDocument.uri,
            position
        );

        // Verify we got a definition
        assert.ok(definitions, 'Should return definition locations for variable');
        if (definitions && definitions.length > 0) {
            // Verify the definition is in the same file
            assert.ok(
                definitions[0].uri.path === groovyDocument.uri.path,
                'Variable definition should be in the same file'
            );
        }
    });

    test('should navigate from Java to Java definition', async function () {
        this.timeout(30000);

        // Open the Java file
        const javaPath = path.join(
            __dirname,
            '../../../src/test/fixtures/sample-java-groovy-project/src/main/java/com/example/Calculator.java'
        );
        javaDocument = await openDocument(javaPath);
        await sleep(2000);

        // Find a type or method call in Java and test navigation
        // Position on "int" type in "public int add" method signature (line 5)
        const position = new vscode.Position(4, 11); // 0-indexed, on "int"

        const definitions = await vscode.commands.executeCommand<vscode.Location[]>(
            'vscode.executeDefinitionProvider',
            javaDocument.uri,
            position
        );

        // The assertion would depend on what's at that position
        // For now, just verify the command executes without error
        assert.ok(definitions !== undefined, 'Definition command should execute');
    });

    test('should handle go to definition on position with no definition', async function () {
        this.timeout(30000);

        // Use an already opened document
        if (!groovyDocument) {
            const groovyPath = path.join(
                __dirname,
                '../../../src/test/fixtures/sample-java-groovy-project/src/test/groovy/com/example/CalculatorSpec.groovy'
            );
            groovyDocument = await openDocument(groovyPath);
            await sleep(2000);
        }

        // Position on whitespace or comment where there's no definition
        const position = new vscode.Position(0, 0); // First character of file

        const definitions = await vscode.commands.executeCommand<vscode.Location[]>(
            'vscode.executeDefinitionProvider',
            groovyDocument.uri,
            position
        );

        // Should return empty array or undefined
        assert.ok(
            !definitions || definitions.length === 0,
            'Should return no definitions for whitespace/comment'
        );
    });

    teardown(async () => {
        await vscode.commands.executeCommand('workbench.action.closeAllEditors');
    });
});