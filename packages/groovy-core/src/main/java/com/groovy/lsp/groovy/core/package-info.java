/**
 * Groovy core language support module.
 * 
 * <p>This module provides core Groovy language functionality including
 * AST operations, type inference, and compiler configuration. It serves
 * as the domain core for Groovy language processing.</p>
 * 
 * <p>The module follows Domain-Driven Design principles with clear 
 * separation between API and internal implementation.</p>
 * 
 * <p>This package is null-safe by default. All types are non-null unless
 * explicitly marked with {@code @Nullable}.</p>
 * 
 * @since 1.0.0
 */
@org.jspecify.annotations.NullMarked
@org.jmolecules.ddd.annotation.Module(
    name = "groovy-core",
    description = "Core Groovy language processing and AST operations"
)
package com.groovy.lsp.groovy.core;