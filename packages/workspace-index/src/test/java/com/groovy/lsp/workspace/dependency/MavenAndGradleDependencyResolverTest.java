package com.groovy.lsp.workspace.dependency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MavenAndGradleDependencyResolverTest {

    @TempDir Path tempDir;

    @Test
    void detectsGradleProject() throws IOException {
        // Create a build.gradle file
        Path buildFile = tempDir.resolve("build.gradle");
        Files.writeString(buildFile, "apply plugin: 'java'");

        MavenAndGradleDependencyResolver resolver = new MavenAndGradleDependencyResolver(tempDir);

        assertTrue(resolver.canHandle(tempDir));
        assertEquals(DependencyResolver.BuildSystem.GRADLE, resolver.getBuildSystem());
    }

    @Test
    void detectsMavenProject() throws IOException {
        // Create a pom.xml file
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, createMinimalPom());

        MavenAndGradleDependencyResolver resolver = new MavenAndGradleDependencyResolver(tempDir);

        assertTrue(resolver.canHandle(tempDir));
        assertEquals(DependencyResolver.BuildSystem.MAVEN, resolver.getBuildSystem());
    }

    @Test
    void detectsNoBuildSystem() {
        MavenAndGradleDependencyResolver resolver = new MavenAndGradleDependencyResolver(tempDir);

        assertFalse(resolver.canHandle(tempDir));
        assertEquals(DependencyResolver.BuildSystem.NONE, resolver.getBuildSystem());
    }

    @Test
    void prefersGradleOverMaven() throws IOException {
        // Create both build.gradle and pom.xml
        Path buildFile = tempDir.resolve("build.gradle");
        Files.writeString(buildFile, "apply plugin: 'java'");
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, createMinimalPom());

        MavenAndGradleDependencyResolver resolver = new MavenAndGradleDependencyResolver(tempDir);

        // Should prefer Gradle
        assertEquals(DependencyResolver.BuildSystem.GRADLE, resolver.getBuildSystem());
    }

    @Test
    void getSourceDirectories_withNoBuildSystem_returnsDefaultDirectories() throws IOException {
        // Create some default directories
        Files.createDirectories(tempDir.resolve("src"));
        Files.createDirectories(tempDir.resolve("groovy"));

        MavenAndGradleDependencyResolver resolver = new MavenAndGradleDependencyResolver(tempDir);
        List<Path> sourceDirs = resolver.getSourceDirectories();

        assertThat(sourceDirs)
                .containsExactlyInAnyOrder(tempDir.resolve("src"), tempDir.resolve("groovy"));
    }

    @Test
    void resolveDependencies_withNoBuildSystem_returnsEmptyList() {
        MavenAndGradleDependencyResolver resolver = new MavenAndGradleDependencyResolver(tempDir);

        List<Path> dependencies = resolver.resolveDependencies();

        assertThat(dependencies).isEmpty();
    }

    @Test
    void resolveDependencies_delegatesToMavenResolver() throws IOException {
        // Create a pom.xml with JUnit dependency
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, createPomWithJunit());

        MavenAndGradleDependencyResolver resolver = new MavenAndGradleDependencyResolver(tempDir);
        List<Path> dependencies = resolver.resolveDependencies();

        // This test requires Maven to be installed and have junit in local repository
        // For now, we just check that the method doesn't throw an exception
        assertThat(dependencies).isNotNull();

        // If junit is in the local repository, it should be resolved
        if (!dependencies.isEmpty()) {
            assertThat(dependencies).anyMatch(path -> path.toString().contains("junit"));
        }
    }

    @Test
    void resolveDependencies_handlesResolverExceptions() throws IOException {
        // Create an invalid pom.xml
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, "invalid xml content");

        MavenAndGradleDependencyResolver resolver = new MavenAndGradleDependencyResolver(tempDir);

        // Should handle exception and return empty list
        List<Path> dependencies = resolver.resolveDependencies();
        assertThat(dependencies).isEmpty();
    }

    private String createMinimalPom() {
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <project xmlns="http://maven.apache.org/POM/4.0.0"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                 http://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>
            <groupId>com.test</groupId>
            <artifactId>test-project</artifactId>
            <version>1.0.0</version>
        </project>
        """;
    }

    private String createPomWithJunit() {
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <project xmlns="http://maven.apache.org/POM/4.0.0"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                 http://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>
            <groupId>com.test</groupId>
            <artifactId>test-project</artifactId>
            <version>1.0.0</version>

            <dependencies>
                <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.2</version>
                    <scope>compile</scope>
                </dependency>
            </dependencies>
        </project>
        """;
    }
}
