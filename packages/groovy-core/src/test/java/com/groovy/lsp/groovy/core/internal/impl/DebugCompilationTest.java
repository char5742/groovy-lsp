package com.groovy.lsp.groovy.core.internal.impl;

import com.groovy.lsp.test.annotations.UnitTest;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.ErrorCollector;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.io.StringReaderSource;

class DebugCompilationTest {

    @UnitTest
    void testParsingPhase() {
        String sourceCode =
                """
                class TestClass {
                    String name

                    void sayHello() {
                        println "Hello, $name"
                    }
                }
                """;

        // Test PARSING phase
        System.out.println("Testing PARSING phase:");
        CompilerConfiguration config = new CompilerConfiguration();
        CompilationUnit unit = new CompilationUnit(config);

        SourceUnit sourceUnit =
                new SourceUnit(
                        "TestClass.groovy",
                        new StringReaderSource(sourceCode, config),
                        config,
                        unit.getClassLoader(),
                        new ErrorCollector(config));

        unit.addSource(sourceUnit);

        try {
            unit.compile(Phases.PARSING);
            System.out.println("Compiled to PARSING phase");
            ModuleNode ast = sourceUnit.getAST();
            System.out.println("AST after PARSING: " + ast);
        } catch (Exception e) {
            System.out.println("Error during PARSING: " + e.getMessage());
        }

        // Test CONVERSION phase
        System.out.println("\nTesting CONVERSION phase:");
        CompilationUnit unit2 = new CompilationUnit(config);

        SourceUnit sourceUnit2 =
                new SourceUnit(
                        "TestClass.groovy",
                        new StringReaderSource(sourceCode, config),
                        config,
                        unit2.getClassLoader(),
                        new ErrorCollector(config));

        unit2.addSource(sourceUnit2);

        try {
            unit2.compile(Phases.CONVERSION);
            System.out.println("Compiled to CONVERSION phase");
            ModuleNode ast = sourceUnit2.getAST();
            System.out.println("AST after CONVERSION: " + ast);
            if (ast != null && ast.getClasses() != null) {
                System.out.println(
                        "Classes: " + ast.getClasses().stream().map(c -> c.getName()).toList());
            }
        } catch (Exception e) {
            System.out.println("Error during CONVERSION: " + e.getMessage());
        }
    }
}
