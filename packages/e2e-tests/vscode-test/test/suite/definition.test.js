import assert from 'assert';
import path from 'path';
import { createRequire } from 'module';

const require = createRequire(import.meta.url);
// vscodeモジュールはVS Code内でCJSとして提供されるためrequireを使用
const vscode = require('vscode');

suite('Definition (Go to Definition) Test Suite', () => {
    let document;
    let editor;

    suiteSetup(async () => {
        console.log('Setting up Definition test suite...');
        // 拡張機能のアクティベーションを待つ
        await new Promise(resolve => setTimeout(resolve, 2000));
    });

    test('Should go to class definition', async () => {
        console.log('Testing go to class definition...');
        
        // 一時的なGroovyファイルを作成
        const uri = vscode.Uri.parse('untitled:definition-test1.groovy');
        document = await vscode.workspace.openTextDocument(uri);
        editor = await vscode.window.showTextDocument(document);
        
        // テストコードを挿入
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(0, 0), 
                'class MyClass {\n' +
                '    String name\n' +
                '}\n\n' +
                'def obj = new MyClass()\n'
            );
        });
        
        // MyClass の参照位置（new MyClass() の MyClass部分）
        const position = new vscode.Position(4, 18); // MyClassの'C'の位置
        console.log(`Finding definition at position ${position.line}:${position.character}`);
        
        // 少し待つ（LSPが解析するため）
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const definitions = await vscode.commands.executeCommand(
            'vscode.executeDefinitionProvider',
            document.uri,
            position
        );
        
        console.log(`Found ${definitions?.length || 0} definitions`);
        
        assert.ok(definitions, 'Definitions should be returned');
        assert.ok(Array.isArray(definitions), 'Definitions should be an array');
        assert.ok(definitions.length > 0, 'Should have at least one definition');
        
        const definition = definitions[0];
        assert.equal(definition.uri.toString(), document.uri.toString(), 'Definition should be in the same file');
        assert.equal(definition.range.start.line, 0, 'Definition should be at line 0');
        console.log('✓ Class definition found correctly');
    });

    test('Should go to method definition', async () => {
        console.log('Testing go to method definition...');
        
        // 新しいドキュメントを作成
        const uri = vscode.Uri.parse('untitled:definition-test2.groovy');
        document = await vscode.workspace.openTextDocument(uri);
        editor = await vscode.window.showTextDocument(document);
        
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(0, 0), 
                'class Calculator {\n' +
                '    def add(a, b) {\n' +
                '        return a + b\n' +
                '    }\n' +
                '    \n' +
                '    def test() {\n' +
                '        return add(1, 2)\n' +
                '    }\n' +
                '}\n'
            );
        });
        
        // add メソッドの呼び出し位置
        const position = new vscode.Position(6, 15); // add の 'a' の位置
        console.log(`Finding definition at position ${position.line}:${position.character}`);
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const definitions = await vscode.commands.executeCommand(
            'vscode.executeDefinitionProvider',
            document.uri,
            position
        );
        
        console.log(`Found ${definitions?.length || 0} definitions`);
        
        assert.ok(definitions, 'Definitions should be returned');
        assert.ok(Array.isArray(definitions), 'Definitions should be an array');
        assert.ok(definitions.length > 0, 'Should have at least one definition');
        
        const definition = definitions[0];
        assert.equal(definition.range.start.line, 1, 'Method definition should be at line 1');
        console.log('✓ Method definition found correctly');
    });

    test('Should go to variable definition', async () => {
        console.log('Testing go to variable definition...');
        
        const uri = vscode.Uri.parse('untitled:definition-test3.groovy');
        document = await vscode.workspace.openTextDocument(uri);
        editor = await vscode.window.showTextDocument(document);
        
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(0, 0), 
                'def myVariable = "Hello"\n' +
                'println(myVariable)\n' +
                'def result = myVariable.toUpperCase()\n'
            );
        });
        
        // myVariable の参照位置（println内）
        const position = new vscode.Position(1, 10); // myVariable の 'm' の位置
        console.log(`Finding definition at position ${position.line}:${position.character}`);
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const definitions = await vscode.commands.executeCommand(
            'vscode.executeDefinitionProvider',
            document.uri,
            position
        );
        
        console.log(`Found ${definitions?.length || 0} definitions`);
        
        assert.ok(definitions, 'Definitions should be returned');
        assert.ok(Array.isArray(definitions), 'Definitions should be an array');
        assert.ok(definitions.length > 0, 'Should have at least one definition');
        
        const definition = definitions[0];
        assert.equal(definition.range.start.line, 0, 'Variable definition should be at line 0');
        console.log('✓ Variable definition found correctly');
    });

    teardown(async () => {
        console.log('Cleaning up definition test...');
        if (editor) {
            await vscode.commands.executeCommand('workbench.action.closeActiveEditor');
        }
    });
});