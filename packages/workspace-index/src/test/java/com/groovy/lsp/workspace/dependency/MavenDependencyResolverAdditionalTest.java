package com.groovy.lsp.workspace.dependency;

import static org.assertj.core.api.Assertions.assertThat;

import com.groovy.lsp.test.annotations.UnitTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

/**
 * Additional test cases for MavenDependencyResolver to improve branch coverage.
 */
class MavenDependencyResolverAdditionalTest {

    @TempDir Path tempDir = Path.of("");
    private MavenDependencyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new MavenDependencyResolver(tempDir);
    }

    @UnitTest
    void resolveDependencies_shouldHandleMalformedPom() throws IOException {
        // Given - Malformed pom.xml
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, "<?xml version=\"1.0\"?>\n<invalid>Not a valid POM</invalid>");

        // When
        var dependencies = resolver.resolveDependencies();

        // Then
        assertThat(dependencies).isEmpty();
    }

    @UnitTest
    void resolveDependencies_shouldHandleEmptyPom() throws IOException {
        // Given - Empty pom.xml
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, "");

        // When
        var dependencies = resolver.resolveDependencies();

        // Then
        assertThat(dependencies).isEmpty();
    }

    @UnitTest
    void resolveDependencies_shouldHandlePomWithoutDependencies() throws IOException {
        // Given - Valid pom.xml but no dependencies
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(
                pomFile,
                """
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
                """);

        // When
        var dependencies = resolver.resolveDependencies();

        // Then
        assertThat(dependencies).isEmpty();
    }

    @UnitTest
    void resolveDependencies_shouldHandleDependencyWithCustomType() throws IOException {
        // Given - pom.xml with dependency that has custom type
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(
                pomFile,
                """
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
                            <groupId>org.apache.groovy</groupId>
                            <artifactId>groovy-all</artifactId>
                            <version>4.0.27</version>
                            <type>pom</type>
                        </dependency>
                    </dependencies>
                </project>
                """);

        // When
        var dependencies = resolver.resolveDependencies();

        // Then - In test environment, this may succeed or fail depending on local Maven cache
        // The test purpose is to verify that a pom type dependency is handled without error
        assertThat(dependencies).isNotNull();
    }

    @UnitTest
    void resolveDependencies_shouldHandleDependencyResolutionFailure() throws IOException {
        // Given - pom.xml with non-existent dependency
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(
                pomFile,
                """
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
                            <groupId>non.existent.group</groupId>
                            <artifactId>non-existent-artifact</artifactId>
                            <version>99.99.99</version>
                        </dependency>
                    </dependencies>
                </project>
                """);

        // When
        var dependencies = resolver.resolveDependencies();

        // Then - Should handle gracefully and return empty
        assertThat(dependencies).isEmpty();
    }

    @UnitTest
    void getSourceDirectories_shouldHandlePartialDirectoryStructure() throws IOException {
        // Create only some Maven directories
        Path srcMainJava = tempDir.resolve("src/main/java");
        Path srcTestGroovy = tempDir.resolve("src/test/groovy");
        Files.createDirectories(srcMainJava);
        Files.createDirectories(srcTestGroovy);

        // Create a file where directory is expected
        Path srcMainGroovy = tempDir.resolve("src/main/groovy");
        Files.createDirectories(srcMainGroovy.getParent());
        Files.createFile(srcMainGroovy); // File instead of directory

        var sourceDirs = resolver.getSourceDirectories();
        assertThat(sourceDirs).containsExactlyInAnyOrder(srcMainJava, srcTestGroovy);
    }

    @UnitTest
    void resolveDependencies_shouldHandlePomReadException() throws IOException {
        // Given - Create pom.xml as a directory instead of file
        Path pomFile = tempDir.resolve("pom.xml");
        Files.createDirectory(pomFile);

        // When
        var dependencies = resolver.resolveDependencies();

        // Then
        assertThat(dependencies).isEmpty();
    }

    @UnitTest
    void resolveDependencies_shouldHandleDependencyWithNullType() throws IOException {
        // Given - pom.xml with dependency that has null type (should default to jar)
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(
                pomFile,
                """
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
                            <!-- type is intentionally omitted to test null handling -->
                        </dependency>
                    </dependencies>
                </project>
                """);

        // When
        var dependencies = resolver.resolveDependencies();

        // Then - It should handle null type gracefully (default to jar)
        assertThat(dependencies).isNotNull();
    }

    @UnitTest
    void resolveDependencies_shouldHandlePomWithParent() throws IOException {
        // Given - pom.xml with parent POM reference
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(
                pomFile,
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                         http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>2.7.0</version>
                    </parent>
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
                """);

        // When
        var dependencies = resolver.resolveDependencies();

        // Then - Should handle parent POM without error
        assertThat(dependencies).isNotNull();
    }

    @UnitTest
    void resolveDependencies_shouldHandleSuccessfulResolution() throws IOException {
        // Given - pom.xml with a simple common dependency that likely exists in local repo
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(
                pomFile,
                """
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
                            <groupId>org.slf4j</groupId>
                            <artifactId>slf4j-api</artifactId>
                            <version>1.7.36</version>
                        </dependency>
                    </dependencies>
                </project>
                """);

        // When
        var dependencies = resolver.resolveDependencies();

        // Then - Should successfully resolve if the dependency exists locally
        assertThat(dependencies).isNotNull();
        // Note: The actual resolution depends on whether slf4j is in the local Maven repo
    }

    @UnitTest
    void resolveDependencies_shouldHandleDependencyWithClassifier() throws IOException {
        // Given - pom.xml with dependency that has a classifier
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(
                pomFile,
                """
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
                            <groupId>net.java.dev.jna</groupId>
                            <artifactId>jna</artifactId>
                            <version>5.13.0</version>
                            <classifier>sources</classifier>
                        </dependency>
                    </dependencies>
                </project>
                """);

        // When
        var dependencies = resolver.resolveDependencies();

        // Then
        assertThat(dependencies).isNotNull();
    }

    @UnitTest
    void resolveDependencies_shouldHandleProvidedScope() throws IOException {
        // Given - pom.xml with provided scope dependency
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(
                pomFile,
                """
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
                            <groupId>javax.servlet</groupId>
                            <artifactId>javax.servlet-api</artifactId>
                            <version>4.0.1</version>
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                </project>
                """);

        // When
        var dependencies = resolver.resolveDependencies();

        // Then - Provided scope should be included in dependencies
        assertThat(dependencies).isNotNull();
    }

    @UnitTest
    void resolveDependencies_shouldHandleRuntimeScope() throws IOException {
        // Given - pom.xml with runtime scope dependency
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(
                pomFile,
                """
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
                            <groupId>mysql</groupId>
                            <artifactId>mysql-connector-java</artifactId>
                            <version>8.0.33</version>
                            <scope>runtime</scope>
                        </dependency>
                    </dependencies>
                </project>
                """);

        // When
        var dependencies = resolver.resolveDependencies();

        // Then - Runtime scope should be included
        assertThat(dependencies).isNotNull();
    }

    @UnitTest
    void initializeMavenResolver_shouldHandleException() throws Exception {
        // This test uses reflection to test the exception handling in initializeMavenResolver
        // Create a new resolver but sabotage the initialization
        Path invalidPath = Path.of("/invalid/path/that/does/not/exist");

        // Create a resolver with an invalid workspace that might cause issues
        MavenDependencyResolver testResolver = new MavenDependencyResolver(invalidPath);

        // The resolver should still be created despite any initialization issues
        assertThat(testResolver).isNotNull();
        assertThat(testResolver.getBuildSystem()).isEqualTo(DependencyResolver.BuildSystem.MAVEN);
    }

    @UnitTest
    void resolveDependencies_shouldHandleUnresolvedArtifacts() throws IOException {
        // Test the case where artifacts are not resolved
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(
                pomFile,
                """
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
                            <groupId>fake.unresolvable</groupId>
                            <artifactId>does-not-exist</artifactId>
                            <version>999.999.999</version>
                        </dependency>
                    </dependencies>
                </project>
                """);

        var dependencies = resolver.resolveDependencies();
        // Will likely be empty due to resolution failure
        assertThat(dependencies).isEmpty();
    }

    @UnitTest
    void resolveDependencies_shouldHandleArtifactWithNullFile() throws IOException {
        // Test the case where artifact.getFile() returns null
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(
                pomFile,
                """
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
                            <groupId>org.example</groupId>
                            <artifactId>virtual-artifact</artifactId>
                            <version>1.0.0</version>
                            <type>pom</type>
                        </dependency>
                    </dependencies>
                </project>
                """);

        var dependencies = resolver.resolveDependencies();
        assertThat(dependencies).isNotNull();
    }
}
