import assert from 'assert';
import path from 'path';
import { createRequire } from 'module';

const require = createRequire(import.meta.url);
// vscodeモジュールはVS Code内でCJSとして提供されるためrequireを使用
const vscode = require('vscode');

suite('Completion Test Suite', () => {
    let document;
    let editor;

    suiteSetup(async () => {
        console.log('Setting up Completion test suite...');
        // 拡張機能のアクティベーションを待つ
        await new Promise(resolve => setTimeout(resolve, 2000));
    });

    test('Should provide basic Groovy completions', async () => {
        console.log('Testing basic Groovy completions...');
        
        // 一時的なGroovyファイルを作成
        const uri = vscode.Uri.parse('untitled:test.groovy');
        document = await vscode.workspace.openTextDocument(uri);
        editor = await vscode.window.showTextDocument(document);
        
        // テストコードを挿入
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(0, 0), 
                'class TestClass {\n' +
                '    String name\n' +
                '    def test() {\n' +
                '        def str = "Hello"\n' +
                '        str.\n' +
                '    }\n' +
                '}\n'
            );
        });
        
        // str. の後ろで補完を実行
        const position = new vscode.Position(4, 12);
        console.log(`Triggering completion at position ${position.line}:${position.character}`);
        
        // 少し待つ（LSPが解析するため）
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const completions = await vscode.commands.executeCommand(
            'vscode.executeCompletionItemProvider',
            document.uri,
            position
        );
        
        console.log(`Received ${completions?.items?.length || 0} completion items`);
        
        assert.ok(completions, 'Completions should be returned');
        assert.ok(completions.items, 'Completion items should exist');
        assert.ok(completions.items.length > 0, 'Should have at least one completion item');
        
        // String メソッドの存在を確認
        const hasStringMethod = completions.items.some(item => {
            const label = typeof item.label === 'string' ? item.label : item.label.label;
            return ['length', 'toUpperCase', 'toLowerCase', 'substring'].includes(label);
        });
        
        console.log(`Found String methods: ${hasStringMethod}`);
        assert.ok(hasStringMethod, 'Should have String methods in completion');
    });

    test('Should provide class property completions', async () => {
        console.log('Testing class property completions...');
        
        // 新しいドキュメントを作成
        const uri = vscode.Uri.parse('untitled:test2.groovy');
        document = await vscode.workspace.openTextDocument(uri);
        editor = await vscode.window.showTextDocument(document);
        
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(0, 0), 
                'class Person {\n' +
                '    String firstName\n' +
                '    String lastName\n' +
                '    int age\n' +
                '}\n\n' +
                'def person = new Person()\n' +
                'person.\n'
            );
        });
        
        // person. の後ろで補完を実行
        const position = new vscode.Position(7, 7);
        console.log(`Triggering completion at position ${position.line}:${position.character}`);
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const completions = await vscode.commands.executeCommand(
            'vscode.executeCompletionItemProvider',
            document.uri,
            position
        );
        
        console.log(`Received ${completions?.items?.length || 0} completion items`);
        
        assert.ok(completions, 'Completions should be returned');
        assert.ok(completions.items, 'Completion items should exist');
        
        // プロパティの存在を確認
        const properties = ['firstName', 'lastName', 'age'];
        for (const prop of properties) {
            const found = completions.items.some(item => {
                const label = typeof item.label === 'string' ? item.label : item.label.label;
                return label === prop;
            });
            console.log(`Property ${prop} found: ${found}`);
            assert.ok(found, `Property ${prop} should be in completion list`);
        }
    });

    test('Should provide Groovy keyword completions', async () => {
        console.log('Testing Groovy keyword completions...');
        
        const uri = vscode.Uri.parse('untitled:test3.groovy');
        document = await vscode.workspace.openTextDocument(uri);
        editor = await vscode.window.showTextDocument(document);
        
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(0, 0), 'de');
        });
        
        const position = new vscode.Position(0, 2);
        console.log(`Triggering completion at position ${position.line}:${position.character}`);
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const completions = await vscode.commands.executeCommand(
            'vscode.executeCompletionItemProvider',
            document.uri,
            position
        );
        
        console.log(`Received ${completions?.items?.length || 0} completion items`);
        
        assert.ok(completions, 'Completions should be returned');
        
        // 'def' キーワードの存在を確認
        const hasDefKeyword = completions.items.some(item => {
            const label = typeof item.label === 'string' ? item.label : item.label.label;
            return label === 'def';
        });
        
        console.log(`Found 'def' keyword: ${hasDefKeyword}`);
        assert.ok(hasDefKeyword, 'Should have "def" keyword in completion');
    });

    teardown(async () => {
        console.log('Cleaning up completion test...');
        if (editor) {
            await vscode.commands.executeCommand('workbench.action.closeActiveEditor');
        }
    });
});