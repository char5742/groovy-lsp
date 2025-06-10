package com.groovy.lsp.workspace.dependency.cache;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for DependencyCacheFactory.
 */
class DependencyCacheFactoryTest {

    @AfterEach
    void tearDown() {
        // Reset factory after each test
        DependencyCacheFactory.reset();
    }

    @Test
    void testGetInstance_returnsSameInstance() {
        DependencyCache cache1 = DependencyCacheFactory.getInstance();
        DependencyCache cache2 = DependencyCacheFactory.getInstance();

        assertNotNull(cache1);
        assertSame(cache1, cache2, "Should return the same singleton instance");
    }

    @Test
    void testGetInstance_createsLRUDependencyCache() {
        DependencyCache cache = DependencyCacheFactory.getInstance();

        assertInstanceOf(LRUDependencyCache.class, cache);
    }

    @Test
    void testReset_clearsInstance() {
        DependencyCache cache1 = DependencyCacheFactory.getInstance();
        assertNotNull(cache1);

        DependencyCacheFactory.reset();

        DependencyCache cache2 = DependencyCacheFactory.getInstance();
        assertNotNull(cache2);
        assertNotSame(cache1, cache2, "Should create new instance after reset");
    }

    @Test
    void testReset_invalidatesExistingCache() {
        DependencyCache cache = DependencyCacheFactory.getInstance();

        // Add some data to the cache
        cache.cacheDependencies(
                java.nio.file.Paths.get("/test"),
                java.util.List.of(java.nio.file.Paths.get("test.jar")));

        assertTrue(cache.getCachedDependencies(java.nio.file.Paths.get("/test")).isPresent());

        // Reset should invalidate the cache
        DependencyCacheFactory.reset();

        // The old cache instance should be cleared
        assertFalse(cache.getCachedDependencies(java.nio.file.Paths.get("/test")).isPresent());
    }

    @Test
    void testThreadSafety_multipleThreadsGetSameInstance() throws InterruptedException {
        final int threadCount = 10;
        DependencyCache[] caches = new DependencyCache[threadCount];
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] =
                    new Thread(
                            () -> {
                                caches[index] = DependencyCacheFactory.getInstance();
                            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // All threads should get the same instance
        DependencyCache firstCache = caches[0];
        for (int i = 1; i < threadCount; i++) {
            assertSame(firstCache, caches[i], "All threads should get the same instance");
        }
    }
}
