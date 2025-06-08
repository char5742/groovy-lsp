package com.groovy.lsp.workspace.dependency.cache;

import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Cache interface for managing shared ClassLoaders and dependencies across CompilationUnits.
 * This cache improves performance by reusing ClassLoaders and avoiding redundant dependency resolution.
 */
public interface DependencyCache {

    /**
     * Get or create a ClassLoader for the given set of dependency paths.
     * If a ClassLoader already exists for this exact set of dependencies, it will be reused.
     *
     * @param dependencies the set of dependency paths (JARs and directories)
     * @return a shared ClassLoader for the dependencies
     */
    URLClassLoader getOrCreateClassLoader(Set<Path> dependencies);

    /**
     * Get or create a ClassLoader for the given dependency key.
     * This method allows caching by a logical key (e.g., "maven:com.example:lib:1.0.0").
     *
     * @param key the dependency key
     * @param dependencies the set of dependency paths if creation is needed
     * @return a shared ClassLoader for the dependencies
     */
    URLClassLoader getOrCreateClassLoader(String key, Set<Path> dependencies);

    /**
     * Cache resolved dependencies for a project.
     *
     * @param projectPath the project root path
     * @param dependencies the resolved dependency paths
     */
    void cacheDependencies(Path projectPath, List<Path> dependencies);

    /**
     * Get cached dependencies for a project.
     *
     * @param projectPath the project root path
     * @return cached dependencies if available
     */
    Optional<List<Path>> getCachedDependencies(Path projectPath);

    /**
     * Invalidate cache entries for a specific project.
     *
     * @param projectPath the project root path
     */
    void invalidateProject(Path projectPath);

    /**
     * Invalidate all cache entries.
     */
    void invalidateAll();

    /**
     * Get current cache statistics.
     *
     * @return cache statistics
     */
    CacheStatistics getStatistics();

    /**
     * Evict least recently used entries if memory pressure is detected.
     * This method should be called periodically or when memory is low.
     *
     * @param targetMemoryMB target memory usage in megabytes
     */
    void evictIfNeeded(long targetMemoryMB);

    /**
     * Cache statistics interface.
     */
    interface CacheStatistics {
        long getHitCount();

        long getMissCount();

        long getEvictionCount();

        long getTotalMemoryUsageMB();

        int getClassLoaderCount();

        int getDependencyCacheSize();
    }
}
