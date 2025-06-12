import { defineConfig } from '@vscode/test-cli';

export default defineConfig({
    files: 'out/test/**/*.test.js',
    version: 'stable',
    workspaceFolder: './src/test/fixtures/sample-java-groovy-project',
    mocha: {
        timeout: 60000,
        retries: 2
    },
    env: {
        // Set environment variables for testing
        NODE_ENV: 'test'
    }
});