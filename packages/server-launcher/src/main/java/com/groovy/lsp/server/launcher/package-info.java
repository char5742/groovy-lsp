/**
 * Server launcher module for Groovy LSP.
 *
 * <p>This module provides the main entry point for launching the
 * Groovy Language Server. It handles command-line arguments,
 * server initialization, and communication setup (stdio/socket).</p>
 *
 * <p>This package is null-safe by default. All types are non-null unless
 * explicitly marked with {@code @Nullable}.</p>
 *
 * @since 1.0.0
 */
@org.jspecify.annotations.NullMarked
@org.jmolecules.ddd.annotation.Module(
        name = "server-launcher",
        description = "Main entry point and launcher for the Groovy Language Server")
package com.groovy.lsp.server.launcher;
