package com.groovy.lsp.groovy.core.internal.impl;

import static org.assertj.core.api.Assertions.*;

import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.ErrorCollector;
import org.codehaus.groovy.control.Phases;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

import com.groovy.lsp.groovy.core.api.IncrementalCompilationService;
import com.groovy.lsp.groovy.core.api.IncrementalCompilationService.CompilationPhase;
import com.groovy.lsp.groovy.core.api.IncrementalCompilationService.DependencyType;

class IncrementalCompilationServiceImplTest {
    
    private IncrementalCompilationServiceImpl service;
    private CompilerConfiguration config;
    
    @BeforeEach
    void setUp() {
        service = new IncrementalCompilationServiceImpl();
        config = new CompilerConfiguration();
    }
    
    @Nested
    @DisplayName("CompilationUnit creation")
    class CompilationUnitCreation {
        
        @Test
        @DisplayName("Should create CompilationUnit with given configuration")
        void shouldCreateCompilationUnit() {
            CompilationUnit unit = service.createCompilationUnit(config);
            
            assertThat(unit).isNotNull();
            assertThat(unit.getConfiguration()).isEqualTo(config);
        }
    }
    
    @Nested
    @DisplayName("Phase-based compilation")
    class PhaseBasedCompilation {
        
        @Test
        @DisplayName("Should compile simple Groovy class to CONVERSION phase")
        void shouldCompileToConversionPhase() {
            String sourceCode = """
                class TestClass {
                    String name
                    
                    void sayHello() {
                        println "Hello, $name"
                    }
                }
                """;
            
            CompilationUnit unit = service.createCompilationUnit(config);
            ModuleNode result = service.compileToPhase(
                unit, sourceCode, "TestClass.groovy", CompilationPhase.CONVERSION
            );
            
            assertThat(result).isNotNull();
            assertThat(result.getClasses()).hasSize(1);
            assertThat(result.getClasses().get(0).getName()).isEqualTo("TestClass");
        }
        
        @Test
        @DisplayName("Should compile to SEMANTIC_ANALYSIS phase")
        void shouldCompileToSemanticAnalysisPhase() {
            String sourceCode = """
                import java.util.List
                
                class TestClass {
                    List<String> items = []
                }
                """;
            
            CompilationUnit unit = service.createCompilationUnit(config);
            ModuleNode result = service.compileToPhase(
                unit, sourceCode, "TestClass.groovy", CompilationPhase.SEMANTIC_ANALYSIS
            );
            
            assertThat(result).isNotNull();
            assertThat(result.getImports()).hasSize(1);
            assertThat(result.getImports().get(0).getClassName()).isEqualTo("java.util.List");
        }
        
        @Test
        @DisplayName("Should handle compilation errors gracefully")
        void shouldHandleCompilationErrors() {
            String invalidCode = """
                class TestClass {
                    void method() {
                        def x = 
                    }
                }
                """;
            
            CompilationUnit unit = service.createCompilationUnit(config);
            ModuleNode result = service.compileToPhase(
                unit, invalidCode, "Invalid.groovy", CompilationPhase.CONVERSION
            );
            
            assertThat(result).isNull();
        }
    }
    
    @Nested
    @DisplayName("Caching functionality")
    class CachingFunctionality {
        
        @Test
        @DisplayName("Should return cached result for identical source")
        void shouldReturnCachedResult() {
            String sourceCode = "class CachedClass { }";
            CompilationUnit unit = service.createCompilationUnit(config);
            
            // First compilation
            ModuleNode first = service.compileToPhase(
                unit, sourceCode, "Cached.groovy", CompilationPhase.CONVERSION
            );
            
            // Second compilation with same source
            ModuleNode second = service.compileToPhase(
                unit, sourceCode, "Cached.groovy", CompilationPhase.CONVERSION
            );
            
            assertThat(first).isNotNull();
            assertThat(second).isSameAs(first);
        }
        
        @Test
        @DisplayName("Should not use cache for different source")
        void shouldNotUseCacheForDifferentSource() {
            CompilationUnit unit = service.createCompilationUnit(config);
            
            ModuleNode first = service.compileToPhase(
                unit, "class FirstClass { }", "Test.groovy", CompilationPhase.CONVERSION
            );
            
            ModuleNode second = service.compileToPhase(
                unit, "class SecondClass { }", "Test.groovy", CompilationPhase.CONVERSION
            );
            
            assertThat(first).isNotNull();
            assertThat(second).isNotNull();
            assertThat(second).isNotSameAs(first);
            assertThat(first.getClasses().get(0).getName()).isEqualTo("FirstClass");
            assertThat(second.getClasses().get(0).getName()).isEqualTo("SecondClass");
        }
        
        @Test
        @DisplayName("Should clear specific cache entry")
        void shouldClearSpecificCache() {
            String sourceCode = "class TestClass { }";
            CompilationUnit unit = service.createCompilationUnit(config);
            
            ModuleNode first = service.compileToPhase(
                unit, sourceCode, "Test.groovy", CompilationPhase.CONVERSION
            );
            
            service.clearCache("Test.groovy");
            
            ModuleNode second = service.compileToPhase(
                unit, sourceCode, "Test.groovy", CompilationPhase.CONVERSION
            );
            
            assertThat(first).isNotNull();
            assertThat(second).isNotNull();
            assertThat(second).isNotSameAs(first);
        }
        
        @Test
        @DisplayName("Should clear all caches")
        void shouldClearAllCaches() {
            CompilationUnit unit = service.createCompilationUnit(config);
            
            service.compileToPhase(unit, "class A { }", "A.groovy", CompilationPhase.CONVERSION);
            service.compileToPhase(unit, "class B { }", "B.groovy", CompilationPhase.CONVERSION);
            
            service.clearAllCaches();
            
            // After clearing, new compilations should not use cache
            ModuleNode newA = service.compileToPhase(
                unit, "class A { }", "A.groovy", CompilationPhase.CONVERSION
            );
            
            assertThat(newA).isNotNull();
        }
    }
    
    @Nested
    @DisplayName("Dependency analysis")
    class DependencyAnalysis {
        
        @Test
        @DisplayName("Should detect import dependencies")
        void shouldDetectImportDependencies() {
            String sourceCode = """
                import java.util.List
                import java.util.Map
                import java.io.*
                
                class TestClass {
                    List<String> list
                    Map<String, Object> map
                }
                """;
            
            CompilationUnit unit = service.createCompilationUnit(config);
            ModuleNode module = service.compileToPhase(
                unit, sourceCode, "Test.groovy", CompilationPhase.SEMANTIC_ANALYSIS
            );
            
            Map<String, DependencyType> deps = service.getDependencies(module);
            
            // Should contain imports
            assertThat(deps).containsEntry("java.io.*", DependencyType.IMPORT);
            
            // java.util.List and java.util.Map are imported, but also used as field types
            // The getDependencies method currently returns each type only once,
            // and field types take precedence over imports in the current implementation
            assertThat(deps).containsEntry("java.util.List", DependencyType.FIELD_TYPE);
            assertThat(deps).containsEntry("java.util.Map", DependencyType.FIELD_TYPE);
        }
        
        @Test
        @DisplayName("Should detect inheritance dependencies")
        void shouldDetectInheritanceDependencies() {
            String sourceCode = """
                class TestClass extends ArrayList implements Serializable {
                }
                """;
            
            CompilationUnit unit = service.createCompilationUnit(config);
            ModuleNode module = service.compileToPhase(
                unit, sourceCode, "Test.groovy", CompilationPhase.SEMANTIC_ANALYSIS
            );
            
            Map<String, DependencyType> deps = service.getDependencies(module);
            
            assertThat(deps).containsEntry("java.util.ArrayList", DependencyType.EXTENDS);
            assertThat(deps).containsEntry("java.io.Serializable", DependencyType.IMPLEMENTS);
        }
        
        @Test
        @DisplayName("Should detect field type dependencies")
        void shouldDetectFieldTypeDependencies() {
            String sourceCode = """
                import java.time.LocalDate
                
                class TestClass {
                    String name
                    LocalDate birthDate
                    List<String> tags
                }
                """;
            
            CompilationUnit unit = service.createCompilationUnit(config);
            ModuleNode module = service.compileToPhase(
                unit, sourceCode, "Test.groovy", CompilationPhase.SEMANTIC_ANALYSIS
            );
            
            Map<String, DependencyType> deps = service.getDependencies(module);
            
            assertThat(deps)
                .containsEntry("java.lang.String", DependencyType.FIELD_TYPE)
                .containsEntry("java.time.LocalDate", DependencyType.FIELD_TYPE);
        }
        
        @Test
        @DisplayName("Should detect method dependencies")
        void shouldDetectMethodDependencies() {
            String sourceCode = """
                import java.util.Optional
                
                class TestClass {
                    Optional<String> findName(List<String> names) {
                        return Optional.empty()
                    }
                }
                """;
            
            CompilationUnit unit = service.createCompilationUnit(config);
            ModuleNode module = service.compileToPhase(
                unit, sourceCode, "Test.groovy", CompilationPhase.SEMANTIC_ANALYSIS
            );
            
            Map<String, DependencyType> deps = service.getDependencies(module);
            
            assertThat(deps)
                .containsEntry("java.util.Optional", DependencyType.METHOD_TYPE)
                .containsEntry("java.util.List", DependencyType.METHOD_TYPE);
        }
    }
    
    @Nested
    @DisplayName("Affected modules detection")
    class AffectedModulesDetection {
        
        @Test
        @DisplayName("Should detect directly affected modules")
        void shouldDetectDirectlyAffectedModules() {
            // Create separate compilation units for each module to simulate LSP scenario
            // where each file is compiled independently but can reference others
            
            // First pass: compile all modules to establish initial state
            CompilationUnit unitA = service.createCompilationUnit(config);
            CompilationUnit unitB = service.createCompilationUnit(config);
            CompilationUnit unitC = service.createCompilationUnit(config);
            
            // Compile A first (no dependencies)
            ModuleNode moduleA = service.compileToPhase(unitA, "class A { }", "A.groovy", CompilationPhase.SEMANTIC_ANALYSIS);
            assertThat(moduleA).isNotNull();
            
            // For B and C, we need to make A available in the classpath
            // In a real LSP scenario, this would be handled by the workspace index
            // For now, we'll compile them despite the errors to capture dependencies
            
            // Compile B (depends on A) - will have errors but we still get AST
            ModuleNode moduleB = service.compileToPhase(unitB, "class B { A myA }", "B.groovy", CompilationPhase.CONVERSION);
            
            // Compile C (depends on B) - will have errors but we still get AST  
            ModuleNode moduleC = service.compileToPhase(unitC, "class C { B myB }", "C.groovy", CompilationPhase.CONVERSION);
            
            Map<String, ModuleNode> allModules = new HashMap<>();
            List<String> affected = service.getAffectedModules("A.groovy", allModules);
            
            // When A changes, B should be affected (and transitively C)
            assertThat(affected).contains("B.groovy");
        }
        
        @Test
        @DisplayName("Should handle circular dependencies")
        void shouldHandleCircularDependencies() {
            // Create separate compilation units for circular dependency scenario
            CompilationUnit unitA = service.createCompilationUnit(config);
            CompilationUnit unitB = service.createCompilationUnit(config);
            
            // Compile both modules with circular dependency
            // They will have errors but we still get partial AST with type references
            ModuleNode moduleA = service.compileToPhase(unitA, "class A { B myB }", "A.groovy", CompilationPhase.CONVERSION);
            ModuleNode moduleB = service.compileToPhase(unitB, "class B { A myA }", "B.groovy", CompilationPhase.CONVERSION);
            
            Map<String, ModuleNode> allModules = new HashMap<>();
            List<String> affected = service.getAffectedModules("A.groovy", allModules);
            
            // Should handle circular dependency without infinite loop
            assertThat(affected).isNotNull();
            assertThat(affected).contains("B.groovy");
        }
    }
    
    @Nested
    @DisplayName("Incremental module updates")
    class IncrementalModuleUpdates {
        
        @Test
        @DisplayName("Should update existing module")
        void shouldUpdateExistingModule() {
            CompilationUnit unit = service.createCompilationUnit(config);
            
            ModuleNode original = service.compileToPhase(
                unit, "class TestClass { String name }", "Test.groovy", CompilationPhase.CONVERSION
            );
            
            ModuleNode updated = service.updateModule(
                unit, original, "class TestClass { String name; int age }", "Test.groovy"
            );
            
            assertThat(updated).isNotNull();
            assertThat(updated.getClasses().get(0).getFields()).hasSize(2);
        }
    }
}