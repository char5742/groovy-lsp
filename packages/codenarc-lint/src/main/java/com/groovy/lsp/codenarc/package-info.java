/**
 * CodeNarc linting integration module for Groovy LSP.
 *
 * <p>This module integrates CodeNarc static analysis tool to provide
 * real-time linting and code quality feedback for Groovy code.
 * It includes quick fix suggestions and rule set management.</p>
 *
 * <p>Key components:</p>
 * <ul>
 *   <li>{@link com.groovy.lsp.codenarc.LintEngine} - Main linting engine</li>
 *   <li>{@link com.groovy.lsp.codenarc.RuleSetProvider} - Rule configuration</li>
 *   <li>{@link com.groovy.lsp.codenarc.QuickFixMapper} - Quick fix suggestions</li>
 * </ul>
 *
 * <p>This package is null-safe by default. All types are non-null unless
 * explicitly marked with {@code @Nullable}.</p>
 *
 * @since 1.0.0
 */
@org.jspecify.annotations.NullMarked
@org.jmolecules.ddd.annotation.Module(
        name = "codenarc-lint",
        description = "CodeNarc static analysis integration for code quality")
package com.groovy.lsp.codenarc;
