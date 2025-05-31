/**
 * Code formatting module for Groovy LSP.
 * 
 * <p>This module provides code formatting capabilities for Groovy source code,
 * supporting various formatting styles and configurations. It integrates
 * with the Language Server Protocol to provide document and range formatting.</p>
 * 
 * <p>This package is null-safe by default. All types are non-null unless
 * explicitly marked with {@code @Nullable}.</p>
 * 
 * @since 1.0.0
 */
@org.jspecify.annotations.NullMarked
@org.jmolecules.ddd.annotation.Module(
    name = "formatting",
    description = "Code formatting and style enforcement for Groovy"
)
package com.groovy.lsp.formatting;