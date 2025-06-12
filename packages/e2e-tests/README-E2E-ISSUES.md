# E2E Test Issues

## Current Status
E2E tests have been fixed and are now working with Java 23 after migrating from Guice to Dagger dependency injection framework.

## Previous Problem (Resolved)
- The server JAR is compiled with Java 23 (class file major version 67)
- Guice 7.0.0's internal ASM could not read Java 23 class files
- This caused the language server to crash on startup with:
  ```
  java.lang.IllegalArgumentException: Unsupported class file major version 67
  ```

## Solution Implemented
The project has been migrated from Guice to Dagger dependency injection framework, which fully supports Java 23.

## Related
- PR #73: Implements cross-file features (unit tests pass)
- The functionality is working correctly as confirmed by both unit tests and E2E tests
