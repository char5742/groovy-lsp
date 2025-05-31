package com.groovy.lsp.server.launcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Main launcher class.
 */
class MainTest {
    
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalErr = System.err;
    
    @BeforeEach
    void setUp() {
        System.setErr(new PrintStream(errContent));
    }
    
    @AfterEach
    void tearDown() {
        System.setErr(originalErr);
    }
    
    @Test
    void testHelpOption() {
        // Test that --help prints usage information
        try {
            Main.main(new String[]{"--help"});
        } catch (SecurityException e) {
            // Expected when System.exit is called
        }
        
        String output = errContent.toString();
        assertTrue(output.contains("Groovy Language Server") || 
                   output.contains("Usage:"), 
                   "Help output should contain usage information");
    }
    
    @Test
    void testInvalidPort() {
        // Test that invalid port number throws exception
        assertThrows(IllegalArgumentException.class, () -> {
            Main.main(new String[]{"--socket", "--port", "invalid"});
        });
    }
    
    @Test
    void testMissingPortValue() {
        // Test that missing port value throws exception
        assertThrows(IllegalArgumentException.class, () -> {
            Main.main(new String[]{"--socket", "--port"});
        });
    }
    
    @Test
    void testMissingHostValue() {
        // Test that missing host value throws exception
        assertThrows(IllegalArgumentException.class, () -> {
            Main.main(new String[]{"--socket", "--host"});
        });
    }
}