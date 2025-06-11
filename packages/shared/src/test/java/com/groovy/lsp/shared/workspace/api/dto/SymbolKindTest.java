package com.groovy.lsp.shared.workspace.api.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.groovy.lsp.test.annotations.UnitTest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;

class SymbolKindTest {

    @UnitTest
    @DisplayName("Should have all expected symbol kinds")
    void testAllSymbolKinds() {
        // Assert
        Set<String> expectedKinds =
                new HashSet<>(
                        Arrays.asList(
                                "CLASS",
                                "INTERFACE",
                                "TRAIT",
                                "METHOD",
                                "FIELD",
                                "PROPERTY",
                                "CONSTRUCTOR",
                                "ENUM",
                                "ENUM_CONSTANT",
                                "ANNOTATION",
                                "CLOSURE"));

        Set<String> actualKinds = new HashSet<>();
        for (SymbolKind kind : SymbolKind.values()) {
            actualKinds.add(kind.name());
        }

        assertEquals(expectedKinds, actualKinds);
    }

    @UnitTest
    @DisplayName("Should return correct enum value for valueOf")
    void testValueOf() {
        // Act & Assert
        assertEquals(SymbolKind.CLASS, SymbolKind.valueOf("CLASS"));
        assertEquals(SymbolKind.INTERFACE, SymbolKind.valueOf("INTERFACE"));
        assertEquals(SymbolKind.TRAIT, SymbolKind.valueOf("TRAIT"));
        assertEquals(SymbolKind.METHOD, SymbolKind.valueOf("METHOD"));
        assertEquals(SymbolKind.FIELD, SymbolKind.valueOf("FIELD"));
        assertEquals(SymbolKind.PROPERTY, SymbolKind.valueOf("PROPERTY"));
        assertEquals(SymbolKind.CONSTRUCTOR, SymbolKind.valueOf("CONSTRUCTOR"));
        assertEquals(SymbolKind.ENUM, SymbolKind.valueOf("ENUM"));
        assertEquals(SymbolKind.ENUM_CONSTANT, SymbolKind.valueOf("ENUM_CONSTANT"));
        assertEquals(SymbolKind.ANNOTATION, SymbolKind.valueOf("ANNOTATION"));
        assertEquals(SymbolKind.CLOSURE, SymbolKind.valueOf("CLOSURE"));
    }

    @UnitTest
    @DisplayName("Should throw exception for invalid valueOf")
    void testValueOfInvalid() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> SymbolKind.valueOf("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> SymbolKind.valueOf(""));
    }

    @UnitTest
    @DisplayName("Should test enum toString")
    void testToString() {
        // Act & Assert
        assertEquals("CLASS", SymbolKind.CLASS.toString());
        assertEquals("METHOD", SymbolKind.METHOD.toString());
        assertEquals("FIELD", SymbolKind.FIELD.toString());
        assertEquals("PROPERTY", SymbolKind.PROPERTY.toString());
    }

    @UnitTest
    @DisplayName("Should test enum name")
    void testName() {
        // Act & Assert
        assertEquals("CLASS", SymbolKind.CLASS.name());
        assertEquals("METHOD", SymbolKind.METHOD.name());
        assertEquals("FIELD", SymbolKind.FIELD.name());
        assertEquals("PROPERTY", SymbolKind.PROPERTY.name());
    }

    @UnitTest
    @DisplayName("Should test values() returns all enums")
    void testValues() {
        // Act
        SymbolKind[] values = SymbolKind.values();

        // Assert
        assertNotNull(values);
        assertEquals(11, values.length);
        assertTrue(Arrays.asList(values).contains(SymbolKind.CLASS));
        assertTrue(Arrays.asList(values).contains(SymbolKind.INTERFACE));
        assertTrue(Arrays.asList(values).contains(SymbolKind.TRAIT));
        assertTrue(Arrays.asList(values).contains(SymbolKind.METHOD));
        assertTrue(Arrays.asList(values).contains(SymbolKind.FIELD));
        assertTrue(Arrays.asList(values).contains(SymbolKind.PROPERTY));
        assertTrue(Arrays.asList(values).contains(SymbolKind.CONSTRUCTOR));
        assertTrue(Arrays.asList(values).contains(SymbolKind.ENUM));
        assertTrue(Arrays.asList(values).contains(SymbolKind.ENUM_CONSTANT));
        assertTrue(Arrays.asList(values).contains(SymbolKind.ANNOTATION));
        assertTrue(Arrays.asList(values).contains(SymbolKind.CLOSURE));
    }

    @UnitTest
    @DisplayName("Should maintain enum constants order")
    void testEnumOrder() {
        // Act
        SymbolKind[] values = SymbolKind.values();

        // Assert - verify the order is maintained
        assertEquals(SymbolKind.CLASS, values[0]);
        assertEquals(SymbolKind.INTERFACE, values[1]);
        assertEquals(SymbolKind.TRAIT, values[2]);
        assertEquals(SymbolKind.METHOD, values[3]);
        assertEquals(SymbolKind.FIELD, values[4]);
        assertEquals(SymbolKind.PROPERTY, values[5]);
        assertEquals(SymbolKind.CONSTRUCTOR, values[6]);
        assertEquals(SymbolKind.ENUM, values[7]);
        assertEquals(SymbolKind.ENUM_CONSTANT, values[8]);
        assertEquals(SymbolKind.ANNOTATION, values[9]);
        assertEquals(SymbolKind.CLOSURE, values[10]);
    }

    @UnitTest
    @DisplayName("Should compare enum constants")
    void testCompareTo() {
        // Act & Assert
        assertTrue(SymbolKind.CLASS.compareTo(SymbolKind.METHOD) < 0);
        assertTrue(SymbolKind.METHOD.compareTo(SymbolKind.CLASS) > 0);
        // Test that same enum returns 0 (using a variable to avoid SelfComparison warning)
        SymbolKind classKind = SymbolKind.CLASS;
        assertEquals(0, classKind.compareTo(SymbolKind.CLASS));
    }

    @UnitTest
    @DisplayName("Should test equals for enum constants")
    void testEquals() {
        // Act & Assert
        assertEquals(SymbolKind.CLASS, SymbolKind.CLASS);
        assertNotNull(SymbolKind.METHOD);
        assertTrue(SymbolKind.CLASS != SymbolKind.METHOD);
    }

    @UnitTest
    @DisplayName("Should test hashCode for enum constants")
    void testHashCode() {
        // Arrange
        Set<Integer> hashCodes = new HashSet<>();

        // Act
        for (SymbolKind kind : SymbolKind.values()) {
            hashCodes.add(kind.hashCode());
        }

        // Assert - all hash codes should be unique
        assertEquals(SymbolKind.values().length, hashCodes.size());
    }

    @UnitTest
    @DisplayName("Should test getDeclaringClass")
    void testGetDeclaringClass() {
        // Act & Assert
        for (SymbolKind kind : SymbolKind.values()) {
            assertEquals(SymbolKind.class, kind.getDeclaringClass());
        }
    }
}
