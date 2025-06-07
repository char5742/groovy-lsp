package com.groovy.lsp.workspace.dependency;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dependency resolver that supports both Maven and Gradle projects.
 * Automatically detects the build system and delegates to the appropriate resolver.
 */
public class MavenAndGradleDependencyResolver implements DependencyResolver {
    private static final Logger logger =
            LoggerFactory.getLogger(MavenAndGradleDependencyResolver.class);

    private final Path workspaceRoot;
    private @Nullable DependencyResolver activeResolver;
    private BuildSystem detectedBuildSystem = BuildSystem.NONE;

    public MavenAndGradleDependencyResolver(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
        detectAndInitializeResolver();
    }

    @Override
    public boolean canHandle(Path workspaceRoot) {
        // This resolver can handle any project that Maven or Gradle can handle
        return new GradleDependencyResolver(workspaceRoot).canHandle(workspaceRoot)
                || new MavenDependencyResolver(workspaceRoot).canHandle(workspaceRoot);
    }

    @Override
    public BuildSystem getBuildSystem() {
        return detectedBuildSystem;
    }

    @Override
    public List<Path> resolveDependencies() {
        if (activeResolver == null) {
            logger.warn("No build system detected at: {}", workspaceRoot);
            return Collections.emptyList();
        }

        try {
            return activeResolver.resolveDependencies();
        } catch (Exception e) {
            logger.error("Failed to resolve dependencies using {}", detectedBuildSystem, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Path> getSourceDirectories() {
        if (activeResolver == null) {
            // Return default source directories if no build system is detected
            return getDefaultSourceDirectories();
        }

        return activeResolver.getSourceDirectories();
    }

    /**
     * Detect which build system is used and initialize the appropriate resolver.
     */
    private void detectAndInitializeResolver() {
        // Check for Gradle first (as it's more common in Groovy projects)
        GradleDependencyResolver gradleResolver = new GradleDependencyResolver(workspaceRoot);
        if (gradleResolver.canHandle(workspaceRoot)) {
            activeResolver = gradleResolver;
            detectedBuildSystem = BuildSystem.GRADLE;
            logger.info("Detected Gradle build system at: {}", workspaceRoot);
            return;
        }

        // Check for Maven
        MavenDependencyResolver mavenResolver = new MavenDependencyResolver(workspaceRoot);
        if (mavenResolver.canHandle(workspaceRoot)) {
            activeResolver = mavenResolver;
            detectedBuildSystem = BuildSystem.MAVEN;
            logger.info("Detected Maven build system at: {}", workspaceRoot);
            return;
        }

        // No build system detected
        detectedBuildSystem = BuildSystem.NONE;
        logger.info("No build system detected at: {}", workspaceRoot);
    }

    /**
     * Get default source directories when no build system is detected.
     */
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
}
