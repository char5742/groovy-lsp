package com.groovy.lsp.workspace.internal.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.groovy.core.api.GroovyCoreFactory;
import com.groovy.lsp.shared.workspace.api.dto.SymbolInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.codehaus.groovy.ast.ModuleNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

/**
 * Additional test cases for GroovyFileParser to improve branch coverage.
 */
class GroovyFileParserAdditionalTest {

    @TempDir Path tempDir;

    private GroovyFileParser parser;

    @BeforeEach
    void setUp() {
        parser = new GroovyFileParser();
    }

    @Test
    void parseFile_shouldReturnEmptyListForOversizedFile() throws IOException {
        // Given - Create a file that's too large (> 10MB)
        Path largeFile = tempDir.resolve("large.groovy");

        // Create a string that's over 10MB
        StringBuilder content = new StringBuilder();
        String chunk = "class Test { void method() { println 'test' } }\n";
        int chunkSize = chunk.length();
        int requiredChunks = (10 * 1024 * 1024 / chunkSize) + 1;

        for (int i = 0; i < requiredChunks; i++) {
            content.append(chunk);
        }

        Files.writeString(largeFile, content.toString());

        // When
        List<SymbolInfo> symbols = parser.parseFile(largeFile);

        // Then
        assertThat(symbols).isEmpty();
    }

    @Test
    void parseFile_shouldReturnEmptyListForFileWithNoClasses() throws IOException {
        // Given - Mock ASTService to return module with no classes
        try (MockedStatic<GroovyCoreFactory> mockedFactory = mockStatic(GroovyCoreFactory.class)) {
            GroovyCoreFactory mockFactory = mock(GroovyCoreFactory.class);
            ASTService mockAstService = mock(ASTService.class);
            ModuleNode mockModuleNode = mock(ModuleNode.class);

            mockedFactory.when(GroovyCoreFactory::getInstance).thenReturn(mockFactory);
            when(mockFactory.getASTService()).thenReturn(mockAstService);
            when(mockAstService.parseSource(anyString(), anyString())).thenReturn(mockModuleNode);
            when(mockModuleNode.getClasses()).thenReturn(Collections.emptyList());

            // Create parser with mocked dependencies
            GroovyFileParser testParser = new GroovyFileParser();

            Path testFile = tempDir.resolve("empty.groovy");
            Files.writeString(testFile, "// Just comments, no classes");

            // When
            List<SymbolInfo> symbols = testParser.parseFile(testFile);

            // Then
            assertThat(symbols).isEmpty();
        }
    }

    @Test
    void parseFile_shouldHandleParsingException() throws IOException {
        // Given - Mock ASTService to throw exception
        try (MockedStatic<GroovyCoreFactory> mockedFactory = mockStatic(GroovyCoreFactory.class)) {
            GroovyCoreFactory mockFactory = mock(GroovyCoreFactory.class);
            ASTService mockAstService = mock(ASTService.class);

            mockedFactory.when(GroovyCoreFactory::getInstance).thenReturn(mockFactory);
            when(mockFactory.getASTService()).thenReturn(mockAstService);
            when(mockAstService.parseSource(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Parsing error"));

            // Create parser with mocked dependencies
            GroovyFileParser testParser = new GroovyFileParser();

            Path testFile = tempDir.resolve("error.groovy");
            Files.writeString(testFile, "class Test { invalid syntax }");

            // When
            List<SymbolInfo> symbols = testParser.parseFile(testFile);

            // Then
            assertThat(symbols).isEmpty();
        }
    }

    @Test
    void parseFile_shouldHandleExceptionDuringSymbolExtraction() throws IOException {
        // Given - Mock to throw exception during symbol extraction
        try (MockedStatic<GroovyCoreFactory> mockedFactory = mockStatic(GroovyCoreFactory.class)) {
            GroovyCoreFactory mockFactory = mock(GroovyCoreFactory.class);
            ASTService mockAstService = mock(ASTService.class);
            ModuleNode mockModuleNode = mock(ModuleNode.class);

            mockedFactory.when(GroovyCoreFactory::getInstance).thenReturn(mockFactory);
            when(mockFactory.getASTService()).thenReturn(mockAstService);
            when(mockAstService.parseSource(anyString(), anyString())).thenReturn(mockModuleNode);
            // Throw exception when getting classes
            when(mockModuleNode.getClasses())
                    .thenThrow(new RuntimeException("Error getting classes"));

            // Create parser with mocked dependencies
            GroovyFileParser testParser = new GroovyFileParser();

            Path testFile = tempDir.resolve("exception.groovy");
            Files.writeString(testFile, "class Test {}");

            // When
            List<SymbolInfo> symbols = testParser.parseFile(testFile);

            // Then
            assertThat(symbols).isEmpty();
        }
    }

    @Test
    void parseFile_shouldHandleEmptyFileContent() throws IOException {
        // Given - File with empty content (empty groovy files create script classes)
        Path emptyFile = tempDir.resolve("empty.groovy");
        Files.writeString(emptyFile, "");

        // When
        List<SymbolInfo> symbols = parser.parseFile(emptyFile);

        // Then - Empty groovy files generate a script class with default methods
        assertThat(symbols).isNotEmpty(); // Script class generates symbols
    }

    @Test
    void parseFile_shouldLogContentForEmptyClasses() throws IOException {
        // Given - Mock to simulate empty classes with long content
        try (MockedStatic<GroovyCoreFactory> mockedFactory = mockStatic(GroovyCoreFactory.class)) {
            GroovyCoreFactory mockFactory = mock(GroovyCoreFactory.class);
            ASTService mockAstService = mock(ASTService.class);
            ModuleNode mockModuleNode = mock(ModuleNode.class);

            mockedFactory.when(GroovyCoreFactory::getInstance).thenReturn(mockFactory);
            when(mockFactory.getASTService()).thenReturn(mockAstService);
            when(mockAstService.parseSource(anyString(), anyString())).thenReturn(mockModuleNode);
            when(mockModuleNode.getClasses()).thenReturn(Collections.emptyList());

            // Create parser with mocked dependencies
            GroovyFileParser testParser = new GroovyFileParser();

            // Create file with content longer than 200 characters
            StringBuilder longContent = new StringBuilder();
            for (int i = 0; i < 250; i++) {
                longContent.append("a");
            }

            Path testFile = tempDir.resolve("long.groovy");
            Files.writeString(testFile, longContent.toString());

            // When
            List<SymbolInfo> symbols = testParser.parseFile(testFile);

            // Then
            assertThat(symbols).isEmpty();
            // The logger will log only the first 200 characters
        }
    }
}
