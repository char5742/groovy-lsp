const { spawn } = require('child_process');
const path = require('path');

// Language Serverを起動してログを確認
const jarPath = path.join(__dirname, '../../server/groovy-language-server.jar');
const workspacePath = path.join(__dirname, 'fixtures/sample-java-groovy-project');
const javaHome = process.env.JAVA_HOME || '/usr/lib/jvm/java-23-amazon-corretto';
const javaExecutable = path.join(javaHome, 'bin', 'java');

console.log('Starting Language Server with:');
console.log(`  JAR: ${jarPath}`);
console.log(`  Workspace: ${workspacePath}`);
console.log(`  Java: ${javaExecutable}`);

const ls = spawn(javaExecutable, [
    '--add-opens', 'java.base/java.nio=ALL-UNNAMED',
    '-jar', jarPath,
    '--workspace', workspacePath
], {
    env: {
        ...process.env,
        'groovy.lsp.debug': 'true',
        'groovy.lsp.log.level': 'DEBUG'
    }
});

ls.stdout.on('data', (data) => {
    console.log(`[STDOUT] ${data}`);
});

ls.stderr.on('data', (data) => {
    console.error(`[STDERR] ${data}`);
});

ls.on('close', (code) => {
    console.log(`Language Server exited with code ${code}`);
});

// 初期化リクエストを送信
setTimeout(() => {
    const initRequest = {
        jsonrpc: '2.0',
        id: 1,
        method: 'initialize',
        params: {
            processId: process.pid,
            rootUri: `file://${workspacePath}`,
            capabilities: {},
            workspaceFolders: [{
                uri: `file://${workspacePath}`,
                name: 'sample-java-groovy-project'
            }]
        }
    };
    
    console.log('\nSending initialize request...');
    ls.stdin.write(`Content-Length: ${JSON.stringify(initRequest).length}\r\n\r\n`);
    ls.stdin.write(JSON.stringify(initRequest));
}, 1000);

// 10秒後に終了
setTimeout(() => {
    console.log('\nStopping Language Server...');
    ls.kill();
}, 10000);