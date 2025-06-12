// 最小限のテストスイートのエントリーポイント
import path from 'path';
import Mocha from 'mocha';
import { glob } from 'glob';
import { fileURLToPath } from 'url';
import { cleanupTestEnvironment } from './test-helper.js';

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

function run() {
    console.log('Test suite index.js loaded');
    
    // Mochaインスタンスを作成
    const mocha = new Mocha({
        ui: 'tdd',
        color: true,
        timeout: 5000  // 5秒のタイムアウト
    });

    const testsRoot = path.resolve(__dirname, '.');

    return new Promise(async (resolve, reject) => {
        try {
            // テストファイルを検索
            const files = await glob('**/**.test.js', { cwd: testsRoot });
            
            console.log('Found test files:', files);

            // テストファイルをMochaに追加
            files.forEach(f => mocha.addFile(path.resolve(testsRoot, f)));

            // テストを実行
            mocha.run(async failures => {
                // テスト完了後のクリーンアップ
                console.log('Cleaning up test environment...');
                try {
                    await cleanupTestEnvironment();
                    console.log('Cleanup completed');
                } catch (cleanupError) {
                    console.error('Cleanup error:', cleanupError);
                }
                
                // 少し待ってから終了
                await new Promise(resolve => setTimeout(resolve, 500));
                
                if (failures > 0) {
                    reject(new Error(`${failures} tests failed.`));
                } else {
                    resolve();
                }
            });
        } catch (err) {
            console.error('Error in test runner:', err);
            reject(err);
        }
    });
}

export { run };