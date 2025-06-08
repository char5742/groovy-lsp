package com.groovy.lsp.workspace.dependency.cache;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation of cache statistics.
 */
class DefaultCacheStatistics implements DependencyCache.CacheStatistics {
    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicLong missCount = new AtomicLong();
    private final AtomicLong evictionCount = new AtomicLong();
    private volatile long totalMemoryUsageMB;
    private volatile int classLoaderCount;
    private volatile int dependencyCacheSize;

    void recordHit() {
        hitCount.incrementAndGet();
    }

    void recordMiss() {
        missCount.incrementAndGet();
    }

    void recordEviction() {
        evictionCount.incrementAndGet();
    }

    void updateMemoryUsage(long memoryMB) {
        this.totalMemoryUsageMB = memoryMB;
    }

    void updateClassLoaderCount(int count) {
        this.classLoaderCount = count;
    }

    void updateDependencyCacheSize(int size) {
        this.dependencyCacheSize = size;
    }

    @Override
    public long getHitCount() {
        return hitCount.get();
    }

    @Override
    public long getMissCount() {
        return missCount.get();
    }

    @Override
    public long getEvictionCount() {
        return evictionCount.get();
    }

    @Override
    public long getTotalMemoryUsageMB() {
        return totalMemoryUsageMB;
    }

    @Override
    public int getClassLoaderCount() {
        return classLoaderCount;
    }

    @Override
    public int getDependencyCacheSize() {
        return dependencyCacheSize;
    }
}
