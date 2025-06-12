const { spawn } = require('child_process');
const path = require('path');

console.log('Testing LSP communication...');

const jarPath = '/home/shuma/workspace/programing/java/groovy-lsp/packages/vscode-extension/server/groovy-language-server.jar';
const workspacePath = '/home/shuma/workspace/programing/java/groovy-lsp/packages/e2e-tests/test/fixtures/cross-file-project';

// Start the language server
const lsp = spawn('/usr/lib/jvm/java-23-amazon-corretto/bin/java', [
    '-jar', jarPath,
    '--workspace', workspacePath
], {
    stdio: ['pipe', 'pipe', 'pipe']
});

// Handle stderr
lsp.stderr.on('data', (data) => {
    console.error('LSP stderr:', data.toString());
});

// Handle stdout
let buffer = '';
lsp.stdout.on('data', (data) => {
    buffer += data.toString();
    console.log('LSP stdout:', data.toString());
});

// Handle process exit
lsp.on('exit', (code, signal) => {
    console.log(`LSP process exited with code ${code} and signal ${signal}`);
});

// Send initialize request
const initRequest = {
    jsonrpc: '2.0',
    id: 1,
    method: 'initialize',
    params: {
        processId: process.pid,
        rootUri: `file://${workspacePath}`,
        capabilities: {
            textDocument: {
                completion: {
                    completionItem: {
                        snippetSupport: true
                    }
                }
            }
        }
    }
};

const content = JSON.stringify(initRequest);
const header = `Content-Length: ${Buffer.byteLength(content)}\r\n\r\n`;
const message = header + content;

console.log('Sending initialize request...');
lsp.stdin.write(message);

// Wait a bit then send initialized notification
setTimeout(() => {
    const initializedNotification = {
        jsonrpc: '2.0',
        method: 'initialized',
        params: {}
    };
    
    const content2 = JSON.stringify(initializedNotification);
    const header2 = `Content-Length: ${Buffer.byteLength(content2)}\r\n\r\n`;
    const message2 = header2 + content2;
    
    console.log('Sending initialized notification...');
    lsp.stdin.write(message2);
}, 1000);

// Keep the process alive for a while
setTimeout(() => {
    console.log('Sending shutdown request...');
    const shutdownRequest = {
        jsonrpc: '2.0',
        id: 2,
        method: 'shutdown',
        params: null
    };
    
    const content3 = JSON.stringify(shutdownRequest);
    const header3 = `Content-Length: ${Buffer.byteLength(content3)}\r\n\r\n`;
    const message3 = header3 + content3;
    
    lsp.stdin.write(message3);
    
    setTimeout(() => {
        lsp.kill();
    }, 1000);
}, 5000);