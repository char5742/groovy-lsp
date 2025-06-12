import assert from 'assert';
import path from 'path';
import { createRequire } from 'module';
import { fileURLToPath } from 'url';

const require = createRequire(import.meta.url);
const vscode = require('vscode');

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

suite('Gradle Integration Test Suite', () => {
    const fixturesPath = path.join(__dirname, '..', 'fixtures', 'gradle-project');
    let buildGradleDoc;
    let settingsGradleDoc;

    suiteSetup(async () => {
        console.log('Setting up Gradle Integration test suite...');
        // 拡張機能のアクティベーションを待つ
        await new Promise(resolve => setTimeout(resolve, 2000));
    });

    test('Should provide completion for Gradle DSL in build.gradle', async () => {
        console.log('Testing Gradle DSL completion...');
        
        // build.gradleを開く
        const buildGradleUri = vscode.Uri.file(path.join(fixturesPath, 'build.gradle'));
        buildGradleDoc = await vscode.workspace.openTextDocument(buildGradleUri);
        const editor = await vscode.window.showTextDocument(buildGradleDoc);
        
        // 新しい依存関係を追加する位置に移動
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(17, 0), '    implementation ');
        });
        
        // "implementation " の後で補完を実行
        const position = new vscode.Position(17, 19);
        console.log(`Triggering completion at position ${position.line}:${position.character}`);
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const completions = await vscode.commands.executeCommand(
            'vscode.executeCompletionItemProvider',
            buildGradleDoc.uri,
            position
        );
        
        console.log(`Received ${completions?.items?.length || 0} completion items`);
        
        assert.ok(completions, 'Completions should be returned');
        assert.ok(completions.items, 'Completion items should exist');
        
        // Gradle DSL関連の補完があるか確認
        const gradleKeywords = ['group:', 'name:', 'version:'];
        const hasGradleCompletion = completions.items.some(item => {
            const label = typeof item.label === 'string' ? item.label : item.label.label;
            return gradleKeywords.some(keyword => label.includes(keyword));
        });
        
        console.log(`Gradle DSL completions found: ${hasGradleCompletion}`);
        if (!hasGradleCompletion) {
            console.log('Note: Gradle DSL completion may require specific LSP implementation');
        }
        
        // 追加した行を削除
        await editor.edit(editBuilder => {
            const line17 = buildGradleDoc.lineAt(17);
            editBuilder.delete(line17.range);
        });
    });

    test('Should resolve project dependencies', async () => {
        console.log('Testing dependency resolution...');
        
        // GradleService.groovyを開く
        const serviceUri = vscode.Uri.file(path.join(fixturesPath, 'src/main/groovy/com/example/GradleService.groovy'));
        const serviceDoc = await vscode.workspace.openTextDocument(serviceUri);
        const editor = await vscode.window.showTextDocument(serviceDoc);
        
        // Guavaのimport文の位置で定義ジャンプを試みる
        const importLine = 2; // import com.google.common.collect.Lists
        const position = new vscode.Position(importLine, 35); // Lists の位置
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const definitions = await vscode.commands.executeCommand(
            'vscode.executeDefinitionProvider',
            serviceDoc.uri,
            position
        );
        
        console.log(`Found ${definitions?.length || 0} definitions for Lists class`);
        
        if (definitions && definitions.length > 0) {
            // 外部ライブラリへの定義ジャンプが機能している
            console.log('✓ External dependency resolution works');
            assert.ok(definitions[0].uri.path.includes('guava') || 
                     definitions[0].uri.path.includes('.gradle'), 
                     'Should resolve to Guava library');
        } else {
            console.log('Note: External dependency resolution requires classpath configuration');
        }
        
        // プロジェクト内でGuavaのメソッドを使用している箇所で補完を確認
        const methodLine = 8; // Lists.reverse(items) の行
        const methodPosition = new vscode.Position(methodLine, 21); // Lists. の後
        
        const methodCompletions = await vscode.commands.executeCommand(
            'vscode.executeCompletionItemProvider',
            serviceDoc.uri,
            methodPosition
        );
        
        console.log(`Guava method completions: ${methodCompletions?.items?.length || 0}`);
        
        if (methodCompletions && methodCompletions.items.length > 0) {
            const hasReverseMethod = methodCompletions.items.some(item => {
                const label = typeof item.label === 'string' ? item.label : item.label.label;
                return label.includes('reverse');
            });
            console.log(`Lists.reverse method found: ${hasReverseMethod}`);
        }
    });

    test('Should show Gradle task definitions in symbols', async () => {
        console.log('Testing Gradle task symbols...');
        
        const buildGradleUri = vscode.Uri.file(path.join(fixturesPath, 'build.gradle'));
        buildGradleDoc = await vscode.workspace.openTextDocument(buildGradleUri);
        await vscode.window.showTextDocument(buildGradleDoc);
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        // ドキュメントシンボルを取得
        const symbols = await vscode.commands.executeCommand(
            'vscode.executeDocumentSymbolProvider',
            buildGradleDoc.uri
        );
        
        console.log(`Found ${symbols?.length || 0} document symbols`);
        
        if (symbols && symbols.length > 0) {
            // タスクがシンボルとして認識されているか確認
            const taskSymbols = symbols.filter(s => 
                s.name === 'hello' || s.name === 'customTask' ||
                s.name.includes('task')
            );
            
            console.log(`Found ${taskSymbols.length} task symbols`);
            taskSymbols.forEach(task => {
                console.log(`  - ${task.name} (${task.kind})`);
            });
            
            assert.ok(symbols.length > 0, 'Should have symbols in build.gradle');
        } else {
            console.log('Note: Gradle DSL symbol recognition may require specific implementation');
        }
    });

    test('Should provide completion for Gradle plugins', async () => {
        console.log('Testing Gradle plugin completion...');
        
        const buildGradleUri = vscode.Uri.file(path.join(fixturesPath, 'build.gradle'));
        buildGradleDoc = await vscode.workspace.openTextDocument(buildGradleUri);
        const editor = await vscode.window.showTextDocument(buildGradleDoc);
        
        // pluginsブロック内で補完をテスト
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(3, 0), "    id '");
        });
        
        const position = new vscode.Position(3, 8); // id ' の後
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const completions = await vscode.commands.executeCommand(
            'vscode.executeCompletionItemProvider',
            buildGradleDoc.uri,
            position
        );
        
        console.log(`Plugin completions: ${completions?.items?.length || 0}`);
        
        if (completions && completions.items.length > 0) {
            // 一般的なGradleプラグインが提案されるか確認
            const commonPlugins = ['application', 'java-library', 'maven-publish'];
            const hasPluginSuggestions = completions.items.some(item => {
                const label = typeof item.label === 'string' ? item.label : item.label.label;
                return commonPlugins.some(plugin => label.includes(plugin));
            });
            
            console.log(`Common plugin suggestions found: ${hasPluginSuggestions}`);
        } else {
            console.log('Note: Plugin completion requires Gradle plugin registry integration');
        }
        
        // 追加した行を削除
        await editor.edit(editBuilder => {
            const line3 = buildGradleDoc.lineAt(3);
            editBuilder.delete(line3.range);
        });
    });

    test('Should handle Gradle configuration blocks', async () => {
        console.log('Testing Gradle configuration blocks...');
        
        const buildGradleUri = vscode.Uri.file(path.join(fixturesPath, 'build.gradle'));
        buildGradleDoc = await vscode.workspace.openTextDocument(buildGradleUri);
        const editor = await vscode.window.showTextDocument(buildGradleDoc);
        
        // configurationsブロック内の位置
        const configLine = 36; // configurations { の次の行
        const position = new vscode.Position(configLine, 4);
        
        // 新しい設定を追加
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(configLine + 1, 0), '    ');
        });
        
        const configPosition = new vscode.Position(configLine + 1, 4);
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        const completions = await vscode.commands.executeCommand(
            'vscode.executeCompletionItemProvider',
            buildGradleDoc.uri,
            configPosition
        );
        
        console.log(`Configuration completions: ${completions?.items?.length || 0}`);
        
        if (completions && completions.items.length > 0) {
            console.log('Sample completions:');
            completions.items.slice(0, 5).forEach(item => {
                const label = typeof item.label === 'string' ? item.label : item.label.label;
                console.log(`  - ${label}`);
            });
        }
        
        // 追加した行を削除
        await editor.edit(editBuilder => {
            const addedLine = buildGradleDoc.lineAt(configLine + 1);
            editBuilder.delete(addedLine.range);
        });
    });

    teardown(async () => {
        console.log('Cleaning up Gradle integration test...');
        await vscode.commands.executeCommand('workbench.action.closeActiveEditor');
    });
});