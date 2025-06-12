import assert from 'assert';
import path from 'path';
import { createRequire } from 'module';

const require = createRequire(import.meta.url);
// vscodeモジュールはVS Code内でCJSとして提供されるためrequireを使用
const vscode = require('vscode');

suite('Symbol Search Test Suite', () => {
    let document;
    let editor;

    suiteSetup(async () => {
        console.log('Setting up Symbol Search test suite...');
        // 拡張機能のアクティベーションを待つ
        await new Promise(resolve => setTimeout(resolve, 2000));
    });

    test('Should find document symbols', async () => {
        console.log('Testing document symbols...');
        
        // 一時的なGroovyファイルを作成
        const uri = vscode.Uri.parse('untitled:symbols-test1.groovy');
        document = await vscode.workspace.openTextDocument(uri);
        editor = await vscode.window.showTextDocument(document);
        
        // テストコードを挿入
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(0, 0), 
                'class Person {\n' +
                '    String firstName\n' +
                '    String lastName\n' +
                '    \n' +
                '    def getFullName() {\n' +
                '        return firstName + " " + lastName\n' +
                '    }\n' +
                '}\n\n' +
                'class Employee extends Person {\n' +
                '    String employeeId\n' +
                '    \n' +
                '    def work() {\n' +
                '        println "Working..."\n' +
                '    }\n' +
                '}\n'
            );
        });
        
        // 少し待つ（LSPが解析するため）
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const symbols = await vscode.commands.executeCommand(
            'vscode.executeDocumentSymbolProvider',
            document.uri
        );
        
        console.log(`Found ${symbols?.length || 0} document symbols`);
        
        assert.ok(symbols, 'Symbols should be returned');
        assert.ok(Array.isArray(symbols), 'Symbols should be an array');
        assert.ok(symbols.length > 0, 'Should have at least one symbol');
        
        // クラスシンボルをチェック
        const classSymbols = symbols.filter(s => 
            s.kind === vscode.SymbolKind.Class
        );
        console.log(`Found ${classSymbols.length} class symbols`);
        assert.ok(classSymbols.length >= 2, 'Should have at least 2 class symbols');
        
        // クラス名をチェック
        const classNames = classSymbols.map(s => s.name);
        assert.ok(classNames.includes('Person'), 'Should have Person class');
        assert.ok(classNames.includes('Employee'), 'Should have Employee class');
        
        console.log('✓ Document symbols found correctly');
    });

    test('Should find workspace symbols', async () => {
        console.log('Testing workspace symbols...');
        
        // ワークスペースシンボルを検索
        const query = 'Person';
        console.log(`Searching for workspace symbol: ${query}`);
        
        const symbols = await vscode.commands.executeCommand(
            'vscode.executeWorkspaceSymbolProvider',
            query
        );
        
        console.log(`Found ${symbols?.length || 0} workspace symbols for query "${query}"`);
        
        assert.ok(symbols, 'Workspace symbols should be returned');
        assert.ok(Array.isArray(symbols), 'Workspace symbols should be an array');
        
        // ワークスペースシンボルが見つかる場合
        if (symbols.length > 0) {
            const personSymbol = symbols.find(s => s.name.includes('Person'));
            console.log(`Person symbol found: ${personSymbol ? 'Yes' : 'No'}`);
            assert.ok(personSymbol, 'Should find Person symbol in workspace');
        } else {
            console.log('No workspace symbols found (this may be expected in minimal test)');
        }
    });

    test('Should find method symbols within class', async () => {
        console.log('Testing method symbols within class...');
        
        const uri = vscode.Uri.parse('untitled:symbols-test2.groovy');
        document = await vscode.workspace.openTextDocument(uri);
        editor = await vscode.window.showTextDocument(document);
        
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(0, 0), 
                'class Calculator {\n' +
                '    def add(a, b) {\n' +
                '        return a + b\n' +
                '    }\n' +
                '    \n' +
                '    def subtract(a, b) {\n' +
                '        return a - b\n' +
                '    }\n' +
                '    \n' +
                '    def multiply(a, b) {\n' +
                '        return a * b\n' +
                '    }\n' +
                '}\n'
            );
        });
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const symbols = await vscode.commands.executeCommand(
            'vscode.executeDocumentSymbolProvider',
            document.uri
        );
        
        console.log(`Found ${symbols?.length || 0} document symbols`);
        
        assert.ok(symbols, 'Symbols should be returned');
        assert.ok(symbols.length > 0, 'Should have at least one symbol');
        
        // Calculatorクラスを探す
        const calculatorClass = symbols.find(s => 
            s.name === 'Calculator' && s.kind === vscode.SymbolKind.Class
        );
        
        assert.ok(calculatorClass, 'Should find Calculator class');
        
        // クラス内のメソッドをチェック
        if (calculatorClass.children) {
            const methodNames = calculatorClass.children
                .filter(c => c.kind === vscode.SymbolKind.Method)
                .map(c => c.name);
            
            console.log(`Found methods: ${methodNames.join(', ')}`);
            assert.ok(methodNames.includes('add'), 'Should have add method');
            assert.ok(methodNames.includes('subtract'), 'Should have subtract method');
            assert.ok(methodNames.includes('multiply'), 'Should have multiply method');
        } else {
            console.log('No children found in Calculator class (may be a flat symbol list)');
            // フラットなリストの場合
            const methods = symbols.filter(s => 
                s.kind === vscode.SymbolKind.Method || 
                s.kind === vscode.SymbolKind.Function
            );
            assert.ok(methods.length >= 3, 'Should have at least 3 methods');
        }
        
        console.log('✓ Method symbols found correctly');
    });

    teardown(async () => {
        console.log('Cleaning up symbols test...');
        if (editor) {
            await vscode.commands.executeCommand('workbench.action.closeActiveEditor');
        }
    });
});