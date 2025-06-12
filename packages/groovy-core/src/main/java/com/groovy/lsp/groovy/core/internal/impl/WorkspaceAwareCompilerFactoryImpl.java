package com.groovy.lsp.groovy.core.internal.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Workspace-aware implementation of CompilerConfigurationService.
 * This implementation adds Java source directories to the classpath for cross-file navigation.
 */
public class WorkspaceAwareCompilerFactoryImpl extends CompilerFactoryImpl {
    private static final Logger logger =
            LoggerFactory.getLogger(WorkspaceAwareCompilerFactoryImpl.class);

    private final Path workspaceRoot;

    public WorkspaceAwareCompilerFactoryImpl(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
        logger.info("Created WorkspaceAwareCompilerFactoryImpl with root: {}", workspaceRoot);
    }

    @Override
    public CompilerConfiguration createDefaultConfiguration() {
        logger.debug("Creating default configuration with workspace-aware classpath");
        CompilerConfiguration config = super.createDefaultConfiguration();
        addWorkspaceSourcePaths(config);
        logger.info("Created configuration with classpath: {}", config.getClasspath());
        return config;
    }

    @Override
    public CompilerConfiguration createConfigurationWithClasspath(List<String> classpath) {
        List<String> enhancedClasspath = new ArrayList<>(classpath);
        addJavaSourcePaths(enhancedClasspath);
        return super.createConfigurationWithClasspath(enhancedClasspath);
    }

    @Override
    public CompilerConfiguration createScriptConfiguration() {
        CompilerConfiguration config = super.createScriptConfiguration();
        addWorkspaceSourcePaths(config);
        return config;
    }

    @Override
    public CompilerConfiguration createTypeCheckingConfiguration(boolean staticTypeChecking) {
        CompilerConfiguration config = super.createTypeCheckingConfiguration(staticTypeChecking);
        addWorkspaceSourcePaths(config);
        return config;
    }

    private void addWorkspaceSourcePaths(CompilerConfiguration config) {
        List<String> classpath = new ArrayList<>(config.getClasspath());
        addJavaSourcePaths(classpath);
        config.setClasspathList(classpath);
    }

    private void addJavaSourcePaths(List<String> classpath) {
        // Common Java source directory patterns
        String[] sourcePatterns = {"src/main/java", "src/test/java", "src/java", "java"};

        for (String pattern : sourcePatterns) {
            Path sourcePath = workspaceRoot.resolve(pattern);
            if (Files.exists(sourcePath) && Files.isDirectory(sourcePath)) {
                String pathStr = sourcePath.toAbsolutePath().toString();
                if (!classpath.contains(pathStr)) {
                    classpath.add(pathStr);
                    logger.debug("Added Java source path to classpath: {}", pathStr);
                }
            }
        }

        // Also add Groovy source directories
        String[] groovyPatterns = {"src/main/groovy", "src/test/groovy", "src/groovy", "groovy"};

        for (String pattern : groovyPatterns) {
            Path sourcePath = workspaceRoot.resolve(pattern);
            if (Files.exists(sourcePath) && Files.isDirectory(sourcePath)) {
                String pathStr = sourcePath.toAbsolutePath().toString();
                if (!classpath.contains(pathStr)) {
                    classpath.add(pathStr);
                    logger.debug("Added Groovy source path to classpath: {}", pathStr);
                }
            }
        }

        // Add build output directories if they exist
        String[] buildPatterns = {
            "build/classes/java/main",
            "build/classes/groovy/main",
            "target/classes",
            "out/production",
            "bin"
        };

        for (String pattern : buildPatterns) {
            Path buildPath = workspaceRoot.resolve(pattern);
            if (Files.exists(buildPath) && Files.isDirectory(buildPath)) {
                String pathStr = buildPath.toAbsolutePath().toString();
                if (!classpath.contains(pathStr)) {
                    classpath.add(pathStr);
                    logger.debug("Added build output path to classpath: {}", pathStr);
                }
            }
        }
    }
}
