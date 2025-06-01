package com.groovy.lsp.workspace.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for SymbolInfo DTO.
 */
class SymbolInfoTest {

    @Test
    void testSymbolInfoCreation() {
        Path location = Paths.get("/path/to/MyClass.groovy");
        SymbolInfo symbol = new SymbolInfo("MyClass", SymbolKind.CLASS, location, 10, 5);

        assertThat(symbol.name()).isEqualTo("MyClass");
        assertThat(symbol.kind()).isEqualTo(SymbolKind.CLASS);
        assertThat(symbol.location()).isEqualTo(location);
        assertThat(symbol.line()).isEqualTo(10);
        assertThat(symbol.column()).isEqualTo(5);
    }

    @Test
    void testSymbolKindValues() {
        assertThat(SymbolKind.values())
                .containsExactlyInAnyOrder(
                        SymbolKind.CLASS,
                        SymbolKind.INTERFACE,
                        SymbolKind.TRAIT,
                        SymbolKind.ENUM,
                        SymbolKind.METHOD,
                        SymbolKind.FIELD,
                        SymbolKind.PROPERTY,
                        SymbolKind.CONSTRUCTOR,
                        SymbolKind.ENUM_CONSTANT);
    }

    @Test
    void testSymbolInfoEquality() {
        Path location = Paths.get("/path/to/MyClass.groovy");
        SymbolInfo symbol1 = new SymbolInfo("MyClass", SymbolKind.CLASS, location, 10, 5);

        SymbolInfo symbol2 = new SymbolInfo("MyClass", SymbolKind.CLASS, location, 10, 5);

        assertThat(symbol1).isEqualTo(symbol2);
        assertThat(symbol1.hashCode()).isEqualTo(symbol2.hashCode());
    }

    @Test
    void testSymbolInfoToString() {
        Path location = Paths.get("/path/to/MyClass.groovy");
        SymbolInfo symbol = new SymbolInfo("MyMethod", SymbolKind.METHOD, location, 20, 10);

        String toString = symbol.toString();
        assertThat(toString).contains("MyMethod");
        assertThat(toString).contains("METHOD");
    }
}
