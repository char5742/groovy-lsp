import path from 'path';
import Mocha from 'mocha';
import { glob } from 'glob';
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

function run() {
    // Create the mocha test
    const mocha = new Mocha({
        ui: 'tdd',
        color: true
    });

    const testsRoot = path.resolve(__dirname, '..');

    return new Promise(async (c, e) => {
        try {
            const files = await glob('**/**.test.js', { cwd: testsRoot });
            
            // Add files to the test suite
            files.forEach(f => mocha.addFile(path.resolve(testsRoot, f)));

            // Run the mocha test
            mocha.run(failures => {
                if (failures > 0) {
                    e(new Error(`${failures} tests failed.`));
                } else {
                    c();
                }
            });
        } catch (err) {
            console.error(err);
            e(err);
        }
    });
}

export { run };