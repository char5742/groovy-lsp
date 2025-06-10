package com.groovy.lsp.workspace.internal.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.groovy.lsp.shared.workspace.api.dto.SymbolInfo;
import com.groovy.lsp.shared.workspace.api.dto.SymbolKind;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Additional test cases for SymbolExtractorVisitor to improve branch coverage.
 */
class SymbolExtractorVisitorAdditionalTest {

    private Path testPath;
    private SymbolExtractorVisitor visitor;

    @BeforeEach
    void setUp() {
        testPath = Paths.get("test.groovy");
    }

    @Test
    void processClassNode_shouldHandleDuplicateClasses() {
        // Given - Create visitor and process a class
        visitor = new SymbolExtractorVisitor(testPath);
        ClassNode classNode = new ClassNode("com.test.TestClass", 0, null);
        classNode.setLineNumber(10);
        classNode.setColumnNumber(5);

        // When - Process the same class twice
        visitor.processClassNode(classNode);
        visitor.processClassNode(classNode); // Should be skipped

        // Then - Should only have one symbol
        List<SymbolInfo> symbols = visitor.getSymbols();
        assertThat(symbols).hasSize(1);
    }

    @Test
    void visitField_shouldHandleNullCurrentClassName() {
        // Given - Create visitor with no current class name
        visitor = new SymbolExtractorVisitor(testPath);
        FieldNode fieldNode = new FieldNode("testField", 0, null, null, null);
        fieldNode.setLineNumber(10);
        fieldNode.setColumnNumber(5);

        // When
        visitor.visitField(fieldNode);

        // Then
        List<SymbolInfo> symbols = visitor.getSymbols();
        assertThat(symbols).hasSize(1);
        assertThat(symbols.get(0).name()).isEqualTo("testField");
    }

    @Test
    void visitProperty_shouldHandleNullCurrentClassName() {
        // Given - Create visitor with no current class name
        visitor = new SymbolExtractorVisitor(testPath);
        // PropertyNode needs a FieldNode and statements
        FieldNode fieldNode = new FieldNode("testProperty", 0, null, null, null);
        PropertyNode propertyNode = new PropertyNode(fieldNode, 0, null, null);
        propertyNode.setLineNumber(10);
        propertyNode.setColumnNumber(5);

        // When
        visitor.visitProperty(propertyNode);

        // Then
        List<SymbolInfo> symbols = visitor.getSymbols();
        assertThat(symbols).hasSize(1);
        assertThat(symbols.get(0).name()).isEqualTo("testProperty");
    }

    @Test
    void visitClosureExpression_shouldHandleNullCurrentClassName() {
        // Given - Create visitor with no current class name
        visitor = new SymbolExtractorVisitor(testPath);
        ClosureExpression closureExpr =
                new ClosureExpression(new Parameter[0], new BlockStatement());
        closureExpr.setLineNumber(10);
        closureExpr.setColumnNumber(5);

        // When
        visitor.visitClosureExpression(closureExpr);

        // Then
        List<SymbolInfo> symbols = visitor.getSymbols();
        assertThat(symbols).hasSize(1);
        assertThat(symbols.get(0).name()).isEqualTo("<closure>");
    }

    @Test
    void getSourceUnit_shouldReturnNull() {
        // Given
        visitor = new SymbolExtractorVisitor(testPath);

        // When/Then - Call protected method via reflection
        try {
            java.lang.reflect.Method method =
                    SymbolExtractorVisitor.class.getDeclaredMethod("getSourceUnit");
            method.setAccessible(true);
            Object result = method.invoke(visitor);
            assertThat(result).isNull();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void isTrait_shouldDetectTraitByGroovyLangTraitInterface() {
        // Given - Create a class that implements groovy.lang.Trait
        visitor = new SymbolExtractorVisitor(testPath);
        ClassNode classNode = new ClassNode("com.test.TestTrait", 0, null);
        classNode.setInterfaces(new ClassNode[] {new ClassNode("groovy.lang.Trait", 0, null)});
        classNode.setModifiers(classNode.getModifiers() | org.objectweb.asm.Opcodes.ACC_INTERFACE);

        // When
        visitor.processClassNode(classNode);

        // Then
        List<SymbolInfo> symbols = visitor.getSymbols();
        assertThat(symbols).hasSize(1);
        assertThat(symbols.get(0).kind()).isEqualTo(SymbolKind.TRAIT);
    }

    @Test
    void isTrait_shouldDetectTraitByHelperClassInAllClassNames() {
        // Given - Create visitor with allClassNames containing helper
        Set<String> allClassNames = new HashSet<>();
        allClassNames.add("com.test.TestTrait$Trait$Helper");
        visitor = new SymbolExtractorVisitor(testPath, allClassNames);

        ClassNode classNode = new ClassNode("com.test.TestTrait", 0, null);
        classNode.setModifiers(classNode.getModifiers() | org.objectweb.asm.Opcodes.ACC_INTERFACE);

        // When
        visitor.processClassNode(classNode);

        // Then
        List<SymbolInfo> symbols = visitor.getSymbols();
        assertThat(symbols).hasSize(1);
        assertThat(symbols.get(0).kind()).isEqualTo(SymbolKind.TRAIT);
    }

    @Test
    void isTrait_shouldDetectTraitByHelperClassInVisitedClasses() {
        // Given - Create visitor and add helper to visited classes
        visitor = new SymbolExtractorVisitor(testPath);

        // First visit the helper class
        ClassNode helperNode = new ClassNode("com.test.TestTrait$Trait$Helper", 0, null);
        helperNode.setLineNumber(1);
        visitor.processClassNode(helperNode);

        // Then visit the trait interface
        ClassNode classNode = new ClassNode("com.test.TestTrait", 0, null);
        classNode.setModifiers(classNode.getModifiers() | org.objectweb.asm.Opcodes.ACC_INTERFACE);
        classNode.setLineNumber(10);

        // When
        visitor.processClassNode(classNode);

        // Then
        List<SymbolInfo> symbols = visitor.getSymbols();
        assertThat(symbols).hasSize(2);
        // Find the trait symbol
        SymbolInfo traitSymbol =
                symbols.stream()
                        .filter(s -> s.name().equals("com.test.TestTrait"))
                        .findFirst()
                        .orElseThrow();
        assertThat(traitSymbol.kind()).isEqualTo(SymbolKind.TRAIT);
    }

    @Test
    void isTrait_shouldDetectTraitByHelperClassInModule() {
        // Given - Create a module with helper class
        visitor = new SymbolExtractorVisitor(testPath);
        ModuleNode moduleNode = new ModuleNode((org.codehaus.groovy.control.SourceUnit) null);

        ClassNode helperNode = new ClassNode("com.test.TestTrait$Trait$Helper", 0, null);
        ClassNode traitNode = new ClassNode("com.test.TestTrait", 0, null);
        traitNode.setModifiers(traitNode.getModifiers() | org.objectweb.asm.Opcodes.ACC_INTERFACE);
        traitNode.setModule(moduleNode);

        moduleNode.addClass(helperNode);
        moduleNode.addClass(traitNode);

        // When
        visitor.processClassNode(traitNode);

        // Then
        List<SymbolInfo> symbols = visitor.getSymbols();
        assertThat(symbols).hasSize(1);
        assertThat(symbols.get(0).kind()).isEqualTo(SymbolKind.TRAIT);
    }

    @Test
    void visitClass_shouldProcessNestedClass() {
        // Given
        visitor = new SymbolExtractorVisitor(testPath);
        ClassNode nestedClass = new ClassNode("com.test.OuterClass$InnerClass", 0, null);
        nestedClass.setLineNumber(10);
        nestedClass.setColumnNumber(5);

        // When - Call visitClass directly (simulating nested class visit)
        visitor.visitClass(nestedClass);

        // Then
        List<SymbolInfo> symbols = visitor.getSymbols();
        assertThat(symbols).hasSize(1);
        assertThat(symbols.get(0).name()).isEqualTo("com.test.OuterClass$InnerClass");
    }

    @Test
    void visitVariableExpression_shouldDoNothing() {
        // Given
        visitor = new SymbolExtractorVisitor(testPath);
        VariableExpression varExpr = new VariableExpression("test");

        // When
        visitor.visitVariableExpression(varExpr);

        // Then - Should not add any symbols
        List<SymbolInfo> symbols = visitor.getSymbols();
        assertThat(symbols).isEmpty();
    }

    @Test
    void processClassNode_withNegativeLineNumbers_shouldUseMinimumOne() {
        // Given
        visitor = new SymbolExtractorVisitor(testPath);
        ClassNode classNode = new ClassNode("com.test.TestClass", 0, null);
        classNode.setLineNumber(-5);
        classNode.setColumnNumber(-10);

        // When
        visitor.processClassNode(classNode);

        // Then
        List<SymbolInfo> symbols = visitor.getSymbols();
        assertThat(symbols).hasSize(1);
        SymbolInfo symbol = symbols.get(0);
        assertThat(symbol.line()).isEqualTo(1);
        assertThat(symbol.column()).isEqualTo(1);
    }

    @Test
    void constructor_withSinglePathArgument_shouldUseEmptyClassNamesSet() {
        // Given/When
        visitor = new SymbolExtractorVisitor(testPath);

        // Process a trait to test that allClassNames is empty
        ClassNode traitNode = new ClassNode("com.test.TestTrait", 0, null);
        traitNode.setModifiers(traitNode.getModifiers() | org.objectweb.asm.Opcodes.ACC_INTERFACE);
        traitNode.setLineNumber(10);

        // When
        visitor.processClassNode(traitNode);

        // Then - Should be interface, not trait (because allClassNames is empty)
        List<SymbolInfo> symbols = visitor.getSymbols();
        assertThat(symbols).hasSize(1);
        assertThat(symbols.get(0).kind()).isEqualTo(SymbolKind.INTERFACE);
    }

    @Test
    void isTrait_shouldReturnFalseForNullModule() {
        // Given - Interface node without module
        visitor = new SymbolExtractorVisitor(testPath);
        ClassNode classNode = new ClassNode("com.test.TestInterface", 0, null);
        classNode.setModifiers(classNode.getModifiers() | org.objectweb.asm.Opcodes.ACC_INTERFACE);
        classNode.setModule(null); // Explicitly set to null

        // When
        visitor.processClassNode(classNode);

        // Then - Should be interface, not trait
        List<SymbolInfo> symbols = visitor.getSymbols();
        assertThat(symbols).hasSize(1);
        assertThat(symbols.get(0).kind()).isEqualTo(SymbolKind.INTERFACE);
    }
}
