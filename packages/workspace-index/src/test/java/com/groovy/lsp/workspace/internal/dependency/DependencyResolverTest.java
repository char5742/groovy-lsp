package com.groovy.lsp.workspace.internal.dependency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DependencyResolverTest {

    @TempDir Path tempDir;
    private DependencyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new DependencyResolver(tempDir);
    }

    @Test
    void detectBuildSystem_shouldDetectGradleWithBuildGradle() throws IOException {
        // Given
        Files.createFile(tempDir.resolve("build.gradle"));

        // When
        DependencyResolver.BuildSystem detected = resolver.detectBuildSystem();

        // Then
        assertThat(detected).isEqualTo(DependencyResolver.BuildSystem.GRADLE);
        assertThat(resolver.getBuildSystem()).isEqualTo(DependencyResolver.BuildSystem.GRADLE);
    }

    @Test
    void detectBuildSystem_shouldDetectGradleWithBuildGradleKts() throws IOException {
        // Given
        Files.createFile(tempDir.resolve("build.gradle.kts"));

        // When
        DependencyResolver.BuildSystem detected = resolver.detectBuildSystem();

        // Then
        assertThat(detected).isEqualTo(DependencyResolver.BuildSystem.GRADLE);
    }

    @Test
    void detectBuildSystem_shouldDetectGradleWithSettingsGradle() throws IOException {
        // Given
        Files.createFile(tempDir.resolve("settings.gradle"));

        // When
        DependencyResolver.BuildSystem detected = resolver.detectBuildSystem();

        // Then
        assertThat(detected).isEqualTo(DependencyResolver.BuildSystem.GRADLE);
    }

    @Test
    void detectBuildSystem_shouldDetectGradleWithSettingsGradleKts() throws IOException {
        // Given
        Files.createFile(tempDir.resolve("settings.gradle.kts"));

        // When
        DependencyResolver.BuildSystem detected = resolver.detectBuildSystem();

        // Then
        assertThat(detected).isEqualTo(DependencyResolver.BuildSystem.GRADLE);
    }

    @Test
    void detectBuildSystem_shouldDetectMaven() throws IOException {
        // Given
        Files.createFile(tempDir.resolve("pom.xml"));

        // When
        DependencyResolver.BuildSystem detected = resolver.detectBuildSystem();

        // Then
        assertThat(detected).isEqualTo(DependencyResolver.BuildSystem.MAVEN);
    }

    @Test
    void detectBuildSystem_shouldReturnNoneWhenNoBuildSystem() {
        // When
        DependencyResolver.BuildSystem detected = resolver.detectBuildSystem();

        // Then
        assertThat(detected).isEqualTo(DependencyResolver.BuildSystem.NONE);
    }

    @Test
    void detectBuildSystem_shouldPreferGradleOverMaven() throws IOException {
        // Given - both Gradle and Maven files exist
        Files.createFile(tempDir.resolve("build.gradle"));
        Files.createFile(tempDir.resolve("pom.xml"));

        // When
        DependencyResolver.BuildSystem detected = resolver.detectBuildSystem();

        // Then - Gradle should be preferred
        assertThat(detected).isEqualTo(DependencyResolver.BuildSystem.GRADLE);
    }

    @Test
    void resolveDependencies_shouldReturnEmptyListForNoBuildSystem() {
        // Given
        resolver.detectBuildSystem();

        // When
        List<Path> dependencies = resolver.resolveDependencies();

        // Then
        assertThat(dependencies).isEmpty();
    }

    @Test
    void resolveDependencies_shouldHandleGradleProjectsGracefully() throws IOException {
        // Given
        Files.createFile(tempDir.resolve("build.gradle"));
        resolver.detectBuildSystem();

        // When
        // This should not throw even though we're not in a real Gradle project
        assertThatCode(() -> resolver.resolveDependencies()).doesNotThrowAnyException();
    }

    @Test
    void resolveDependencies_shouldReturnEmptyListForMaven() throws IOException {
        // Given
        Files.createFile(tempDir.resolve("pom.xml"));
        resolver.detectBuildSystem();

        // When
        List<Path> dependencies = resolver.resolveDependencies();

        // Then
        assertThat(dependencies).isEmpty(); // Maven is not implemented yet
    }

    @Test
    void getSourceDirectories_shouldReturnGradleSourceDirs() throws IOException {
        // Given
        Files.createDirectories(tempDir.resolve("src/main/groovy"));
        Files.createDirectories(tempDir.resolve("src/main/java"));
        Files.createDirectories(tempDir.resolve("src/test/groovy"));
        Files.createDirectories(tempDir.resolve("src/test/java"));
        Files.createFile(tempDir.resolve("build.gradle"));
        resolver.detectBuildSystem();

        // When
        List<Path> sourceDirs = resolver.getSourceDirectories();

        // Then
        assertThat(sourceDirs)
                .containsExactly(
                        tempDir.resolve("src/main/groovy"),
                        tempDir.resolve("src/main/java"),
                        tempDir.resolve("src/test/groovy"),
                        tempDir.resolve("src/test/java"));
    }

    @Test
    void getSourceDirectories_shouldReturnOnlyExistingGradleDirs() throws IOException {
        // Given
        Files.createDirectories(tempDir.resolve("src/main/groovy"));
        Files.createFile(tempDir.resolve("build.gradle"));
        resolver.detectBuildSystem();

        // When
        List<Path> sourceDirs = resolver.getSourceDirectories();

        // Then
        assertThat(sourceDirs).containsExactly(tempDir.resolve("src/main/groovy"));
    }

    @Test
    void getSourceDirectories_shouldReturnMavenSourceDirs() throws IOException {
        // Given
        Files.createDirectories(tempDir.resolve("src/main/groovy"));
        Files.createDirectories(tempDir.resolve("src/test/java"));
        Files.createFile(tempDir.resolve("pom.xml"));
        resolver.detectBuildSystem();

        // When
        List<Path> sourceDirs = resolver.getSourceDirectories();

        // Then
        assertThat(sourceDirs)
                .containsExactly(
                        tempDir.resolve("src/main/groovy"), tempDir.resolve("src/test/java"));
    }

    @Test
    void getSourceDirectories_shouldReturnDefaultDirs() throws IOException {
        // Given
        Files.createDirectories(tempDir.resolve("src"));
        Files.createDirectories(tempDir.resolve("groovy"));
        resolver.detectBuildSystem(); // Should detect NONE

        // When
        List<Path> sourceDirs = resolver.getSourceDirectories();

        // Then
        assertThat(sourceDirs).containsExactly(tempDir.resolve("src"), tempDir.resolve("groovy"));
    }

    @Test
    void getSourceDirectories_shouldSkipNonDirectories() throws IOException {
        // Given
        Files.createFile(tempDir.resolve("src")); // File, not directory
        Files.createDirectories(tempDir.resolve("groovy"));
        resolver.detectBuildSystem();

        // When
        List<Path> sourceDirs = resolver.getSourceDirectories();

        // Then
        assertThat(sourceDirs).containsExactly(tempDir.resolve("groovy"));
    }

    @Test
    void getSourceDirectories_shouldReturnEmptyListWhenNoSourceDirs() {
        // Given
        resolver.detectBuildSystem();

        // When
        List<Path> sourceDirs = resolver.getSourceDirectories();

        // Then
        assertThat(sourceDirs).isEmpty();
    }
}
