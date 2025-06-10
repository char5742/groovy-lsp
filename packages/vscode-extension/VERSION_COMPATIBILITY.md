# Version Compatibility Guide

## Overview

The Groovy Language Server VS Code extension manages version compatibility between the extension and the Language Server JAR to ensure proper functionality.

## Version Compatibility Matrix

| Extension Version | Compatible Server Versions |
|------------------|---------------------------|
| 0.1.x            | 0.1.*                     |
| 0.2.x            | 0.2.*                     |

## Compatibility Strategy

1. **Semantic Versioning**: Both the extension and server follow semantic versioning (MAJOR.MINOR.PATCH)
2. **Minor Version Compatibility**: Extensions are compatible with servers of the same MAJOR.MINOR version
3. **Backward Compatibility**: Newer server versions maintain compatibility with older protocol versions

## Security Features

### JAR Download Verification

The extension implements several security measures when downloading the Language Server JAR:

1. **SHA256 Checksum Verification**
   - Each JAR file has an accompanying `.sha256` file
   - The extension verifies the checksum after download
   - Downloads are rejected if checksums don't match

2. **File Size Verification**
   - The extension verifies that the downloaded file size matches the expected size from GitHub API
   - Partial or corrupted downloads are detected and rejected

3. **Temporary File Handling**
   - Downloads are first saved to a temporary file
   - Only moved to the final location after all verifications pass
   - Failed downloads are automatically cleaned up

### Resource Management

- **Automatic Cleanup**: Old JAR versions are automatically removed when a new version is installed
- **Storage Location**: JARs are stored in VS Code's global storage directory
- **Manual Override**: Users can specify a custom JAR location via settings

## Configuration

### Extension Settings

```json
{
  "groovyLanguageServer.serverJar": "/path/to/custom/groovy-language-server.jar",
  "groovyLanguageServer.javaHome": "/path/to/java/home"
}
```

### Environment Variables

The Language Server receives the following environment variables:
- `groovy.lsp.client.version`: The VS Code extension version
- `groovy.lsp.debug`: Debug mode flag

## Troubleshooting

### Version Mismatch

If you encounter version compatibility issues:

1. Check the extension version: `code --list-extensions --show-versions | grep groovy-language-server`
2. Check the server JAR version in the global storage directory
3. Remove old JAR files and restart VS Code to trigger a fresh download

### Download Issues

If JAR download fails:

1. Check your internet connection
2. Verify GitHub API access is not blocked
3. Manually download the JAR from the [releases page](https://github.com/char5742/groovy-lsp/releases)
4. Set the `groovyLanguageServer.serverJar` setting to the downloaded file path

## Development

### Adding New Versions

1. Update the compatibility matrix in `package.json`:
   ```json
   "groovyLanguageServer": {
     "compatibleVersions": {
       "0.1.x": "0.1.*",
       "0.2.x": "0.2.*"
     }
   }
   ```

2. Ensure the server implements version negotiation in the initialization handshake

3. Test compatibility between different version combinations
