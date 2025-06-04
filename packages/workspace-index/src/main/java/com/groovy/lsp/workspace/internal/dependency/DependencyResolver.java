package com.groovy.lsp.workspace.internal.dependency;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves project dependencies using Gradle Tooling API or Maven.
 * Detects the build system and extracts classpath information.
 */
public class DependencyResolver {
    private static final Logger logger = LoggerFactory.getLogger(DependencyResolver.class);

    private final Path workspaceRoot;
    private BuildSystem buildSystem = BuildSystem.NONE;

    public DependencyResolver(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
    }

    /**
     * Detect which build system is used in the workspace.
     */
    public BuildSystem detectBuildSystem() {
        if (Files.exists(workspaceRoot.resolve("build.gradle"))
                || Files.exists(workspaceRoot.resolve("build.gradle.kts"))
                || Files.exists(workspaceRoot.resolve("settings.gradle"))
                || Files.exists(workspaceRoot.resolve("settings.gradle.kts"))) {
            buildSystem = BuildSystem.GRADLE;
            logger.info("Detected Gradle build system");
        } else if (Files.exists(workspaceRoot.resolve("pom.xml"))) {
            buildSystem = BuildSystem.MAVEN;
            logger.info("Detected Maven build system");
        } else {
            buildSystem = BuildSystem.NONE;
            logger.info("No build system detected");
        }
        return buildSystem;
    }

    /**
     * Resolve all project dependencies.
     * Returns a list of paths to dependency JARs and directories.
     */
    public List<Path> resolveDependencies() {
        return switch (buildSystem) {
            case GRADLE -> resolveGradleDependencies();
            case MAVEN -> resolveMavenDependencies();
            case NONE -> Collections.emptyList();
        };
    }

    /**
     * Resolve dependencies using Gradle Tooling API.
     */
    private List<Path> resolveGradleDependencies() {
        List<Path> dependencies = new ArrayList<>();

        GradleConnector connector = GradleConnector.newConnector();
        connector.forProjectDirectory(workspaceRoot.toFile());

        try (ProjectConnection connection = connector.connect()) {
            logger.info("Connected to Gradle project at: {}", workspaceRoot);

            // Get the IDEA model which includes dependency information
            IdeaProject ideaProject = connection.getModel(IdeaProject.class);

            // Extract dependencies from all modules
            ideaProject
                    .getModules()
                    .forEach(
                            module -> {
                                module.getDependencies()
                                        .forEach(
                                                dep -> {
                                                    // Check if it's a single entry library
                                                    // dependency
                                                    if (dep
                                                            instanceof
                                                            IdeaSingleEntryLibraryDependency
                                                                    libDep) {
                                                        if (libDep.getFile() != null) {
                                                            dependencies.add(
                                                                    libDep.getFile().toPath());
                                                            logger.debug(
                                                                    "Found dependency: {}",
                                                                    libDep.getFile());
                                                        }
                                                    }
                                                });
                            });

            // Also get compile classpath using a custom model
            try {
                var classpath = getGradleClasspath(connection);
                dependencies.addAll(classpath);
            } catch (Exception e) {
                logger.warn("Failed to get additional classpath information", e);
            }

        } catch (Exception e) {
            logger.error("Failed to resolve Gradle dependencies", e);
        }

        return dependencies.stream().distinct().filter(Files::exists).collect(Collectors.toList());
    }

    /**
     * Get classpath from Gradle using custom tooling model.
     */
    private List<Path> getGradleClasspath(ProjectConnection connection) {
        List<Path> classpath = new ArrayList<>();

        try {
            // Execute a task to get the runtime classpath
            // Note: run() returns void, not a result
            connection
                    .newBuild()
                    .forTasks("dependencies")
                    .withArguments("--configuration", "compileClasspath")
                    .run();

            // Parse the output to extract JAR paths
            // This is a simplified approach; in production, use a custom tooling model

        } catch (Exception e) {
            logger.debug("Could not retrieve classpath via task execution", e);
        }

        return classpath;
    }

    /**
     * Resolve dependencies from Maven pom.xml.
     */
    private List<Path> resolveMavenDependencies() {
        List<Path> dependencies = new ArrayList<>();

        // TODO: Implement Maven dependency resolution
        // Options:
        // 1. Use Maven Invoker API
        // 2. Parse pom.xml and resolve from local repository
        // 3. Execute mvn command and parse output

        logger.warn("Maven dependency resolution not yet implemented");

        return dependencies;
    }

    /**
     * Get source directories for the project.
     */
    public List<Path> getSourceDirectories() {
        return switch (buildSystem) {
            case GRADLE -> getGradleSourceDirectories();
            case MAVEN -> getMavenSourceDirectories();
            case NONE -> getDefaultSourceDirectories();
        };
    }

    private List<Path> getGradleSourceDirectories() {
        List<Path> sourceDirs = new ArrayList<>();

        // Common Gradle source directories
        addIfExists(sourceDirs, "src/main/groovy");
        addIfExists(sourceDirs, "src/main/java");
        addIfExists(sourceDirs, "src/test/groovy");
        addIfExists(sourceDirs, "src/test/java");

        // TODO: Parse build.gradle to find custom source sets

        return sourceDirs;
    }

    private List<Path> getMavenSourceDirectories() {
        List<Path> sourceDirs = new ArrayList<>();

        // Standard Maven layout
        addIfExists(sourceDirs, "src/main/groovy");
        addIfExists(sourceDirs, "src/main/java");
        addIfExists(sourceDirs, "src/test/groovy");
        addIfExists(sourceDirs, "src/test/java");

        return sourceDirs;
    }

    private List<Path> getDefaultSourceDirectories() {
        List<Path> sourceDirs = new ArrayList<>();

        // Just check common patterns
        addIfExists(sourceDirs, "src");
        addIfExists(sourceDirs, "groovy");
        addIfExists(sourceDirs, "java");

        return sourceDirs;
    }

    private void addIfExists(List<Path> list, String relativePath) {
        Path path = workspaceRoot.resolve(relativePath);
        if (Files.exists(path) && Files.isDirectory(path)) {
            list.add(path);
        }
    }

    /**
     * Supported build systems.
     */
    public enum BuildSystem {
        GRADLE,
        MAVEN,
        NONE
    }

    public BuildSystem getBuildSystem() {
        return buildSystem;
    }
}
