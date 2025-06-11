# Test Pyramid Strategy

This document describes the test pyramid strategy implemented in the Groovy Language Server project, based on Google's testing best practices.

## Overview

The test pyramid is a concept that guides the distribution of tests across different levels:

```
       /\
      /E2E\      <- Fewest, slowest
     /------\
    /Integra-\   <- Medium count, medium speed
   / tion     \
  /------------\
 /   Unit Tests \ <- Most numerous, fastest
/________________\
```

## Test Categories

### 1. Unit Tests (`@UnitTest`)
- **Tag**: `unit`
- **Purpose**: Test individual components in isolation
- **Characteristics**:
  - Fast execution (< 100ms per test)
  - No external dependencies (mocked)
  - Form the foundation of the test pyramid
  - Should comprise ~70% of all tests
- **Command**: `./gradlew test`

### 2. Integration Tests (`@IntegrationTest`)
- **Tag**: `integration`
- **Purpose**: Test interaction between multiple components
- **Characteristics**:
  - Medium execution time (100ms - 2s per test)
  - May use real dependencies
  - Test component interactions
  - Should comprise ~20% of all tests
- **Command**: `./gradlew integrationTest`

### 3. End-to-End Tests (`@E2ETest`)
- **Tag**: `e2e`
- **Purpose**: Test complete system behavior
- **Characteristics**:
  - Slow execution (> 2s per test)
  - Test from user perspective
  - Use real system components
  - Should comprise ~10% of all tests
- **Command**: `./gradlew e2eTest`

## Special Test Categories

### Performance Tests (`@PerformanceTest`)
- **Tag**: `performance`
- **Purpose**: Measure and verify performance characteristics
- **Command**: `./gradlew performanceTest`

### Slow Tests (`@SlowTest`)
- **Tag**: `slow`
- **Purpose**: Tests that take more than 2 seconds
- **Command**: `./gradlew slowTest`

## Running Tests

### Running specific test categories:
```bash
# Unit tests only (default)
./gradlew test

# Integration tests only
./gradlew integrationTest

# E2E tests only
./gradlew e2eTest

# Performance tests
./gradlew performanceTest

# All tests
./gradlew allTests
```

### Running tests with specific tags:
```bash
# Run only tests with specific tag
./gradlew test -Dgroups="unit"

# Exclude tests with specific tag
./gradlew test -DexcludedGroups="slow"
```

## Test Execution Optimization

The project uses gradle-test-logger-plugin v4.0.0 to visualize test execution times:
- Tests taking > 2 seconds are highlighted as slow
- Parallel test execution is enabled for faster feedback
- Test results are displayed with execution times

## Best Practices

1. **Write more unit tests**: They provide fast feedback and are easier to maintain
2. **Tag tests appropriately**: Use the correct annotation for each test type
3. **Monitor test execution time**: Keep unit tests fast (< 100ms)
4. **Use `@SlowTest` when appropriate**: For tests that inherently take longer
5. **Run unit tests frequently**: During development for fast feedback
6. **Run all tests before merging**: Ensure comprehensive coverage

## CI/CD Integration

In CI/CD pipelines, tests should be run in stages:
1. **Stage 1**: Unit tests (fast feedback)
2. **Stage 2**: Integration tests (if unit tests pass)
3. **Stage 3**: E2E tests (if integration tests pass)
4. **Stage 4**: Performance tests (optional, for performance-critical changes)

This approach provides fast feedback while ensuring comprehensive testing.
