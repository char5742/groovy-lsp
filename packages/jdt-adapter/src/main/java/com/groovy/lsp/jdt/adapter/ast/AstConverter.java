package com.groovy.lsp.jdt.adapter.ast;

import org.codehaus.groovy.ast.*;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Converts between Groovy AST nodes and Eclipse JDT AST nodes.
 * This class handles the structural mapping between the two AST representations.
 */
public class AstConverter {
    private static final Logger logger = LoggerFactory.getLogger(AstConverter.class);
    
    /**
     * Converts a Groovy package node to JDT package declaration.
     */
    public void convertPackage(PackageNode packageNode, CompilationUnit compilationUnit) {
        if (packageNode == null || packageNode.getName() == null) {
            return;
        }
        
        AST ast = compilationUnit.getAST();
        PackageDeclaration packageDecl = ast.newPackageDeclaration();
        packageDecl.setName(createQualifiedName(ast, packageNode.getName()));
        compilationUnit.setPackage(packageDecl);
    }
    
    /**
     * Converts a Groovy import node to JDT import declaration.
     */
    public void convertImport(ImportNode importNode, CompilationUnit compilationUnit) {
        AST ast = compilationUnit.getAST();
        ImportDeclaration importDecl = ast.newImportDeclaration();
        
        String className = importNode.getClassName();
        if (importNode.getAlias() != null && !importNode.getAlias().equals(extractSimpleName(className))) {
            // Handle aliased imports - JDT doesn't support aliases directly
            // We'll need to handle this through type resolution
            logger.warn("Import alias '{}' for '{}' cannot be directly represented in JDT", 
                       importNode.getAlias(), className);
        }
        
        importDecl.setName(createQualifiedName(ast, className));
        importDecl.setStatic(false);
        compilationUnit.imports().add(importDecl);
    }
    
    /**
     * Converts a Groovy star import to JDT on-demand import.
     */
    public void convertStarImport(ImportNode starImport, CompilationUnit compilationUnit) {
        AST ast = compilationUnit.getAST();
        ImportDeclaration importDecl = ast.newImportDeclaration();
        
        String packageName = starImport.getPackageName();
        if (packageName != null && !packageName.isEmpty()) {
            importDecl.setName(createQualifiedName(ast, packageName));
            importDecl.setOnDemand(true);
            importDecl.setStatic(false);
            compilationUnit.imports().add(importDecl);
        }
    }
    
    /**
     * Converts a Groovy static import to JDT static import.
     */
    public void convertStaticImport(ImportNode importNode, String alias, CompilationUnit compilationUnit) {
        AST ast = compilationUnit.getAST();
        ImportDeclaration importDecl = ast.newImportDeclaration();
        
        String className = importNode.getClassName();
        String fieldName = importNode.getFieldName();
        
        if (fieldName != null) {
            // Static member import
            Name typeName = createQualifiedName(ast, className);
            SimpleName memberName = ast.newSimpleName(fieldName);
            importDecl.setName(ast.newQualifiedName(typeName, memberName));
        } else {
            // Static type import
            importDecl.setName(createQualifiedName(ast, className));
        }
        
        importDecl.setStatic(true);
        compilationUnit.imports().add(importDecl);
    }
    
    /**
     * Converts a Groovy static star import to JDT static on-demand import.
     */
    public void convertStaticStarImport(ImportNode importNode, String alias, CompilationUnit compilationUnit) {
        AST ast = compilationUnit.getAST();
        ImportDeclaration importDecl = ast.newImportDeclaration();
        
        importDecl.setName(createQualifiedName(ast, importNode.getClassName()));
        importDecl.setStatic(true);
        importDecl.setOnDemand(true);
        compilationUnit.imports().add(importDecl);
    }
    
    /**
     * Converts a Groovy class node to JDT type declaration.
     */
    public void convertClass(ClassNode classNode, CompilationUnit compilationUnit) {
        AST ast = compilationUnit.getAST();
        
        TypeDeclaration typeDecl = ast.newTypeDeclaration();
        typeDecl.setName(ast.newSimpleName(classNode.getNameWithoutPackage()));
        
        // Set modifiers
        int modifiers = convertModifiers(classNode.getModifiers());
        typeDecl.modifiers().addAll(ast.newModifiers(modifiers));
        
        // Set interface flag
        typeDecl.setInterface(classNode.isInterface());
        
        // Convert superclass
        if (classNode.getSuperClass() != null && 
            !classNode.getSuperClass().getName().equals("java.lang.Object")) {
            Type superType = createType(ast, classNode.getSuperClass());
            typeDecl.setSuperclassType(superType);
        }
        
        // Convert interfaces
        for (ClassNode iface : classNode.getInterfaces()) {
            Type interfaceType = createType(ast, iface);
            typeDecl.superInterfaceTypes().add(interfaceType);
        }
        
        // Convert fields
        for (FieldNode field : classNode.getFields()) {
            convertField(field, typeDecl);
        }
        
        // Convert methods
        for (MethodNode method : classNode.getMethods()) {
            convertMethod(method, typeDecl);
        }
        
        // Convert constructors
        for (ConstructorNode constructor : classNode.getDeclaredConstructors()) {
            convertConstructor(constructor, typeDecl);
        }
        
        // Convert inner classes
        for (ClassNode innerClass : classNode.getInnerClasses()) {
            if (innerClass != classNode) { // Avoid self-reference
                convertClass(innerClass, compilationUnit);
            }
        }
        
        compilationUnit.types().add(typeDecl);
    }
    
    /**
     * Converts a Groovy field node to JDT field declaration.
     */
    private void convertField(FieldNode fieldNode, TypeDeclaration typeDecl) {
        AST ast = typeDecl.getAST();
        
        VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
        fragment.setName(ast.newSimpleName(fieldNode.getName()));
        
        // TODO: Convert initializer expression if present
        
        FieldDeclaration fieldDecl = ast.newFieldDeclaration(fragment);
        fieldDecl.setType(createType(ast, fieldNode.getType()));
        fieldDecl.modifiers().addAll(ast.newModifiers(convertModifiers(fieldNode.getModifiers())));
        
        typeDecl.bodyDeclarations().add(fieldDecl);
    }
    
    /**
     * Converts a Groovy method node to JDT method declaration.
     */
    private void convertMethod(MethodNode methodNode, TypeDeclaration typeDecl) {
        AST ast = typeDecl.getAST();
        
        MethodDeclaration methodDecl = ast.newMethodDeclaration();
        methodDecl.setName(ast.newSimpleName(methodNode.getName()));
        methodDecl.setReturnType2(createType(ast, methodNode.getReturnType()));
        methodDecl.modifiers().addAll(ast.newModifiers(convertModifiers(methodNode.getModifiers())));
        
        // Convert parameters
        for (Parameter param : methodNode.getParameters()) {
            SingleVariableDeclaration paramDecl = ast.newSingleVariableDeclaration();
            paramDecl.setName(ast.newSimpleName(param.getName()));
            paramDecl.setType(createType(ast, param.getType()));
            methodDecl.parameters().add(paramDecl);
        }
        
        // TODO: Convert method body
        Block body = ast.newBlock();
        methodDecl.setBody(body);
        
        typeDecl.bodyDeclarations().add(methodDecl);
    }
    
    /**
     * Converts a Groovy constructor node to JDT method declaration.
     */
    private void convertConstructor(ConstructorNode constructorNode, TypeDeclaration typeDecl) {
        AST ast = typeDecl.getAST();
        
        MethodDeclaration constructorDecl = ast.newMethodDeclaration();
        constructorDecl.setName(typeDecl.getName());
        constructorDecl.setConstructor(true);
        constructorDecl.modifiers().addAll(ast.newModifiers(convertModifiers(constructorNode.getModifiers())));
        
        // Convert parameters
        for (Parameter param : constructorNode.getParameters()) {
            SingleVariableDeclaration paramDecl = ast.newSingleVariableDeclaration();
            paramDecl.setName(ast.newSimpleName(param.getName()));
            paramDecl.setType(createType(ast, param.getType()));
            constructorDecl.parameters().add(paramDecl);
        }
        
        // TODO: Convert constructor body
        Block body = ast.newBlock();
        constructorDecl.setBody(body);
        
        typeDecl.bodyDeclarations().add(constructorDecl);
    }
    
    // Reverse conversion methods (JDT to Groovy)
    
    public void convertJdtPackage(PackageDeclaration packageDecl, ModuleNode moduleNode) {
        String packageName = packageDecl.getName().getFullyQualifiedName();
        moduleNode.setPackage(new PackageNode(packageName));
    }
    
    public void convertJdtImport(Object importObj, ModuleNode moduleNode) {
        if (!(importObj instanceof ImportDeclaration)) {
            return;
        }
        
        ImportDeclaration importDecl = (ImportDeclaration) importObj;
        String importName = importDecl.getName().getFullyQualifiedName();
        
        if (importDecl.isOnDemand()) {
            // Star import
            if (importDecl.isStatic()) {
                ImportNode starImport = new ImportNode(null, importName, null);
                moduleNode.addStaticStarImport(importName, starImport);
            } else {
                ImportNode starImport = new ImportNode(null, null, null);
                starImport.setPackageName(importName + ".");
                moduleNode.addStarImport(starImport);
            }
        } else {
            // Regular import
            if (importDecl.isStatic()) {
                // Extract class and member names
                int lastDot = importName.lastIndexOf('.');
                String className = importName.substring(0, lastDot);
                String memberName = importName.substring(lastDot + 1);
                ImportNode staticImport = new ImportNode(className, memberName, null);
                moduleNode.addStaticImport(className, memberName, memberName);
            } else {
                ImportNode importNode = new ImportNode(importName, null);
                moduleNode.addImport(importNode);
            }
        }
    }
    
    public void convertJdtType(Object typeObj, ModuleNode moduleNode) {
        if (!(typeObj instanceof TypeDeclaration)) {
            return;
        }
        
        TypeDeclaration typeDecl = (TypeDeclaration) typeObj;
        
        // Create ClassNode
        String className = moduleNode.getPackageName() != null ? 
            moduleNode.getPackageName() + typeDecl.getName().getIdentifier() :
            typeDecl.getName().getIdentifier();
            
        ClassNode classNode = new ClassNode(className, convertJdtModifiers(typeDecl), null, null, null);
        
        // TODO: Convert superclass, interfaces, members, etc.
        
        moduleNode.addClass(classNode);
    }
    
    // Helper methods
    
    private Name createQualifiedName(AST ast, String qualifiedName) {
        String[] parts = qualifiedName.split("\\.");
        if (parts.length == 1) {
            return ast.newSimpleName(parts[0]);
        }
        
        Name name = ast.newSimpleName(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            SimpleName simpleName = ast.newSimpleName(parts[i]);
            name = ast.newQualifiedName(name, simpleName);
        }
        return name;
    }
    
    private Type createType(AST ast, ClassNode classNode) {
        String typeName = classNode.getName();
        
        // Handle primitive types
        if (classNode.isPrimaryClassNode()) {
            return ast.newPrimitiveType(getPrimitiveTypeCode(typeName));
        }
        
        // Handle array types
        if (classNode.isArray()) {
            Type componentType = createType(ast, classNode.getComponentType());
            return ast.newArrayType(componentType);
        }
        
        // Handle parameterized types
        if (classNode.isUsingGenerics() && classNode.getGenericsTypes() != null) {
            ParameterizedType paramType = ast.newParameterizedType(
                ast.newSimpleType(createQualifiedName(ast, classNode.getName()))
            );
            // TODO: Add type arguments
            return paramType;
        }
        
        // Regular type
        return ast.newSimpleType(createQualifiedName(ast, typeName));
    }
    
    private PrimitiveType.Code getPrimitiveTypeCode(String typeName) {
        switch (typeName) {
            case "boolean": return PrimitiveType.BOOLEAN;
            case "byte": return PrimitiveType.BYTE;
            case "char": return PrimitiveType.CHAR;
            case "double": return PrimitiveType.DOUBLE;
            case "float": return PrimitiveType.FLOAT;
            case "int": return PrimitiveType.INT;
            case "long": return PrimitiveType.LONG;
            case "short": return PrimitiveType.SHORT;
            case "void": return PrimitiveType.VOID;
            default: throw new IllegalArgumentException("Unknown primitive type: " + typeName);
        }
    }
    
    private int convertModifiers(int groovyModifiers) {
        int jdtModifiers = 0;
        
        if ((groovyModifiers & org.objectweb.asm.Opcodes.ACC_PUBLIC) != 0) {
            jdtModifiers |= Modifier.PUBLIC;
        }
        if ((groovyModifiers & org.objectweb.asm.Opcodes.ACC_PRIVATE) != 0) {
            jdtModifiers |= Modifier.PRIVATE;
        }
        if ((groovyModifiers & org.objectweb.asm.Opcodes.ACC_PROTECTED) != 0) {
            jdtModifiers |= Modifier.PROTECTED;
        }
        if ((groovyModifiers & org.objectweb.asm.Opcodes.ACC_STATIC) != 0) {
            jdtModifiers |= Modifier.STATIC;
        }
        if ((groovyModifiers & org.objectweb.asm.Opcodes.ACC_FINAL) != 0) {
            jdtModifiers |= Modifier.FINAL;
        }
        if ((groovyModifiers & org.objectweb.asm.Opcodes.ACC_ABSTRACT) != 0) {
            jdtModifiers |= Modifier.ABSTRACT;
        }
        if ((groovyModifiers & org.objectweb.asm.Opcodes.ACC_SYNCHRONIZED) != 0) {
            jdtModifiers |= Modifier.SYNCHRONIZED;
        }
        if ((groovyModifiers & org.objectweb.asm.Opcodes.ACC_NATIVE) != 0) {
            jdtModifiers |= Modifier.NATIVE;
        }
        
        return jdtModifiers;
    }
    
    private int convertJdtModifiers(TypeDeclaration typeDecl) {
        int groovyModifiers = 0;
        List modifiers = typeDecl.modifiers();
        
        for (Object mod : modifiers) {
            if (mod instanceof Modifier) {
                Modifier modifier = (Modifier) mod;
                if (modifier.isPublic()) groovyModifiers |= org.objectweb.asm.Opcodes.ACC_PUBLIC;
                if (modifier.isPrivate()) groovyModifiers |= org.objectweb.asm.Opcodes.ACC_PRIVATE;
                if (modifier.isProtected()) groovyModifiers |= org.objectweb.asm.Opcodes.ACC_PROTECTED;
                if (modifier.isStatic()) groovyModifiers |= org.objectweb.asm.Opcodes.ACC_STATIC;
                if (modifier.isFinal()) groovyModifiers |= org.objectweb.asm.Opcodes.ACC_FINAL;
                if (modifier.isAbstract()) groovyModifiers |= org.objectweb.asm.Opcodes.ACC_ABSTRACT;
            }
        }
        
        return groovyModifiers;
    }
    
    private String extractSimpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }
}