package com.groovy.lsp.workspace.internal.jar;

import com.groovy.lsp.shared.workspace.api.dto.SymbolInfo;
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
    private static final int MAX_ENTRIES = 100000; // Maximum number of entries to process
    private static final long MAX_ENTRY_SIZE = 50 * 1024 * 1024; // 50MB max per entry
    private static final long MAX_TOTAL_SIZE =
            500 * 1024 * 1024; // 500MB max total uncompressed size

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

            int entryCount = 0;
            long totalSize = 0;
            var entries = jarFile.entries();

            while (entries.hasMoreElements() && entryCount < MAX_ENTRIES) {
                JarEntry entry = entries.nextElement();
                entryCount++;

                // Check entry size for Zip Bomb protection
                long entrySize = entry.getSize();
                if (entrySize > MAX_ENTRY_SIZE) {
                    logger.warn(
                            "Skipping oversized entry {} in {}: {} bytes",
                            entry.getName(),
                            jarPath,
                            entrySize);
                    continue;
                }

                totalSize += entrySize > 0 ? entrySize : 0;
                if (totalSize > MAX_TOTAL_SIZE) {
                    logger.warn(
                            "JAR file {} exceeds maximum total size limit, stopping at entry {}",
                            jarPath,
                            entryCount);
                    break;
                }

                if (isClassFile(entry)) {
                    try {
                        List<SymbolInfo> classSymbols = indexClassEntry(jarFile, entry, jarPath);
                        symbols.addAll(classSymbols);
                    } catch (Exception e) {
                        logger.debug(
                                "Error indexing class entry {}: {}",
                                entry.getName(),
                                e.getMessage());
                    }
                }
            }

            if (entryCount >= MAX_ENTRIES) {
                logger.warn(
                        "JAR file {} has too many entries (>{} ), processing stopped",
                        jarPath,
                        MAX_ENTRIES);
            }

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
            var entries = jarFile.entries();
            int count = 0;
            while (entries.hasMoreElements() && count < 100) {
                JarEntry entry = entries.nextElement();
                count++;
                if (isClassFile(entry)) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            logger.debug("Error checking JAR relevance: {}", jarPath, e);
            return false;
        }
    }
}
