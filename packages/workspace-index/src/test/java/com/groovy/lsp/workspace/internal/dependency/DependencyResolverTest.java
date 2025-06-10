package com.groovy.lsp.workspace.internal.dependency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.groovy.lsp.test.annotations.UnitTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

class DependencyResolverTest {

    @TempDir @Nullable Path tempDir;
    private DependencyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver =
                new DependencyResolver(
                        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit"));
    }

    @UnitTest
    void detectBuildSystem_shouldDetectGradleWithBuildGradle() throws IOException {
        // Given
        Files.createFile(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("build.gradle"));

        // When
        DependencyResolver.BuildSystem detected = resolver.detectBuildSystem();

        // Then
        assertThat(detected).isEqualTo(DependencyResolver.BuildSystem.GRADLE);
        assertThat(resolver.getBuildSystem()).isEqualTo(DependencyResolver.BuildSystem.GRADLE);
    }

    @UnitTest
    void detectBuildSystem_shouldDetectGradleWithBuildGradleKts() throws IOException {
        // Given
        Files.createFile(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("build.gradle.kts"));

        // When
        DependencyResolver.BuildSystem detected = resolver.detectBuildSystem();

        // Then
        assertThat(detected).isEqualTo(DependencyResolver.BuildSystem.GRADLE);
    }

    @UnitTest
    void detectBuildSystem_shouldDetectGradleWithSettingsGradle() throws IOException {
        // Given
        Files.createFile(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("settings.gradle"));

        // When
        DependencyResolver.BuildSystem detected = resolver.detectBuildSystem();

        // Then
        assertThat(detected).isEqualTo(DependencyResolver.BuildSystem.GRADLE);
    }

    @UnitTest
    void detectBuildSystem_shouldDetectGradleWithSettingsGradleKts() throws IOException {
        // Given
        Files.createFile(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("settings.gradle.kts"));

        // When
        DependencyResolver.BuildSystem detected = resolver.detectBuildSystem();

        // Then
        assertThat(detected).isEqualTo(DependencyResolver.BuildSystem.GRADLE);
    }

    @UnitTest
    void detectBuildSystem_shouldDetectMaven() throws IOException {
        // Given
        Files.createFile(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("pom.xml"));

        // When
        DependencyResolver.BuildSystem detected = resolver.detectBuildSystem();

        // Then
        assertThat(detected).isEqualTo(DependencyResolver.BuildSystem.MAVEN);
    }

    @UnitTest
    void detectBuildSystem_shouldReturnNoneWhenNoBuildSystem() {
        // When
        DependencyResolver.BuildSystem detected = resolver.detectBuildSystem();

        // Then
        assertThat(detected).isEqualTo(DependencyResolver.BuildSystem.NONE);
    }

    @UnitTest
    void detectBuildSystem_shouldPreferGradleOverMaven() throws IOException {
        // Given - both Gradle and Maven files exist
        Files.createFile(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("build.gradle"));
        Files.createFile(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("pom.xml"));

        // When
        DependencyResolver.BuildSystem detected = resolver.detectBuildSystem();

        // Then - Gradle should be preferred
        assertThat(detected).isEqualTo(DependencyResolver.BuildSystem.GRADLE);
    }

    @UnitTest
    void resolveDependencies_shouldReturnEmptyListForNoBuildSystem() {
        // Given
        resolver.detectBuildSystem();

        // When
        List<Path> dependencies = resolver.resolveDependencies();

        // Then
        assertThat(dependencies).isEmpty();
    }

    @UnitTest
    void resolveDependencies_shouldHandleGradleProjectsGracefully() throws IOException {
        // Given
        Files.createFile(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("build.gradle"));
        resolver.detectBuildSystem();

        // When
        // This should not throw even though we're not in a real Gradle project
        assertThatCode(() -> resolver.resolveDependencies()).doesNotThrowAnyException();
    }

    @UnitTest
    void resolveDependencies_shouldReturnEmptyListForMaven() throws IOException {
        // Given
        Files.createFile(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("pom.xml"));
        resolver.detectBuildSystem();

        // When
        List<Path> dependencies = resolver.resolveDependencies();

        // Then
        assertThat(dependencies).isEmpty(); // Maven is not implemented yet
    }

    @UnitTest
    void getSourceDirectories_shouldReturnGradleSourceDirs() throws IOException {
        // Given
        Files.createDirectories(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("src/main/groovy"));
        Files.createDirectories(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("src/main/java"));
        Files.createDirectories(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("src/test/groovy"));
        Files.createDirectories(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("src/test/java"));
        Files.createFile(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("build.gradle"));
        resolver.detectBuildSystem();

        // When
        List<Path> sourceDirs = resolver.getSourceDirectories();

        // Then
        assertThat(sourceDirs)
                .containsExactly(
                        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                                .resolve("src/main/groovy"),
                        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                                .resolve("src/main/java"),
                        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                                .resolve("src/test/groovy"),
                        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                                .resolve("src/test/java"));
    }

    @UnitTest
    void getSourceDirectories_shouldReturnOnlyExistingGradleDirs() throws IOException {
        // Given
        Files.createDirectories(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("src/main/groovy"));
        Files.createFile(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("build.gradle"));
        resolver.detectBuildSystem();

        // When
        List<Path> sourceDirs = resolver.getSourceDirectories();

        // Then
        assertThat(sourceDirs)
                .containsExactly(
                        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                                .resolve("src/main/groovy"));
    }

    @UnitTest
    void getSourceDirectories_shouldReturnMavenSourceDirs() throws IOException {
        // Given
        Files.createDirectories(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("src/main/groovy"));
        Files.createDirectories(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("src/test/java"));
        Files.createFile(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("pom.xml"));
        resolver.detectBuildSystem();

        // When
        List<Path> sourceDirs = resolver.getSourceDirectories();

        // Then
        assertThat(sourceDirs)
                .containsExactly(
                        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                                .resolve("src/main/groovy"),
                        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                                .resolve("src/test/java"));
    }

    @UnitTest
    void getSourceDirectories_shouldReturnDefaultDirs() throws IOException {
        // Given
        Files.createDirectories(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("src"));
        Files.createDirectories(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("groovy"));
        resolver.detectBuildSystem(); // Should detect NONE

        // When
        List<Path> sourceDirs = resolver.getSourceDirectories();

        // Then
        assertThat(sourceDirs)
                .containsExactly(
                        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                                .resolve("src"),
                        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                                .resolve("groovy"));
    }

    @UnitTest
    void getSourceDirectories_shouldSkipNonDirectories() throws IOException {
        // Given
        Files.createFile(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("src")); // File, not directory
        Files.createDirectories(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("groovy"));
        resolver.detectBuildSystem();

        // When
        List<Path> sourceDirs = resolver.getSourceDirectories();

        // Then
        assertThat(sourceDirs)
                .containsExactly(
                        Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                                .resolve("groovy"));
    }

    @UnitTest
    void getSourceDirectories_shouldReturnEmptyListWhenNoSourceDirs() {
        // Given
        resolver.detectBuildSystem();

        // When
        List<Path> sourceDirs = resolver.getSourceDirectories();

        // Then
        assertThat(sourceDirs).isEmpty();
    }

    @UnitTest
    void resolveDependencies_shouldHandleInvalidGradleProject() throws IOException {
        // Given - Create a file that is not a gradle build file but in test mode
        Files.createFile(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("notGradle.txt"));
        resolver.detectBuildSystem(); // Will detect NONE

        // When - Try to resolve dependencies for non-gradle project
        List<Path> dependencies = resolver.resolveDependencies();

        // Then - Should return empty list
        assertThat(dependencies).isEmpty();
    }

    @UnitTest
    void resolveDependencies_shouldHandleGradleProjectWithOnlySettingsGradle() throws IOException {
        // Given - Only settings.gradle file exists
        Files.createFile(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("settings.gradle"));
        resolver.detectBuildSystem();

        // When
        List<Path> dependencies = resolver.resolveDependencies();

        // Then - Should handle gracefully and return empty list
        assertThat(dependencies).isEmpty();
    }

    @UnitTest
    void resolveDependencies_shouldHandleGradleProjectWithOnlySettingsGradleKts()
            throws IOException {
        // Given - Only settings.gradle.kts file exists
        Files.createFile(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("settings.gradle.kts"));
        resolver.detectBuildSystem();

        // When
        List<Path> dependencies = resolver.resolveDependencies();

        // Then - Should handle gracefully and return empty list
        assertThat(dependencies).isEmpty();
    }

    @UnitTest
    void resolveDependencies_shouldHandleGradleProjectWithBuildGradleKts() throws IOException {
        // Given - Only build.gradle.kts file exists
        Files.createFile(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("build.gradle.kts"));
        resolver.detectBuildSystem();

        // When
        List<Path> dependencies = resolver.resolveDependencies();

        // Then - Should handle gracefully and return empty list
        assertThat(dependencies).isEmpty();
    }

    @UnitTest
    void resolveDependencies_shouldReturnEmptyListInTestEnvironment() throws IOException {
        // Given - Set test mode system property
        System.setProperty("test.mode", "true");
        try {
            Files.createFile(
                    Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                            .resolve("build.gradle"));
            resolver.detectBuildSystem();

            // When
            List<Path> dependencies = resolver.resolveDependencies();

            // Then - Should return empty list in test environment
            assertThat(dependencies).isEmpty();
        } finally {
            System.clearProperty("test.mode");
        }
    }

    @UnitTest
    void resolveDependencies_shouldReturnEmptyListInBuildEnvTest() throws IOException {
        // Given - Set build.env to test
        System.setProperty("build.env", "test");
        try {
            Files.createFile(
                    Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                            .resolve("build.gradle"));
            resolver.detectBuildSystem();

            // When
            List<Path> dependencies = resolver.resolveDependencies();

            // Then - Should return empty list in test environment
            assertThat(dependencies).isEmpty();
        } finally {
            System.clearProperty("build.env");
        }
    }

    @UnitTest
    void resolveDependencies_shouldReturnEmptyListInGradleTestTask() throws IOException {
        // Given - Set gradle task name to test
        System.setProperty("gradle.task.name", "test");
        try {
            Files.createFile(
                    Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                            .resolve("build.gradle"));
            resolver.detectBuildSystem();

            // When
            List<Path> dependencies = resolver.resolveDependencies();

            // Then - Should return empty list in test environment
            assertThat(dependencies).isEmpty();
        } finally {
            System.clearProperty("gradle.task.name");
        }
    }

    @UnitTest
    void resolveDependencies_shouldReturnEmptyListWhenRunningTests() throws IOException {
        // Given - Set multiple test indicators at once
        System.setProperty("gradle.task.name", "check");
        try {
            Files.createFile(
                    Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                            .resolve("build.gradle"));
            resolver.detectBuildSystem();

            // When
            List<Path> dependencies = resolver.resolveDependencies();

            // Then - Should return empty list in test environment
            assertThat(dependencies).isEmpty();
        } finally {
            System.clearProperty("gradle.task.name");
        }
    }

    @UnitTest
    void resolveDependencies_shouldReturnEmptyListWhenTestClasspath() throws IOException {
        // Given - Set java.class.path containing "test-classes"
        String originalClassPath = System.getProperty("java.class.path", "");
        System.setProperty("java.class.path", "/path/to/test-classes:" + originalClassPath);
        try {
            Files.createFile(
                    Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                            .resolve("build.gradle"));
            resolver.detectBuildSystem();

            // When
            List<Path> dependencies = resolver.resolveDependencies();

            // Then - Should return empty list in test environment
            assertThat(dependencies).isEmpty();
        } finally {
            System.setProperty("java.class.path", originalClassPath);
        }
    }

    @UnitTest
    void resolveDependencies_shouldReturnEmptyListWithJunitPlatform() throws IOException {
        // Given - Set junit.platform.launcher.interceptors.enabled
        System.setProperty("junit.platform.launcher.interceptors.enabled", "true");
        try {
            Files.createFile(
                    Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                            .resolve("build.gradle"));
            resolver.detectBuildSystem();

            // When
            List<Path> dependencies = resolver.resolveDependencies();

            // Then - Should return empty list in test environment
            assertThat(dependencies).isEmpty();
        } finally {
            System.clearProperty("junit.platform.launcher.interceptors.enabled");
        }
    }

    @UnitTest
    void resolveDependencies_shouldReturnEmptyListWithEnvironmentVariable() throws IOException {
        // Given - gradle.task.name starts with "test"
        System.setProperty("gradle.task.name", "testClasses");
        try {
            Files.createFile(
                    Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                            .resolve("build.gradle"));
            resolver.detectBuildSystem();

            // When
            List<Path> dependencies = resolver.resolveDependencies();

            // Then - Should return empty list in test environment
            assertThat(dependencies).isEmpty();
        } finally {
            System.clearProperty("gradle.task.name");
        }
    }

    @UnitTest
    void resolveDependencies_shouldReturnEmptyListWithGradleWrapper() throws IOException {
        // Given - Create Gradle wrapper files
        Files.createFile(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("gradlew"));
        Files.createFile(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("build.gradle"));
        resolver.detectBuildSystem();

        // When
        List<Path> dependencies = resolver.resolveDependencies();

        // Then - Should handle gracefully and return empty list
        assertThat(dependencies).isEmpty();
    }

    @UnitTest
    void resolveDependencies_shouldHandleMultipleBuildFiles() throws IOException {
        // Given - Multiple Gradle build files exist for valid project check
        Files.createFile(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("build.gradle"));
        Files.createFile(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("build.gradle.kts"));
        Files.createFile(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("settings.gradle"));
        Files.createFile(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("settings.gradle.kts"));
        resolver.detectBuildSystem();

        // When
        List<Path> dependencies = resolver.resolveDependencies();

        // Then - Should handle gracefully
        assertThat(dependencies).isEmpty();
    }

    @UnitTest
    void resolveDependencies_shouldHandleProjectWithoutWrapper() throws IOException {
        // Given - Gradle project without wrapper
        Files.createFile(
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("build.gradle"));
        // Ensure gradlew does not exist
        assertThat(
                        Files.exists(
                                Objects.requireNonNull(
                                                tempDir, "tempDir should be initialized by JUnit")
                                        .resolve("gradlew")))
                .isFalse();
        resolver.detectBuildSystem();

        // When
        List<Path> dependencies = resolver.resolveDependencies();

        // Then - Should handle gracefully
        assertThat(dependencies).isEmpty();
    }

    @UnitTest
    void resolveDependencies_shouldHandleNonTestEnvironment() throws IOException {
        // Given - Ensure no test environment indicators
        System.clearProperty("test.mode");
        System.clearProperty("build.env");
        System.clearProperty("gradle.task.name");
        String classpath = System.getProperty("java.class.path", "");
        // Remove test-classes from classpath if present
        if (classpath.contains("test-classes")) {
            System.setProperty("java.class.path", classpath.replace("test-classes", "classes"));
        }

        try {
            Files.createFile(
                    Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                            .resolve("build.gradle"));
            resolver.detectBuildSystem();

            // When
            List<Path> dependencies = resolver.resolveDependencies();

            // Then - Should return empty (because it's still a dummy gradle project)
            assertThat(dependencies).isEmpty();
        } finally {
            System.setProperty("java.class.path", classpath);
        }
    }
}
