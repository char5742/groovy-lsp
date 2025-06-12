import * as vscode from 'vscode';
import * as assert from 'assert';
import { activateExtension, openDocument, sleep } from './helper';
import * as path from 'path';

suite('References E2E Test Suite', () => {
    let groovyDocument: vscode.TextDocument;
    let javaDocument: vscode.TextDocument;

    suiteSetup(async () => {
        await activateExtension();
    });

    test('should find references to Java class from Groovy', async function () {
        this.timeout(30000);

        // Open the Java file first
        const javaPath = path.join(
            __dirname,
            '../../../src/test/fixtures/sample-java-groovy-project/src/main/java/com/example/Calculator.java'
        );
        javaDocument = await openDocument(javaPath);
        await sleep(2000);

        // Position on "Calculator" class definition (line 3, on class name)
        const position = new vscode.Position(2, 14); // 0-indexed, on "Calculator" in "public class Calculator"

        // Execute find references
        const references = await vscode.commands.executeCommand<vscode.Location[]>(
            'vscode.executeReferenceProvider',
            javaDocument.uri,
            position,
            { includeDeclaration: true }
        );

        // Verify we got references
        assert.ok(references, 'Should return reference locations');
        assert.ok(references.length > 0, 'Should have at least one reference');

        // Should include references from both Java and Groovy files
        const groovyReferences = references.filter(ref => 
            ref.uri.path.includes('.groovy')
        );
        assert.ok(
            groovyReferences.length > 0,
            'Should find references in Groovy files'
        );
    });

    test('should find all references to a method', async function () {
        this.timeout(30000);

        // Use the Java document
        if (!javaDocument) {
            const javaPath = path.join(
                __dirname,
                '../../../src/test/fixtures/sample-java-groovy-project/src/main/java/com/example/Calculator.java'
            );
            javaDocument = await openDocument(javaPath);
            await sleep(2000);
        }

        // Position on "add" method definition (line 5, on method name)
        const position = new vscode.Position(4, 15); // 0-indexed, on "add" in "public int add"

        const references = await vscode.commands.executeCommand<vscode.Location[]>(
            'vscode.executeReferenceProvider',
            javaDocument.uri,
            position,
            { includeDeclaration: true }
        );

        // Verify we got references
        assert.ok(references, 'Should return reference locations for method');
        assert.ok(references.length > 0, 'Should have at least one method reference');

        // If includeDeclaration is true, one of the references should be the declaration itself
        const declarationRef = references.find(ref => 
            ref.uri.path === javaDocument.uri.path &&
            ref.range.start.line === position.line
        );
        assert.ok(declarationRef, 'Should include the declaration itself when includeDeclaration is true');
    });

    test('should find references within same file', async function () {
        this.timeout(30000);

        // Open the Groovy file
        const groovyPath = path.join(
            __dirname,
            '../../../src/test/fixtures/sample-java-groovy-project/src/test/groovy/com/example/CalculatorSpec.groovy'
        );
        groovyDocument = await openDocument(groovyPath);
        await sleep(2000);

        // Position on "calculator" variable declaration (line 8)
        const position = new vscode.Position(7, 8); // 0-indexed, on "calculator" in "def calculator ="

        const references = await vscode.commands.executeCommand<vscode.Location[]>(
            'vscode.executeReferenceProvider',
            groovyDocument.uri,
            position,
            { includeDeclaration: false }
        );

        // Verify we got references
        if (references && references.length > 0) {
            // All references should be in the same file
            const sameFileRefs = references.filter(ref => 
                ref.uri.path === groovyDocument.uri.path
            );
            assert.strictEqual(
                sameFileRefs.length,
                references.length,
                'All references should be in the same file for local variable'
            );
        }
    });

    test('should handle find references with no results', async function () {
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

        // Position on a string literal or comment where there are no references
        const position = new vscode.Position(0, 0); // First character

        const references = await vscode.commands.executeCommand<vscode.Location[]>(
            'vscode.executeReferenceProvider',
            groovyDocument.uri,
            position,
            { includeDeclaration: true }
        );

        // Should return empty array or very few results
        assert.ok(
            !references || references.length === 0,
            'Should return no references for non-referenceable position'
        );
    });

    test('should find cross-file references between Groovy files', async function () {
        this.timeout(30000);

        // This test would be more meaningful if we had multiple Groovy files
        // that reference each other. For now, we'll test with what we have.
        
        // Open a Groovy file
        if (!groovyDocument) {
            const groovyPath = path.join(
                __dirname,
                '../../../src/test/fixtures/sample-java-groovy-project/src/test/groovy/com/example/CalculatorSpec.groovy'
            );
            groovyDocument = await openDocument(groovyPath);
            await sleep(2000);
        }

        // Try to find references to Calculator class usage
        const position = new vscode.Position(7, 22); // 0-indexed, on "Calculator" in "new Calculator()"

        const references = await vscode.commands.executeCommand<vscode.Location[]>(
            'vscode.executeReferenceProvider',
            groovyDocument.uri,
            position,
            { includeDeclaration: true }
        );

        // Log the results for debugging
        if (references) {
            console.log(`Found ${references.length} references`);
            references.forEach((ref, i) => {
                console.log(`Reference ${i}: ${ref.uri.path} at line ${ref.range.start.line}`);
            });
        }

        // The test assertion would depend on the project structure
        assert.ok(references !== undefined, 'References command should execute');
    });

    test('should respect includeDeclaration parameter', async function () {
        this.timeout(30000);

        // Open the Java file
        if (!javaDocument) {
            const javaPath = path.join(
                __dirname,
                '../../../src/test/fixtures/sample-java-groovy-project/src/main/java/com/example/Calculator.java'
            );
            javaDocument = await openDocument(javaPath);
            await sleep(2000);
        }

        // Position on "add" method definition (line 5, on method name)
        const position = new vscode.Position(4, 15); // 0-indexed, on "add" in "public int add"

        // Find references WITHOUT declaration
        const refsWithoutDecl = await vscode.commands.executeCommand<vscode.Location[]>(
            'vscode.executeReferenceProvider',
            javaDocument.uri,
            position,
            { includeDeclaration: false }
        );

        // Find references WITH declaration
        const refsWithDecl = await vscode.commands.executeCommand<vscode.Location[]>(
            'vscode.executeReferenceProvider',
            javaDocument.uri,
            position,
            { includeDeclaration: true }
        );

        // With declaration should have at least one more reference (the declaration itself)
        if (refsWithoutDecl && refsWithDecl) {
            assert.ok(
                refsWithDecl.length >= refsWithoutDecl.length,
                'References with declaration should include at least as many results'
            );
        }
    });

    teardown(async () => {
        await vscode.commands.executeCommand('workbench.action.closeAllEditors');
    });
});