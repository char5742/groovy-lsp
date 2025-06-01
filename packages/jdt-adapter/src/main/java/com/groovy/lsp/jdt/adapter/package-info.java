/**
 * JDT adapter module for Groovy LSP.
 * 
 * <p>This module provides integration with Eclipse JDT (Java Development Tools)
 * for enhanced Java interoperability and type resolution in Groovy code.
 * It acts as an adapter between Groovy's type system and JDT's type model.</p>
 * 
 * <p>This package is null-safe by default. All types are non-null unless
 * explicitly marked with {@code @Nullable}.</p>
 * 
 * @since 1.0.0
 */
@org.jspecify.annotations.NullMarked
@org.jmolecules.ddd.annotation.Module(
    name = "jdt-adapter",
    description = "Eclipse JDT integration for Java interoperability"
)
package com.groovy.lsp.jdt.adapter;