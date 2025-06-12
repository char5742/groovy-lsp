// 最小限のテストケース
import assert from 'assert';
import { createRequire } from 'module';

const require = createRequire(import.meta.url);
// vscodeモジュールはVS Code内でCJSとして提供されるためrequireを使用
const vscode = require('vscode');

suite('Minimal Extension Test Suite', () => {
    console.log('Minimal test suite loaded');
    
    test('VS Code API should be available', () => {
        console.log('Testing VS Code API...');
        assert.ok(vscode, 'VS Code API is not available');
        console.log('✓ VS Code API is available');
    });
    
    test('Extension should exist', () => {
        console.log('Looking for extension...');
        const extensionId = 'groovy-lsp.groovy-language-server';
        const extension = vscode.extensions.getExtension(extensionId);
        
        console.log('Extension found:', extension ? 'Yes' : 'No');
        assert.ok(extension, `Extension ${extensionId} not found`);
        console.log('✓ Extension exists');
    });
});