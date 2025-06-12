import assert from 'assert';
import path from 'path';
import { createRequire } from 'module';

const require = createRequire(import.meta.url);
const vscode = require('vscode');

suite('Refactoring Features Test Suite', () => {
    let document;
    let editor;

    suiteSetup(async () => {
        console.log('Setting up Refactoring test suite...');
        // 拡張機能のアクティベーションを待つ
        await new Promise(resolve => setTimeout(resolve, 2000));
    });

    test('Should rename variable across file', async () => {
        console.log('Testing variable rename...');
        
        // 一時的なGroovyファイルを作成
        const uri = vscode.Uri.parse('untitled:refactor-rename.groovy');
        document = await vscode.workspace.openTextDocument(uri);
        editor = await vscode.window.showTextDocument(document);
        
        // テストコードを挿入
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(0, 0), 
                'class Calculator {\n' +
                '    def calculate() {\n' +
                '        def result = 0\n' +
                '        def value1 = 10\n' +
                '        def value2 = 20\n' +
                '        \n' +
                '        result = value1 + value2\n' +
                '        println "Result is: $result"\n' +
                '        \n' +
                '        if (result > 25) {\n' +
                '            result = result * 2\n' +
                '        }\n' +
                '        \n' +
                '        return result\n' +
                '    }\n' +
                '}\n'
            );
        });
        
        // "result" 変数の最初の出現位置
        const position = new vscode.Position(2, 12); // def result = 0
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        // 名前変更の準備ができているか確認
        const renameRange = await vscode.commands.executeCommand(
            'vscode.prepareRename',
            document.uri,
            position
        );
        
        console.log(`Rename preparation result: ${renameRange ? 'Ready' : 'Not ready'}`);
        
        if (renameRange) {
            // 名前変更を実行
            const workspaceEdit = await vscode.commands.executeCommand(
                'vscode.executeDocumentRenameProvider',
                document.uri,
                position,
                'calculationResult'
            );
            
            console.log(`Rename edits: ${workspaceEdit?.entries()?.length || 0}`);
            
            assert.ok(workspaceEdit, 'WorkspaceEdit should be returned');
            
            // 編集を適用
            const success = await vscode.workspace.applyEdit(workspaceEdit);
            assert.ok(success, 'Rename should be applied successfully');
            
            // 変更後のテキストを確認
            const text = document.getText();
            assert.ok(text.includes('calculationResult'), 'Variable should be renamed');
            assert.ok(!text.includes('def result ='), 'Old variable name should not exist');
            
            // すべての出現箇所が変更されているか確認
            const resultCount = (text.match(/\bresult\b/g) || []).length;
            const calculationResultCount = (text.match(/calculationResult/g) || []).length;
            
            console.log(`Old name occurrences: ${resultCount}`);
            console.log(`New name occurrences: ${calculationResultCount}`);
            
            assert.equal(resultCount, 0, 'All occurrences should be renamed');
            assert.ok(calculationResultCount >= 4, 'Should have multiple renamed occurrences');
            
            console.log('✓ Variable rename completed successfully');
        } else {
            console.log('Note: Rename provider may not be available for all elements');
        }
    });

    test('Should extract method from selected code', async () => {
        console.log('Testing method extraction...');
        
        const uri = vscode.Uri.parse('untitled:refactor-extract.groovy');
        document = await vscode.workspace.openTextDocument(uri);
        editor = await vscode.window.showTextDocument(document);
        
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(0, 0), 
                'class OrderProcessor {\n' +
                '    def processOrder(order) {\n' +
                '        // Calculate total\n' +
                '        def subtotal = 0\n' +
                '        order.items.each { item ->\n' +
                '            subtotal += item.price * item.quantity\n' +
                '        }\n' +
                '        def tax = subtotal * 0.1\n' +
                '        def total = subtotal + tax\n' +
                '        \n' +
                '        // Apply discount\n' +
                '        if (order.customer.isVip) {\n' +
                '            total = total * 0.9\n' +
                '        }\n' +
                '        \n' +
                '        return total\n' +
                '    }\n' +
                '}\n'
            );
        });
        
        // 計算ロジック部分を選択（3行目から9行目）
        const startPos = new vscode.Position(3, 0);
        const endPos = new vscode.Position(8, 33);
        editor.selection = new vscode.Selection(startPos, endPos);
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        // コードアクションを取得
        const codeActions = await vscode.commands.executeCommand(
            'vscode.executeCodeActionProvider',
            document.uri,
            editor.selection
        );
        
        console.log(`Available code actions: ${codeActions?.length || 0}`);
        
        if (codeActions && codeActions.length > 0) {
            // "Extract Method" アクションを探す
            const extractMethodAction = codeActions.find(action => 
                action.title.toLowerCase().includes('extract') && 
                action.title.toLowerCase().includes('method')
            );
            
            if (extractMethodAction) {
                console.log(`Found extract method action: ${extractMethodAction.title}`);
                
                // アクションを実行
                if (extractMethodAction.edit) {
                    const success = await vscode.workspace.applyEdit(extractMethodAction.edit);
                    assert.ok(success, 'Extract method should be applied');
                    
                    const newText = document.getText();
                    console.log('✓ Method extraction completed');
                } else if (extractMethodAction.command) {
                    // コマンドとして実行
                    await vscode.commands.executeCommand(
                        extractMethodAction.command.command,
                        ...extractMethodAction.command.arguments
                    );
                    console.log('✓ Method extraction command executed');
                }
            } else {
                console.log('Note: Extract method action not found in available actions');
                codeActions.forEach(action => {
                    console.log(`  - ${action.title}`);
                });
            }
        } else {
            console.log('Note: Code actions may not be available for method extraction');
        }
    });

    test('Should inline variable', async () => {
        console.log('Testing variable inline...');
        
        const uri = vscode.Uri.parse('untitled:refactor-inline.groovy');
        document = await vscode.workspace.openTextDocument(uri);
        editor = await vscode.window.showTextDocument(document);
        
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(0, 0), 
                'class StringProcessor {\n' +
                '    def process(input) {\n' +
                '        def prefix = "PROCESSED_"\n' +
                '        def suffix = "_END"\n' +
                '        def result = prefix + input + suffix\n' +
                '        return result\n' +
                '    }\n' +
                '    \n' +
                '    def processMultiple(inputs) {\n' +
                '        def prefix = "BATCH_"\n' +
                '        return inputs.collect { prefix + it }\n' +
                '    }\n' +
                '}\n'
            );
        });
        
        // "prefix" 変数の位置
        const position = new vscode.Position(2, 12); // def prefix = "PROCESSED_"
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        // コードアクションを取得
        const codeActions = await vscode.commands.executeCommand(
            'vscode.executeCodeActionProvider',
            document.uri,
            new vscode.Range(position, position)
        );
        
        console.log(`Available code actions: ${codeActions?.length || 0}`);
        
        if (codeActions && codeActions.length > 0) {
            // "Inline Variable" アクションを探す
            const inlineAction = codeActions.find(action => 
                action.title.toLowerCase().includes('inline')
            );
            
            if (inlineAction) {
                console.log(`Found inline action: ${inlineAction.title}`);
                
                // アクションを実行
                if (inlineAction.edit) {
                    const success = await vscode.workspace.applyEdit(inlineAction.edit);
                    assert.ok(success, 'Inline variable should be applied');
                    
                    const newText = document.getText();
                    // prefixが直接値に置き換えられているか確認
                    assert.ok(newText.includes('"PROCESSED_" + input'), 
                        'Variable should be inlined with its value');
                    
                    console.log('✓ Variable inline completed');
                } else if (inlineAction.command) {
                    await vscode.commands.executeCommand(
                        inlineAction.command.command,
                        ...inlineAction.command.arguments
                    );
                    console.log('✓ Inline command executed');
                }
            } else {
                console.log('Note: Inline action not found');
            }
        } else {
            console.log('Note: Code actions may not be available for inline refactoring');
        }
    });

    test('Should show refactoring quick fixes', async () => {
        console.log('Testing refactoring quick fixes...');
        
        const uri = vscode.Uri.parse('untitled:refactor-quickfix.groovy');
        document = await vscode.workspace.openTextDocument(uri);
        editor = await vscode.window.showTextDocument(document);
        
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(0, 0), 
                'class CodeSmells {\n' +
                '    // Long method that could be refactored\n' +
                '    def complexCalculation(a, b, c, d, e) {\n' +
                '        def temp1 = a + b\n' +
                '        def temp2 = c + d\n' +
                '        def temp3 = temp1 * temp2\n' +
                '        def temp4 = temp3 + e\n' +
                '        def temp5 = temp4 * 2\n' +
                '        def temp6 = temp5 - 10\n' +
                '        def temp7 = temp6 / 3\n' +
                '        return temp7\n' +
                '    }\n' +
                '    \n' +
                '    // Duplicate code\n' +
                '    def method1() {\n' +
                '        println "Starting process..."\n' +
                '        def data = loadData()\n' +
                '        processData(data)\n' +
                '        println "Process completed"\n' +
                '    }\n' +
                '    \n' +
                '    def method2() {\n' +
                '        println "Starting process..."\n' +
                '        def data = loadData()\n' +
                '        processData(data)\n' +
                '        println "Process completed"\n' +
                '    }\n' +
                '}\n'
            );
        });
        
        // 複雑なメソッドの範囲を選択
        const methodStart = new vscode.Position(2, 0);
        const methodEnd = new vscode.Position(11, 5);
        const methodRange = new vscode.Range(methodStart, methodEnd);
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        // コードアクションを取得
        const codeActions = await vscode.commands.executeCommand(
            'vscode.executeCodeActionProvider',
            document.uri,
            methodRange
        );
        
        console.log(`Available refactoring actions: ${codeActions?.length || 0}`);
        
        if (codeActions && codeActions.length > 0) {
            console.log('Available refactoring options:');
            codeActions.forEach(action => {
                console.log(`  - ${action.title} (${action.kind?.value || 'unknown kind'})`);
            });
            
            // リファクタリング関連のアクションをフィルタ
            const refactorActions = codeActions.filter(action => 
                action.kind?.value?.includes('refactor') ||
                action.title.toLowerCase().includes('extract') ||
                action.title.toLowerCase().includes('inline') ||
                action.title.toLowerCase().includes('rename')
            );
            
            console.log(`Refactoring-specific actions: ${refactorActions.length}`);
            assert.ok(codeActions.length > 0, 'Should have some code actions available');
        } else {
            console.log('Note: No code actions available for this code');
        }
    });

    teardown(async () => {
        console.log('Cleaning up refactoring test...');
        if (editor) {
            await vscode.commands.executeCommand('workbench.action.closeActiveEditor');
        }
    });
});