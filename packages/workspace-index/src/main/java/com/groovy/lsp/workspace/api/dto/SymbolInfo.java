package com.groovy.lsp.workspace.api.dto;

import java.nio.file.Path;
import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Represents information about a symbol in the workspace.
 * This is an immutable data transfer object used to communicate symbol information.
 *
 * Marked as a ValueObject in DDD terms as it's immutable and represents
 * a descriptive aspect of the domain with no conceptual identity.
 */
@ValueObject
public record SymbolInfo(String name, SymbolKind kind, Path location, int line, int column) {
    /**
     * Creates a new SymbolInfo instance with validation.
     *
     * @param name the symbol name
     * @param kind the symbol kind
     * @param location the file path where the symbol is defined
     * @param line the line number (1-based)
     * @param column the column number (1-based)
     */
    public SymbolInfo {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Symbol name cannot be null or blank");
        }
        if (kind == null) {
            throw new IllegalArgumentException("Symbol kind cannot be null");
        }
        if (location == null) {
            throw new IllegalArgumentException("Symbol location cannot be null");
        }
        if (line < 1) {
            throw new IllegalArgumentException("Line number must be positive");
        }
        if (column < 1) {
            throw new IllegalArgumentException("Column number must be positive");
        }
    }
}
