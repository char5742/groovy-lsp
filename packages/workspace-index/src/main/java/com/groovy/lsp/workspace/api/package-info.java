/**
 * Public API for workspace indexing functionality.
 *
 * <p>This package provides interfaces and DTOs for indexing and searching
 * symbols in a Groovy workspace. The main entry point is
 * {@link com.groovy.lsp.workspace.api.WorkspaceIndexService}.</p>
 *
 * <p>Key components:</p>
 * <ul>
 *   <li>{@link com.groovy.lsp.workspace.api.WorkspaceIndexService} - Main service interface</li>
 *   <li>{@link com.groovy.lsp.workspace.api.dto.SymbolInfo} - Symbol information DTO</li>
 *   <li>{@link com.groovy.lsp.workspace.api.dto.SymbolKind} - Symbol type enumeration</li>
 * </ul>
 *
 * <p>This package is null-safe by default. All types are non-null unless
 * explicitly marked with {@code @Nullable}.</p>
 *
 * @since 1.0.0
 */
@org.jspecify.annotations.NullMarked
package com.groovy.lsp.workspace.api;
