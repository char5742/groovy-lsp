package com.groovy.lsp.workspace.internal.jar;

import com.groovy.lsp.workspace.api.dto.SymbolInfo;
import com.groovy.lsp.workspace.api.dto.SymbolKind;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM ClassVisitor implementation that extracts symbols from class files.
 * Visits classes, methods, and fields to create SymbolInfo instances.
 */
public class ClassFileVisitor extends ClassVisitor {
    private final List<SymbolInfo> symbols = new ArrayList<>();
    private final Path jarPath;
    private String className = "";
    private String qualifiedClassName = "";
    private Path jarEntryPath = Paths.get("");

    public ClassFileVisitor(Path jarPath) {
        super(Opcodes.ASM9);
        this.jarPath = jarPath;
    }

    @Override
    public void visit(
            int version,
            int access,
            String name,
            String signature,
            String superName,
            String[] interfaces) {
        // Convert internal name (com/example/MyClass) to qualified name (com.example.MyClass)
        qualifiedClassName = name.replace('/', '.');

        // Extract simple class name
        int lastDot = qualifiedClassName.lastIndexOf('.');
        if (lastDot > 0) {
            className = qualifiedClassName.substring(lastDot + 1);
        } else {
            className = qualifiedClassName;
        }

        // Create virtual path for the JAR entry
        jarEntryPath = Paths.get(jarPath + "!/" + name + ".class");

        // Create symbol for the class itself
        SymbolInfo classSymbol =
                new SymbolInfo(
                        className,
                        determineClassKind(access),
                        jarEntryPath,
                        1, // Line number not available for JAR entries
                        1 // Column number not available for JAR entries
                        );

        symbols.add(classSymbol);
    }

    @Override
    @Nullable
    public FieldVisitor visitField(
            int access, String name, String descriptor, String signature, Object value) {
        // Create symbol for field
        String fieldFullName = qualifiedClassName + "." + name;
        SymbolInfo fieldSymbol =
                new SymbolInfo(
                        fieldFullName,
                        SymbolKind.FIELD,
                        jarEntryPath,
                        1, // Line number not available for JAR entries
                        1 // Column number not available for JAR entries
                        );

        symbols.add(fieldSymbol);

        return null; // We don't need to visit field annotations
    }

    @Override
    @Nullable
    public MethodVisitor visitMethod(
            int access, String name, String descriptor, String signature, String[] exceptions) {
        // Skip synthetic methods
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
            return null;
        }

        // Create symbol for method
        String methodFullName = qualifiedClassName + "." + name;
        SymbolInfo methodSymbol =
                new SymbolInfo(
                        methodFullName,
                        determineMethodKind(name),
                        jarEntryPath,
                        1, // Line number not available for JAR entries
                        1 // Column number not available for JAR entries
                        );

        symbols.add(methodSymbol);

        return null; // We don't need to visit method instructions
    }

    /**
     * Determine the symbol kind for a class based on its access flags.
     */
    private SymbolKind determineClassKind(int access) {
        if ((access & Opcodes.ACC_INTERFACE) != 0) {
            return SymbolKind.INTERFACE;
        } else if ((access & Opcodes.ACC_ENUM) != 0) {
            return SymbolKind.ENUM;
        } else {
            return SymbolKind.CLASS;
        }
    }

    /**
     * Determine the symbol kind for a method based on its name.
     */
    private SymbolKind determineMethodKind(String methodName) {
        if ("<init>".equals(methodName)) {
            return SymbolKind.CONSTRUCTOR;
        } else {
            return SymbolKind.METHOD;
        }
    }

    /**
     * Get all symbols extracted from the class.
     */
    public List<SymbolInfo> getSymbols() {
        return symbols;
    }
}
