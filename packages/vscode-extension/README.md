# Groovy Language Server for VS Code

This VS Code extension provides language support for Groovy through the Groovy Language Server.

## Features

- Syntax highlighting
- Code completion
- Go to definition
- Find references
- Hover information
- Diagnostics (errors and warnings)
- Code formatting

## Requirements

- Java 11 or higher must be installed and available on your PATH or specified in settings
- Groovy Language Server JAR file (automatically downloaded on first use)

## Installation

### From GitHub Release

1. Download the `.vsix` file from the [latest release](https://github.com/char5742/groovy-lsp/releases)
2. Install using:
   ```bash
   code --install-extension groovy-language-server-*.vsix
   ```

The Language Server JAR will be automatically downloaded when you first open a Groovy file.

### Manual JAR Installation (Optional)

If you prefer to manage the JAR manually:

1. Download the JAR from the [latest release](https://github.com/char5742/groovy-lsp/releases)
2. Set the path in VS Code settings:
   ```json
   {
     "groovyLanguageServer.serverJar": "/path/to/groovy-language-server.jar"
   }
   ```

## Extension Settings

This extension contributes the following settings:

* `groovyLanguageServer.maxNumberOfProblems`: Controls the maximum number of problems produced by the server (default: 100)
* `groovyLanguageServer.trace.server`: Traces the communication between VS Code and the language server
* `groovyLanguageServer.javaHome`: Specifies the folder path to the JDK used to launch the Groovy Language Server
* `groovyLanguageServer.serverJar`: Path to the Groovy Language Server JAR file

## Building

1. Install dependencies: `npm install`
2. Compile: `npm run compile`
3. Package: `npm run package`

## Development

1. Clone the repository
2. Install dependencies: `npm install`
3. Open in VS Code
4. Press F5 to run the extension in a new Extension Development Host window

## Known Issues

Please report issues at the project's GitHub repository.

## Release Notes

### 0.1.0

Initial release of Groovy Language Server for VS Code
