import assert from 'assert';
import path from 'path';
import fs from 'fs';
import { createRequire } from 'module';
import { fileURLToPath } from 'url';

const require = createRequire(import.meta.url);
const vscode = require('vscode');

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

suite('Performance and Scalability Test Suite', () => {
    const largeProjectPath = path.join(__dirname, '..', 'fixtures', 'large-project');
    let generatedFiles = [];

    suiteSetup(async () => {
        console.log('Setting up Performance test suite...');
        // 拡張機能のアクティベーションを待つ
        await new Promise(resolve => setTimeout(resolve, 2000));
    });

    test('Should handle large files efficiently', async () => {
        console.log('Testing large file handling...');
        
        // 1000行以上の大きなGroovyファイルを生成
        const largeFilePath = path.join(largeProjectPath, 'src/main/groovy/com/example/LargeClass.groovy');
        const largeFileContent = generateLargeGroovyFile(1500);
        
        // ディレクトリが存在することを確認
        const dir = path.dirname(largeFilePath);
        if (!fs.existsSync(dir)) {
            fs.mkdirSync(dir, { recursive: true });
        }
        
        fs.writeFileSync(largeFilePath, largeFileContent);
        generatedFiles.push(largeFilePath);
        
        const uri = vscode.Uri.file(largeFilePath);
        const document = await vscode.workspace.openTextDocument(uri);
        const editor = await vscode.window.showTextDocument(document);
        
        // ファイルの中央付近で補完を実行
        const middleLine = 750;
        const position = new vscode.Position(middleLine, 20);
        
        console.log('Measuring completion response time for large file...');
        const startTime = Date.now();
        
        const completions = await vscode.commands.executeCommand(
            'vscode.executeCompletionItemProvider',
            document.uri,
            position
        );
        
        const responseTime = Date.now() - startTime;
        console.log(`Completion response time: ${responseTime}ms`);
        
        assert.ok(completions, 'Completions should be returned');
        assert.ok(responseTime < 1000, `Response time (${responseTime}ms) should be under 1 second`);
        
        // 診断情報の取得時間も測定
        console.log('Measuring diagnostics response time...');
        const diagStartTime = Date.now();
        
        await new Promise(resolve => setTimeout(resolve, 2000)); // 診断の更新を待つ
        const diagnostics = vscode.languages.getDiagnostics(document.uri);
        
        const diagResponseTime = Date.now() - diagStartTime;
        console.log(`Diagnostics response time: ${diagResponseTime}ms`);
        console.log(`Found ${diagnostics.length} diagnostics`);
        
        await vscode.commands.executeCommand('workbench.action.closeActiveEditor');
    });

    test('Should work with projects containing 100+ Groovy files', async () => {
        console.log('Testing large project handling...');
        
        // 100個のGroovyファイルを生成
        console.log('Generating 100 Groovy files...');
        const startGenTime = Date.now();
        
        for (let i = 0; i < 100; i++) {
            const category = i < 25 ? 'model' : i < 50 ? 'service' : i < 75 ? 'util' : 'controller';
            const filePath = path.join(largeProjectPath, `src/main/groovy/com/example/${category}/Generated${i}.groovy`);
            const content = generateGroovyClass(`Generated${i}`, category, i);
            
            const dir = path.dirname(filePath);
            if (!fs.existsSync(dir)) {
                fs.mkdirSync(dir, { recursive: true });
            }
            
            fs.writeFileSync(filePath, content);
            generatedFiles.push(filePath);
        }
        
        console.log(`Generated 100 files in ${Date.now() - startGenTime}ms`);
        
        // ワークスペースシンボルの検索性能をテスト
        console.log('Testing workspace symbol search performance...');
        const searchStartTime = Date.now();
        
        const symbols = await vscode.commands.executeCommand(
            'vscode.executeWorkspaceSymbolProvider',
            'Generated'
        );
        
        const searchTime = Date.now() - searchStartTime;
        console.log(`Workspace symbol search completed in ${searchTime}ms`);
        console.log(`Found ${symbols?.length || 0} symbols`);
        
        assert.ok(searchTime < 5000, `Search time (${searchTime}ms) should be under 5 seconds`);
        
        // 特定のファイルを開いてクロスファイル参照をテスト
        const testFilePath = path.join(largeProjectPath, 'src/main/groovy/com/example/service/Generated30.groovy');
        const uri = vscode.Uri.file(testFilePath);
        const document = await vscode.workspace.openTextDocument(uri);
        const editor = await vscode.window.showTextDocument(document);
        
        // 他のクラスへの参照を追加
        await editor.edit(editBuilder => {
            editBuilder.insert(new vscode.Position(10, 0), 
                '        def model = new Generated5()\n'
            );
        });
        
        // 参照解決の性能をテスト
        const refPosition = new vscode.Position(10, 28); // Generated5 の位置
        const refStartTime = Date.now();
        
        const definitions = await vscode.commands.executeCommand(
            'vscode.executeDefinitionProvider',
            document.uri,
            refPosition
        );
        
        const refTime = Date.now() - refStartTime;
        console.log(`Reference resolution completed in ${refTime}ms`);
        console.log(`Found ${definitions?.length || 0} definitions`);
        
        await vscode.commands.executeCommand('workbench.action.closeActiveEditor');
    });

    test('Should maintain performance with deep inheritance hierarchies', async () => {
        console.log('Testing deep inheritance performance...');
        
        // 深い継承階層を持つクラスを生成
        const baseClasses = [];
        for (let i = 0; i < 10; i++) {
            const className = `BaseClass${i}`;
            const parentClass = i > 0 ? `BaseClass${i - 1}` : '';
            const filePath = path.join(largeProjectPath, `src/main/groovy/com/example/model/${className}.groovy`);
            const content = generateInheritanceClass(className, parentClass, i);
            
            fs.writeFileSync(filePath, content);
            generatedFiles.push(filePath);
            baseClasses.push(className);
        }
        
        // 最も深いクラスを開く
        const deepestClassPath = path.join(largeProjectPath, 'src/main/groovy/com/example/model/BaseClass9.groovy');
        const uri = vscode.Uri.file(deepestClassPath);
        const document = await vscode.workspace.openTextDocument(uri);
        const editor = await vscode.window.showTextDocument(document);
        
        // 継承したメソッドの補完性能をテスト
        const position = new vscode.Position(15, 0);
        await editor.edit(editBuilder => {
            editBuilder.insert(position, '        this.');
        });
        
        const completionPos = new vscode.Position(15, 13);
        const startTime = Date.now();
        
        const completions = await vscode.commands.executeCommand(
            'vscode.executeCompletionItemProvider',
            document.uri,
            completionPos
        );
        
        const responseTime = Date.now() - startTime;
        console.log(`Deep inheritance completion time: ${responseTime}ms`);
        console.log(`Found ${completions?.items?.length || 0} completion items`);
        
        // 継承したメソッドが含まれているか確認
        if (completions && completions.items.length > 0) {
            const inheritedMethods = completions.items.filter(item => {
                const label = typeof item.label === 'string' ? item.label : item.label.label;
                return label.includes('method');
            });
            console.log(`Found ${inheritedMethods.length} inherited methods`);
        }
        
        assert.ok(responseTime < 2000, `Response time (${responseTime}ms) should be under 2 seconds`);
        
        await vscode.commands.executeCommand('workbench.action.closeActiveEditor');
    });

    test('Should handle concurrent file operations efficiently', async () => {
        console.log('Testing concurrent file operations...');
        
        // 複数のファイルを同時に開いて操作
        const filePromises = [];
        const numFiles = 5;
        
        for (let i = 0; i < numFiles; i++) {
            const filePath = path.join(largeProjectPath, `src/main/groovy/com/example/ConcurrentTest${i}.groovy`);
            const content = generateGroovyClass(`ConcurrentTest${i}`, 'concurrent', i);
            fs.writeFileSync(filePath, content);
            generatedFiles.push(filePath);
            
            // 非同期でファイルを開く
            filePromises.push(vscode.workspace.openTextDocument(vscode.Uri.file(filePath)));
        }
        
        console.log(`Opening ${numFiles} files concurrently...`);
        const openStartTime = Date.now();
        const documents = await Promise.all(filePromises);
        const openTime = Date.now() - openStartTime;
        console.log(`Opened ${numFiles} files in ${openTime}ms`);
        
        // 各ファイルで同時に補完を実行
        const completionPromises = documents.map((doc, index) => {
            const position = new vscode.Position(10, 20);
            return vscode.commands.executeCommand(
                'vscode.executeCompletionItemProvider',
                doc.uri,
                position
            );
        });
        
        console.log(`Executing completion on ${numFiles} files concurrently...`);
        const completionStartTime = Date.now();
        const completionResults = await Promise.all(completionPromises);
        const completionTime = Date.now() - completionStartTime;
        
        console.log(`Completed ${numFiles} completion requests in ${completionTime}ms`);
        console.log(`Average time per file: ${(completionTime / numFiles).toFixed(2)}ms`);
        
        assert.ok(completionTime < 5000, `Total completion time (${completionTime}ms) should be under 5 seconds`);
        assert.ok(completionResults.every(r => r !== null), 'All completions should return results');
    });

    suiteTeardown(async () => {
        console.log('Cleaning up generated files...');
        // 生成したファイルを削除
        generatedFiles.forEach(filePath => {
            if (fs.existsSync(filePath)) {
                fs.unlinkSync(filePath);
            }
        });
        
        // 空のディレクトリも削除
        const dirs = ['model', 'service', 'util', 'controller'];
        dirs.forEach(dir => {
            const dirPath = path.join(largeProjectPath, `src/main/groovy/com/example/${dir}`);
            if (fs.existsSync(dirPath) && fs.readdirSync(dirPath).length === 0) {
                fs.rmdirSync(dirPath);
            }
        });
    });
});

// ヘルパー関数：大きなGroovyファイルを生成
function generateLargeGroovyFile(lines) {
    let content = `package com.example

/**
 * Large auto-generated class for performance testing
 */
class LargeClass {
    // Properties
`;
    
    // プロパティを追加
    for (let i = 0; i < lines / 10; i++) {
        content += `    private String property${i}\n`;
    }
    
    content += '\n    // Methods\n';
    
    // メソッドを追加
    for (let i = 0; i < lines / 5; i++) {
        content += `
    def method${i}(param1, param2) {
        def localVar = "value${i}"
        if (param1 > param2) {
            return localVar + property${i % 10}
        } else {
            return method${Math.max(0, i - 1)}(param2, param1)
        }
    }
`;
    }
    
    content += '}\n';
    return content;
}

// ヘルパー関数：Groovyクラスを生成
function generateGroovyClass(className, category, index) {
    return `package com.example.${category}

/**
 * Auto-generated ${category} class ${className}
 */
class ${className} {
    private Long id = ${index}
    private String name = "${className}"
    private Date createdAt = new Date()
    
    def process() {
        println "Processing ${className}"
        return "Result from ${className}"
    }
    
    def calculate(value) {
        return value * ${index + 1}
    }
    
    String toString() {
        return "${className}[id=$id, name=$name]"
    }
}
`;
}

// ヘルパー関数：継承階層を持つクラスを生成
function generateInheritanceClass(className, parentClass, level) {
    const extendsClause = parentClass ? ` extends ${parentClass}` : '';
    return `package com.example.model

/**
 * Inheritance test class level ${level}
 */
class ${className}${extendsClause} {
    private String level${level}Property = "Level ${level}"
    
    def method${level}() {
        println "Method at level ${level}"
        ${parentClass ? `super.method${level - 1}()` : ''}
    }
    
    @Override
    String toString() {
        return "${className} at level ${level}"
    }
}
`;
}