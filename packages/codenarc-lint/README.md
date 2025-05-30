# CodeNarc Lint Module

This module provides static analysis and quick fixes for Groovy code using CodeNarc.

## Features

- **Static Analysis**: Analyzes Groovy files using CodeNarc rules
- **Quick Fixes**: Provides automatic fixes for common violations
- **Customizable Rules**: Support for custom rule sets and configurations
- **JPMS Support**: Full Java Platform Module System compatibility

## Architecture

### Core Components

1. **LintEngine**: Main entry point for linting operations
   - Analyzes single files or directories
   - Converts CodeNarc violations to LSP diagnostics
   - Manages asynchronous analysis operations

2. **RuleSetProvider**: Manages CodeNarc rule sets
   - Loads default and custom rule sets
   - Supports XML and properties-based configurations
   - Caches rule sets for performance

3. **QuickFixMapper**: Maps violations to LSP code actions
   - Provides automatic fixes for common issues
   - Extensible for custom quick fix providers
   - Generates LSP-compatible workspace edits

## Configuration

### Default Rules

The module includes a default rule set that covers:
- Basic code quality rules
- Import optimization
- Groovy idioms and conventions
- Design patterns
- Exception handling
- Code formatting

### Custom Rules

Users can customize rules by placing files in their project root:

1. **codenarc-ruleset.xml**: Custom XML rule set
2. **codenarc.properties**: Rule-specific property overrides

Example `codenarc.properties`:
```properties
# Customize line length
LineLength.maximumLineLength=100

# Customize method naming
MethodName.regex=^[a-z][a-zA-Z0-9_]*$
```

## Usage

```java
// Create components
RuleSetProvider ruleSetProvider = new RuleSetProvider();
QuickFixMapper quickFixMapper = new QuickFixMapper();
LintEngine lintEngine = new LintEngine(ruleSetProvider, quickFixMapper);

// Analyze a single file
CompletableFuture<List<Diagnostic>> diagnostics = 
    lintEngine.analyzeFile("/path/to/file.groovy");

// Analyze a directory
CompletableFuture<List<FileAnalysisResult>> results = 
    lintEngine.analyzeDirectory("/project", "**/*.groovy", "**/test/**");
```

## Quick Fixes

The module provides automatic fixes for:

### Import Issues
- Remove unused imports
- Remove duplicate imports
- Remove imports from same package
- Remove unnecessary Groovy imports

### Empty Blocks
- Add logging to empty catch blocks
- Remove empty else/finally blocks
- Remove empty if/while statements

### Naming Conventions
- Fix field/method/class/variable names

### Groovy Idioms
- Replace explicit ArrayList/HashMap with literals
- Convert GString keys to String
- Add missing Locale to SimpleDateFormat

## Dependencies

- CodeNarc 3.3.0
- Groovy 4.0.15
- SLF4J for logging
- Internal modules: groovy-core, lsp-protocol

## Testing

The module includes comprehensive unit tests using JUnit 5 and Mockito.