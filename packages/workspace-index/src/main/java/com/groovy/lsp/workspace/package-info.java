/**
 * Workspace indexing and analysis module for Groovy LSP.
 *
 * This module provides the core workspace indexing functionality,
 * including file watching, dependency resolution, and symbol indexing.
 * It follows Domain-Driven Design principles.
 *
 * <p>This package is null-safe by default. All types are non-null unless
 * explicitly marked with {@code @Nullable}.</p>
 */
@org.jspecify.annotations.NullMarked
@org.jmolecules.ddd.annotation.Module(
        name = "workspace-index",
        description = "Workspace indexing and symbol search capabilities")
package com.groovy.lsp.workspace;
