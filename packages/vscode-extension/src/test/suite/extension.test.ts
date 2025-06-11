import * as assert from 'assert';
import * as vscode from 'vscode';

suite('Extension Test Suite', () => {
    vscode.window.showInformationMessage('Start all tests.');

    test('Extension should be present', () => {
        assert.ok(vscode.extensions.getExtension('groovy-lsp.groovy-language-server'));
    });

    test('Should activate', async () => {
        const ext = vscode.extensions.getExtension('groovy-lsp.groovy-language-server');
        assert.ok(ext);
        await ext!.activate();
        assert.ok(ext!.isActive);
    });
});