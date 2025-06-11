// 簡単な動作確認用のテストスクリプト

console.log('Starting simple test runner...');

const path = require('path');
const fs = require('fs');

// パスの確認
const extensionPath = path.resolve(__dirname, '../../../vscode-extension');
console.log('Extension path:', extensionPath);
console.log('Extension path exists:', fs.existsSync(extensionPath));

const packageJsonPath = path.join(extensionPath, 'package.json');
console.log('package.json exists:', fs.existsSync(packageJsonPath));

if (fs.existsSync(packageJsonPath)) {
    const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
    console.log('Extension name:', packageJson.name);
    console.log('Extension display name:', packageJson.displayName);
    console.log('Extension publisher:', packageJson.publisher);
}

// VS Code test-electronのインポートテスト
try {
    const { runTests } = require('@vscode/test-electron');
    console.log('Successfully imported @vscode/test-electron');
    console.log('runTests function available:', typeof runTests === 'function');
} catch (error) {
    console.error('Failed to import @vscode/test-electron:', error.message);
}

console.log('\nEnvironment check complete.');
console.log('Node version:', process.version);
console.log('Platform:', process.platform);
console.log('Architecture:', process.arch);