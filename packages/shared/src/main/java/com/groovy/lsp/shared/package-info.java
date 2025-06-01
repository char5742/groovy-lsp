/**
 * Shared components module for Groovy LSP modular architecture.
 * 
 * <p>This module contains cross-cutting concerns and shared infrastructure
 * that is used by multiple other modules. It follows Domain-Driven Design
 * principles and is marked with jMolecules annotations.</p>
 * 
 * <p>Key components:</p>
 * <ul>
 *   <li>Event Bus - For event-driven communication between modules</li>
 *   <li>Domain Events - Base infrastructure for domain events</li>
 *   <li>Common utilities - Shared utilities and helpers</li>
 * </ul>
 * 
 * <p>This package is null-safe by default. All types are non-null unless
 * explicitly marked with {@code @Nullable}.</p>
 * 
 * @since 1.0.0
 */
@org.jspecify.annotations.NullMarked
@org.jmolecules.ddd.annotation.Module(
    name = "shared",
    description = "Shared infrastructure and cross-cutting concerns"
)
package com.groovy.lsp.shared;