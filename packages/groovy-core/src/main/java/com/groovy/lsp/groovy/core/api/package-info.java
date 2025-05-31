/**
 * Public API for Groovy core language server functionality.
 * This package contains interfaces and DTOs that other modules can depend on.
 * 
 * <p>The main services provided are:</p>
 * <ul>
 *   <li>{@link com.groovy.lsp.groovy.core.api.ASTService} - AST parsing and analysis</li>
 *   <li>{@link com.groovy.lsp.groovy.core.api.CompilerConfigurationService} - Compiler configuration factory</li>
 *   <li>{@link com.groovy.lsp.groovy.core.api.TypeInferenceService} - Type inference functionality</li>
 * </ul>
 * 
 * <p>This package is null-safe by default. All types are non-null unless
 * explicitly marked with {@code @Nullable}.</p>
 * 
 * @since 1.0.0
 */
@org.jspecify.annotations.NullMarked
package com.groovy.lsp.groovy.core.api;