import { startServer, client } from 'ts-lsp-client';
import assert from 'node:assert/strict';
import { test, describe, after } from 'node:test';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

describe('Groovy LSP Completion Tests', () => {
  let srv: any;
  
  test('should provide code completion for Groovy', async () => {
    // Start the Groovy LSP server
    srv = await startServer({
      command: 'java',
      args: [
        '-cp', join(__dirname, '../../../../packages/server-launcher/build/libs/server-launcher-all.jar'),
        'com.groovy.lsp.server.launcher.Main'
      ]
    });
    
    // Initialize the LSP connection
    await client.initialize({
      rootUri: 'file:///tmp/test-project',
      capabilities: {
        textDocument: {
          completion: {
            completionItem: {
              snippetSupport: true,
              commitCharactersSupport: true,
              documentationFormat: ['markdown', 'plaintext']
            }
          }
        }
      }
    });
    
    // Open a test document
    await client.sendNotification('textDocument/didOpen', {
      textDocument: {
        uri: 'file:///tmp/test-project/Test.groovy',
        languageId: 'groovy',
        version: 1,
        text: `class Test {
  String name = "test"
  
  def example() {
    name.
  }
}`
      }
    });
    
    // Request completion at the position after "name."
    const completion = await client.sendRequest('textDocument/completion', {
      textDocument: { uri: 'file:///tmp/test-project/Test.groovy' },
      position: { line: 4, character: 9 } // Position after "name."
    });
    
    // Verify completion results
    assert.ok(completion);
    assert.ok(Array.isArray(completion.items) || Array.isArray(completion));
    
    const items = Array.isArray(completion) ? completion : completion.items;
    assert.ok(items.length > 0);
    
    // Check for String methods like length(), toLowerCase(), etc.
    const methodNames = items.map((item: any) => item.label);
    assert.ok(methodNames.some((name: string) => name.includes('length')));
  });
  
  after(async () => {
    if (srv) {
      await srv.shutdown();
    }
  });
});