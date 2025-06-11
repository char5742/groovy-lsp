package com.groovy.lsp.workspace.dependency.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.groovy.lsp.test.annotations.UnitTest;
import com.groovy.lsp.workspace.dependency.DependencyResolver;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

/**
 * Unit tests for CachedDependencyResolver.
 */
class CachedDependencyResolverTest {

    private DependencyResolver mockDelegate;
    private DependencyCache mockCache;
    private Path workspaceRoot;
    private CachedDependencyResolver resolver;

    @BeforeEach
    void setUp() {
        mockDelegate = Mockito.mock(DependencyResolver.class);
        mockCache = Mockito.mock(DependencyCache.class);
        workspaceRoot = Paths.get("/workspace");
        resolver = new CachedDependencyResolver(mockDelegate, mockCache, workspaceRoot);
    }

    @UnitTest
    void testResolveDependencies_cacheHit_returnsFromCache() {
        List<Path> cachedDeps = List.of(Paths.get("cached1.jar"), Paths.get("cached2.jar"));

        when(mockCache.getCachedDependencies(workspaceRoot)).thenReturn(Optional.of(cachedDeps));

        List<Path> result = resolver.resolveDependencies();

        assertEquals(cachedDeps, result);
        verify(mockDelegate, never()).resolveDependencies();
        verify(mockCache, never()).cacheDependencies(any(), any());
    }

    @UnitTest
    void testResolveDependencies_cacheMiss_resolvesAndCaches() {
        List<Path> resolvedDeps = List.of(Paths.get("resolved1.jar"), Paths.get("resolved2.jar"));

        when(mockCache.getCachedDependencies(workspaceRoot)).thenReturn(Optional.empty());
        when(mockDelegate.resolveDependencies()).thenReturn(resolvedDeps);

        List<Path> result = resolver.resolveDependencies();

        assertEquals(resolvedDeps, result);
        verify(mockDelegate).resolveDependencies();
        verify(mockCache).cacheDependencies(workspaceRoot, resolvedDeps);
    }

    @UnitTest
    void testGetSourceDirectories_delegatesWithoutCaching() {
        List<Path> sourceDirs = List.of(Paths.get("src/main/java"), Paths.get("src/main/groovy"));

        when(mockDelegate.getSourceDirectories()).thenReturn(sourceDirs);

        List<Path> result = resolver.getSourceDirectories();

        assertEquals(sourceDirs, result);
        verify(mockDelegate).getSourceDirectories();
        verifyNoInteractions(mockCache);
    }

    @UnitTest
    void testCanHandle_delegates() {
        when(mockDelegate.canHandle(workspaceRoot)).thenReturn(true);

        boolean result = resolver.canHandle(workspaceRoot);

        assertTrue(result);
        verify(mockDelegate).canHandle(workspaceRoot);
    }

    @UnitTest
    void testGetBuildSystem_delegates() {
        when(mockDelegate.getBuildSystem()).thenReturn(DependencyResolver.BuildSystem.GRADLE);

        DependencyResolver.BuildSystem result = resolver.getBuildSystem();

        assertEquals(DependencyResolver.BuildSystem.GRADLE, result);
        verify(mockDelegate).getBuildSystem();
    }

    @UnitTest
    void testInvalidateCache_invalidatesProjectCache() {
        resolver.invalidateCache();

        verify(mockCache).invalidateProject(workspaceRoot);
    }

    @UnitTest
    void testGetDelegate_returnsOriginalResolver() {
        DependencyResolver delegate = resolver.getDelegate();

        assertSame(mockDelegate, delegate);
    }

    @UnitTest
    void testGetCacheStatistics_returnsCacheStats() {
        DependencyCache.CacheStatistics mockStats = mock(DependencyCache.CacheStatistics.class);
        when(mockCache.getStatistics()).thenReturn(mockStats);

        DependencyCache.CacheStatistics stats = resolver.getCacheStatistics();

        assertSame(mockStats, stats);
        verify(mockCache).getStatistics();
    }
}
