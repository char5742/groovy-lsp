package com.groovy.lsp.workspace.dependency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.groovy.lsp.workspace.dependency.DependencyResolver.BuildSystem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GradleDependencyResolverTest {

    @TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        // Set test environment property
        System.setProperty("test.mode", "true");
    }

    @Test
    void canHandle_withBuildGradle_returnsTrue() throws IOException {
        Path buildFile = tempDir.resolve("build.gradle");
        Files.writeString(buildFile, "apply plugin: 'java'");

        GradleDependencyResolver resolver = new GradleDependencyResolver(tempDir);

        assertTrue(resolver.canHandle(tempDir));
    }

    @Test
    void canHandle_withBuildGradleKts_returnsTrue() throws IOException {
        Path buildFile = tempDir.resolve("build.gradle.kts");
        Files.writeString(buildFile, "plugins { java }");

        GradleDependencyResolver resolver = new GradleDependencyResolver(tempDir);

        assertTrue(resolver.canHandle(tempDir));
    }

    @Test
    void canHandle_withSettingsGradle_returnsTrue() throws IOException {
        Path settingsFile = tempDir.resolve("settings.gradle");
        Files.writeString(settingsFile, "rootProject.name = 'test'");

        GradleDependencyResolver resolver = new GradleDependencyResolver(tempDir);

        assertTrue(resolver.canHandle(tempDir));
    }

    @Test
    void canHandle_withoutGradleFiles_returnsFalse() {
        GradleDependencyResolver resolver = new GradleDependencyResolver(tempDir);

        assertFalse(resolver.canHandle(tempDir));
    }

    @Test
    void getBuildSystem_returnsGradle() {
        GradleDependencyResolver resolver = new GradleDependencyResolver(tempDir);

        assertEquals(DependencyResolver.BuildSystem.GRADLE, resolver.getBuildSystem());
    }

    @Test
    void getSourceDirectories_returnsStandardGradleDirectories() throws IOException {
        // Create standard Gradle directory structure
        Files.createDirectories(tempDir.resolve("src/main/java"));
        Files.createDirectories(tempDir.resolve("src/main/groovy"));
        Files.createDirectories(tempDir.resolve("src/test/java"));
        Files.createDirectories(tempDir.resolve("src/test/groovy"));

        GradleDependencyResolver resolver = new GradleDependencyResolver(tempDir);
        List<Path> sourceDirs = resolver.getSourceDirectories();

        assertThat(sourceDirs)
                .containsExactlyInAnyOrder(
                        tempDir.resolve("src/main/groovy"),
                        tempDir.resolve("src/main/java"),
                        tempDir.resolve("src/test/groovy"),
                        tempDir.resolve("src/test/java"));
    }

    @Test
    void resolveDependencies_inTestEnvironment_returnsEmptyList() throws IOException {
        // Create a build.gradle file
        Path buildFile = tempDir.resolve("build.gradle");
        Files.writeString(buildFile, "apply plugin: 'java'");

        GradleDependencyResolver resolver = new GradleDependencyResolver(tempDir);

        // In test environment, should return empty list
        List<Path> dependencies = resolver.resolveDependencies();

        assertThat(dependencies).isEmpty();
    }

    @Test
    void resolveDependencies_withInvalidProject_returnsEmptyList() {
        // No build files
        GradleDependencyResolver resolver = new GradleDependencyResolver(tempDir);

        List<Path> dependencies = resolver.resolveDependencies();

        assertThat(dependencies).isEmpty();
    }

    @Test
    void constructor_shouldAcceptCustomTimeout() throws IOException {
        Path buildFile = tempDir.resolve("build.gradle");
        Files.writeString(buildFile, "apply plugin: 'java'");

        // Create resolver with custom timeout
        long customTimeout = 10L;
        GradleDependencyResolver resolver = new GradleDependencyResolver(tempDir, customTimeout);

        // Verify it can handle Gradle projects
        assertTrue(resolver.canHandle(tempDir));
        assertEquals(BuildSystem.GRADLE, resolver.getBuildSystem());

        // In test environment, should return empty list regardless of timeout
        List<Path> dependencies = resolver.resolveDependencies();
        assertThat(dependencies).isEmpty();
    }
}
