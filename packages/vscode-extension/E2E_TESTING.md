# VSCode Extension E2E Testing Guide

This document describes the End-to-End (E2E) testing framework for the Groovy Language Server VSCode extension.

## Overview

The E2E testing framework validates the integration between the VSCode extension and the Groovy Language Server, ensuring all features work correctly in real-world scenarios.

## Test Structure

```
packages/vscode-extension/
├── src/test/
│   ├── e2e/                    # E2E test suites
│   │   ├── CompletionE2ETest.ts
│   │   ├── DiagnosticsE2ETest.ts
│   │   ├── HoverE2ETest.ts
│   │   ├── FormattingE2ETest.ts
│   │   ├── helper.ts           # Shared test utilities
│   │   └── index.ts            # Test runner configuration
│   ├── performance/            # Performance test suites
│   │   ├── IndexingPerformanceTest.ts
│   │   └── index.ts
│   ├── fixtures/               # Sample projects for testing
│   │   ├── sample-java-groovy-project/
│   │   └── sample-groovy-dsl-project/
│   ├── runE2ETest.ts          # E2E test entry point
│   └── runPerformanceTest.ts  # Performance test entry point
```

## Running Tests

### Prerequisites

1. Build the language server:
   ```bash
   ./gradlew :server-launcher:shadowJar
   ```

2. Copy the JAR to the extension directory:
   ```bash
   mkdir -p packages/vscode-extension/server
   cp packages/server-launcher/build/libs/*-all.jar packages/vscode-extension/server/groovy-language-server.jar
   ```

3. Install dependencies:
   ```bash
   cd packages/vscode-extension
   npm ci
   ```

### Running Tests Locally

```bash
# Compile TypeScript
npm run compile

# Run unit tests
npm run test:unit

# Run E2E tests
npm run test:e2e

# Run performance tests
npm run test:performance

# Run all tests
npm run test:all
```

### Test Environment Variables

- `VSCODE_VERSION`: Specify VSCode version ('stable' or 'insiders')
- `DISPLAY`: Required for Linux environments (set automatically in CI)

## Test Coverage

### Completion Tests
- Java class completion in Groovy files
- Method completion for Java objects
- Spock framework keyword completion

### Diagnostics Tests
- Syntax error detection
- Undefined reference detection
- Error clearing when fixed
- Quick fix suggestions

### Hover Tests
- Java class information
- Method signatures
- Groovy keywords
- Spock framework elements

### Formatting Tests
- Code indentation
- Spock test formatting
- DSL structure preservation
- Error handling during formatting

### Performance Tests
- Project indexing time (< 30s)
- Completion response time (< 1s)
- Hover response time (< 500ms)
- Document formatting time (< 2s)

## CI/CD Integration

The E2E tests run automatically on GitHub Actions with the following matrix:

- **Operating Systems**: Ubuntu, Windows, macOS
- **VSCode Versions**: Stable, Insiders

See `.github/workflows/vscode-e2e.yml` for the complete CI configuration.

## Writing New Tests

### E2E Test Template

```typescript
import * as assert from 'assert';
import * as vscode from 'vscode';
import { activateExtension, openDocument } from './helper';

suite('Feature E2E Test Suite', () => {
    suiteSetup(async () => {
        await activateExtension();
    });

    test('should test specific feature', async () => {
        // Open test document
        const document = await openDocument('path/to/test/file.groovy');

        // Perform test actions
        // ...

        // Assert results
        assert.ok(result, 'Expected result');
    });

    teardown(async () => {
        await vscode.commands.executeCommand('workbench.action.closeActiveEditor');
    });
});
```

### Helper Functions

The `helper.ts` module provides common utilities:

- `activateExtension()`: Activates the Groovy extension
- `openDocument(path)`: Opens a document and waits for it to load
- `waitForDiagnostics(uri, timeout)`: Waits for diagnostics to appear
- `typeText(editor, text)`: Types text in the editor
- `formatDocument(document)`: Formats a document
- `sleep(ms)`: Pauses execution

## Troubleshooting

### Tests Fail with "Extension not found"
- Ensure the extension is properly compiled (`npm run compile`)
- Check that package.json contains the correct extension ID

### Tests Timeout
- Increase timeout in test configuration
- Check that the language server JAR is in the correct location
- Verify Java is installed and accessible

### No Diagnostics Appear
- Wait longer for language server initialization
- Check language server logs for errors
- Ensure test files have correct syntax

## Best Practices

1. **Isolation**: Each test should be independent and not rely on state from other tests
2. **Cleanup**: Always close editors and clean up resources in teardown
3. **Timeouts**: Use appropriate timeouts for async operations
4. **Assertions**: Make assertions specific and include helpful error messages
5. **Performance**: Keep performance benchmarks realistic and platform-aware

## Future Improvements

- [ ] Add tests for refactoring features
- [ ] Test debugging integration
- [ ] Add tests for multi-root workspace support
- [ ] Implement stress tests for large projects
- [ ] Add visual regression tests for syntax highlighting
