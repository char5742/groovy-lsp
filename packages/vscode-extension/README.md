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
- Groovy Language Server JAR file (download from [releases](https://github.com/groovy-lsp/groovy-lsp/releases))

## Installation

1. Install this extension from the VS Code Marketplace
2. Download the `groovy-language-server.jar` from the [releases page](https://github.com/groovy-lsp/groovy-lsp/releases)
3. Either:
   - Place the JAR in your VS Code global storage directory (the extension will guide you)
   - Or set the `groovyLanguageServer.serverJar` setting to the path of the JAR file

## Extension Settings

This extension contributes the following settings:

* `groovyLanguageServer.maxNumberOfProblems`: Controls the maximum number of problems produced by the server (default: 100)
* `groovyLanguageServer.trace.server`: Traces the communication between VS Code and the language server
* `groovyLanguageServer.javaHome`: Specifies the folder path to the JDK used to launch the Groovy Language Server
* `groovyLanguageServer.serverJar`: Path to the Groovy Language Server JAR file

## Building

1. Install dependencies: `npm install`
2. Compile: `npm run compile`
3. Package: `vsce package`

## Known Issues

Please report issues at the project's GitHub repository.

## Release Notes

### 0.1.0

Initial release of Groovy Language Server for VS Code
