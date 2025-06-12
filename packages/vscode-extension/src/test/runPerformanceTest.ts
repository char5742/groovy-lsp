import * as path from 'path';
import { runTests } from '@vscode/test-electron';

async function main() {
    try {
        const extensionDevelopmentPath = path.resolve(__dirname, '../../');
        const extensionTestsPath = path.resolve(__dirname, './performance/index');
        
        // Use a larger workspace for performance testing
        const workspaceFolder = path.resolve(__dirname, './fixtures/sample-java-groovy-project');

        await runTests({
            extensionDevelopmentPath,
            extensionTestsPath,
            launchArgs: [
                workspaceFolder,
                '--disable-extensions',
                '--skip-welcome',
                '--skip-release-notes'
            ],
            version: process.env.VSCODE_VERSION || 'stable'
        });
    } catch (err) {
        console.error('Failed to run performance tests');
        process.exit(1);
    }
}

main();