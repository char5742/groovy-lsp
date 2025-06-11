const { downloadAndUnzipVSCode } = require('@vscode/test-electron');
const path = require('path');

async function downloadVSCode() {
    try {
        console.log('Downloading VS Code...');
        const vscodeExecutablePath = await downloadAndUnzipVSCode('stable');
        console.log('VS Code downloaded to:', vscodeExecutablePath);
        
        // ダウンロードしたVS Codeの情報を表示
        const fs = require('fs');
        const vscodeDir = path.dirname(vscodeExecutablePath);
        console.log('\nVS Code directory contents:');
        const files = fs.readdirSync(vscodeDir).slice(0, 10);
        files.forEach(file => console.log(' -', file));
        
        // VS Codeバイナリの情報
        const stats = fs.statSync(vscodeExecutablePath);
        console.log('\nVS Code executable:');
        console.log(' - Size:', stats.size, 'bytes');
        console.log(' - Permissions:', stats.mode.toString(8));
        
    } catch (error) {
        console.error('Failed to download VS Code:', error);
    }
}

downloadVSCode();