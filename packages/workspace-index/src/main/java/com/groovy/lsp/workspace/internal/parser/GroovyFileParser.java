package com.groovy.lsp.workspace.internal.parser;

import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.groovy.core.api.GroovyCoreFactory;
import com.groovy.lsp.workspace.api.dto.SymbolInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.codehaus.groovy.ast.ModuleNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for Groovy source files that extracts symbol information.
 */
public class GroovyFileParser {
    private static final Logger logger = LoggerFactory.getLogger(GroovyFileParser.class);
    private final ASTService astService;

    public GroovyFileParser() {
        this.astService = GroovyCoreFactory.getInstance().getASTService();
    }

    /**
     * Parse a Groovy source file and extract symbol information.
     *
     * @param file The file to parse
     * @return List of symbols found in the file
     * @throws IOException if the file cannot be read
     */
    public List<SymbolInfo> parseFile(Path file) throws IOException {
        String content = Files.readString(file);
        String fileName = file.getFileName().toString();

        try {
            ModuleNode moduleNode = astService.parseSource(content, fileName);

            if (moduleNode == null) {
                logger.warn("Failed to parse file: {}", file);
                return Collections.emptyList();
            }

            if (moduleNode.getClasses().isEmpty()) {
                logger.warn(
                        "No classes found in file: {} - File content: {}",
                        file,
                        content.substring(0, Math.min(content.length(), 200)));
                return Collections.emptyList();
            }

            SymbolExtractorVisitor visitor = new SymbolExtractorVisitor(file);
            moduleNode
                    .getClasses()
                    .forEach(
                            classNode -> {
                                // Process the class node itself first
                                visitor.processClassNode(classNode);
                                // Then visit its contents (methods, fields, etc.)
                                classNode.visitContents(visitor);
                            });

            return visitor.getSymbols();
        } catch (Exception e) {
            logger.error("Error parsing Groovy file: {}", file, e);
            return Collections.emptyList();
        }
    }
}
