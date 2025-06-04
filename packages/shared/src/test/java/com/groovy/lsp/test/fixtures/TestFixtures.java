package com.groovy.lsp.test.fixtures;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;

/**
 * Central test fixtures management for all test modules.
 * Provides utilities to load test data and manage test resources.
 */
public final class TestFixtures {
    private static final Map<String, String> FIXTURE_CACHE = new ConcurrentHashMap<>();
    private static final String FIXTURES_ROOT = "fixtures/";

    private TestFixtures() {
        // Utility class
    }

    /**
     * Load a Groovy source file fixture.
     */
    @NonNull
    public static String loadGroovyFixture(@NonNull String name) {
        return loadFixture("groovy/" + name);
    }

    /**
     * Load a JSON fixture.
     */
    @NonNull
    public static String loadJsonFixture(@NonNull String name) {
        return loadFixture("json/" + name);
    }

    /**
     * Load a fixture file from the classpath.
     */
    @NonNull
    public static String loadFixture(@NonNull String path) {
        return FIXTURE_CACHE.computeIfAbsent(
                path,
                p -> {
                    try {
                        return loadResource(FIXTURES_ROOT + p);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to load fixture: " + p, e);
                    }
                });
    }

    /**
     * Load a resource from the classpath.
     */
    @NonNull
    public static String loadResource(@NonNull String path) throws IOException {
        URL resource = TestFixtures.class.getClassLoader().getResource(path);
        if (resource == null) {
            throw new IOException("Resource not found: " + path);
        }

        try (InputStream is = resource.openStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Create a temporary directory for testing.
     */
    @NonNull
    public static Path createTempDirectory(@NonNull String prefix) throws IOException {
        return Files.createTempDirectory("groovy-lsp-test-" + prefix);
    }

    /**
     * Create a temporary file with content.
     */
    @NonNull
    public static Path createTempFile(
            @NonNull Path dir, @NonNull String name, @NonNull String content) throws IOException {
        Path file = dir.resolve(name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    /**
     * Create a temporary Groovy project structure.
     */
    @NonNull
    public static TestProject createTestProject(@NonNull String name) throws IOException {
        Path root = createTempDirectory(name);
        return new TestProject(root);
    }

    /**
     * Test project structure helper.
     */
    public static class TestProject implements AutoCloseable {
        private final Path root;
        private final Path srcMain;
        private final Path srcTest;
        private final Path resources;

        TestProject(Path root) throws IOException {
            this.root = root;
            this.srcMain = Files.createDirectories(root.resolve("src/main/groovy"));
            this.srcTest = Files.createDirectories(root.resolve("src/test/groovy"));
            this.resources = Files.createDirectories(root.resolve("src/main/resources"));
        }

        @NonNull
        public Path getRoot() {
            return root;
        }

        @NonNull
        public Path getSrcMain() {
            return srcMain;
        }

        @NonNull
        public Path getSrcTest() {
            return srcTest;
        }

        @NonNull
        public Path getResources() {
            return resources;
        }

        @NonNull
        public Path addGroovyFile(
                @NonNull String packageName, @NonNull String className, @NonNull String content)
                throws IOException {
            Path packageDir = srcMain;
            if (!packageName.isEmpty()) {
                // Use replace instead of split to avoid StringSplitter warning
                String pathSegments = packageName.replace('.', '/');
                packageDir = packageDir.resolve(pathSegments);
                Files.createDirectories(packageDir);
            }

            Path file = packageDir.resolve(className + ".groovy");
            Files.writeString(file, content, StandardCharsets.UTF_8);
            return file;
        }

        @NonNull
        public Path addTestFile(
                @NonNull String packageName, @NonNull String className, @NonNull String content)
                throws IOException {
            Path packageDir = srcTest;
            if (!packageName.isEmpty()) {
                // Use replace instead of split to avoid StringSplitter warning
                String pathSegments = packageName.replace('.', '/');
                packageDir = packageDir.resolve(pathSegments);
                Files.createDirectories(packageDir);
            }

            Path file = packageDir.resolve(className + ".groovy");
            Files.writeString(file, content, StandardCharsets.UTF_8);
            return file;
        }

        @NonNull
        public Path addResource(@NonNull String name, @NonNull String content) throws IOException {
            Path file = resources.resolve(name);
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);
            return file;
        }

        @Override
        public void close() throws IOException {
            deleteRecursively(root);
        }

        private void deleteRecursively(Path path) throws IOException {
            if (Files.exists(path)) {
                try (Stream<Path> pathStream = Files.walk(path)) {
                    pathStream
                            .sorted((a, b) -> b.compareTo(a)) // Delete in reverse order
                            .forEach(
                                    p -> {
                                        try {
                                            Files.delete(p);
                                        } catch (IOException e) {
                                            // Ignore errors during cleanup
                                        }
                                    });
                }
            }
        }
    }
}
