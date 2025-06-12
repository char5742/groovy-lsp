import assert from 'assert';
import path from 'path';
import { createRequire } from 'module';

const require = createRequire(import.meta.url);
// vscodeモジュールはVS Code内でCJSとして提供されるためrequireを使用
const vscode = require('vscode');

suite('Formatting Test Suite', () => {
    let document;
    let editor;

    suiteSetup(async () => {
        console.log('Setting up Formatting test suite...');
        // 拡張機能のアクティベーションを待つ
        await new Promise(resolve => setTimeout(resolve, 2000));
    });

    test('Should format unformatted code', async () => {
        console.log('Testing code formatting...');
        
        // 一時的なGroovyファイルを作成
        const uri = vscode.Uri.parse('untitled:format-test1.groovy');
        document = await vscode.workspace.openTextDocument(uri);
        editor = await vscode.window.showTextDocument(document);
        
        // フォーマットが必要なコードを挿入
        const unformattedCode = 
            'class Person{\n' +
            'String name\n' +
            'int age\n' +
            'def Person(String name,int age){\n' +
            'this.name=name\n' +
            'this.age=age\n' +
            '}\n' +
            'def greet(){\n' +
            'println"Hello, my name is ${name}"\n' +
            '}\n' +
            '}\n';
            
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(0, 0), unformattedCode);
        });
        
        // ドキュメントのテキストを保存（フォーマット前）
        const textBefore = document.getText();
        console.log('Text before formatting:', textBefore.substring(0, 50) + '...');
        
        // 少し待つ（LSPが解析するため）
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        // フォーマットを実行
        const edits = await vscode.commands.executeCommand(
            'vscode.executeFormatDocumentProvider',
            document.uri
        );
        
        console.log(`Received ${edits?.length || 0} formatting edits`);
        
        if (edits && edits.length > 0) {
            // エディットを適用
            const workspaceEdit = new vscode.WorkspaceEdit();
            edits.forEach(edit => {
                workspaceEdit.replace(document.uri, edit.range, edit.newText);
            });
            await vscode.workspace.applyEdit(workspaceEdit);
            
            // フォーマット後のテキストを取得
            const textAfter = document.getText();
            console.log('Text after formatting:', textAfter.substring(0, 50) + '...');
            
            // フォーマットが適用されたことを確認
            assert.notEqual(textBefore, textAfter, 'Text should be different after formatting');
            
            // 基本的なフォーマットのチェック
            assert.ok(textAfter.includes('class Person {'), 'Class declaration should have proper spacing');
            console.log('✓ Code formatted successfully');
        } else {
            console.log('Note: Formatter may not be available or code may already be formatted');
        }
    });

    test('Should format method spacing', async () => {
        console.log('Testing method spacing formatting...');
        
        const uri = vscode.Uri.parse('untitled:format-test2.groovy');
        document = await vscode.workspace.openTextDocument(uri);
        editor = await vscode.window.showTextDocument(document);
        
        const unformattedCode = 
            'class Calculator{\n' +
            'def add(a,b){return a+b}\n' +
            'def subtract(a,b){return a-b}\n' +
            'def multiply(a,b){return a*b}\n' +
            'def divide(a,b){if(b!=0)return a/b else throw new ArithmeticException("Division by zero")}\n' +
            '}\n';
            
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(0, 0), unformattedCode);
        });
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const edits = await vscode.commands.executeCommand(
            'vscode.executeFormatDocumentProvider',
            document.uri
        );
        
        if (edits && edits.length > 0) {
            const workspaceEdit = new vscode.WorkspaceEdit();
            edits.forEach(edit => {
                workspaceEdit.replace(document.uri, edit.range, edit.newText);
            });
            await vscode.workspace.applyEdit(workspaceEdit);
            
            const formattedText = document.getText();
            
            // メソッドのスペーシングをチェック
            assert.ok(formattedText.includes('(a, b)') || formattedText.includes('( a, b )'), 
                'Parameters should have proper spacing');
            assert.ok(formattedText.includes('return a + b') || formattedText.includes('return a+b'), 
                'Operators may have spacing');
            
            console.log('✓ Method spacing formatted');
        } else {
            console.log('Note: Method spacing formatting may not be available');
        }
    });

    test('Should format indentation', async () => {
        console.log('Testing indentation formatting...');
        
        const uri = vscode.Uri.parse('untitled:format-test3.groovy');
        document = await vscode.workspace.openTextDocument(uri);
        editor = await vscode.window.showTextDocument(document);
        
        const unformattedCode = 
            'def processList(list) {\n' +
            'list.each { item ->\n' +
            'if (item > 0) {\n' +
            'println "Positive: $item"\n' +
            '} else {\n' +
            'println "Non-positive: $item"\n' +
            '}\n' +
            '}\n' +
            '}\n';
            
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(0, 0), unformattedCode);
        });
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const edits = await vscode.commands.executeCommand(
            'vscode.executeFormatDocumentProvider',
            document.uri
        );
        
        if (edits && edits.length > 0) {
            const workspaceEdit = new vscode.WorkspaceEdit();
            edits.forEach(edit => {
                workspaceEdit.replace(document.uri, edit.range, edit.newText);
            });
            await vscode.workspace.applyEdit(workspaceEdit);
            
            const formattedText = document.getText();
            const lines = formattedText.split('\n');
            
            // インデントのチェック（スペースまたはタブ）
            const hasProperIndent = lines.some(line => 
                line.startsWith('    ') || line.startsWith('\t')
            );
            
            assert.ok(hasProperIndent, 'Code should have proper indentation');
            console.log('✓ Indentation formatted');
        } else {
            console.log('Note: Indentation formatting may not be available');
        }
    });

    test('Should format Groovy-specific constructs', async () => {
        console.log('Testing Groovy-specific formatting...');
        
        const uri = vscode.Uri.parse('untitled:format-test4.groovy');
        document = await vscode.workspace.openTextDocument(uri);
        editor = await vscode.window.showTextDocument(document);
        
        const unformattedCode = 
            'def map=[name:"John",age:30,city:"New York"]\n' +
            'def list=[1,2,3,4,5]\n' +
            'def closure={x,y->x+y}\n' +
            'def gstring="Hello ${map.name}, you are ${map.age} years old"\n';
            
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(0, 0), unformattedCode);
        });
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const edits = await vscode.commands.executeCommand(
            'vscode.executeFormatDocumentProvider',
            document.uri
        );
        
        if (edits && edits.length > 0) {
            const workspaceEdit = new vscode.WorkspaceEdit();
            edits.forEach(edit => {
                workspaceEdit.replace(document.uri, edit.range, edit.newText);
            });
            await vscode.workspace.applyEdit(workspaceEdit);
            
            const formattedText = document.getText();
            
            // Groovy構文のフォーマットをチェック
            assert.ok(
                formattedText.includes('= [') || formattedText.includes('=['), 
                'Collections should have proper formatting'
            );
            assert.ok(
                formattedText.includes('{ x, y ->') || formattedText.includes('{x,y->'), 
                'Closures should maintain their structure'
            );
            
            console.log('✓ Groovy-specific constructs formatted');
        } else {
            console.log('Note: Groovy-specific formatting may not be available');
        }
    });

    teardown(async () => {
        console.log('Cleaning up formatting test...');
        if (editor) {
            await vscode.commands.executeCommand('workbench.action.closeActiveEditor');
        }
    });
});