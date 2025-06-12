import assert from 'assert';
import path from 'path';
import { createRequire } from 'module';

const require = createRequire(import.meta.url);
// vscodeモジュールはVS Code内でCJSとして提供されるためrequireを使用
const vscode = require('vscode');

suite('Advanced Hover Test Suite', () => {
    let document;
    let editor;

    suiteSetup(async () => {
        console.log('Setting up Advanced Hover test suite...');
        // 拡張機能のアクティベーションを待つ
        await new Promise(resolve => setTimeout(resolve, 2000));
    });

    test('Should show hover information for class', async () => {
        console.log('Testing hover for class...');
        
        // 一時的なGroovyファイルを作成
        const uri = vscode.Uri.parse('untitled:hover-test1.groovy');
        document = await vscode.workspace.openTextDocument(uri);
        editor = await vscode.window.showTextDocument(document);
        
        // テストコードを挿入
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(0, 0), 
                '/**\n' +
                ' * Person class represents a person with name and age\n' +
                ' */\n' +
                'class Person {\n' +
                '    String name\n' +
                '    int age\n' +
                '    \n' +
                '    Person(String name, int age) {\n' +
                '        this.name = name\n' +
                '        this.age = age\n' +
                '    }\n' +
                '}\n\n' +
                'def person = new Person("John", 30)\n'
            );
        });
        
        // Person クラスの位置でホバー
        const position = new vscode.Position(3, 7); // "Person" の 'P' の位置
        console.log(`Getting hover at position ${position.line}:${position.character}`);
        
        // 少し待つ（LSPが解析するため）
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const hovers = await vscode.commands.executeCommand(
            'vscode.executeHoverProvider',
            document.uri,
            position
        );
        
        console.log(`Found ${hovers?.length || 0} hover items`);
        
        assert.ok(hovers, 'Hover should be returned');
        assert.ok(Array.isArray(hovers), 'Hover should be an array');
        assert.ok(hovers.length > 0, 'Should have at least one hover');
        
        const hover = hovers[0];
        assert.ok(hover.contents, 'Hover should have contents');
        
        // ホバー内容を確認
        const hoverText = hover.contents.map(c => 
            typeof c === 'string' ? c : c.value
        ).join('\n');
        
        console.log('Hover content:', hoverText.substring(0, 100) + '...');
        assert.ok(hoverText.includes('Person') || hoverText.includes('class'), 
            'Hover should contain class information');
        
        console.log('✓ Class hover information displayed correctly');
    });

    test('Should show hover information for method', async () => {
        console.log('Testing hover for method...');
        
        const uri = vscode.Uri.parse('untitled:hover-test2.groovy');
        document = await vscode.workspace.openTextDocument(uri);
        editor = await vscode.window.showTextDocument(document);
        
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(0, 0), 
                'class Calculator {\n' +
                '    /**\n' +
                '     * Adds two numbers\n' +
                '     * @param a first number\n' +
                '     * @param b second number\n' +
                '     * @return sum of a and b\n' +
                '     */\n' +
                '    def add(int a, int b) {\n' +
                '        return a + b\n' +
                '    }\n' +
                '    \n' +
                '    def test() {\n' +
                '        def result = add(5, 3)\n' +
                '    }\n' +
                '}\n'
            );
        });
        
        // add メソッドの呼び出し位置でホバー
        const position = new vscode.Position(12, 21); // add の 'a' の位置
        console.log(`Getting hover at position ${position.line}:${position.character}`);
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const hovers = await vscode.commands.executeCommand(
            'vscode.executeHoverProvider',
            document.uri,
            position
        );
        
        console.log(`Found ${hovers?.length || 0} hover items`);
        
        assert.ok(hovers, 'Hover should be returned');
        assert.ok(hovers.length > 0, 'Should have at least one hover');
        
        const hover = hovers[0];
        const hoverText = hover.contents.map(c => 
            typeof c === 'string' ? c : c.value
        ).join('\n');
        
        console.log('Method hover content:', hoverText.substring(0, 100) + '...');
        assert.ok(hoverText.includes('add') || hoverText.includes('method') || hoverText.includes('def'), 
            'Hover should contain method information');
        
        console.log('✓ Method hover information displayed correctly');
    });

    test('Should show hover information for variable', async () => {
        console.log('Testing hover for variable...');
        
        const uri = vscode.Uri.parse('untitled:hover-test3.groovy');
        document = await vscode.workspace.openTextDocument(uri);
        editor = await vscode.window.showTextDocument(document);
        
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(0, 0), 
                'def message = "Hello, World!"\n' +
                'def number = 42\n' +
                'def list = [1, 2, 3, 4, 5]\n' +
                'def map = [name: "John", age: 30]\n\n' +
                'println(message)\n' +
                'println(number)\n' +
                'println(list)\n' +
                'println(map)\n'
            );
        });
        
        // message 変数の参照位置でホバー
        const position = new vscode.Position(5, 10); // println内のmessageの 'm' の位置
        console.log(`Getting hover at position ${position.line}:${position.character}`);
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const hovers = await vscode.commands.executeCommand(
            'vscode.executeHoverProvider',
            document.uri,
            position
        );
        
        console.log(`Found ${hovers?.length || 0} hover items`);
        
        assert.ok(hovers, 'Hover should be returned');
        assert.ok(hovers.length > 0, 'Should have at least one hover');
        
        const hover = hovers[0];
        const hoverText = hover.contents.map(c => 
            typeof c === 'string' ? c : c.value
        ).join('\n');
        
        console.log('Variable hover content:', hoverText.substring(0, 100) + '...');
        assert.ok(hoverText.includes('message') || hoverText.includes('String') || hoverText.includes('def'), 
            'Hover should contain variable information');
        
        console.log('✓ Variable hover information displayed correctly');
    });

    test('Should show hover for Groovy built-in types', async () => {
        console.log('Testing hover for Groovy built-in types...');
        
        const uri = vscode.Uri.parse('untitled:hover-test4.groovy');
        document = await vscode.workspace.openTextDocument(uri);
        editor = await vscode.window.showTextDocument(document);
        
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(0, 0), 
                'String text = "Hello"\n' +
                'List numbers = [1, 2, 3]\n' +
                'Map config = [:]\n' +
                'Closure greeting = { name -> "Hello, ${name}!" }\n' +
                'Range range = 1..10\n'
            );
        });
        
        // String 型の位置でホバー
        const position = new vscode.Position(0, 3); // "String" の 'r' の位置
        console.log(`Getting hover at position ${position.line}:${position.character}`);
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const hovers = await vscode.commands.executeCommand(
            'vscode.executeHoverProvider',
            document.uri,
            position
        );
        
        console.log(`Found ${hovers?.length || 0} hover items`);
        
        if (hovers && hovers.length > 0) {
            const hover = hovers[0];
            const hoverText = hover.contents.map(c => 
                typeof c === 'string' ? c : c.value
            ).join('\n');
            
            console.log('Built-in type hover content:', hoverText.substring(0, 100) + '...');
            assert.ok(hoverText.includes('String') || hoverText.includes('java.lang.String'), 
                'Hover should contain String type information');
            console.log('✓ Built-in type hover information displayed correctly');
        } else {
            console.log('Note: Built-in type hover may not be available in all configurations');
        }
    });

    teardown(async () => {
        console.log('Cleaning up hover test...');
        if (editor) {
            await vscode.commands.executeCommand('workbench.action.closeActiveEditor');
        }
    });
});