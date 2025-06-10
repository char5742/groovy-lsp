package com.groovy.lsp.workspace.api.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.groovy.lsp.shared.workspace.api.dto.SymbolInfo;
import com.groovy.lsp.shared.workspace.api.dto.SymbolKind;
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
                        SymbolKind.ENUM_CONSTANT,
                        SymbolKind.ANNOTATION,
                        SymbolKind.CLOSURE);
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

    @Test
    void testSymbolInfoValidation_NullName() throws Exception {
        // Use reflection to bypass NullAway for null parameter testing
        Path location = Paths.get("/path/to/MyClass.groovy");
        java.lang.reflect.Constructor<SymbolInfo> constructor =
                SymbolInfo.class.getDeclaredConstructor(
                        String.class, SymbolKind.class, Path.class, int.class, int.class);

        assertThatThrownBy(
                        () -> {
                            try {
                                constructor.newInstance(null, SymbolKind.CLASS, location, 10, 5);
                            } catch (java.lang.reflect.InvocationTargetException e) {
                                throw e.getCause();
                            }
                        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Symbol name cannot be null or blank");
    }

    @Test
    void testSymbolInfoValidation_BlankName() {
        Path location = Paths.get("/path/to/MyClass.groovy");
        assertThatThrownBy(() -> new SymbolInfo("   ", SymbolKind.CLASS, location, 10, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Symbol name cannot be null or blank");
    }

    @Test
    void testSymbolInfoValidation_NullKind() throws Exception {
        // Use reflection to bypass NullAway for null parameter testing
        Path location = Paths.get("/path/to/MyClass.groovy");
        java.lang.reflect.Constructor<SymbolInfo> constructor =
                SymbolInfo.class.getDeclaredConstructor(
                        String.class, SymbolKind.class, Path.class, int.class, int.class);

        assertThatThrownBy(
                        () -> {
                            try {
                                constructor.newInstance("MyClass", null, location, 10, 5);
                            } catch (java.lang.reflect.InvocationTargetException e) {
                                throw e.getCause();
                            }
                        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Symbol kind cannot be null");
    }

    @Test
    void testSymbolInfoValidation_NullLocation() throws Exception {
        // Use reflection to bypass NullAway for null parameter testing
        java.lang.reflect.Constructor<SymbolInfo> constructor =
                SymbolInfo.class.getDeclaredConstructor(
                        String.class, SymbolKind.class, Path.class, int.class, int.class);

        assertThatThrownBy(
                        () -> {
                            try {
                                constructor.newInstance("MyClass", SymbolKind.CLASS, null, 10, 5);
                            } catch (java.lang.reflect.InvocationTargetException e) {
                                throw e.getCause();
                            }
                        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Symbol location cannot be null");
    }

    @Test
    void testSymbolInfoValidation_InvalidLine() {
        Path location = Paths.get("/path/to/MyClass.groovy");
        assertThatThrownBy(() -> new SymbolInfo("MyClass", SymbolKind.CLASS, location, 0, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Line number must be positive");
    }

    @Test
    void testSymbolInfoValidation_InvalidColumn() {
        Path location = Paths.get("/path/to/MyClass.groovy");
        assertThatThrownBy(() -> new SymbolInfo("MyClass", SymbolKind.CLASS, location, 10, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Column number must be positive");
    }
}
