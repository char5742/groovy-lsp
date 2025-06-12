import * as path from 'path';
import { runTests } from '@vscode/test-electron';

async function main() {
    try {
        const extensionDevelopmentPath = path.resolve(__dirname, '../../');
        const extensionTestsPath = path.resolve(__dirname, './e2e/index');
        
        // The folder containing the Extension Manifest package.json
        // Passed to `--extensionDevelopmentPath`
        const workspaceFolder = path.resolve(__dirname, './fixtures/sample-java-groovy-project');

        // Download VS Code, unzip it and run the integration test
        await runTests({
            extensionDevelopmentPath,
            extensionTestsPath,
            launchArgs: [
                workspaceFolder,
                '--disable-extensions', // Disable other extensions
                '--skip-welcome',
                '--skip-release-notes'
            ],
            version: process.env.VSCODE_VERSION || 'stable'
        });
    } catch (err) {
        console.error('Failed to run E2E tests');
        process.exit(1);
    }
}

main();