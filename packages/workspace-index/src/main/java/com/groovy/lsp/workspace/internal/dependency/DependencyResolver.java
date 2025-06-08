package com.groovy.lsp.workspace.internal.dependency;

import com.groovy.lsp.workspace.dependency.MavenAndGradleDependencyResolver;
import java.nio.file.Path;
import java.util.List;

/**
 * Resolves project dependencies using Gradle Tooling API or Maven.
 * Detects the build system and extracts classpath information.
 *
 * @deprecated Use {@link com.groovy.lsp.workspace.dependency.MavenAndGradleDependencyResolver} instead.
 *             This class now delegates to the new implementation for backward compatibility.
 */
@Deprecated
public class DependencyResolver {

    private final Path workspaceRoot;
    private com.groovy.lsp.workspace.dependency.DependencyResolver resolver;

    public DependencyResolver(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
        this.resolver = new MavenAndGradleDependencyResolver(workspaceRoot);
    }

    /**
     * Detect which build system is used in the workspace.
     */
    public BuildSystem detectBuildSystem() {
        // Re-create resolver to force re-detection for backward compatibility with tests
        com.groovy.lsp.workspace.dependency.DependencyResolver newResolver =
                new MavenAndGradleDependencyResolver(workspaceRoot);

        com.groovy.lsp.workspace.dependency.DependencyResolver.BuildSystem newBuildSystem =
                newResolver.getBuildSystem();

        // Update the internal resolver if build system changed
        if (newResolver.getBuildSystem() != resolver.getBuildSystem()) {
            this.resolver = newResolver;
        }

        return switch (newBuildSystem) {
            case GRADLE -> BuildSystem.GRADLE;
            case MAVEN -> BuildSystem.MAVEN;
            case NONE -> BuildSystem.NONE;
        };
    }

    /**
     * Resolve all project dependencies.
     * Returns a list of paths to dependency JARs and directories.
     */
    public List<Path> resolveDependencies() {
        return resolver.resolveDependencies();
    }

    /**
     * Get source directories for the project.
     */
    public List<Path> getSourceDirectories() {
        return resolver.getSourceDirectories();
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
        return detectBuildSystem();
    }
}

/* Old implementation removed - now using new dependency resolver classes
    private List<Path> resolveGradleDependencies() {
        // Check if running in test environment and skip Gradle connection
        if (isTestEnvironment()) {
            logger.info("Test environment detected, skipping Gradle dependency resolution");
            return Collections.emptyList();
        }

        List<Path> dependencies = new ArrayList<>();

        // Check if this is a valid Gradle project
        if (!isValidGradleProject()) {
            logger.debug(
                    "Not a valid Gradle project or missing gradle wrapper at: {}", workspaceRoot);
            return Collections.emptyList();
        }

        GradleConnector connector = GradleConnector.newConnector();
        connector.forProjectDirectory(workspaceRoot.toFile());

        // Use gradle wrapper if available, otherwise use gradle distribution
        if (hasGradleWrapper()) {
            logger.debug("Using Gradle wrapper for project at: {}", workspaceRoot);
        } else {
            logger.debug(
                    "No Gradle wrapper found, using Gradle distribution for project at: {}",
                    workspaceRoot);
            // Let Gradle Tooling API use its default distribution
        }

        ProjectConnection connection = null;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // Execute the entire Gradle operation with timeout
            Future<List<Path>> future =
                    executor.submit(
                            () -> {
                                ProjectConnection conn = null;
                                try {
                                    // Connect to Gradle project
                                    conn = connector.connect();
                                    logger.info(
                                            "Connected to Gradle project at: {}", workspaceRoot);

                                    // Get the IDEA model
                                    IdeaProject ideaProject = conn.getModel(IdeaProject.class);
                                    List<Path> deps = new ArrayList<>();

                                    // Extract dependencies from all modules
                                    ideaProject
                                            .getModules()
                                            .forEach(
                                                    module -> {
                                                        module.getDependencies()
                                                                .forEach(
                                                                        dep -> {
                                                                            // Check if it's a
                                                                            // single entry library
                                                                            // dependency
                                                                            if (dep
                                                                                    instanceof
                                                                                    IdeaSingleEntryLibraryDependency
                                                                                            libDep) {
                                                                                if (libDep.getFile()
                                                                                        != null) {
                                                                                    deps.add(
                                                                                            libDep.getFile()
                                                                                                    .toPath());
                                                                                    logger.debug(
                                                                                            "Found"
                                                                                                + " dependency:"
                                                                                                + " {}",
                                                                                            libDep
                                                                                                    .getFile());
                                                                                }
                                                                            }
                                                                        });
                                                    });

                                    // Also get compile classpath using a custom model
                                    try {
                                        var classpath = getGradleClasspath(conn);
                                        deps.addAll(classpath);
                                    } catch (Exception e) {
                                        logger.warn(
                                                "Failed to get additional classpath information",
                                                e);
                                    }

                                    return deps;
                                } finally {
                                    if (conn != null) {
                                        try {
                                            conn.close();
                                        } catch (Exception e) {
                                            logger.debug("Error closing Gradle connection", e);
                                        }
                                    }
                                }
                            });

            // Wait for completion with aggressive timeout
            dependencies = future.get(5, TimeUnit.SECONDS);

        } catch (TimeoutException e) {
            logger.warn(
                    "Timeout while resolving Gradle dependencies for project at {} (5 second"
                            + " limit): {}",
                    workspaceRoot,
                    e.getMessage());
            logger.debug(
                    "Gradle connection timeout - this may indicate an incomplete or invalid project"
                            + " structure");
            return Collections.emptyList();
        } catch (Exception e) {
            logger.warn(
                    "Failed to resolve Gradle dependencies for project at {}: {}",
                    workspaceRoot,
                    e.getMessage());
            logger.debug("Full stack trace for Gradle dependency resolution failure", e);
            // Return empty list instead of letting the exception bubble up
            return Collections.emptyList();
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

*/
