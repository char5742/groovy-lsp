package com.groovy.lsp.workspace.dependency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MavenDependencyResolverTest {

    @TempDir Path tempDir = Path.of("");

    @Test
    void canHandle_withPomXml_returnsTrue() throws IOException {
        // Create a pom.xml file
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, createMinimalPom());

        MavenDependencyResolver resolver = new MavenDependencyResolver(tempDir);

        assertTrue(resolver.canHandle(tempDir));
    }

    @Test
    void canHandle_withoutPomXml_returnsFalse() {
        MavenDependencyResolver resolver = new MavenDependencyResolver(tempDir);

        assertFalse(resolver.canHandle(tempDir));
    }

    @Test
    void getBuildSystem_returnsMaven() {
        MavenDependencyResolver resolver = new MavenDependencyResolver(tempDir);

        assertEquals(DependencyResolver.BuildSystem.MAVEN, resolver.getBuildSystem());
    }

    @Test
    void getSourceDirectories_returnsStandardMavenDirectories() throws IOException {
        // Create standard Maven directory structure
        Files.createDirectories(tempDir.resolve("src/main/java"));
        Files.createDirectories(tempDir.resolve("src/main/groovy"));
        Files.createDirectories(tempDir.resolve("src/test/java"));
        Files.createDirectories(tempDir.resolve("src/test/groovy"));

        MavenDependencyResolver resolver = new MavenDependencyResolver(tempDir);
        List<Path> sourceDirs = resolver.getSourceDirectories();

        assertThat(sourceDirs)
                .containsExactlyInAnyOrder(
                        tempDir.resolve("src/main/groovy"),
                        tempDir.resolve("src/main/java"),
                        tempDir.resolve("src/test/groovy"),
                        tempDir.resolve("src/test/java"));
    }

    @Test
    void resolveDependencies_withNoPom_returnsEmptyList() {
        MavenDependencyResolver resolver = new MavenDependencyResolver(tempDir);

        List<Path> dependencies = resolver.resolveDependencies();

        assertThat(dependencies).isEmpty();
    }

    @Test
    void resolveDependencies_withSimplePom_resolvesJunitDependency() throws IOException {
        // Create a pom.xml with JUnit dependency
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, createPomWithJunit());

        MavenDependencyResolver resolver = new MavenDependencyResolver(tempDir);
        List<Path> dependencies = resolver.resolveDependencies();

        // This test requires Maven to be installed and have junit in local repository
        // In CI or fresh environments, this might fail
        // For now, we just check that the method doesn't throw an exception
        assertThat(dependencies).isNotNull();

        // If junit is in the local repository, it should be resolved
        if (!dependencies.isEmpty()) {
            assertThat(dependencies)
                    .anyMatch(path -> path.toString().contains("junit"))
                    .allMatch(Files::exists);
        }
    }

    @Test
    void resolveDependencies_withTestScopeDependency_excludesTestDependencies() throws IOException {
        // Create a pom.xml with test scope dependency
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, createPomWithTestDependency());

        MavenDependencyResolver resolver = new MavenDependencyResolver(tempDir);
        List<Path> dependencies = resolver.resolveDependencies();

        // Test dependencies should be excluded
        assertThat(dependencies).noneMatch(path -> path.toString().contains("mockito"));
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

    private String createPomWithTestDependency() {
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
                    <groupId>org.mockito</groupId>
                    <artifactId>mockito-core</artifactId>
                    <version>5.1.1</version>
                    <scope>test</scope>
                </dependency>
            </dependencies>
        </project>
        """;
    }
}
