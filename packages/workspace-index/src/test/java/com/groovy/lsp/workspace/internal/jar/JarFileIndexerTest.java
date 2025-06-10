package com.groovy.lsp.workspace.internal.jar;

import static org.assertj.core.api.Assertions.assertThat;

import com.groovy.lsp.shared.workspace.api.dto.SymbolInfo;
import com.groovy.lsp.shared.workspace.api.dto.SymbolKind;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class JarFileIndexerTest {

    @TempDir @Nullable Path tempDir;

    private JarFileIndexer jarFileIndexer;

    @BeforeEach
    void setUp() {
        jarFileIndexer = new JarFileIndexer();
    }

    @Test
    void testIndexJar_EmptyJar() throws IOException {
        // Create empty JAR
        Path jarFile =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("empty.jar");
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile.toFile()))) {
            // Empty JAR
        }

        List<SymbolInfo> symbols = jarFileIndexer.indexJar(jarFile);

        assertThat(symbols).isEmpty();
    }

    @Test
    void testIndexJar_WithClasses() throws IOException {
        // Create JAR with sample classes
        Path jarFile =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("test.jar");
        createTestJar(jarFile);

        List<SymbolInfo> symbols = jarFileIndexer.indexJar(jarFile);

        // Should have symbols for:
        // - TestClass (class)
        // - TestClass.testField (field)
        // - TestClass.<init> (constructor)
        // - TestClass.testMethod (method)
        assertThat(symbols).hasSize(4);

        // Verify class symbol
        SymbolInfo classSymbol =
                symbols.stream()
                        .filter(s -> s.name().equals("TestClass") && s.kind() == SymbolKind.CLASS)
                        .findFirst()
                        .orElse(null);
        assertThat(classSymbol).isNotNull();
        assertThat(
                        Objects.requireNonNull(classSymbol, "classSymbol should not be null")
                                .location()
                                .toString())
                .contains("test.jar");

        // Verify field symbol
        SymbolInfo fieldSymbol =
                symbols.stream()
                        .filter(s -> s.name().contains("testField") && s.kind() == SymbolKind.FIELD)
                        .findFirst()
                        .orElse(null);
        assertThat(fieldSymbol).isNotNull();

        // Verify constructor symbol
        SymbolInfo constructorSymbol =
                symbols.stream()
                        .filter(
                                s ->
                                        s.name().contains("<init>")
                                                && s.kind() == SymbolKind.CONSTRUCTOR)
                        .findFirst()
                        .orElse(null);
        assertThat(constructorSymbol).isNotNull();

        // Verify method symbol
        SymbolInfo methodSymbol =
                symbols.stream()
                        .filter(
                                s ->
                                        s.name().contains("testMethod")
                                                && s.kind() == SymbolKind.METHOD)
                        .findFirst()
                        .orElse(null);
        assertThat(methodSymbol).isNotNull();
    }

    @Test
    void testIndexJar_WithInterface() throws IOException {
        // Create JAR with interface
        Path jarFile =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("interface.jar");
        createJarWithInterface(jarFile);

        List<SymbolInfo> symbols = jarFileIndexer.indexJar(jarFile);

        // Verify interface symbol
        SymbolInfo interfaceSymbol =
                symbols.stream()
                        .filter(
                                s ->
                                        s.name().equals("TestInterface")
                                                && s.kind() == SymbolKind.INTERFACE)
                        .findFirst()
                        .orElse(null);
        assertThat(interfaceSymbol).isNotNull();
    }

    @Test
    void testIndexJar_NonExistentFile() {
        Path nonExistentJar =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("nonexistent.jar");

        List<SymbolInfo> symbols = jarFileIndexer.indexJar(nonExistentJar);

        assertThat(symbols).isEmpty();
    }

    @Test
    void testIsRelevantJar() throws IOException {
        // Create JAR with classes
        Path jarFile =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("relevant.jar");
        createTestJar(jarFile);

        assertThat(jarFileIndexer.isRelevantJar(jarFile)).isTrue();

        // Test non-existent JAR
        Path nonExistentJar =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("nonexistent.jar");
        assertThat(jarFileIndexer.isRelevantJar(nonExistentJar)).isFalse();
    }

    private void createTestJar(Path jarFile) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile.toFile()))) {
            // Create a test class using ASM
            byte[] classBytes = createTestClass();

            JarEntry entry = new JarEntry("com/example/TestClass.class");
            jos.putNextEntry(entry);
            jos.write(classBytes);
            jos.closeEntry();
        }
    }

    private void createJarWithInterface(Path jarFile) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile.toFile()))) {
            // Create a test interface using ASM
            byte[] interfaceBytes = createTestInterface();

            JarEntry entry = new JarEntry("com/example/TestInterface.class");
            jos.putNextEntry(entry);
            jos.write(interfaceBytes);
            jos.closeEntry();
        }
    }

    private byte[] createTestClass() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        // Create class
        cw.visit(
                Opcodes.V11,
                Opcodes.ACC_PUBLIC,
                "com/example/TestClass",
                null,
                "java/lang/Object",
                null);

        // Add field
        cw.visitField(Opcodes.ACC_PRIVATE, "testField", "Ljava/lang/String;", null, null);

        // Add constructor
        MethodVisitor constructor = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(
                Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(1, 1);
        constructor.visitEnd();

        // Add method
        MethodVisitor method =
                cw.visitMethod(
                        Opcodes.ACC_PUBLIC, "testMethod", "()Ljava/lang/String;", null, null);
        method.visitCode();
        method.visitLdcInsn("Hello");
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(1, 1);
        method.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private byte[] createTestInterface() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        // Create interface
        cw.visit(
                Opcodes.V11,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT,
                "com/example/TestInterface",
                null,
                "java/lang/Object",
                null);

        // Add abstract method
        cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "doSomething", "()V", null, null);

        cw.visitEnd();
        return cw.toByteArray();
    }

    @Test
    void testIndexJar_WithTooManyEntries() throws IOException {
        // Create JAR with more than MAX_ENTRIES
        Path jarFile =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("too-many-entries.jar");

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile.toFile()))) {
            byte[] classBytes = createTestClass();

            // Try to add more than MAX_ENTRIES (100,000)
            for (int i = 0; i < 100_005; i++) {
                JarEntry entry = new JarEntry("com/example/TestClass" + i + ".class");
                jos.putNextEntry(entry);
                jos.write(classBytes);
                jos.closeEntry();
            }
        }

        List<SymbolInfo> symbols = jarFileIndexer.indexJar(jarFile);

        // Should process only up to MAX_ENTRIES
        assertThat(symbols).isNotEmpty();
        // Each class has 4 symbols (class, field, constructor, method), so 100,000 * 4 = 400,000
        assertThat(symbols.size()).isLessThanOrEqualTo(400_000);
    }

    @Test
    void testIndexJar_WithOversizedEntry() throws IOException {
        Path jarFile =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("oversized-entry.jar");

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile.toFile()))) {
            // Add normal entry
            byte[] normalClassBytes = createTestClass();
            JarEntry normalEntry = new JarEntry("com/example/NormalClass.class");
            jos.putNextEntry(normalEntry);
            jos.write(normalClassBytes);
            jos.closeEntry();

            // Try to add oversized entry (> 50MB)
            JarEntry oversizedEntry = new JarEntry("com/example/OversizedClass.class");
            oversizedEntry.setSize(60 * 1024 * 1024); // 60MB
            jos.putNextEntry(oversizedEntry);
            // Write dummy data
            byte[] dummyData = new byte[1024];
            for (int i = 0; i < 60 * 1024; i++) {
                jos.write(dummyData);
            }
            jos.closeEntry();
        }

        List<SymbolInfo> symbols = jarFileIndexer.indexJar(jarFile);

        // Should process only the normal entry, skip the oversized one
        assertThat(symbols)
                .hasSize(4); // Only symbols from NormalClass (class, field, constructor, method)
    }
}
