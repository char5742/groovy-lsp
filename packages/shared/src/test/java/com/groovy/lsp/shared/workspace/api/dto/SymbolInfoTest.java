package com.groovy.lsp.shared.workspace.api.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SymbolInfoTest {

    @Test
    @DisplayName("Should create SymbolInfo with valid parameters")
    void testValidSymbolInfo() {
        // Arrange
        String name = "TestClass";
        SymbolKind kind = SymbolKind.CLASS;
        Path location = Paths.get("/test/path/file.groovy");
        int line = 10;
        int column = 5;

        // Act
        SymbolInfo symbolInfo = new SymbolInfo(name, kind, location, line, column);

        // Assert
        assertEquals(name, symbolInfo.name());
        assertEquals(kind, symbolInfo.kind());
        assertEquals(location, symbolInfo.location());
        assertEquals(line, symbolInfo.line());
        assertEquals(column, symbolInfo.column());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for null name")
    @SuppressWarnings("NullAway")
    void testNullName() {
        // Arrange
        SymbolKind kind = SymbolKind.METHOD;
        Path location = Paths.get("/test/path/file.groovy");
        int line = 1;
        int column = 1;

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new SymbolInfo(null, kind, location, line, column));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for empty name")
    void testEmptyName() {
        // Arrange
        SymbolKind kind = SymbolKind.METHOD;
        Path location = Paths.get("/test/path/file.groovy");
        int line = 1;
        int column = 1;

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new SymbolInfo("", kind, location, line, column));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for null kind")
    @SuppressWarnings("NullAway")
    void testNullKind() {
        // Arrange
        String name = "testMethod";
        Path location = Paths.get("/test/path/file.groovy");
        int line = 1;
        int column = 1;

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new SymbolInfo(name, null, location, line, column));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for null location")
    @SuppressWarnings("NullAway")
    void testNullLocation() {
        // Arrange
        String name = "testMethod";
        SymbolKind kind = SymbolKind.METHOD;
        int line = 1;
        int column = 1;

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new SymbolInfo(name, kind, null, line, column));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for negative line")
    void testNegativeLine() {
        // Arrange
        String name = "testMethod";
        SymbolKind kind = SymbolKind.METHOD;
        Path location = Paths.get("/test/path/file.groovy");
        int line = -1;
        int column = 1;

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new SymbolInfo(name, kind, location, line, column));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for zero line")
    void testZeroLine() {
        // Arrange
        String name = "testMethod";
        SymbolKind kind = SymbolKind.METHOD;
        Path location = Paths.get("/test/path/file.groovy");
        int line = 0;
        int column = 1;

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new SymbolInfo(name, kind, location, line, column));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for negative column")
    void testNegativeColumn() {
        // Arrange
        String name = "testMethod";
        SymbolKind kind = SymbolKind.METHOD;
        Path location = Paths.get("/test/path/file.groovy");
        int line = 1;
        int column = -1;

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new SymbolInfo(name, kind, location, line, column));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for zero column")
    void testZeroColumn() {
        // Arrange
        String name = "testMethod";
        SymbolKind kind = SymbolKind.METHOD;
        Path location = Paths.get("/test/path/file.groovy");
        int line = 1;
        int column = 0;

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new SymbolInfo(name, kind, location, line, column));
    }

    @Test
    @DisplayName("Should test equals for same values")
    void testEqualsWithSameValues() {
        // Arrange
        String name = "TestClass";
        SymbolKind kind = SymbolKind.CLASS;
        Path location = Paths.get("/test/path/file.groovy");
        int line = 10;
        int column = 5;

        SymbolInfo symbolInfo1 = new SymbolInfo(name, kind, location, line, column);
        SymbolInfo symbolInfo2 = new SymbolInfo(name, kind, location, line, column);

        // Act & Assert
        assertEquals(symbolInfo1, symbolInfo2);
        assertEquals(symbolInfo1.hashCode(), symbolInfo2.hashCode());
    }

    @Test
    @DisplayName("Should test equals for different values")
    void testEqualsWithDifferentValues() {
        // Arrange
        Path location = Paths.get("/test/path/file.groovy");
        SymbolInfo symbolInfo1 = new SymbolInfo("Class1", SymbolKind.CLASS, location, 10, 5);
        SymbolInfo symbolInfo2 = new SymbolInfo("Class2", SymbolKind.CLASS, location, 10, 5);

        // Act & Assert
        assertNotEquals(symbolInfo1, symbolInfo2);
    }

    @Test
    @DisplayName("Should test equals with different kind")
    void testEqualsWithDifferentKind() {
        // Arrange
        Path location = Paths.get("/test/path/file.groovy");
        SymbolInfo symbolInfo1 = new SymbolInfo("test", SymbolKind.CLASS, location, 10, 5);
        SymbolInfo symbolInfo2 = new SymbolInfo("test", SymbolKind.METHOD, location, 10, 5);

        // Act & Assert
        assertNotEquals(symbolInfo1, symbolInfo2);
    }

    @Test
    @DisplayName("Should test equals with different location")
    void testEqualsWithDifferentLocation() {
        // Arrange
        Path location1 = Paths.get("/test/path/file1.groovy");
        Path location2 = Paths.get("/test/path/file2.groovy");
        SymbolInfo symbolInfo1 = new SymbolInfo("test", SymbolKind.CLASS, location1, 10, 5);
        SymbolInfo symbolInfo2 = new SymbolInfo("test", SymbolKind.CLASS, location2, 10, 5);

        // Act & Assert
        assertNotEquals(symbolInfo1, symbolInfo2);
    }

    @Test
    @DisplayName("Should test equals with different line")
    void testEqualsWithDifferentLine() {
        // Arrange
        Path location = Paths.get("/test/path/file.groovy");
        SymbolInfo symbolInfo1 = new SymbolInfo("test", SymbolKind.CLASS, location, 10, 5);
        SymbolInfo symbolInfo2 = new SymbolInfo("test", SymbolKind.CLASS, location, 20, 5);

        // Act & Assert
        assertNotEquals(symbolInfo1, symbolInfo2);
    }

    @Test
    @DisplayName("Should test equals with different column")
    void testEqualsWithDifferentColumn() {
        // Arrange
        Path location = Paths.get("/test/path/file.groovy");
        SymbolInfo symbolInfo1 = new SymbolInfo("test", SymbolKind.CLASS, location, 10, 5);
        SymbolInfo symbolInfo2 = new SymbolInfo("test", SymbolKind.CLASS, location, 10, 15);

        // Act & Assert
        assertNotEquals(symbolInfo1, symbolInfo2);
    }

    @Test
    @DisplayName("Should test toString method")
    void testToString() {
        // Arrange
        String name = "TestClass";
        SymbolKind kind = SymbolKind.CLASS;
        Path location = Paths.get("/test/path/file.groovy");
        int line = 10;
        int column = 5;

        SymbolInfo symbolInfo = new SymbolInfo(name, kind, location, line, column);

        // Act
        String result = symbolInfo.toString();

        // Assert
        assertTrue(result.contains("TestClass"));
        assertTrue(result.contains("CLASS"));
        assertTrue(result.contains("/test/path/file.groovy"));
        assertTrue(result.contains("10"));
        assertTrue(result.contains("5"));
    }

    @Test
    @DisplayName("Should handle large line and column numbers")
    void testLargeLineAndColumnNumbers() {
        // Arrange
        String name = "TestClass";
        SymbolKind kind = SymbolKind.CLASS;
        Path location = Paths.get("/test/path/file.groovy");
        int line = Integer.MAX_VALUE;
        int column = Integer.MAX_VALUE;

        // Act
        SymbolInfo symbolInfo = new SymbolInfo(name, kind, location, line, column);

        // Assert
        assertEquals(line, symbolInfo.line());
        assertEquals(column, symbolInfo.column());
    }

    @Test
    @DisplayName("Should test all SymbolKind values")
    void testAllSymbolKindValues() {
        // Arrange
        Path location = Paths.get("/test/path/file.groovy");

        // Act & Assert
        for (SymbolKind kind : SymbolKind.values()) {
            SymbolInfo symbolInfo = new SymbolInfo("test", kind, location, 1, 1);
            assertEquals(kind, symbolInfo.kind());
        }
    }

    @Test
    @DisplayName("Should test equals with null")
    void testEqualsWithNull() {
        // Arrange
        SymbolInfo symbolInfo = new SymbolInfo("test", SymbolKind.CLASS, Paths.get("/test"), 1, 1);

        // Act & Assert
        assertNotEquals(symbolInfo, null);
    }

    @Test
    @DisplayName("Should test equals with different class")
    void testEqualsWithDifferentClass() {
        // Arrange
        SymbolInfo symbolInfo = new SymbolInfo("test", SymbolKind.CLASS, Paths.get("/test"), 1, 1);
        String other = "Not a SymbolInfo";

        // Act & Assert
        assertNotEquals(symbolInfo, other);
    }

    @Test
    @DisplayName("Should test equals with same object")
    void testEqualsWithSameObject() {
        // Arrange
        SymbolInfo symbolInfo = new SymbolInfo("test", SymbolKind.CLASS, Paths.get("/test"), 1, 1);

        // Act & Assert
        assertEquals(symbolInfo, symbolInfo);
    }
}
