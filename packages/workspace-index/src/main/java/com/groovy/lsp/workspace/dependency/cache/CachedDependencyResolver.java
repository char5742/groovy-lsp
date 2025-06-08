package com.groovy.lsp.workspace.dependency.cache;

import com.groovy.lsp.workspace.dependency.DependencyResolver;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Decorator that adds caching capabilities to any DependencyResolver.
 * This class wraps an existing resolver and caches its results.
 */
public class CachedDependencyResolver implements DependencyResolver {
    private static final Logger LOGGER = Logger.getLogger(CachedDependencyResolver.class.getName());

    private final DependencyResolver delegate;
    private final DependencyCache cache;
    private final Path workspaceRoot;

    public CachedDependencyResolver(
            DependencyResolver delegate, DependencyCache cache, Path workspaceRoot) {
        this.delegate = delegate;
        this.cache = cache;
        this.workspaceRoot = workspaceRoot;
    }

    @Override
    public List<Path> resolveDependencies() {
        // Check cache first
        Optional<List<Path>> cached = cache.getCachedDependencies(workspaceRoot);
        if (cached.isPresent()) {
            LOGGER.log(Level.FINE, "Using cached dependencies for: {0}", workspaceRoot);
            return cached.get();
        }

        // Cache miss - resolve dependencies
        LOGGER.log(Level.INFO, "Resolving dependencies for: {0}", workspaceRoot);
        List<Path> dependencies = delegate.resolveDependencies();

        // Cache the result
        cache.cacheDependencies(workspaceRoot, dependencies);

        return dependencies;
    }

    @Override
    public List<Path> getSourceDirectories() {
        // Source directories are usually fast to compute, so we don't cache them
        return delegate.getSourceDirectories();
    }

    @Override
    public boolean canHandle(Path workspaceRoot) {
        return delegate.canHandle(workspaceRoot);
    }

    @Override
    public BuildSystem getBuildSystem() {
        return delegate.getBuildSystem();
    }

    /**
     * Invalidate the cache for this resolver's workspace.
     */
    public void invalidateCache() {
        cache.invalidateProject(workspaceRoot);
    }

    /**
     * Get the underlying delegate resolver.
     */
    public DependencyResolver getDelegate() {
        return delegate;
    }

    /**
     * Get cache statistics.
     */
    public DependencyCache.CacheStatistics getCacheStatistics() {
        return cache.getStatistics();
    }
}
