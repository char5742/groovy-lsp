package com.groovy.lsp.groovy.core.api;

import java.util.List;
import java.util.Map;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.jspecify.annotations.Nullable;

/**
 * Service for managing incremental compilation of Groovy source files.
 * This service supports phase-based compilation to optimize LSP performance.
 */
public interface IncrementalCompilationService {

    /**
     * Creates a new compilation unit for incremental compilation.
     *
     * @param config the compiler configuration
     * @return a new CompilationUnit instance
     */
    CompilationUnit createCompilationUnit(CompilerConfiguration config);

    /**
     * Compiles a source file up to the specified phase.
     *
     * @param unit the compilation unit
     * @param sourceCode the source code to compile
     * @param sourceName the name of the source file
     * @param phase the target compilation phase
     * @return the compiled ModuleNode, or null if compilation fails
     */
    @Nullable ModuleNode compileToPhase(
            CompilationUnit unit, String sourceCode, String sourceName, CompilationPhase phase);

    /**
     * Compiles a source file up to the specified phase with detailed error information.
     *
     * @param unit the compilation unit
     * @param sourceCode the source code to compile
     * @param sourceName the name of the source file
     * @param phase the target compilation phase
     * @return the compilation result including AST and any errors
     */
    CompilationResult compileToPhaseWithResult(
            CompilationUnit unit, String sourceCode, String sourceName, CompilationPhase phase);

    /**
     * Incrementally updates a previously compiled module.
     *
     * @param unit the compilation unit
     * @param moduleNode the existing module node
     * @param sourceCode the updated source code
     * @param sourceName the name of the source file
     * @return the updated ModuleNode, or null if update fails
     */
    @Nullable ModuleNode updateModule(
            CompilationUnit unit, ModuleNode moduleNode, String sourceCode, String sourceName);

    /**
     * Gets the dependencies of a compiled module.
     *
     * @param moduleNode the module to analyze
     * @return a map of dependency names to their types
     */
    Map<String, DependencyType> getDependencies(ModuleNode moduleNode);

    /**
     * Determines which modules need recompilation based on changes.
     *
     * @param changedModule the module that changed
     * @param allModules all modules in the workspace
     * @return list of module names that need recompilation
     */
    List<String> getAffectedModules(String changedModule, Map<String, ModuleNode> allModules);

    /**
     * Clears the compilation cache for a specific source.
     *
     * @param sourceName the name of the source to clear from cache
     */
    void clearCache(String sourceName);

    /**
     * Clears all compilation caches.
     */
    void clearAllCaches();

    /**
     * Compilation phases supported by incremental compilation.
     */
    enum CompilationPhase {
        /** Initialization phase */
        INITIALIZATION,
        /** Parsing phase - produces AST */
        PARSING,
        /** Conversion phase - resolves imports */
        CONVERSION,
        /** Semantic analysis phase - type checking */
        SEMANTIC_ANALYSIS,
        /** Canonicalization phase - normalizes AST */
        CANONICALIZATION,
        /** Instruction selection phase */
        INSTRUCTION_SELECTION,
        /** Class generation phase */
        CLASS_GENERATION,
        /** Output phase */
        OUTPUT,
        /** Finalization phase */
        FINALIZATION
    }

    /**
     * Types of dependencies between modules.
     */
    enum DependencyType {
        /** Import dependency */
        IMPORT,
        /** Inheritance dependency */
        EXTENDS,
        /** Implementation dependency */
        IMPLEMENTS,
        /** Field type dependency */
        FIELD_TYPE,
        /** Method parameter/return type dependency */
        METHOD_TYPE,
        /** Annotation dependency */
        ANNOTATION
    }
}
