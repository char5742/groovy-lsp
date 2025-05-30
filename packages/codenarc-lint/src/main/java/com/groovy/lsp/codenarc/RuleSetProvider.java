package com.groovy.lsp.codenarc;

import org.codenarc.ruleset.CompositeRuleSet;
import org.codenarc.ruleset.PropertiesFileRuleSet;
import org.codenarc.ruleset.RuleSet;
import org.codenarc.ruleset.XmlFileRuleSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Manages CodeNarc rule sets for the linting engine.
 * Supports loading rule sets from various sources including
 * built-in defaults, XML files, and properties files.
 */
public class RuleSetProvider {
    private static final Logger logger = LoggerFactory.getLogger(RuleSetProvider.class);
    
    private static final String DEFAULT_RULESET_PATH = "rulesets/basic.xml";
    private static final String CUSTOM_RULESET_FILENAME = "codenarc-ruleset.xml";
    private static final String CUSTOM_PROPERTIES_FILENAME = "codenarc.properties";
    
    private final List<String> ruleSetPaths;
    private RuleSet cachedRuleSet;
    
    public RuleSetProvider() {
        this.ruleSetPaths = new ArrayList<>();
        initializeDefaultRuleSets();
    }
    
    /**
     * Initialize with default built-in rule sets.
     */
    private void initializeDefaultRuleSets() {
        // Add default CodeNarc rule sets
        ruleSetPaths.add("rulesets/basic.xml");
        ruleSetPaths.add("rulesets/imports.xml");
        ruleSetPaths.add("rulesets/groovyism.xml");
        ruleSetPaths.add("rulesets/convention.xml");
        ruleSetPaths.add("rulesets/design.xml");
    }
    
    /**
     * Get the configured rule set, loading from cache if available.
     * 
     * @return The configured RuleSet
     */
    public RuleSet getRuleSet() {
        if (cachedRuleSet == null) {
            cachedRuleSet = loadRuleSet();
        }
        return cachedRuleSet;
    }
    
    /**
     * Force reload of the rule set, clearing the cache.
     * 
     * @return The newly loaded RuleSet
     */
    public RuleSet reloadRuleSet() {
        cachedRuleSet = null;
        return getRuleSet();
    }
    
    /**
     * Add a custom rule set path.
     * 
     * @param ruleSetPath Path to the rule set (can be classpath resource or file path)
     */
    public void addRuleSetPath(String ruleSetPath) {
        ruleSetPaths.add(ruleSetPath);
        cachedRuleSet = null; // Clear cache to force reload
    }
    
    /**
     * Clear all rule set paths and reset to defaults.
     */
    public void resetToDefaults() {
        ruleSetPaths.clear();
        initializeDefaultRuleSets();
        cachedRuleSet = null;
    }
    
    /**
     * Load rule set from configuration.
     */
    private RuleSet loadRuleSet() {
        CompositeRuleSet compositeRuleSet = new CompositeRuleSet();
        
        // First, try to load custom rule sets from the project
        RuleSet customRuleSet = loadCustomRuleSet();
        if (customRuleSet != null) {
            compositeRuleSet.addRuleSet(customRuleSet);
            logger.info("Loaded custom rule set");
        }
        
        // Load configured rule sets
        for (String ruleSetPath : ruleSetPaths) {
            try {
                RuleSet ruleSet = loadRuleSetFromPath(ruleSetPath);
                if (ruleSet != null) {
                    compositeRuleSet.addRuleSet(ruleSet);
                    logger.debug("Loaded rule set: {}", ruleSetPath);
                }
            } catch (Exception e) {
                logger.warn("Failed to load rule set: " + ruleSetPath, e);
            }
        }
        
        // Load custom properties if available
        loadCustomProperties(compositeRuleSet);
        
        return compositeRuleSet;
    }
    
    /**
     * Try to load custom rule set from project directory.
     */
    private RuleSet loadCustomRuleSet() {
        // Look for custom rule set in project root
        Path projectRoot = findProjectRoot();
        if (projectRoot != null) {
            Path customRuleSetPath = projectRoot.resolve(CUSTOM_RULESET_FILENAME);
            if (Files.exists(customRuleSetPath)) {
                try {
                    return new XmlFileRuleSet(customRuleSetPath.toString());
                } catch (Exception e) {
                    logger.error("Failed to load custom rule set from: " + customRuleSetPath, e);
                }
            }
        }
        return null;
    }
    
    /**
     * Load rule set from a given path.
     */
    private RuleSet loadRuleSetFromPath(String path) {
        // First try as a file path
        File file = new File(path);
        if (file.exists()) {
            if (path.endsWith(".xml")) {
                return new XmlFileRuleSet(path);
            } else if (path.endsWith(".properties")) {
                return new PropertiesFileRuleSet(path);
            }
        }
        
        // Try as classpath resource
        if (path.endsWith(".xml")) {
            return new XmlFileRuleSet(path);
        } else if (path.endsWith(".properties")) {
            return new PropertiesFileRuleSet(path);
        }
        
        logger.warn("Unable to determine rule set type for: {}", path);
        return null;
    }
    
    /**
     * Load custom properties for rule configuration.
     */
    private void loadCustomProperties(CompositeRuleSet compositeRuleSet) {
        Path projectRoot = findProjectRoot();
        if (projectRoot != null) {
            Path propertiesPath = projectRoot.resolve(CUSTOM_PROPERTIES_FILENAME);
            if (Files.exists(propertiesPath)) {
                try {
                    Properties properties = new Properties();
                    try (InputStream is = Files.newInputStream(propertiesPath)) {
                        properties.load(is);
                    }
                    
                    // Apply properties to rules
                    compositeRuleSet.getRules().forEach(rule -> {
                        String rulePrefix = rule.getName() + ".";
                        properties.stringPropertyNames().stream()
                            .filter(key -> key.startsWith(rulePrefix))
                            .forEach(key -> {
                                String propertyName = key.substring(rulePrefix.length());
                                String value = properties.getProperty(key);
                                try {
                                    rule.setProperty(propertyName, value);
                                    logger.debug("Set property {} = {} for rule {}", 
                                        propertyName, value, rule.getName());
                                } catch (Exception e) {
                                    logger.warn("Failed to set property {} for rule {}: {}", 
                                        propertyName, rule.getName(), e.getMessage());
                                }
                            });
                    });
                    
                    logger.info("Loaded custom properties from: {}", propertiesPath);
                } catch (IOException e) {
                    logger.error("Failed to load custom properties", e);
                }
            }
        }
    }
    
    /**
     * Find the project root directory by looking for common project markers.
     */
    private Path findProjectRoot() {
        Path currentPath = Paths.get(System.getProperty("user.dir"));
        
        while (currentPath != null) {
            // Check for common project root indicators
            if (Files.exists(currentPath.resolve("build.gradle")) ||
                Files.exists(currentPath.resolve("build.gradle.kts")) ||
                Files.exists(currentPath.resolve("pom.xml")) ||
                Files.exists(currentPath.resolve(".git"))) {
                return currentPath;
            }
            currentPath = currentPath.getParent();
        }
        
        // Fallback to current directory
        return Paths.get(System.getProperty("user.dir"));
    }
    
    /**
     * Get the list of configured rule set paths.
     * 
     * @return List of rule set paths
     */
    public List<String> getRuleSetPaths() {
        return new ArrayList<>(ruleSetPaths);
    }
}