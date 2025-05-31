# Groovy LSP Formatting Module

This module provides code formatting capabilities for Groovy source code based on Google Java Format with Groovy-specific extensions.

## Features

- **Google Java Format Integration**: Leverages the robust Google Java Format library as the foundation
- **Groovy-Specific Support**: Handles Groovy syntax including:
  - Closures
  - GStrings with interpolation
  - Triple-quoted strings
  - Method calls without parentheses
  - Dynamic typing with `def`
- **LSP Protocol Support**: Implements LSP formatting services for document and range formatting
- **Configurable Options**: Customizable formatting preferences including:
  - Indentation size and style (spaces/tabs)
  - Maximum line length
  - Compact closure formatting
  - Line break preservation

## Usage

### Basic Formatting

```java
GroovyFormatter formatter = new GroovyFormatter();
String formatted = formatter.format(groovySourceCode);
```

### Custom Options

```java
FormatOptions options = FormatOptions.builder()
    .indentSize(2)
    .maxLineLength(120)
    .compactClosures(true)
    .build();

GroovyFormatter formatter = new GroovyFormatter(options);
String formatted = formatter.format(groovySourceCode);
```

### LSP Integration

```java
FormattingService service = new FormattingService();

// Format entire document
CompletableFuture<List<TextEdit>> edits = service.formatDocument(params, documentContent);

// Format specific range
CompletableFuture<List<TextEdit>> rangeEdits = service.formatRange(rangeParams, documentContent);
```

## Dependencies

- Google Java Format 1.19.1
- Groovy 4.0.18
- SLF4J for logging
- Internal modules: groovy-core, lsp-protocol

## Module Structure

- `com.groovy.lsp.formatting` - Main formatter implementation
- `com.groovy.lsp.formatting.options` - Configuration options
- `com.groovy.lsp.formatting.service` - LSP service implementation

## Future Enhancements

- Complete implementation of Groovy-specific formatting rules
- Range formatting optimization
- Additional formatting styles (AOSP, Groovy-specific)
- Integration with Groovy AST for more accurate formatting
- Support for Groovy DSLs and builders