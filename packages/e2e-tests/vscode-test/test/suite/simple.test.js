const assert = require('assert');
const vscode = require('vscode');

suite('Simple Extension Test', function() {
    this.timeout(10000); // 10秒のタイムアウト
    
    test('Extension should be installed', () => {
        const extension = vscode.extensions.getExtension('groovy-lsp.groovy-language-server');
        assert.ok(extension, 'Extension not found');
    });
    
    test('Extension should be activated', async () => {
        const extension = vscode.extensions.getExtension('groovy-lsp.groovy-language-server');
        if (extension && !extension.isActive) {
            await extension.activate();
        }
        assert.ok(extension.isActive, 'Extension is not active');
    });
});