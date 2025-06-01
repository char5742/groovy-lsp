import { startServer, client } from 'ts-lsp-client';
import assert from 'node:assert/strict';
import { test, describe, after } from 'node:test';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

describe('Groovy LSP Hover Tests', () => {
  let srv: any;
  
  test('should provide hover information for Groovy types', async () => {
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
          hover: {
            contentFormat: ['markdown', 'plaintext']
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
  
  def greet() {
    println "Hello, ${name}"
  }
}`
      }
    });
    
    // Request hover information
    const hover = await client.sendRequest('textDocument/hover', {
      textDocument: { uri: 'file:///tmp/test-project/Test.groovy' },
      position: { line: 1, character: 2 } // Position on "String"
    });
    
    // Verify hover content
    assert.ok(hover);
    assert.ok(hover.contents);
    assert.ok(hover.contents.value?.includes('String') || hover.contents.value?.includes('java.lang.String'));
  });
  
  after(async () => {
    if (srv) {
      await srv.shutdown();
    }
  });
});