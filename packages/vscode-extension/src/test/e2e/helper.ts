import * as vscode from 'vscode';
import * as path from 'path';

export async function activateExtension(): Promise<void> {
    const ext = vscode.extensions.getExtension('groovy-lsp.groovy-language-server');
    if (!ext) {
        throw new Error('Extension not found');
    }
    
    if (!ext.isActive) {
        await ext.activate();
    }
    
    // Wait for language server to start and index workspace
    console.log('Waiting for language server to initialize and index workspace...');
    await sleep(10000); // Increase wait time to 10 seconds for indexing
}

export async function openDocument(filePath: string): Promise<vscode.TextDocument> {
    const document = await vscode.workspace.openTextDocument(filePath);
    await vscode.window.showTextDocument(document);
    // Wait for document to be fully loaded and indexed by language server
    await sleep(2000); // Increase wait time
    return document;
}

export function sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
}

export async function waitForDiagnostics(uri: vscode.Uri, timeout: number = 5000): Promise<vscode.Diagnostic[]> {
    const startTime = Date.now();
    
    while (Date.now() - startTime < timeout) {
        const diagnostics = vscode.languages.getDiagnostics(uri);
        if (diagnostics.length > 0) {
            return diagnostics;
        }
        await sleep(100);
    }
    
    return [];
}

export async function typeText(editor: vscode.TextEditor, text: string): Promise<void> {
    await editor.edit(editBuilder => {
        editBuilder.insert(editor.selection.active, text);
    });
    await sleep(500); // Wait for text to be processed
}

export function getLineText(document: vscode.TextDocument, lineNumber: number): string {
    return document.lineAt(lineNumber).text;
}

export async function formatDocument(document: vscode.TextDocument): Promise<vscode.TextEdit[]> {
    const edits = await vscode.commands.executeCommand<vscode.TextEdit[]>(
        'vscode.executeFormatDocumentProvider',
        document.uri
    );
    return edits || [];
}