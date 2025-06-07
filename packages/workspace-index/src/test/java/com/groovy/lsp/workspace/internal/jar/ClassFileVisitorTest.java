package com.groovy.lsp.workspace.internal.jar;

import static org.assertj.core.api.Assertions.assertThat;

import com.groovy.lsp.workspace.api.dto.SymbolInfo;
import com.groovy.lsp.workspace.api.dto.SymbolKind;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class ClassFileVisitorTest {
    
    private Path mockJarPath;
    
    @BeforeEach
    void setUp() {
        mockJarPath = Paths.get("/test/library.jar");
    }
    
    @Test
    void testVisitClass() {
        byte[] classBytes = createSimpleClass();
        ClassReader reader = new ClassReader(classBytes);
        ClassFileVisitor visitor = new ClassFileVisitor(mockJarPath);
        
        reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        
        List<SymbolInfo> symbols = visitor.getSymbols();
        assertThat(symbols).hasSize(1);
        
        SymbolInfo classSymbol = symbols.get(0);
        assertThat(classSymbol.name()).isEqualTo("SimpleClass");
        assertThat(classSymbol.kind()).isEqualTo(SymbolKind.CLASS);
        assertThat(classSymbol.location().toString()).contains("library.jar");
    }
    
    @Test
    void testVisitClassWithPackage() {
        byte[] classBytes = createClassWithPackage();
        ClassReader reader = new ClassReader(classBytes);
        ClassFileVisitor visitor = new ClassFileVisitor(mockJarPath);
        
        reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        
        List<SymbolInfo> symbols = visitor.getSymbols();
        assertThat(symbols).hasSize(1);
        
        SymbolInfo classSymbol = symbols.get(0);
        assertThat(classSymbol.name()).isEqualTo("PackagedClass");
        assertThat(classSymbol.kind()).isEqualTo(SymbolKind.CLASS);
    }
    
    @Test
    void testVisitEnum() {
        byte[] enumBytes = createEnum();
        ClassReader reader = new ClassReader(enumBytes);
        ClassFileVisitor visitor = new ClassFileVisitor(mockJarPath);
        
        reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        
        List<SymbolInfo> symbols = visitor.getSymbols();
        
        SymbolInfo enumSymbol = symbols.stream()
                .filter(s -> s.kind() == SymbolKind.ENUM)
                .findFirst()
                .orElse(null);
        assertThat(enumSymbol).isNotNull();
        assertThat(enumSymbol.name()).isEqualTo("TestEnum");
    }
    
    @Test
    void testVisitMethodsAndFields() {
        byte[] classBytes = createClassWithMembers();
        ClassReader reader = new ClassReader(classBytes);
        ClassFileVisitor visitor = new ClassFileVisitor(mockJarPath);
        
        reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        
        List<SymbolInfo> symbols = visitor.getSymbols();
        
        // Should have: 1 class + 2 fields + 1 constructor + 2 methods = 6 symbols
        assertThat(symbols).hasSize(6);
        
        // Check for fields
        long fieldCount = symbols.stream()
                .filter(s -> s.kind() == SymbolKind.FIELD)
                .count();
        assertThat(fieldCount).isEqualTo(2);
        
        // Check for constructor
        boolean hasConstructor = symbols.stream()
                .anyMatch(s -> s.kind() == SymbolKind.CONSTRUCTOR);
        assertThat(hasConstructor).isTrue();
        
        // Check for methods
        long methodCount = symbols.stream()
                .filter(s -> s.kind() == SymbolKind.METHOD)
                .count();
        assertThat(methodCount).isEqualTo(2);
    }
    
    @Test
    void testSkipSyntheticMethods() {
        byte[] classBytes = createClassWithSyntheticMethod();
        ClassReader reader = new ClassReader(classBytes);
        ClassFileVisitor visitor = new ClassFileVisitor(mockJarPath);
        
        reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        
        List<SymbolInfo> symbols = visitor.getSymbols();
        
        // Should not include synthetic methods
        boolean hasSynthetic = symbols.stream()
                .anyMatch(s -> s.name().contains("synthetic"));
        assertThat(hasSynthetic).isFalse();
    }
    
    private byte[] createSimpleClass() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V11, Opcodes.ACC_PUBLIC, "SimpleClass", null, "java/lang/Object", null);
        cw.visitEnd();
        return cw.toByteArray();
    }
    
    private byte[] createClassWithPackage() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V11, Opcodes.ACC_PUBLIC, "com/example/PackagedClass", null, "java/lang/Object", null);
        cw.visitEnd();
        return cw.toByteArray();
    }
    
    private byte[] createEnum() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V11, Opcodes.ACC_PUBLIC | Opcodes.ACC_ENUM, "TestEnum", null, "java/lang/Enum", null);
        cw.visitEnd();
        return cw.toByteArray();
    }
    
    private byte[] createClassWithMembers() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V11, Opcodes.ACC_PUBLIC, "TestClass", null, "java/lang/Object", null);
        
        // Add fields
        cw.visitField(Opcodes.ACC_PRIVATE, "field1", "Ljava/lang/String;", null, null);
        cw.visitField(Opcodes.ACC_PUBLIC, "field2", "I", null, null);
        
        // Add constructor
        MethodVisitor constructor = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitEnd();
        
        // Add methods
        MethodVisitor method1 = cw.visitMethod(Opcodes.ACC_PUBLIC, "method1", "()V", null, null);
        method1.visitEnd();
        
        MethodVisitor method2 = cw.visitMethod(Opcodes.ACC_PRIVATE, "method2", "(Ljava/lang/String;)I", null, null);
        method2.visitEnd();
        
        cw.visitEnd();
        return cw.toByteArray();
    }
    
    private byte[] createClassWithSyntheticMethod() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V11, Opcodes.ACC_PUBLIC, "TestClass", null, "java/lang/Object", null);
        
        // Add regular method
        MethodVisitor regularMethod = cw.visitMethod(Opcodes.ACC_PUBLIC, "regularMethod", "()V", null, null);
        regularMethod.visitEnd();
        
        // Add synthetic method (should be skipped)
        MethodVisitor syntheticMethod = cw.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, 
                "syntheticMethod", 
                "()V", 
                null, 
                null);
        syntheticMethod.visitEnd();
        
        cw.visitEnd();
        return cw.toByteArray();
    }
}