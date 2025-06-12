package com.groovy.lsp.workspace.internal.parser;

import com.groovy.lsp.shared.workspace.api.dto.SymbolInfo;
import com.groovy.lsp.shared.workspace.api.dto.SymbolKind;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Java file parser that extracts basic symbol information for cross-file navigation.
 * This is a basic implementation that uses regex patterns to identify classes and methods.
 */
public class JavaFileParser {
    private static final Logger logger = LoggerFactory.getLogger(JavaFileParser.class);

    // Pattern to match package declaration
    private static final Pattern PACKAGE_PATTERN =
            Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;");

    // Pattern to match class/interface declaration
    private static final Pattern CLASS_PATTERN =
            Pattern.compile(
                    "(?:public\\s+)?(?:abstract\\s+)?(?:final\\s+)?(?:class|interface|enum)\\s+(\\w+)(?:\\s+extends\\s+\\w+)?(?:\\s+implements\\s+[\\w,\\s]+)?\\s*\\{");

    // Pattern to match method declarations
    private static final Pattern METHOD_PATTERN =
            Pattern.compile(
                    "(?:public|protected|private)?\\s*(?:static\\s+)?(?:final\\s+)?(?:synchronized\\s+)?(?:[\\w<>\\[\\]]+\\s+)?(\\w+)\\s*\\([^)]*\\)\\s*(?:throws\\s+[\\w,\\s]+)?\\s*\\{");

    /**
     * Parse a Java file and extract symbol information.
     *
     * @param file the Java file to parse
     * @return list of symbols found in the file
     */
    public List<SymbolInfo> parseFile(Path file) {
        List<SymbolInfo> symbols = new ArrayList<>();

        if (!file.toString().endsWith(".java")) {
            logger.debug("Not a Java file: {}", file);
            return symbols;
        }

        try {
            String content = Files.readString(file);
            String[] lines = content.split("\\n", -1);

            // Extract package name
            String packageName = "";
            for (String line : lines) {
                Matcher packageMatcher = PACKAGE_PATTERN.matcher(line);
                if (packageMatcher.find()) {
                    packageName = packageMatcher.group(1);
                    break;
                }
            }

            // Extract classes
            int lineNumber = 0;
            for (String line : lines) {
                lineNumber++;

                // Match class declarations
                Matcher classMatcher = CLASS_PATTERN.matcher(line);
                if (classMatcher.find()) {
                    String className = classMatcher.group(1);
                    String fullyQualifiedName =
                            packageName.isEmpty() ? className : packageName + "." + className;

                    SymbolInfo classSymbol =
                            new SymbolInfo(
                                    fullyQualifiedName,
                                    SymbolKind.CLASS,
                                    file,
                                    lineNumber,
                                    classMatcher.start(1) + 1 // 1-indexed
                                    );

                    symbols.add(classSymbol);
                    logger.debug(
                            "Found Java class: {} at {}:{}",
                            fullyQualifiedName,
                            lineNumber,
                            classMatcher.start(1));
                }

                // Match method declarations
                Matcher methodMatcher = METHOD_PATTERN.matcher(line);
                if (methodMatcher.find()) {
                    String methodName = methodMatcher.group(1);

                    // Skip constructors and common method names that might match patterns
                    // incorrectly
                    if (methodName.equals("if")
                            || methodName.equals("for")
                            || methodName.equals("while")
                            || methodName.equals("switch")) {
                        continue;
                    }

                    SymbolInfo methodSymbol =
                            new SymbolInfo(
                                    methodName,
                                    SymbolKind.METHOD,
                                    file,
                                    lineNumber,
                                    methodMatcher.start(1) + 1 // 1-indexed
                                    );

                    symbols.add(methodSymbol);
                    logger.debug(
                            "Found Java method: {} at {}:{}",
                            methodName,
                            lineNumber,
                            methodMatcher.start(1));
                }
            }

            logger.info("Parsed Java file {}: found {} symbols", file, symbols.size());

        } catch (IOException e) {
            logger.error("Failed to parse Java file: {}", file, e);
        }

        return symbols;
    }
}
