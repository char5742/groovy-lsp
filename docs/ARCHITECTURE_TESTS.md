# Architecture Tests

This document describes the architecture tests implemented in the Groovy LSP project using ArchUnit.

## Test Annotation Rules

To ensure consistent test organization and execution, we enforce the following rules:

### 1. No Direct @Test Usage

**Rule**: Test methods must not use `@Test` annotation directly.

**Reason**: We use custom test annotations that combine `@Test` with `@Tag` to categorize tests according to Google's test pyramid strategy.

**Allowed Annotations**:
- `@UnitTest` - For unit tests (70% of tests)
- `@IntegrationTest` - For integration tests (20% of tests)
- `@E2ETest` - For end-to-end tests (10% of tests)
- `@PerformanceTest` - For performance tests
- `@SlowTest` - For tests that take more than 2 seconds

### 2. Test Method Naming

**Rule**: All public test methods in test classes must use one of our custom test annotations.

**Exceptions**:
- Static methods
- Abstract methods
- Utility methods that don't follow test naming patterns

## Implementation

The architecture tests are implemented in `TestAnnotationUsageTest` in the shared module.

### Running Architecture Tests

Architecture tests run automatically as part of the test suite:

```bash
./gradlew test
```

To run only architecture tests:

```bash
./gradlew :shared:test --tests "*TestAnnotationUsageTest"
```

### Adding Architecture Tests to Other Modules

To add these rules to other modules:

1. Add ArchUnit dependency to your module's `build.gradle`:
```gradle
testImplementation libs.archunit
```

2. Create a test class that extends or copies the rules from `TestAnnotationRules`:
```java
@UnitTest
public class ModuleArchitectureTest {
    @UnitTest
    void testNoDirectTestAnnotation() {
        // Copy the test from TestAnnotationUsageTest
        // Adjust package filters as needed
    }
}
```

## Benefits

1. **Consistent Test Categorization**: All tests are properly categorized for the test pyramid
2. **Execution Control**: Tests can be run by category using Gradle tasks
3. **Performance Tracking**: gradle-test-logger-plugin can track execution times by category
4. **Future-Proof**: Prevents accidental usage of @Test annotation directly

## Violations

If a violation is detected, the test will fail with a clear message indicating:
- The class or method that violates the rule
- The reason for the violation
- The list of allowed annotations to use instead
