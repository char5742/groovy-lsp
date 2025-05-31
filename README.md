# Groovy LSP

Groovy Tier-3 Language Server implementation based on LSP4J.

## JPMS (Java Platform Module System) Support

This project has partial JPMS support for `shared` and `groovy-core` modules. Other modules use classpath due to automatic module dependencies. See [JPMS-STRATEGY.md](JPMS-STRATEGY.md) for details.

## Development Setup

### Using Dev Container (Recommended)

1. Open this project in VS Code
2. Install the "Dev Containers" extension
3. Run "Reopen in Container" command

The Dev Container includes:
- Eclipse Temurin JDK 21
- Gradle 8.11.1
- Groovy 4.0.27

### Manual Setup

Requirements:
- Java 21
- Gradle 8.x
- Groovy 4.0.27

### Hot Reload Execution

Run the language server with hot reload enabled:

```bash
./gradlew :server-launcher:run --stdin
```

### Debugging

#### VS Code
1. Start the server with debug options (already configured in run task)
2. Use the "Debug LSP Server" or "debug:lsp" configuration in VS Code
3. The server will listen on port 5005 for debugger connections

#### IntelliJ IDEA
1. Run the server with the gradle command above
2. Create a Remote JVM Debug configuration with port 5005
3. Start debugging

## Project Structure

- `server-launcher` - Main entry point and LSP server implementation
- `lsp-protocol` - LSP protocol DTOs and adapters
- `groovy-core` - Groovy compiler API wrapper
- `jdt-adapter` - Groovy ↔ JDT conversion utilities
- `codenarc-lint` - CodeNarc integration for linting
- `workspace-index` - Workspace indexing and symbol management
- `formatting` - Code formatting based on google-java-format