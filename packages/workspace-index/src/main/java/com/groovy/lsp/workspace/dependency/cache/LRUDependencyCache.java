package com.groovy.lsp.workspace.dependency.cache;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jmolecules.ddd.annotation.Entity;

/**
 * LRU-based implementation of DependencyCache with memory pressure handling.
 * Uses weak references to allow garbage collection under memory pressure.
 */
@Entity
public class LRUDependencyCache implements DependencyCache {
    private static final Logger LOGGER = Logger.getLogger(LRUDependencyCache.class.getName());

    private final Map<String, WeakReference<URLClassLoader>> classLoaderCache =
            new ConcurrentHashMap<>();
    private final Map<Path, CachedDependencies> dependencyCache = new ConcurrentHashMap<>();
    private final LinkedHashMap<String, Long> accessOrder = new LinkedHashMap<>(16, 0.75f, true);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final DefaultCacheStatistics statistics = new DefaultCacheStatistics();

    private static final long MAX_CACHE_SIZE = 100; // Maximum number of ClassLoaders
    private static final long MEMORY_CHECK_INTERVAL = 60_000; // 1 minute
    private volatile long lastMemoryCheck = System.currentTimeMillis();

    @Override
    public URLClassLoader getOrCreateClassLoader(Set<Path> dependencies) {
        String key = generateKey(dependencies);
        return getOrCreateClassLoader(key, dependencies);
    }

    @Override
    public URLClassLoader getOrCreateClassLoader(String key, Set<Path> dependencies) {
        lock.readLock().lock();
        try {
            WeakReference<URLClassLoader> ref = classLoaderCache.get(key);
            if (ref != null) {
                URLClassLoader loader = ref.get();
                if (loader != null) {
                    statistics.recordHit();
                    updateAccessOrder(key);
                    return loader;
                } else {
                    // WeakReference was collected
                    classLoaderCache.remove(key);
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        // Cache miss - create new ClassLoader
        lock.writeLock().lock();
        try {
            // Double-check after acquiring write lock
            WeakReference<URLClassLoader> ref = classLoaderCache.get(key);
            if (ref != null) {
                URLClassLoader loader = ref.get();
                if (loader != null) {
                    statistics.recordHit();
                    updateAccessOrder(key);
                    return loader;
                }
            }

            statistics.recordMiss();
            URLClassLoader loader = createClassLoader(dependencies);
            classLoaderCache.put(key, new WeakReference<>(loader));
            updateAccessOrder(key);

            // Check if eviction is needed
            if (accessOrder.size() > MAX_CACHE_SIZE) {
                evictOldest();
            }

            updateStatistics();
            return loader;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void cacheDependencies(Path projectPath, List<Path> dependencies) {
        dependencyCache.put(projectPath, new CachedDependencies(dependencies));
        updateStatistics();
    }

    @Override
    public Optional<List<Path>> getCachedDependencies(Path projectPath) {
        CachedDependencies cached = dependencyCache.get(projectPath);
        if (cached != null && !cached.isExpired()) {
            return Optional.of(cached.dependencies);
        }
        return Optional.empty();
    }

    @Override
    public void invalidateProject(Path projectPath) {
        dependencyCache.remove(projectPath);
        // Also remove any ClassLoaders associated with this project
        lock.writeLock().lock();
        try {
            classLoaderCache
                    .entrySet()
                    .removeIf(entry -> entry.getKey().contains(projectPath.toString()));
            updateStatistics();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void invalidateAll() {
        lock.writeLock().lock();
        try {
            // Close all ClassLoaders before clearing
            classLoaderCache
                    .values()
                    .forEach(
                            ref -> {
                                URLClassLoader loader = ref.get();
                                if (loader != null) {
                                    try {
                                        loader.close();
                                    } catch (Exception e) {
                                        LOGGER.log(Level.WARNING, "Failed to close ClassLoader", e);
                                    }
                                }
                            });
            classLoaderCache.clear();
            dependencyCache.clear();
            accessOrder.clear();
            updateStatistics();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public CacheStatistics getStatistics() {
        return statistics;
    }

    @Override
    public void evictIfNeeded(long targetMemoryMB) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMemoryCheck < MEMORY_CHECK_INTERVAL) {
            return; // Don't check too frequently
        }
        lastMemoryCheck = currentTime;

        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);

        if (usedMemory > targetMemoryMB) {
            lock.writeLock().lock();
            try {
                // Evict half of the cache
                int toEvict = accessOrder.size() / 2;
                Iterator<Map.Entry<String, Long>> iterator = accessOrder.entrySet().iterator();
                while (iterator.hasNext() && toEvict > 0) {
                    Map.Entry<String, Long> entry = iterator.next();
                    classLoaderCache.remove(entry.getKey());
                    iterator.remove();
                    statistics.recordEviction();
                    toEvict--;
                }

                // Force garbage collection
                System.gc();
                updateStatistics();
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    private String generateKey(Set<Path> dependencies) {
        // Create a stable key by sorting paths
        List<Path> sorted = new ArrayList<>(dependencies);
        sorted.sort(Comparator.comparing(Path::toString));
        return sorted.toString();
    }

    private URLClassLoader createClassLoader(Set<Path> dependencies) {
        try {
            URL[] urls =
                    dependencies.stream()
                            .map(
                                    path -> {
                                        try {
                                            return path.toUri().toURL();
                                        } catch (MalformedURLException e) {
                                            LOGGER.log(
                                                    Level.WARNING,
                                                    "Invalid dependency path: " + path,
                                                    e);
                                            return null;
                                        }
                                    })
                            .filter(Objects::nonNull)
                            .toArray(URL[]::new);

            return new URLClassLoader(urls, getClass().getClassLoader());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ClassLoader", e);
        }
    }

    private void updateAccessOrder(String key) {
        accessOrder.put(key, System.currentTimeMillis());
    }

    private void evictOldest() {
        Iterator<Map.Entry<String, Long>> iterator = accessOrder.entrySet().iterator();
        if (iterator.hasNext()) {
            Map.Entry<String, Long> oldest = iterator.next();
            classLoaderCache.remove(oldest.getKey());
            iterator.remove();
            statistics.recordEviction();
        }
    }

    private void updateStatistics() {
        statistics.updateClassLoaderCount(classLoaderCache.size());
        statistics.updateDependencyCacheSize(dependencyCache.size());

        // Estimate memory usage
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        statistics.updateMemoryUsage(usedMemory);
    }

    /**
     * Internal class to hold cached dependencies with expiration.
     */
    private static class CachedDependencies {
        private static final long CACHE_DURATION = 60 * 60 * 1000; // 1 hour

        private final List<Path> dependencies;
        private final long timestamp;

        CachedDependencies(List<Path> dependencies) {
            this.dependencies = new ArrayList<>(dependencies);
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION;
        }
    }
}
