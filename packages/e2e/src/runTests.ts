import { runTests } from '@vscode/test-electron';
import { resolve } from 'path';
import { fileURLToPath } from 'url';
import { dirname } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

async function main() {
  try {
    // The folder containing the Extension Manifest package.json
    const extensionDevelopmentPath = resolve(__dirname, '../../../');

    // The path to the extension test runner script
    const extensionTestsPath = resolve(__dirname, './suite/index');

    // Download VS Code, unzip it and run the integration test
    await runTests({
      version: 'stable',
      extensionDevelopmentPath,
      extensionTestsPath,
      launchArgs: ['--disable-workspace-trust']
    });
  } catch (err) {
    console.error('Failed to run tests:', err);
    process.exit(1);
  }
}

main();