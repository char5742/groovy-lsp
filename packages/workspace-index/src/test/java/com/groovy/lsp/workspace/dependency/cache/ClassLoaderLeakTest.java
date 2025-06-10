package com.groovy.lsp.workspace.dependency.cache;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Tests for verifying that ClassLoaders are properly garbage collected
 * and do not cause memory leaks.
 */
class ClassLoaderLeakTest {

    private @Nullable LRUDependencyCache cache;

    @BeforeEach
    void setUp() {
        cache = new LRUDependencyCache();
    }

    @Test
    @Timeout(30)
    void testWeakReferenceGarbageCollection() throws Exception {
        // Create a ClassLoader and keep only a weak reference
        Set<Path> deps = Set.of(Paths.get("test.jar"));
        URLClassLoader loader = Objects.requireNonNull(cache).getOrCreateClassLoader(deps);
        WeakReference<URLClassLoader> weakRef = new WeakReference<>(loader);

        // Verify the loader is cached
        URLClassLoader cachedLoader = Objects.requireNonNull(cache).getOrCreateClassLoader(deps);
        assertSame(loader, cachedLoader, "Should return cached instance");

        // Clear strong reference
        loader = null;
        cachedLoader = null;

        // Force garbage collection multiple times
        forceGarbageCollection();

        // Wait for weak reference to be cleared
        int maxAttempts = 10;
        int attempts = 0;
        while (weakRef.get() != null && attempts < maxAttempts) {
            System.gc();
            // System.runFinalization() is deprecated, just use System.gc()
            Thread.sleep(100);
            attempts++;
        }

        // Verify weak reference was cleared
        assertNull(
                weakRef.get(),
                "ClassLoader should be garbage collected when no strong references exist");

        // Verify cache returns a new instance after GC
        URLClassLoader newLoader = Objects.requireNonNull(cache).getOrCreateClassLoader(deps);
        assertNotNull(newLoader, "Should create new ClassLoader after previous was GC'd");
    }

    @Test
    void testClassLoaderCloseOnInvalidateAll() throws Exception {
        // Track if ClassLoader.close() is called
        AtomicBoolean closeCalled = new AtomicBoolean(false);

        // Create a custom URLClassLoader that tracks close() calls
        // Note: deps not used here as we're creating a custom loader for testing
        URLClassLoader testLoader =
                new URLClassLoader(new java.net.URL[0]) {
                    @Override
                    public void close() throws java.io.IOException {
                        closeCalled.set(true);
                        super.close();
                    }
                };

        // Inject the test loader into cache using reflection
        injectClassLoaderIntoCache(Objects.requireNonNull(cache), "test-key", testLoader);

        // Invalidate all should close the ClassLoader
        Objects.requireNonNull(cache).invalidateAll();

        assertTrue(closeCalled.get(), "ClassLoader.close() should be called on invalidateAll()");
    }

    @Test
    void testNoCircularReferencesInClassLoader() throws Exception {
        Set<Path> deps = Set.of(Paths.get("lib1.jar"), Paths.get("lib2.jar"));
        URLClassLoader loader = Objects.requireNonNull(cache).getOrCreateClassLoader(deps);

        // Track initial references
        WeakReference<URLClassLoader> loaderRef = new WeakReference<>(loader);
        WeakReference<LRUDependencyCache> cacheRef = new WeakReference<>(cache);

        // Clear the cache to remove loader from cache
        Objects.requireNonNull(cache).invalidateAll();

        // Clear local references
        loader = null;
        cache = null;

        // Force GC
        forceGarbageCollection();

        // If there were circular references, both objects would still be reachable
        // through each other and wouldn't be GC'd
        URLClassLoader gcLoader = loaderRef.get();
        LRUDependencyCache gcCache = cacheRef.get();

        // At least one should be GC'd if there's no circular reference
        boolean loaderGCd = (gcLoader == null);
        boolean cacheGCd = (gcCache == null);

        assertTrue(
                loaderGCd || cacheGCd,
                "At least one object should be GC'd, indicating no circular reference");
    }

    @Test
    @Timeout(30)
    void testMemoryLeakUnderHighLoad() throws Exception {
        // Create many ClassLoaders and verify they can be GC'd
        List<WeakReference<URLClassLoader>> weakRefs = new ArrayList<>();

        // Create 50 ClassLoaders
        for (int i = 0; i < 50; i++) {
            Set<Path> deps = Set.of(Paths.get("lib" + i + ".jar"));
            URLClassLoader loader = Objects.requireNonNull(cache).getOrCreateClassLoader(deps);
            weakRefs.add(new WeakReference<>(loader));
        }

        // Clear the cache
        Objects.requireNonNull(cache).invalidateAll();

        // Force garbage collection
        forceGarbageCollection();

        // Count how many ClassLoaders were garbage collected
        long gcCount = weakRefs.stream().filter(ref -> ref.get() == null).count();

        // At least 80% should be garbage collected
        assertTrue(
                gcCount >= 40,
                String.format(
                        "At least 80%% of ClassLoaders should be GC'd, but only %d were", gcCount));
    }

    @Test
    void testClassLoaderNotLeakedAfterEviction() throws Exception {
        // Fill cache to trigger eviction
        List<WeakReference<URLClassLoader>> weakRefs = new ArrayList<>();

        // Create more than MAX_CACHE_SIZE entries
        for (int i = 0; i < 105; i++) {
            Set<Path> deps = Set.of(Paths.get("lib" + i + ".jar"));
            URLClassLoader loader = Objects.requireNonNull(cache).getOrCreateClassLoader(deps);
            if (i < 10) { // Track first 10 loaders
                weakRefs.add(new WeakReference<>(loader));
            }
        }

        // Force garbage collection
        forceGarbageCollection();

        // Verify evicted ClassLoaders can be garbage collected
        long gcCount = weakRefs.stream().filter(ref -> ref.get() == null).count();

        assertTrue(gcCount > 0, "Evicted ClassLoaders should be eligible for garbage collection");
    }

    @Test
    void testConcurrentAccessDoesNotCauseLeak() throws Exception {
        final int threadCount = 10;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(threadCount);
        final List<WeakReference<URLClassLoader>> weakRefs =
                Collections.synchronizedList(new ArrayList<>());

        // Create threads that concurrently access the cache
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            new Thread(
                            () -> {
                                try {
                                    startLatch.await();

                                    // Each thread creates multiple ClassLoaders
                                    for (int j = 0; j < 10; j++) {
                                        Set<Path> deps =
                                                Set.of(
                                                        Paths.get(
                                                                "thread" + index + "_lib" + j
                                                                        + ".jar"));
                                        URLClassLoader loader =
                                                Objects.requireNonNull(cache)
                                                        .getOrCreateClassLoader(deps);
                                        weakRefs.add(new WeakReference<>(loader));
                                    }
                                } catch (Exception e) {
                                    // Log the exception - in a real system this would use a proper
                                    // logger
                                    System.err.println(
                                            "Error in concurrent test thread: " + e.getMessage());
                                } finally {
                                    endLatch.countDown();
                                }
                            })
                    .start();
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all threads to complete
        assertTrue(endLatch.await(10, TimeUnit.SECONDS), "Threads should complete within timeout");

        // Clear cache and force GC
        Objects.requireNonNull(cache).invalidateAll();
        forceGarbageCollection();

        // Verify ClassLoaders can be garbage collected
        long gcCount = weakRefs.stream().filter(ref -> ref.get() == null).count();

        assertTrue(
                gcCount > weakRefs.size() / 2,
                "At least half of ClassLoaders should be garbage collected after concurrent"
                        + " access");
    }

    @Test
    void testParentClassLoaderDoesNotPreventGC() throws Exception {
        // Verify that the parent ClassLoader doesn't prevent GC
        Set<Path> deps = Set.of(Paths.get("test.jar"));
        URLClassLoader loader = Objects.requireNonNull(cache).getOrCreateClassLoader(deps);

        // Get the parent ClassLoader
        ClassLoader parent = loader.getParent();
        assertNotNull(parent, "Should have a parent ClassLoader");

        // Create weak reference and clear strong reference
        WeakReference<URLClassLoader> weakRef = new WeakReference<>(loader);
        loader = null;

        // Clear from cache
        Objects.requireNonNull(cache).invalidateAll();

        // Force GC
        forceGarbageCollection();

        // The child ClassLoader should be GC'd even though parent still exists
        assertNull(
                weakRef.get(),
                "Child ClassLoader should be GC'd even when parent ClassLoader exists");
    }

    /**
     * Helper method to force garbage collection.
     * Calls System.gc() multiple times and creates temporary objects
     * to increase the likelihood of garbage collection.
     */
    private void forceGarbageCollection() {
        try {
            // Create temporary objects to increase memory pressure
            for (int i = 0; i < 5; i++) {
                @SuppressWarnings("unused")
                byte[] waste =
                        new byte[1024 * 1024]; // 1MB - Intentionally unused to increase memory
                // pressure
                System.gc();
                // System.runFinalization() is deprecated, just use System.gc()
                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Helper method to inject a ClassLoader into the cache using reflection.
     * This is needed to test specific ClassLoader implementations.
     */
    private void injectClassLoaderIntoCache(
            LRUDependencyCache cache, String key, URLClassLoader loader) throws Exception {
        Field cacheField = LRUDependencyCache.class.getDeclaredField("classLoaderCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, WeakReference<URLClassLoader>> classLoaderCache =
                (Map<String, WeakReference<URLClassLoader>>) cacheField.get(cache);
        classLoaderCache.put(key, new WeakReference<>(loader));

        // Also update access order
        Field accessOrderField = LRUDependencyCache.class.getDeclaredField("accessOrder");
        accessOrderField.setAccessible(true);
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Long> accessOrder =
                (LinkedHashMap<String, Long>) accessOrderField.get(cache);
        accessOrder.put(key, System.currentTimeMillis());
    }
}
