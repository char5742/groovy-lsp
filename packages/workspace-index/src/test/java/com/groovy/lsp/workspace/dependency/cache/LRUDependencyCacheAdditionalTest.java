package com.groovy.lsp.workspace.dependency.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.groovy.lsp.test.annotations.UnitTest;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;

/**
 * Additional unit tests for LRUDependencyCache to improve branch coverage.
 */
class LRUDependencyCacheAdditionalTest {

    private LRUDependencyCache cache;

    @BeforeEach
    void setUp() {
        cache = new LRUDependencyCache();
    }

    @UnitTest
    void testEvictionWhenCacheSizeExceeded() {
        // Force eviction by adding more than MAX_CACHE_SIZE (100) entries
        // Add 101 different classloaders
        for (int i = 0; i < 101; i++) {
            Set<Path> deps = Set.of(Paths.get("lib" + i + ".jar"));
            cache.getOrCreateClassLoader(deps);
        }

        // The cache should have evicted the oldest entry
        var stats = cache.getStatistics();
        assertTrue(stats.getEvictionCount() > 0, "Should have evicted at least one entry");
    }

    @UnitTest
    void testWeakReferenceCollection() throws InterruptedException {
        // Create a classloader that can be garbage collected
        Set<Path> deps = Set.of(Paths.get("gctest.jar"));
        cache.getOrCreateClassLoader(deps);

        // Force garbage collection
        System.gc();
        Thread.sleep(100);
        System.gc();

        // Access the same dependencies again
        cache.getOrCreateClassLoader(deps);

        // This should be a cache miss since the weak reference was collected
        var stats = cache.getStatistics();
        assertTrue(stats.getMissCount() >= 2, "Should have at least 2 misses");
    }

    @UnitTest
    void testConcurrentAccess() throws Exception {
        Set<Path> deps = Set.of(Paths.get("concurrent.jar"));

        // Run multiple threads accessing the same dependencies
        List<CompletableFuture<URLClassLoader>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> cache.getOrCreateClassLoader(deps)));
        }

        // Wait for all threads to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);

        // All threads should get the same classloader instance
        URLClassLoader firstLoader = futures.get(0).get();
        for (CompletableFuture<URLClassLoader> future : futures) {
            assertSame(firstLoader, future.get(), "All threads should get the same instance");
        }
    }

    @UnitTest
    void testCacheDependenciesWithExpiration() throws InterruptedException {
        Path projectPath = Paths.get("/test/project");
        List<Path> deps = List.of(Paths.get("dep1.jar"), Paths.get("dep2.jar"));

        // Cache dependencies
        cache.cacheDependencies(projectPath, deps);

        // Get cached dependencies
        Optional<List<Path>> cached = cache.getCachedDependencies(projectPath);
        assertTrue(cached.isPresent(), "Dependencies should be cached");
        assertEquals(deps, cached.get());
    }

    @UnitTest
    void testInvalidateCacheForProject() {
        Path projectPath = Paths.get("/test/project");
        List<Path> deps = List.of(Paths.get("dep1.jar"));

        // Cache dependencies
        cache.cacheDependencies(projectPath, deps);

        // Invalidate cache
        cache.invalidateProject(projectPath);

        // Should no longer be cached
        Optional<List<Path>> cached = cache.getCachedDependencies(projectPath);
        assertFalse(cached.isPresent(), "Dependencies should be invalidated");
    }

    @UnitTest
    void testClearCache() {
        // Add multiple entries
        for (int i = 0; i < 5; i++) {
            Set<Path> deps = Set.of(Paths.get("lib" + i + ".jar"));
            cache.getOrCreateClassLoader(deps);

            Path projectPath = Paths.get("/project" + i);
            cache.cacheDependencies(projectPath, List.of(Paths.get("dep" + i + ".jar")));
        }

        // Clear cache
        cache.invalidateAll();

        // Statistics should reflect cleared state
        var stats = cache.getStatistics();
        assertEquals(0, stats.getClassLoaderCount(), "ClassLoader count should be 0");
        assertEquals(0, stats.getDependencyCacheSize(), "Dependency cache size should be 0");
    }

    @UnitTest
    void testGetOrCreateClassLoaderWithKey() {
        String customKey = "custom-key";
        Set<Path> deps = Set.of(Paths.get("custom.jar"));

        // Create with custom key
        URLClassLoader loader1 = cache.getOrCreateClassLoader(customKey, deps);

        // Get with same key should return same instance
        URLClassLoader loader2 = cache.getOrCreateClassLoader(customKey, deps);

        assertSame(loader1, loader2, "Should return same instance for same key");

        var stats = cache.getStatistics();
        assertEquals(1, stats.getHitCount());
    }

    @UnitTest
    void testEmptyDependencies() {
        Set<Path> emptyDeps = Set.of();

        URLClassLoader loader = cache.getOrCreateClassLoader(emptyDeps);
        assertNotNull(loader, "Should handle empty dependencies");
    }

    @UnitTest
    void testLargeDependencySet() {
        // Test with a large number of dependencies
        Set<Path> largeDeps = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            largeDeps.add(Paths.get("lib" + i + ".jar"));
        }

        URLClassLoader loader = cache.getOrCreateClassLoader(largeDeps);
        assertNotNull(loader, "Should handle large dependency sets");
    }

    @UnitTest
    @Timeout(10)
    void testMemoryCheckInterval() throws InterruptedException {
        // Add entries to trigger memory check
        for (int i = 0; i < 10; i++) {
            Set<Path> deps = Set.of(Paths.get("memtest" + i + ".jar"));
            cache.getOrCreateClassLoader(deps);
            Thread.sleep(10);
        }

        var stats = cache.getStatistics();
        assertTrue(stats.getTotalMemoryUsageMB() >= 0, "Memory usage should be tracked");
    }

    @UnitTest
    void testCachedDependenciesExpiration() throws InterruptedException {
        Path projectPath = Paths.get("/expiring/project");
        List<Path> deps = List.of(Paths.get("exp.jar"));

        // Cache with default expiration
        cache.cacheDependencies(projectPath, deps);

        // Should be present immediately
        assertTrue(cache.getCachedDependencies(projectPath).isPresent());

        // Note: Full expiration test would require waiting for actual expiration time
        // which is not practical in unit tests
    }

    @UnitTest
    void testMultipleProjectCaching() {
        Map<Path, List<Path>> projectDeps = new HashMap<>();

        // Cache dependencies for multiple projects
        for (int i = 0; i < 5; i++) {
            Path project = Paths.get("/project" + i);
            List<Path> deps = List.of(Paths.get("dep" + i + ".jar"), Paths.get("lib" + i + ".jar"));
            projectDeps.put(project, deps);
            cache.cacheDependencies(project, deps);
        }

        // Verify all are cached
        for (Map.Entry<Path, List<Path>> entry : projectDeps.entrySet()) {
            Optional<List<Path>> cached = cache.getCachedDependencies(entry.getKey());
            assertTrue(cached.isPresent());
            assertEquals(entry.getValue(), cached.get());
        }
    }

    @UnitTest
    void testEvictIfNeededWithHighMemoryUsage() throws Exception {
        // Test evictIfNeeded when memory usage is high
        // First, set lastMemoryCheck to past time to bypass time check
        Field lastMemoryCheckField = LRUDependencyCache.class.getDeclaredField("lastMemoryCheck");
        lastMemoryCheckField.setAccessible(true);
        lastMemoryCheckField.setLong(cache, System.currentTimeMillis() - 120000); // 2 minutes ago

        // Add multiple classloaders
        for (int i = 0; i < 10; i++) {
            Set<Path> deps = Set.of(Paths.get("mem" + i + ".jar"));
            cache.getOrCreateClassLoader(deps);
        }

        // Call evictIfNeeded with very low target memory to trigger eviction
        cache.evictIfNeeded(1); // 1MB target - should trigger eviction

        var stats = cache.getStatistics();
        assertTrue(stats.getEvictionCount() > 0, "Should have evicted entries");
    }

    @UnitTest
    void testEvictIfNeededWithinTimeInterval() {
        // Test that evictIfNeeded returns early when called too frequently
        cache.evictIfNeeded(100);

        // Second call should return early without eviction
        var statsBefore = cache.getStatistics();
        cache.evictIfNeeded(1); // Even with low memory target
        var statsAfter = cache.getStatistics();

        assertEquals(
                statsBefore.getEvictionCount(),
                statsAfter.getEvictionCount(),
                "Should not evict when called within time interval");
    }

    @UnitTest
    void testInvalidateProjectWithMatchingClassLoaders() {
        Path projectPath = Paths.get("/test/project");

        // Create classloaders with keys containing the project path
        String key1 = "deps-for-" + projectPath.toString();
        String key2 = "other-deps";
        String key3 = projectPath.toString() + "-dependencies";

        cache.getOrCreateClassLoader(key1, Set.of(Paths.get("dep1.jar")));
        cache.getOrCreateClassLoader(key2, Set.of(Paths.get("dep2.jar")));
        cache.getOrCreateClassLoader(key3, Set.of(Paths.get("dep3.jar")));

        // Invalidate project
        cache.invalidateProject(projectPath);

        // key1 and key3 should be removed, key2 should remain
        var statsBefore = cache.getStatistics();
        long missCountBefore = statsBefore.getMissCount();
        long hitCountBefore = statsBefore.getHitCount();

        // key2 should still be in cache (hit)
        URLClassLoader loader2 = cache.getOrCreateClassLoader(key2, Set.of(Paths.get("dep2.jar")));
        assertNotNull(loader2);

        // key1 and key3 should be removed (misses)
        cache.getOrCreateClassLoader(key1, Set.of(Paths.get("dep1.jar")));
        cache.getOrCreateClassLoader(key3, Set.of(Paths.get("dep3.jar")));

        var statsAfter = cache.getStatistics();

        // We should have 1 hit (key2) and 2 misses (key1 and key3)
        assertEquals(hitCountBefore + 1, statsAfter.getHitCount(), "Should have one hit for key2");
        assertEquals(
                missCountBefore + 2,
                statsAfter.getMissCount(),
                "Should have two misses for removed entries");
    }

    @UnitTest
    void testGetCachedDependenciesWithExpiredEntry() throws Exception {
        Path projectPath = Paths.get("/expired/project");
        List<Path> deps = List.of(Paths.get("expired.jar"));

        // Use reflection to create an expired entry
        cache.cacheDependencies(projectPath, deps);

        // Access private dependencyCache field
        Field dependencyCacheField = LRUDependencyCache.class.getDeclaredField("dependencyCache");
        dependencyCacheField.setAccessible(true);
        Map<Path, Object> dependencyCache = (Map<Path, Object>) dependencyCacheField.get(cache);

        // Get the CachedDependencies object and modify its timestamp
        Object cachedDeps = dependencyCache.get(projectPath);
        if (cachedDeps != null) {
            Field timestampField = cachedDeps.getClass().getDeclaredField("timestamp");
            timestampField.setAccessible(true);
            // Set timestamp to very old time (more than 24 hours ago)
            timestampField.setLong(cachedDeps, System.currentTimeMillis() - 25 * 60 * 60 * 1000);
        }

        // Should return empty due to expiration
        Optional<List<Path>> result = cache.getCachedDependencies(projectPath);
        assertFalse(result.isPresent(), "Should not return expired dependencies");
    }

    @UnitTest
    void testEvictOldestWithEmptyCache() throws Exception {
        // Access private evictOldest method
        var evictOldestMethod = LRUDependencyCache.class.getDeclaredMethod("evictOldest");
        evictOldestMethod.setAccessible(true);

        // Should handle empty cache gracefully
        evictOldestMethod.invoke(cache);

        var stats = cache.getStatistics();
        assertEquals(0, stats.getEvictionCount(), "Should not evict from empty cache");
    }

    @UnitTest
    void testCreateClassLoaderWithInvalidPath() {
        // Test with a path that cannot be converted to URL
        Set<Path> invalidDeps = Set.of(Paths.get("::invalid::path::"));

        // Should handle the exception and still create a classloader
        URLClassLoader loader = cache.getOrCreateClassLoader(invalidDeps);
        assertNotNull(loader, "Should create classloader even with invalid paths");
    }

    @UnitTest
    void testDoubleCheckLockingInGetOrCreateClassLoader() throws Exception {
        String key = "concurrent-test-key";
        Set<Path> deps = Set.of(Paths.get("concurrent.jar"));

        // Create a situation where multiple threads try to create the same classloader
        List<CompletableFuture<URLClassLoader>> futures = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            futures.add(
                    CompletableFuture.supplyAsync(
                            () -> {
                                // Small delay to increase chance of concurrent access
                                try {
                                    Thread.sleep(10);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                                return cache.getOrCreateClassLoader(key, deps);
                            }));
        }

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);

        // All should get the same instance
        URLClassLoader firstLoader = futures.get(0).get();
        for (CompletableFuture<URLClassLoader> future : futures) {
            assertSame(firstLoader, future.get(), "All threads should get the same instance");
        }

        // Should have some hits due to double-check locking
        var stats = cache.getStatistics();
        assertTrue(stats.getHitCount() > 0, "Should have cache hits from concurrent access");
    }

    @UnitTest
    void testInvalidateAllWithNullClassLoader() throws Exception {
        // Add a classloader and then make it null via weak reference
        Set<Path> deps = Set.of(Paths.get("nulltest.jar"));
        cache.getOrCreateClassLoader(deps);

        // Access the internal cache to manipulate weak reference
        Field classLoaderCacheField = LRUDependencyCache.class.getDeclaredField("classLoaderCache");
        classLoaderCacheField.setAccessible(true);
        Map<String, WeakReference<URLClassLoader>> classLoaderCache =
                (Map<String, WeakReference<URLClassLoader>>) classLoaderCacheField.get(cache);

        // Replace with a weak reference that returns null
        classLoaderCache.put("test-null", new WeakReference<>(null));

        // Should handle null classloader gracefully
        cache.invalidateAll();

        // Verify no exceptions and cache is cleared
        assertEquals(0, cache.getStatistics().getClassLoaderCount());
    }
}
