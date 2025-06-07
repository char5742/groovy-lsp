package com.groovy.lsp.workspace.internal.jar;

import com.groovy.lsp.workspace.api.dto.SymbolInfo;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Indexes JAR files to extract symbols (classes, methods, fields) for the workspace index.
 * Uses ASM library for bytecode analysis.
 */
public class JarFileIndexer {
    private static final Logger logger = LoggerFactory.getLogger(JarFileIndexer.class);

    /**
     * Index a JAR file and extract all symbols.
     *
     * @param jarPath path to the JAR file
     * @return list of symbols found in the JAR
     */
    public List<SymbolInfo> indexJar(Path jarPath) {
        List<SymbolInfo> symbols = new ArrayList<>();

        if (!jarPath.toFile().exists()) {
            logger.warn("JAR file does not exist: {}", jarPath);
            return symbols;
        }

        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            logger.debug("Indexing JAR file: {}", jarPath);

            jarFile.entries()
                    .asIterator()
                    .forEachRemaining(
                            entry -> {
                                if (isClassFile(entry)) {
                                    try {
                                        List<SymbolInfo> classSymbols =
                                                indexClassEntry(jarFile, entry, jarPath);
                                        symbols.addAll(classSymbols);
                                    } catch (Exception e) {
                                        logger.debug(
                                                "Error indexing class entry {}: {}",
                                                entry.getName(),
                                                e.getMessage());
                                    }
                                }
                            });

            logger.info("Indexed {} symbols from JAR: {}", symbols.size(), jarPath.getFileName());
        } catch (IOException e) {
            logger.error("Error reading JAR file: {}", jarPath, e);
        }

        return symbols;
    }

    /**
     * Check if the JAR entry is a class file.
     */
    private boolean isClassFile(JarEntry entry) {
        return !entry.isDirectory() && entry.getName().endsWith(".class");
    }

    /**
     * Index a single class entry in the JAR.
     */
    private List<SymbolInfo> indexClassEntry(JarFile jarFile, JarEntry entry, Path jarPath)
            throws IOException {
        List<SymbolInfo> symbols = new ArrayList<>();

        try (InputStream is = jarFile.getInputStream(entry)) {
            ClassReader classReader = new ClassReader(is);
            ClassFileVisitor visitor = new ClassFileVisitor(jarPath);
            classReader.accept(
                    visitor,
                    ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            symbols.addAll(visitor.getSymbols());
        }

        return symbols;
    }

    /**
     * Check if a JAR file contains Groovy or Java classes by examining its structure.
     *
     * @param jarPath path to the JAR file
     * @return true if the JAR likely contains relevant classes
     */
    public boolean isRelevantJar(Path jarPath) {
        if (!jarPath.toFile().exists()) {
            return false;
        }

        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            // Check if JAR contains any class files
            return jarFile.entries()
                    .asIterator()
                    .hasNext(); // Simple check - can be enhanced to look for specific patterns
        } catch (IOException e) {
            logger.debug("Error checking JAR relevance: {}", jarPath, e);
            return false;
        }
    }
}
