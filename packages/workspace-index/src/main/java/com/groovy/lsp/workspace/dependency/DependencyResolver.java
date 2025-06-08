package com.groovy.lsp.workspace.dependency;

import java.nio.file.Path;
import java.util.List;

/**
 * Interface for resolving project dependencies.
 * Implementations should handle specific build systems (Gradle, Maven, etc.).
 */
public interface DependencyResolver {

    /**
     * Resolve all project dependencies.
     * Returns a list of paths to dependency JARs and directories.
     *
     * @return list of dependency paths, never null
     */
    List<Path> resolveDependencies();

    /**
     * Get source directories for the project.
     *
     * @return list of source directory paths, never null
     */
    List<Path> getSourceDirectories();

    /**
     * Check if this resolver can handle the given project.
     *
     * @param workspaceRoot the root directory of the workspace
     * @return true if this resolver can handle the project
     */
    boolean canHandle(Path workspaceRoot);

    /**
     * Get the build system type this resolver handles.
     *
     * @return the build system type
     */
    BuildSystem getBuildSystem();

    /**
     * Supported build systems.
     */
    enum BuildSystem {
        GRADLE,
        MAVEN,
        NONE
    }
}
