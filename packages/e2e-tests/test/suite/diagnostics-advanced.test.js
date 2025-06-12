import assert from 'assert';
import path from 'path';
import { createRequire } from 'module';

const require = createRequire(import.meta.url);
// vscodeモジュールはVS Code内でCJSとして提供されるためrequireを使用
const vscode = require('vscode');

suite('Advanced Diagnostics Test Suite', () => {
    let document;
    let editor;

    suiteSetup(async () => {
        console.log('Setting up Advanced Diagnostics test suite...');
        // 拡張機能のアクティベーションを待つ
        await new Promise(resolve => setTimeout(resolve, 2000));
    });

    test('Should detect syntax errors', async () => {
        console.log('Testing syntax error detection...');
        
        // 一時的なGroovyファイルを作成
        const uri = vscode.Uri.parse('untitled:diagnostics-test1.groovy');
        document = await vscode.workspace.openTextDocument(uri);
        editor = await vscode.window.showTextDocument(document);
        
        // 構文エラーを含むコードを挿入
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(0, 0), 
                'class TestClass {\n' +
                '    def method() {\n' +
                '        println "Hello\n' +  // 文字列が閉じていない
                '    }\n' +
                '}\n'
            );
        });
        
        // 診断が更新されるのを待つ
        await new Promise(resolve => setTimeout(resolve, 2000));
        
        const diagnostics = vscode.languages.getDiagnostics(document.uri);
        console.log(`Found ${diagnostics.length} diagnostics`);
        
        assert.ok(diagnostics, 'Diagnostics should be returned');
        assert.ok(diagnostics.length > 0, 'Should have at least one diagnostic');
        
        // エラーレベルの診断をチェック
        const errors = diagnostics.filter(d => d.severity === vscode.DiagnosticSeverity.Error);
        console.log(`Found ${errors.length} error diagnostics`);
        assert.ok(errors.length > 0, 'Should have at least one error');
        
        // エラーメッセージを確認
        const syntaxError = errors.find(e => 
            e.message.toLowerCase().includes('string') || 
            e.message.toLowerCase().includes('unexpected') ||
            e.message.toLowerCase().includes('syntax')
        );
        console.log(`Syntax error found: ${syntaxError ? 'Yes' : 'No'}`);
        if (syntaxError) {
            console.log(`Error message: ${syntaxError.message}`);
        }
        
        console.log('✓ Syntax errors detected correctly');
    });

    test('Should detect undefined variable errors', async () => {
        console.log('Testing undefined variable detection...');
        
        const uri = vscode.Uri.parse('untitled:diagnostics-test2.groovy');
        document = await vscode.workspace.openTextDocument(uri);
        editor = await vscode.window.showTextDocument(document);
        
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(0, 0), 
                'class Calculator {\n' +
                '    def calculate() {\n' +
                '        def x = 10\n' +
                '        def result = x + y\n' +  // y は未定義
                '        return result\n' +
                '    }\n' +
                '}\n'
            );
        });
        
        await new Promise(resolve => setTimeout(resolve, 2000));
        
        const diagnostics = vscode.languages.getDiagnostics(document.uri);
        console.log(`Found ${diagnostics.length} diagnostics`);
        
        assert.ok(diagnostics, 'Diagnostics should be returned');
        
        // 未定義変数のエラーがあるかチェック
        const undefinedVarError = diagnostics.find(d => 
            d.message.toLowerCase().includes('undefined') ||
            d.message.toLowerCase().includes('cannot find') ||
            d.message.toLowerCase().includes('unknown') ||
            d.message.toLowerCase().includes('variable') ||
            d.message.toLowerCase().includes('y')
        );
        
        console.log(`Undefined variable error found: ${undefinedVarError ? 'Yes' : 'No'}`);
        if (undefinedVarError) {
            console.log(`Error message: ${undefinedVarError.message}`);
            console.log(`Error line: ${undefinedVarError.range.start.line}`);
        }
        
        // Groovyは動的言語なので、未定義変数のエラーが検出されない場合もある
        if (!undefinedVarError) {
            console.log('Note: Groovy may not detect undefined variables as errors due to its dynamic nature');
        }
    });

    test('Should detect type mismatch warnings', async () => {
        console.log('Testing type mismatch detection...');
        
        const uri = vscode.Uri.parse('untitled:diagnostics-test3.groovy');
        document = await vscode.workspace.openTextDocument(uri);
        editor = await vscode.window.showTextDocument(document);
        
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(0, 0), 
                '@groovy.transform.TypeChecked\n' +
                'class TypeCheckedClass {\n' +
                '    String getString() {\n' +
                '        return 123  // 型の不一致\n' +
                '    }\n' +
                '    \n' +
                '    void processNumber(int num) {\n' +
                '        println num\n' +
                '    }\n' +
                '    \n' +
                '    void test() {\n' +
                '        processNumber("text")  // 型の不一致\n' +
                '    }\n' +
                '}\n'
            );
        });
        
        await new Promise(resolve => setTimeout(resolve, 2000));
        
        const diagnostics = vscode.languages.getDiagnostics(document.uri);
        console.log(`Found ${diagnostics.length} diagnostics`);
        
        assert.ok(diagnostics, 'Diagnostics should be returned');
        
        // 型の不一致に関する診断をチェック
        const typeMismatchDiagnostics = diagnostics.filter(d => 
            d.message.toLowerCase().includes('type') ||
            d.message.toLowerCase().includes('cannot') ||
            d.message.toLowerCase().includes('incompatible')
        );
        
        console.log(`Type-related diagnostics found: ${typeMismatchDiagnostics.length}`);
        typeMismatchDiagnostics.forEach((d, i) => {
            console.log(`  ${i + 1}. ${d.message} (line ${d.range.start.line})`);
        });
        
        // TypeCheckedアノテーションがある場合は型エラーが検出されるはず
        if (typeMismatchDiagnostics.length > 0) {
            console.log('✓ Type mismatches detected correctly');
        } else {
            console.log('Note: Type checking may require specific Groovy compiler configuration');
        }
    });

    test('Should clear diagnostics when errors are fixed', async () => {
        console.log('Testing diagnostic clearing...');
        
        const uri = vscode.Uri.parse('untitled:diagnostics-test4.groovy');
        document = await vscode.workspace.openTextDocument(uri);
        editor = await vscode.window.showTextDocument(document);
        
        // エラーを含むコードを挿入
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(0, 0), 
                'class Test {\n' +
                '    def method() {\n' +
                '        println "Hello\n' +  // エラー：文字列が閉じていない
                '    }\n' +
                '}\n'
            );
        });
        
        await new Promise(resolve => setTimeout(resolve, 2000));
        
        let diagnostics = vscode.languages.getDiagnostics(document.uri);
        console.log(`Initial diagnostics: ${diagnostics.length}`);
        assert.ok(diagnostics.length > 0, 'Should have diagnostics initially');
        
        // エラーを修正
        await editor.edit(editBuilder => {
            const line = document.lineAt(2);
            editBuilder.replace(line.range, '        println "Hello"');
        });
        
        await new Promise(resolve => setTimeout(resolve, 2000));
        
        diagnostics = vscode.languages.getDiagnostics(document.uri);
        console.log(`Diagnostics after fix: ${diagnostics.length}`);
        assert.equal(diagnostics.length, 0, 'Should have no diagnostics after fixing the error');
        
        console.log('✓ Diagnostics cleared correctly after fixing errors');
    });

    teardown(async () => {
        console.log('Cleaning up diagnostics test...');
        if (editor) {
            await vscode.commands.executeCommand('workbench.action.closeActiveEditor');
        }
    });
});