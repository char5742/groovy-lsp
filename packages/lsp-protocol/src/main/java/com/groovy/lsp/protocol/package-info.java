/**
 * Language Server Protocol implementation module.
 *
 * <p>This module provides the LSP protocol implementation for Groovy,
 * acting as the primary interface adapter for external LSP clients.
 * It adapts external protocol requests to internal domain operations.</p>
 *
 * <p>This package is null-safe by default. All types are non-null unless
 * explicitly marked with {@code @Nullable}.</p>
 *
 * @since 1.0.0
 */
@org.jspecify.annotations.NullMarked
@org.jmolecules.ddd.annotation.Module(
        name = "lsp-protocol",
        description = "Language Server Protocol adapter for Groovy LSP")
package com.groovy.lsp.protocol;
