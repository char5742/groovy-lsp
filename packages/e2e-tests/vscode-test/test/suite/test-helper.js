// テストヘルパー関数
import { createRequire } from 'module';

const require = createRequire(import.meta.url);
const vscode = require('vscode');

/**
 * 拡張機能を安全にアクティベートする
 */
export async function activateExtension() {
    const extensionId = 'groovy-lsp.groovy-language-server';
    const extension = vscode.extensions.getExtension(extensionId);
    
    if (!extension) {
        throw new Error(`Extension ${extensionId} not found`);
    }
    
    if (!extension.isActive) {
        await extension.activate();
    }
    
    return extension;
}

/**
 * 拡張機能を安全にディアクティベートする
 */
export async function deactivateExtension() {
    const extensionId = 'groovy-lsp.groovy-language-server';
    const extension = vscode.extensions.getExtension(extensionId);
    
    if (extension && extension.isActive) {
        try {
            // deactivate関数を直接呼び出す
            const deactivateFunction = require(extension.extensionPath + '/out/extension').deactivate;
            if (deactivateFunction) {
                const result = deactivateFunction();
                if (result && typeof result.then === 'function') {
                    await result;
                }
            }
        } catch (error) {
            console.log('Error during extension deactivation:', error.message);
        }
    }
}

/**
 * テスト環境をクリーンアップする
 */
export async function cleanupTestEnvironment() {
    // すべてのエディタを閉じる
    await vscode.commands.executeCommand('workbench.action.closeAllEditors');
    
    // 少し待つ
    await new Promise(resolve => setTimeout(resolve, 100));
    
    // 拡張機能をディアクティベート
    await deactivateExtension();
}

/**
 * Language Serverの準備ができるまで待つ
 */
export async function waitForLanguageServer(maxWaitTime = 5000) {
    const startTime = Date.now();
    
    while (Date.now() - startTime < maxWaitTime) {
        try {
            // 簡単なコンプリーションリクエストを送信してサーバーの準備状態を確認
            const doc = await vscode.workspace.openTextDocument({
                language: 'groovy',
                content: 'def test = '
            });
            
            const position = new vscode.Position(0, 11);
            const completions = await vscode.commands.executeCommand(
                'vscode.executeCompletionItemProvider',
                doc.uri,
                position
            );
            
            if (completions) {
                // ドキュメントを閉じる
                await vscode.commands.executeCommand('workbench.action.closeActiveEditor');
                return true;
            }
        } catch (error) {
            // エラーは無視して再試行
        }
        
        await new Promise(resolve => setTimeout(resolve, 500));
    }
    
    return false;
}