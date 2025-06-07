package com.groovy.lsp.workspace.internal.dependency;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test cases for the deprecated DependencyResolver wrapper class.
 * This class tests the delegation to the new MavenAndGradleDependencyResolver.
 */
class DependencyResolverAdditionalTest {

    @TempDir Path tempDir;
    private DependencyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new DependencyResolver(tempDir);
    }

    @Test
    void resolveDependencies_shouldDelegateToNewResolver() throws IOException {
        // Create a valid gradle project
        Files.createFile(tempDir.resolve("build.gradle"));
        resolver.detectBuildSystem();

        // Call resolveDependencies which should delegate to the new resolver
        var dependencies = resolver.resolveDependencies();

        // In test environment, should return empty list
        assertThat(dependencies).isNotNull();
        assertThat(dependencies).isEmpty(); // Test environment returns empty
    }

    @Test
    void detectBuildSystem_shouldReCreateResolverOnReDetection() throws IOException {
        // First detection - should be NONE
        DependencyResolver.BuildSystem firstSystem = resolver.detectBuildSystem();
        assertThat(firstSystem).isEqualTo(DependencyResolver.BuildSystem.NONE);

        // Create a Gradle build file
        Files.createFile(tempDir.resolve("build.gradle"));

        // Second detection - should be GRADLE
        DependencyResolver.BuildSystem secondSystem = resolver.detectBuildSystem();
        assertThat(secondSystem).isEqualTo(DependencyResolver.BuildSystem.GRADLE);
    }

    @Test
    void getBuildSystem_shouldCallDetectBuildSystem() throws IOException {
        // Create a Maven pom.xml
        Files.createFile(tempDir.resolve("pom.xml"));

        // getBuildSystem should detect MAVEN
        DependencyResolver.BuildSystem system = resolver.getBuildSystem();
        assertThat(system).isEqualTo(DependencyResolver.BuildSystem.MAVEN);
    }

    @Test
    void enumMappingTest() {
        // Test that enum mapping works correctly
        assertThat(DependencyResolver.BuildSystem.values())
                .containsExactly(
                        DependencyResolver.BuildSystem.GRADLE,
                        DependencyResolver.BuildSystem.MAVEN,
                        DependencyResolver.BuildSystem.NONE);
    }

    @Test
    void getSourceDirectories_shouldDelegateToNewResolver() {
        // This tests that getSourceDirectories delegates correctly
        var sourceDirs = resolver.getSourceDirectories();
        assertThat(sourceDirs).isNotNull();
    }
}
