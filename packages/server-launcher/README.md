# Server Launcher Module

This module provides the main entry point for the Groovy Language Server. It handles:

- LSP4J launcher setup
- JSON-RPC communication over stdio or socket
- Server initialization with Guice dependency injection
- Command-line argument parsing

## Usage

### Stdio Mode (Default)
```bash
java -jar groovy-language-server.jar
```

### Socket Mode
```bash
java -jar groovy-language-server.jar --socket --host localhost --port 5007
```

## Build

To build the fat JAR:
```bash
./gradlew :packages:server-launcher:shadowJar
```

To run the server directly:
```bash
./gradlew :packages:server-launcher:runServer
```

## Architecture

The module uses:
- **LSP4J** for Language Server Protocol implementation
- **Guice** for dependency injection
- **Logback** for logging
- **Shadow plugin** to create an executable fat JAR

The main class (`Main.java`) sets up the LSP launcher and connects to the language server implementation provided by the `language-server` module.