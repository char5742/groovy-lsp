import * as assert from 'assert';
import * as vscode from 'vscode';
import * as path from 'path';

suite('Groovy LSP Extension Test Suite', () => {
  vscode.window.showInformationMessage('Start all tests.');

  test('Extension should be present', () => {
    assert.ok(vscode.extensions.getExtension('groovy-lang.groovy-lsp'));
  });

  test('Should activate extension', async () => {
    const ext = vscode.extensions.getExtension('groovy-lang.groovy-lsp');
    assert.ok(ext);
    await ext.activate();
    assert.ok(ext.isActive);
  });

  test('Should provide hover for Groovy files', async () => {
    // Create a test Groovy file
    const document = await vscode.workspace.openTextDocument({
      language: 'groovy',
      content: `class HelloWorld {
  String message = "Hello"
  
  def greet() {
    println message
  }
}`
    });

    const editor = await vscode.window.showTextDocument(document);
    
    // Position cursor on "String"
    const position = new vscode.Position(1, 2);
    
    // Get hover information
    const hovers = await vscode.commands.executeCommand<vscode.Hover[]>(
      'vscode.executeHoverProvider',
      document.uri,
      position
    );

    assert.ok(hovers);
    assert.ok(hovers.length > 0);
    assert.ok(hovers[0].contents.length > 0);
  });

  test('Should provide code completion', async () => {
    const document = await vscode.workspace.openTextDocument({
      language: 'groovy',
      content: `class Test {
  String name = "test"
  
  def example() {
    name.
  }
}`
    });

    const editor = await vscode.window.showTextDocument(document);
    
    // Position cursor after "name."
    const position = new vscode.Position(4, 9);
    
    // Get completion items
    const completions = await vscode.commands.executeCommand<vscode.CompletionList>(
      'vscode.executeCompletionItemProvider',
      document.uri,
      position
    );

    assert.ok(completions);
    assert.ok(completions.items.length > 0);
    
    // Check for String methods
    const labels = completions.items.map(item => item.label);
    assert.ok(labels.some(label => 
      typeof label === 'string' ? label.includes('length') : label.label.includes('length')
    ));
  });
});