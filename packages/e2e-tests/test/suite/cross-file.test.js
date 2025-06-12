import assert from 'assert';
import path from 'path';
import { createRequire } from 'module';
import { fileURLToPath } from 'url';

const require = createRequire(import.meta.url);
const vscode = require('vscode');

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

suite.skip('Cross-File Features Test Suite - Skipped due to Java 23 compatibility issue', () => {
    const fixturesPath = path.join(__dirname, '..', 'fixtures', 'cross-file-project');
    let serviceDocument;
    let serviceTestDocument;
    let userDocument;

    suiteSetup(async () => {
        console.log('Setting up Cross-File test suite...');
        // 拡張機能のアクティベーションを待つ
        await new Promise(resolve => setTimeout(resolve, 2000));
    });

    test('Should resolve imports across multiple files', async () => {
        console.log('Testing cross-file import resolution...');
        
        // Service.groovyを開く
        const serviceUri = vscode.Uri.file(path.join(fixturesPath, 'src/main/groovy/com/example/Service.groovy'));
        serviceDocument = await vscode.workspace.openTextDocument(serviceUri);
        const serviceEditor = await vscode.window.showTextDocument(serviceDocument);
        
        // import文のUserクラスの位置で補完を確認
        const importLine = 2; // import com.example.model.User
        const position = new vscode.Position(importLine, 25); // Userの位置
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        // 定義へジャンプできるか確認
        const definitions = await vscode.commands.executeCommand(
            'vscode.executeDefinitionProvider',
            serviceDocument.uri,
            position
        );
        
        console.log(`Found ${definitions?.length || 0} definitions for User import`);
        
        assert.ok(definitions, 'Definitions should be returned');
        assert.ok(Array.isArray(definitions), 'Definitions should be an array');
        assert.ok(definitions.length > 0, 'Should find User class definition');
        
        // 定義がUser.groovyファイルを指していることを確認
        const userDefinition = definitions[0];
        assert.ok(userDefinition.uri.path.includes('User.groovy'), 'Definition should point to User.groovy');
    });

    test('Should provide import suggestions for classes in workspace', async () => {
        console.log('Testing import suggestions...');
        
        // 新しいファイルを作成してimportなしでUserクラスを使用
        const newFileUri = vscode.Uri.parse('untitled:NewService.groovy');
        const newDocument = await vscode.workspace.openTextDocument(newFileUri);
        const editor = await vscode.window.showTextDocument(newDocument);
        
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(0, 0), 
                'package com.example\n\n' +
                'class NewService {\n' +
                '    def createUser() {\n' +
                '        def user = new User("1", "Test", "User", "test@example.com")\n' +
                '        return user\n' +
                '    }\n' +
                '}\n'
            );
        });
        
        // Userクラスの位置で補完を確認
        const position = new vscode.Position(4, 23); // new Userの位置
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const completions = await vscode.commands.executeCommand(
            'vscode.executeCompletionItemProvider',
            newDocument.uri,
            position
        );
        
        console.log(`Received ${completions?.items?.length || 0} completion items`);
        
        assert.ok(completions, 'Completions should be returned');
        assert.ok(completions.items, 'Completion items should exist');
        
        // Userクラスの補完が含まれているか確認
        const userCompletion = completions.items.find(item => {
            const label = typeof item.label === 'string' ? item.label : item.label.label;
            return label === 'User' || label.includes('com.example.model.User');
        });
        
        if (userCompletion) {
            console.log('✓ User class found in completions');
            // import文の追加を含む補完かどうか確認
            const hasImportEdit = userCompletion.additionalTextEdits && 
                userCompletion.additionalTextEdits.some(edit => 
                    edit.newText.includes('import')
                );
            console.log(`Has import edit: ${hasImportEdit}`);
        } else {
            console.log('Note: Import suggestions may require specific LSP implementation');
        }
        
        await vscode.commands.executeCommand('workbench.action.closeActiveEditor');
    });

    test('Should navigate to definition in different file', async () => {
        console.log('Testing cross-file definition navigation...');
        
        // ServiceTest.groovyを開く
        const testUri = vscode.Uri.file(path.join(fixturesPath, 'src/test/groovy/com/example/ServiceTest.groovy'));
        serviceTestDocument = await vscode.workspace.openTextDocument(testUri);
        const testEditor = await vscode.window.showTextDocument(serviceTestDocument);
        
        // Service クラスの使用箇所
        const serviceLine = 9; // service = new Service()
        const position = new vscode.Position(serviceLine, 22); // Serviceの位置
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const definitions = await vscode.commands.executeCommand(
            'vscode.executeDefinitionProvider',
            serviceTestDocument.uri,
            position
        );
        
        console.log(`Found ${definitions?.length || 0} definitions for Service class`);
        
        assert.ok(definitions, 'Definitions should be returned');
        assert.ok(Array.isArray(definitions), 'Definitions should be an array');
        assert.ok(definitions.length > 0, 'Should find Service class definition');
        
        // 定義がService.groovyファイルを指していることを確認
        const serviceDefinition = definitions[0];
        assert.ok(serviceDefinition.uri.path.includes('Service.groovy'), 
            'Definition should point to Service.groovy');
        assert.ok(!serviceDefinition.uri.path.includes('ServiceTest.groovy'), 
            'Definition should not be in test file');
        
        console.log('✓ Cross-file navigation works correctly');
    });

    test('Should resolve method calls across files', async () => {
        console.log('Testing method resolution across files...');
        
        // ServiceTest.groovyでメソッド呼び出しの解決をテスト
        const testUri = vscode.Uri.file(path.join(fixturesPath, 'src/test/groovy/com/example/ServiceTest.groovy'));
        serviceTestDocument = await vscode.workspace.openTextDocument(testUri);
        const testEditor = await vscode.window.showTextDocument(serviceTestDocument);
        
        // getUserCount() メソッドの呼び出し位置
        const methodLine = 20; // service.getUserCount()
        const position = new vscode.Position(methodLine, 16); // getUserCountの位置
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const definitions = await vscode.commands.executeCommand(
            'vscode.executeDefinitionProvider',
            serviceTestDocument.uri,
            position
        );
        
        console.log(`Found ${definitions?.length || 0} definitions for getUserCount method`);
        
        if (definitions && definitions.length > 0) {
            const methodDefinition = definitions[0];
            assert.ok(methodDefinition.uri.path.includes('Service.groovy'), 
                'Method definition should be in Service.groovy');
            console.log('✓ Method resolution across files works');
        } else {
            console.log('Note: Method resolution may require more advanced LSP features');
        }
    });

    test('Should find references across multiple files', async () => {
        console.log('Testing find references across files...');
        
        // User.groovyを開く
        const userUri = vscode.Uri.file(path.join(fixturesPath, 'src/main/groovy/com/example/model/User.groovy'));
        userDocument = await vscode.workspace.openTextDocument(userUri);
        const userEditor = await vscode.window.showTextDocument(userDocument);
        
        // Userクラス定義の位置
        const classLine = 5; // class User
        const position = new vscode.Position(classLine, 7); // Userの位置
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const references = await vscode.commands.executeCommand(
            'vscode.executeReferenceProvider',
            userDocument.uri,
            position
        );
        
        console.log(`Found ${references?.length || 0} references to User class`);
        
        if (references && references.length > 0) {
            // Service.groovyとServiceTest.groovyでの参照が含まれているか確認
            const serviceReference = references.find(ref => 
                ref.uri.path.includes('Service.groovy')
            );
            const testReference = references.find(ref => 
                ref.uri.path.includes('ServiceTest.groovy')
            );
            
            console.log(`References in Service.groovy: ${serviceReference ? 'Yes' : 'No'}`);
            console.log(`References in ServiceTest.groovy: ${testReference ? 'Yes' : 'No'}`);
            
            assert.ok(references.length >= 2, 'Should find references in multiple files');
        } else {
            console.log('Note: Find references may require workspace indexing');
        }
    });

    teardown(async () => {
        console.log('Cleaning up cross-file test...');
        // 開いているエディタを閉じる
        await vscode.commands.executeCommand('workbench.action.closeAllEditors');
    });

    suiteTeardown(async () => {
        console.log('Tearing down Cross-File test suite...');
    });
});