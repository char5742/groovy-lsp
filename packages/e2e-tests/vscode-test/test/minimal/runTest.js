// 最小限のテストランナー
import path from 'path';
import { runTests } from '@vscode/test-electron';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// 未処理のPromise拒否をキャッチして警告を抑制
process.on('unhandledRejection', (reason, promise) => {
    // Language Server関連の警告は無視
    if (reason && reason.message) {
        if (reason.message.includes('Pending response rejected') ||
            reason.message.includes('Client is not running') ||
            reason.message.includes('connection got disposed')) {
            // これらのエラーは想定内なのでログに記録しない
            return;
        }
    }
    // その他のエラーはログに記録
    console.error('Unhandled Rejection:', reason);
});

async function main() {
    try {
        console.log('=== Minimal Test Runner ===');
        console.log('Starting minimal test...');
        
        // 拡張機能のパス（3階層上）
        const extensionDevelopmentPath = path.resolve(__dirname, '../../../../vscode-extension');
        
        // テストスイートのパス
        const extensionTestsPath = path.resolve(__dirname, './suite');
        
        console.log('Extension path:', extensionDevelopmentPath);
        console.log('Test suite path:', extensionTestsPath);
        
        // JAVA_HOMEを設定
        const javaHome = '/usr/lib/jvm/java-23-amazon-corretto';
        console.log('Setting JAVA_HOME:', javaHome);
        
        // 最小限の設定でテストを実行
        await runTests({
            extensionDevelopmentPath,
            extensionTestsPath,
            extensionTestsEnv: {
                JAVA_HOME: javaHome,
                ELECTRON_NO_ATTACH_CONSOLE: '1',
                VSCODE_SKIP_PRELAUNCH: '1',
                NODE_NO_WARNINGS: '1'
            },
            launchArgs: [
                '--disable-gpu',
                '--disable-gpu-sandbox',
                '--disable-dev-shm-usage',
                '--no-sandbox'
            ]
        });
        
        console.log('Test completed successfully!');
    } catch (err) {
        console.error('Failed to run tests:', err);
        process.exit(1);
    }
}

main();