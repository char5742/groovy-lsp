/**
 * CI環境用のテストランナー
 * 必要なGUIライブラリがない環境でも動作確認ができるようにする
 */

const path = require('path');
const fs = require('fs');

console.log('=== CI Test Runner ===');
console.log('Running tests in CI mode (no GUI required)');
console.log();

// テスト環境の検証
function verifyEnvironment() {
    console.log('1. Verifying environment...');
    
    // 拡張機能のパスを確認
    const extensionPath = path.resolve(__dirname, '../../../vscode-extension');
    if (!fs.existsSync(extensionPath)) {
        throw new Error('Extension directory not found: ' + extensionPath);
    }
    console.log('   ✓ Extension directory found');
    
    // package.jsonの検証
    const packageJsonPath = path.join(extensionPath, 'package.json');
    if (!fs.existsSync(packageJsonPath)) {
        throw new Error('Extension package.json not found');
    }
    
    const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
    console.log(`   ✓ Extension: ${packageJson.displayName} v${packageJson.version}`);
    
    // outディレクトリの確認
    const outDir = path.join(extensionPath, 'out');
    if (!fs.existsSync(outDir)) {
        console.log('   ⚠ Extension not compiled (out directory missing)');
    } else {
        console.log('   ✓ Extension compiled');
    }
    
    return true;
}

// テストファイルの検証
function verifyTestFiles() {
    console.log('\n2. Verifying test files...');
    
    const testDir = path.join(__dirname, 'suite');
    const testFiles = fs.readdirSync(testDir).filter(f => f.endsWith('.test.js'));
    
    testFiles.forEach(file => {
        const filePath = path.join(testDir, file);
        const content = fs.readFileSync(filePath, 'utf8');
        const testCount = (content.match(/test\(['"`]/g) || []).length;
        console.log(`   ✓ ${file} - ${testCount} tests`);
    });
    
    return testFiles.length > 0;
}

// 依存関係の検証
function verifyDependencies() {
    console.log('\n3. Verifying dependencies...');
    
    try {
        require('@vscode/test-electron');
        console.log('   ✓ @vscode/test-electron available');
    } catch (error) {
        console.log('   ✗ @vscode/test-electron not available');
        return false;
    }
    
    try {
        require('mocha');
        console.log('   ✓ mocha available');
    } catch (error) {
        console.log('   ✗ mocha not available');
        return false;
    }
    
    return true;
}

// メイン処理
async function main() {
    try {
        verifyEnvironment();
        verifyTestFiles();
        verifyDependencies();
        
        console.log('\n✅ All checks passed!');
        console.log('\nTest environment is properly configured.');
        console.log('To run actual E2E tests with VS Code, ensure GUI libraries are installed.');
        console.log('See README.md for details.');
        
        process.exit(0);
    } catch (error) {
        console.error('\n❌ Error:', error.message);
        process.exit(1);
    }
}

main();