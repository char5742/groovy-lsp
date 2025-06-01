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
        
        try {
            // Check cache first
            CompilationCacheEntry cached = compilationCache.get(sourceName);
            if (cached != null && cached.sourceCode.equals(sourceCode) && 
                cached.phase.ordinal() >= phase.ordinal()) {
                logger.debug("Using cached compilation result for {}", sourceName);
                return cached.moduleNode;
            }
            
            // Create source unit
            SourceUnit sourceUnit = new SourceUnit(
                sourceName,
                new StringReaderSource(sourceCode, unit.getConfiguration()),
                unit.getConfiguration(),
                unit.getClassLoader(),
                new ErrorCollector(unit.getConfiguration())
            );
            
            unit.addSource(sourceUnit);
            
            // Compile to the requested phase
            int targetPhase = mapToGroovyPhase(phase);
            unit.compile(targetPhase);
            
            ModuleNode moduleNode = sourceUnit.getAST();
            
            // Cache the result
            compilationCache.put(sourceName, new CompilationCacheEntry(
                sourceCode, moduleNode, phase, System.currentTimeMillis()
            ));
            
            // Update dependency graph
            updateDependencyGraph(sourceName, moduleNode);
            
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
            dependencies.put(importNode.getClassName(), DependencyType.IMPORT);
        }
        
        // Collect star imports
        for (ImportNode importNode : moduleNode.getStarImports()) {
            dependencies.put(importNode.getPackageName() + ".*", DependencyType.IMPORT);
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
                dependencies.put(annotation.getClassNode().getName(), DependencyType.ANNOTATION);
            }
        }
        
        return dependencies;
    }
    
    @Override
    public List<String> getAffectedModules(String changedModule, Map<String, ModuleNode> allModules) {
        Set<String> affected = new HashSet<>();
        Queue<String> toProcess = new LinkedList<>();
        toProcess.add(changedModule);
        
        while (!toProcess.isEmpty()) {
            String current = toProcess.poll();
            
            // Find modules that depend on the current module
            for (Map.Entry<String, Set<String>> entry : dependencyGraph.entrySet()) {
                if (entry.getValue().contains(current) && !affected.contains(entry.getKey())) {
                    affected.add(entry.getKey());
                    toProcess.add(entry.getKey());
                }
            }
        }
        
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
                return Phases.PARSING;
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
        Map<String, DependencyType> dependencies = getDependencies(moduleNode);
        Set<String> dependencyNames = dependencies.keySet().stream()
            .map(this::normalizeClassName)
            .collect(Collectors.toSet());
        
        dependencyGraph.put(sourceName, dependencyNames);
        logger.debug("Updated dependency graph for {}: {}", sourceName, dependencyNames);
    }
    
    private String normalizeClassName(String className) {
        // Remove package prefix for local classes
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