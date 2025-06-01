package com.groovy.lsp.groovy.core.internal.impl;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.ImportNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.ErrorCollector;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.io.StringReaderSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.groovy.lsp.groovy.core.api.IncrementalCompilationService;

/**
 * Implementation of IncrementalCompilationService for phase-based compilation.
 */
public class IncrementalCompilationServiceImpl implements IncrementalCompilationService {
    private static final Logger logger = LoggerFactory.getLogger(IncrementalCompilationServiceImpl.class);
    
    private final Map<String, CompilationCacheEntry> compilationCache = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> dependencyGraph = new ConcurrentHashMap<>();
    
    @Override
    public CompilationUnit createCompilationUnit(CompilerConfiguration config) {
        return new CompilationUnit(config);
    }
    
    @Override
    @Nullable
    public ModuleNode compileToPhase(
            CompilationUnit unit, 
            String sourceCode, 
            String sourceName,
            CompilationPhase phase) {
        
        logger.debug("Compiling {} to phase {}", sourceName, phase);
        
        CompilationUnit compilationUnit = null;
        try {
            // Check cache first
            CompilationCacheEntry cached = compilationCache.get(sourceName);
            if (cached != null && cached.sourceCode.equals(sourceCode) && 
                cached.phase.ordinal() >= phase.ordinal()) {
                logger.debug("Using cached compilation result for {}", sourceName);
                return cached.moduleNode;
            }
            
            // Create a new CompilationUnit for each compilation to avoid state issues
            compilationUnit = new CompilationUnit(unit.getConfiguration());
            
            // Create source unit
            SourceUnit sourceUnit = new SourceUnit(
                sourceName,
                new StringReaderSource(sourceCode, compilationUnit.getConfiguration()),
                compilationUnit.getConfiguration(),
                compilationUnit.getClassLoader(),
                new ErrorCollector(compilationUnit.getConfiguration())
            );
            
            compilationUnit.addSource(sourceUnit);
            
            // Compile to the requested phase
            int targetPhase = mapToGroovyPhase(phase);
            try {
                compilationUnit.compile(targetPhase);
            } catch (Exception compilationError) {
                // If compilation fails with an exception, return null
                logger.debug("Compilation of {} failed with exception: {}", sourceName, compilationError.getMessage());
                return null;
            }
            
            // Get the module node from the source unit
            ModuleNode moduleNode = sourceUnit.getAST();
            
            // Check for compilation errors
            ErrorCollector errorCollector = sourceUnit.getErrorCollector();
            boolean hasErrors = errorCollector != null && errorCollector.hasErrors();
            
            if (hasErrors) {
                logger.debug("Compilation of {} has errors", sourceName);
                return null;
            }
            
            if (moduleNode != null) {
                // Cache the result only if there are no errors
                compilationCache.put(sourceName, new CompilationCacheEntry(
                    sourceCode, moduleNode, phase, System.currentTimeMillis()
                ));
                
                // Update dependency graph
                updateDependencyGraph(sourceName, moduleNode);
            }
            
            return moduleNode;
            
        } catch (Exception e) {
            logger.error("Failed to compile {} to phase {}", sourceName, phase, e);
            return null;
        }
    }
    
    @Override
    @Nullable
    public ModuleNode updateModule(
            CompilationUnit unit,
            ModuleNode moduleNode,
            String sourceCode,
            String sourceName) {
        
        logger.debug("Incrementally updating module {}", sourceName);
        
        // For now, do a full recompilation
        // In a more advanced implementation, we could do partial updates
        return compileToPhase(unit, sourceCode, sourceName, CompilationPhase.SEMANTIC_ANALYSIS);
    }
    
    @Override
    public Map<String, DependencyType> getDependencies(ModuleNode moduleNode) {
        Map<String, DependencyType> dependencies = new HashMap<>();
        
        // Collect import dependencies
        for (ImportNode importNode : moduleNode.getImports()) {
            String className = importNode.getClassName();
            dependencies.put(className, DependencyType.IMPORT);
        }
        
        // Collect star imports
        for (ImportNode importNode : moduleNode.getStarImports()) {
            String packageName = importNode.getPackageName();
            // Remove trailing dot if present
            if (packageName.endsWith(".")) {
                packageName = packageName.substring(0, packageName.length() - 1);
            }
            String starImport = packageName + ".*";
            dependencies.put(starImport, DependencyType.IMPORT);
        }
        
        // Analyze classes
        for (ClassNode classNode : moduleNode.getClasses()) {
            // Superclass dependency
            ClassNode superClass = classNode.getSuperClass();
            if (superClass != null && !superClass.getName().equals("java.lang.Object")) {
                dependencies.put(superClass.getName(), DependencyType.EXTENDS);
            }
            
            // Interface dependencies
            for (ClassNode iface : classNode.getInterfaces()) {
                dependencies.put(iface.getName(), DependencyType.IMPLEMENTS);
            }
            
            // Field type dependencies
            for (FieldNode field : classNode.getFields()) {
                ClassNode fieldType = field.getType();
                if (!ClassHelper.isPrimitiveType(fieldType)) {
                    dependencies.put(fieldType.getName(), DependencyType.FIELD_TYPE);
                }
            }
            
            // Method dependencies
            for (MethodNode method : classNode.getMethods()) {
                // Return type
                ClassNode returnType = method.getReturnType();
                if (!ClassHelper.isPrimitiveType(returnType)) {
                    dependencies.put(returnType.getName(), DependencyType.METHOD_TYPE);
                }
                
                // Parameter types
                for (Parameter param : method.getParameters()) {
                    ClassNode paramType = param.getType();
                    if (!ClassHelper.isPrimitiveType(paramType)) {
                        dependencies.put(paramType.getName(), DependencyType.METHOD_TYPE);
                    }
                }
            }
            
            // Annotation dependencies
            for (AnnotationNode annotation : classNode.getAnnotations()) {
                String annotationName = annotation.getClassNode().getName();
                // Only add if not already in dependencies
                if (!dependencies.containsKey(annotationName)) {
                    dependencies.put(annotationName, DependencyType.ANNOTATION);
                }
            }
        }
        
        return dependencies;
    }
    
    @Override
    public List<String> getAffectedModules(String changedModule, Map<String, ModuleNode> allModules) {
        Set<String> affected = new HashSet<>();
        Queue<String> toProcess = new LinkedList<>();
        toProcess.add(changedModule);
        
        // Extract the class name from the changed module
        String changedClassName = changedModule.replace(".groovy", "");
        
        logger.debug("Finding modules affected by changes to {}", changedModule);
        logger.debug("Current dependency graph: {}", dependencyGraph);
        
        while (!toProcess.isEmpty()) {
            String current = toProcess.poll();
            String currentClassName = current.replace(".groovy", "");
            
            logger.debug("Processing: {} (className: {})", current, currentClassName);
            
            // Find modules that depend on the current module
            for (Map.Entry<String, Set<String>> entry : dependencyGraph.entrySet()) {
                String moduleName = entry.getKey();
                if (!affected.contains(moduleName) && !moduleName.equals(changedModule)) {
                    // Check if this module depends on the current class
                    for (String dependency : entry.getValue()) {
                        logger.debug("  Checking if {} depends on {} (comparing with '{}')", 
                                   moduleName, dependency, currentClassName);
                        if (dependency.equals(currentClassName) || 
                            dependency.equals(current)) {
                            logger.debug("  -> Found dependency! {} is affected", moduleName);
                            affected.add(moduleName);
                            toProcess.add(moduleName);
                            break;
                        }
                    }
                }
            }
        }
        
        logger.debug("Affected modules for {}: {}", changedModule, affected);
        return new ArrayList<>(affected);
    }
    
    @Override
    public void clearCache(String sourceName) {
        compilationCache.remove(sourceName);
        dependencyGraph.remove(sourceName);
        logger.debug("Cleared cache for {}", sourceName);
    }
    
    @Override
    public void clearAllCaches() {
        compilationCache.clear();
        dependencyGraph.clear();
        logger.debug("Cleared all compilation caches");
    }
    
    private int mapToGroovyPhase(CompilationPhase phase) {
        switch (phase) {
            case INITIALIZATION:
                return Phases.INITIALIZATION;
            case PARSING:
                // PARSING phase doesn't produce AST, need at least CONVERSION
                return Phases.CONVERSION;
            case CONVERSION:
                return Phases.CONVERSION;
            case SEMANTIC_ANALYSIS:
                return Phases.SEMANTIC_ANALYSIS;
            case CANONICALIZATION:
                return Phases.CANONICALIZATION;
            case INSTRUCTION_SELECTION:
                return Phases.INSTRUCTION_SELECTION;
            case CLASS_GENERATION:
                return Phases.CLASS_GENERATION;
            case OUTPUT:
                return Phases.OUTPUT;
            case FINALIZATION:
                return Phases.FINALIZATION;
            default:
                return Phases.SEMANTIC_ANALYSIS;
        }
    }
    
    private void updateDependencyGraph(String sourceName, ModuleNode moduleNode) {
        if (moduleNode == null) {
            logger.warn("ModuleNode is null for {}, skipping dependency graph update", sourceName);
            return;
        }
        
        Map<String, DependencyType> dependencies = getDependencies(moduleNode);
        Set<String> dependencyNames = dependencies.keySet().stream()
            .map(className -> {
                // Don't normalize star imports
                if (className.endsWith(".*")) {
                    return className;
                }
                return normalizeClassName(className);
            })
            .collect(Collectors.toSet());
        
        dependencyGraph.put(sourceName, dependencyNames);
        logger.debug("Updated dependency graph for {}: {}", sourceName, dependencyNames);
    }
    
    private String normalizeClassName(String className) {
        // For local classes in the same package, just return the simple name
        // This allows matching between "A" and "A.groovy"
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(lastDot + 1) : className;
    }
    
    private static class CompilationCacheEntry {
        final String sourceCode;
        final ModuleNode moduleNode;
        final CompilationPhase phase;
        final long timestamp;
        
        CompilationCacheEntry(String sourceCode, ModuleNode moduleNode, 
                            CompilationPhase phase, long timestamp) {
            this.sourceCode = sourceCode;
            this.moduleNode = moduleNode;
            this.phase = phase;
            this.timestamp = timestamp;
        }
    }
}