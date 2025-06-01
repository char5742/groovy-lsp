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
import org.codehaus.groovy.control.messages.Message;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import com.groovy.lsp.groovy.core.api.IncrementalCompilationService;
import com.groovy.lsp.groovy.core.api.CompilationResult;
import com.groovy.lsp.groovy.core.api.CompilationResult.CompilationError;

/**
 * Implementation of IncrementalCompilationService for phase-based compilation.
 * This implementation includes:
 * - LRU cache with configurable size limit
 * - Thread-safe operations for concurrent access
 * - TTL (Time To Live) for cache entries
 */
public class IncrementalCompilationServiceImpl implements IncrementalCompilationService {
    private static final Logger logger = LoggerFactory.getLogger(IncrementalCompilationServiceImpl.class);
    
    private static final int DEFAULT_MAX_CACHE_SIZE = 1000;
    private static final long DEFAULT_CACHE_TTL_MS = 30 * 60 * 1000; // 30 minutes
    
    private final int maxCacheSize;
    private final long cacheTtlMs;
    
    // Using LinkedHashMap with access order for LRU eviction
    private final Map<String, CompilationCacheEntry> compilationCache;
    private final Map<String, Set<String>> dependencyGraph = new ConcurrentHashMap<>();
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    
    public IncrementalCompilationServiceImpl() {
        this(DEFAULT_MAX_CACHE_SIZE, DEFAULT_CACHE_TTL_MS);
    }
    
    public IncrementalCompilationServiceImpl(int maxCacheSize, long cacheTtlMs) {
        this.maxCacheSize = maxCacheSize;
        this.cacheTtlMs = cacheTtlMs;
        this.compilationCache = new LinkedHashMap<String, CompilationCacheEntry>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CompilationCacheEntry> eldest) {
                boolean shouldRemove = size() > maxCacheSize;
                if (shouldRemove) {
                    logger.debug("Evicting oldest cache entry: {}", eldest.getKey());
                }
                return shouldRemove;
            }
        };
    }
    
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
            // Check cache first with read lock
            cacheLock.readLock().lock();
            try {
                CompilationCacheEntry cached = compilationCache.get(sourceName);
                if (cached != null && cached.sourceCode.equals(sourceCode) && 
                    cached.phase.ordinal() >= phase.ordinal() &&
                    !isCacheEntryExpired(cached)) {
                    logger.debug("Using cached compilation result for {}", sourceName);
                    return cached.moduleNode;
                }
            } finally {
                cacheLock.readLock().unlock();
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
                cacheLock.writeLock().lock();
                try {
                    compilationCache.put(sourceName, new CompilationCacheEntry(
                        sourceCode, moduleNode, phase, System.currentTimeMillis()
                    ));
                } finally {
                    cacheLock.writeLock().unlock();
                }
                
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
    public CompilationResult compileToPhaseWithResult(
            CompilationUnit unit,
            String sourceCode,
            String sourceName,
            CompilationPhase phase) {
        
        logger.debug("Compiling {} to phase {} with detailed results", sourceName, phase);
        
        try {
            // Check cache first with read lock
            cacheLock.readLock().lock();
            try {
                CompilationCacheEntry cached = compilationCache.get(sourceName);
                if (cached != null && cached.sourceCode.equals(sourceCode) && 
                    cached.phase.ordinal() >= phase.ordinal() &&
                    !isCacheEntryExpired(cached)) {
                    logger.debug("Using cached compilation result for {}", sourceName);
                    return CompilationResult.success(cached.moduleNode);
                }
            } finally {
                cacheLock.readLock().unlock();
            }
            
            // Create a new CompilationUnit for each compilation
            CompilationUnit compilationUnit = new CompilationUnit(unit.getConfiguration());
            
            // Create source unit with error collector
            ErrorCollector errorCollector = new ErrorCollector(compilationUnit.getConfiguration());
            SourceUnit sourceUnit = new SourceUnit(
                sourceName,
                new StringReaderSource(sourceCode, compilationUnit.getConfiguration()),
                compilationUnit.getConfiguration(),
                compilationUnit.getClassLoader(),
                errorCollector
            );
            
            compilationUnit.addSource(sourceUnit);
            
            // Compile to the requested phase
            int targetPhase = mapToGroovyPhase(phase);
            List<CompilationError> errors = new ArrayList<>();
            
            try {
                compilationUnit.compile(targetPhase);
            } catch (Exception compilationError) {
                // Collect errors from the error collector
                if (errorCollector.hasErrors()) {
                    List<? extends Message> messages = errorCollector.getErrors();
                    for (Message msg : messages) {
                        errors.add(CompilationError.fromGroovyMessage(msg, sourceName));
                    }
                } else {
                    // If no specific errors, create a generic error
                    errors.add(new CompilationError(
                        compilationError.getMessage(),
                        1, 1, sourceName,
                        CompilationError.ErrorType.SYNTAX
                    ));
                }
                
                logger.debug("Compilation of {} failed with {} errors", sourceName, errors.size());
                return CompilationResult.failure(errors);
            }
            
            // Get the module node
            ModuleNode moduleNode = sourceUnit.getAST();
            
            // Check for errors even if compilation succeeded
            if (errorCollector.hasErrors()) {
                List<? extends Message> messages = errorCollector.getErrors();
                for (Message msg : messages) {
                    errors.add(CompilationError.fromGroovyMessage(msg, sourceName));
                }
            }
            
            // Check for warnings
            if (errorCollector.hasWarnings()) {
                List<? extends Message> warnings = errorCollector.getWarnings();
                for (Message warning : warnings) {
                    CompilationError warningError = CompilationError.fromGroovyMessage(warning, sourceName);
                    errors.add(new CompilationError(
                        warningError.getMessage(),
                        warningError.getLine(),
                        warningError.getColumn(),
                        warningError.getSourceName(),
                        CompilationError.ErrorType.WARNING
                    ));
                }
            }
            
            if (moduleNode != null && errors.isEmpty()) {
                // Cache successful result
                cacheLock.writeLock().lock();
                try {
                    compilationCache.put(sourceName, new CompilationCacheEntry(
                        sourceCode, moduleNode, phase, System.currentTimeMillis()
                    ));
                } finally {
                    cacheLock.writeLock().unlock();
                }
                
                // Update dependency graph
                updateDependencyGraph(sourceName, moduleNode);
                
                return CompilationResult.success(moduleNode);
            } else if (moduleNode != null) {
                // Partial success - has AST but also errors
                return CompilationResult.partial(moduleNode, errors);
            } else {
                // Complete failure
                return CompilationResult.failure(errors);
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error compiling {} to phase {}", sourceName, phase, e);
            List<CompilationError> errors = Collections.singletonList(
                new CompilationError(
                    "Internal compilation error: " + e.getMessage(),
                    1, 1, sourceName,
                    CompilationError.ErrorType.SYNTAX
                )
            );
            return CompilationResult.failure(errors);
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
        cacheLock.writeLock().lock();
        try {
            compilationCache.remove(sourceName);
        } finally {
            cacheLock.writeLock().unlock();
        }
        dependencyGraph.remove(sourceName);
        logger.debug("Cleared cache for {}", sourceName);
    }
    
    @Override
    public void clearAllCaches() {
        cacheLock.writeLock().lock();
        try {
            compilationCache.clear();
        } finally {
            cacheLock.writeLock().unlock();
        }
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
        // Return the full class name for proper dependency tracking
        // This ensures accurate dependency resolution across packages
        return className;
    }
    
    private boolean isCacheEntryExpired(CompilationCacheEntry entry) {
        long currentTime = System.currentTimeMillis();
        boolean expired = (currentTime - entry.timestamp) > cacheTtlMs;
        if (expired) {
            logger.debug("Cache entry expired (age: {} ms)", currentTime - entry.timestamp);
        }
        return expired;
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