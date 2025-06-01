package com.groovy.lsp.jdt.adapter.ast;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.codehaus.groovy.ast.*;
import org.eclipse.jdt.core.dom.*;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts between Groovy AST nodes and Eclipse JDT AST nodes.
 * This class handles the structural mapping between the two AST representations.
 */
public class AstConverter {
    private static final Logger logger = LoggerFactory.getLogger(AstConverter.class);

    /**
     * Default constructor for AstConverter.
     */
    public AstConverter() {
        // Default constructor
    }

    /**
     * Converts a Groovy package node to JDT package declaration.
     * @param packageNode the Groovy package node to convert, may be null
     * @param compilationUnit the JDT compilation unit to add the package declaration to
     */
    public void convertPackage(@Nullable PackageNode packageNode, CompilationUnit compilationUnit) {
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
     * @param importNode the Groovy import node to convert
     * @param compilationUnit the JDT compilation unit to add the import declaration to
     */
    public void convertImport(ImportNode importNode, CompilationUnit compilationUnit) {
        AST ast = compilationUnit.getAST();
        ImportDeclaration importDecl = ast.newImportDeclaration();

        String className = importNode.getClassName();
        if (importNode.getAlias() != null
                && !importNode.getAlias().equals(extractSimpleName(className))) {
            // Handle aliased imports - JDT doesn't support aliases directly
            // We'll need to handle this through type resolution
            logger.warn(
                    "Import alias '{}' for '{}' cannot be directly represented in JDT",
                    importNode.getAlias(),
                    className);
        }

        importDecl.setName(createQualifiedName(ast, className));
        importDecl.setStatic(false);
        compilationUnit.imports().add(importDecl);
    }

    /**
     * Converts a Groovy star import to JDT on-demand import.
     * @param starImport the Groovy star import node to convert
     * @param compilationUnit the JDT compilation unit to add the import declaration to
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
     * @param importNode the Groovy import node to convert
     * @param alias the import alias, if any
     * @param compilationUnit the JDT compilation unit to add the static import to
     */
    public void convertStaticImport(
            ImportNode importNode, String alias, CompilationUnit compilationUnit) {
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
     * @param importNode the Groovy import node to convert
     * @param alias the import alias, if any
     * @param compilationUnit the JDT compilation unit to add the static import to
     */
    public void convertStaticStarImport(
            ImportNode importNode, String alias, CompilationUnit compilationUnit) {
        AST ast = compilationUnit.getAST();
        ImportDeclaration importDecl = ast.newImportDeclaration();

        importDecl.setName(createQualifiedName(ast, importNode.getClassName()));
        importDecl.setStatic(true);
        importDecl.setOnDemand(true);
        compilationUnit.imports().add(importDecl);
    }

    /**
     * Converts a Groovy class node to JDT type declaration.
     * @param classNode the Groovy class node to convert
     * @param compilationUnit the JDT compilation unit to add the type declaration to
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
        if (classNode.getSuperClass() != null
                && !classNode.getSuperClass().getName().equals("java.lang.Object")) {
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
        Iterator<InnerClassNode> innerClassIter = classNode.getInnerClasses();
        while (innerClassIter != null && innerClassIter.hasNext()) {
            InnerClassNode innerClassNode = innerClassIter.next();
            // InnerClassNode extends ClassNode, so we can use it directly
            if (!innerClassNode.equals(classNode)) { // Avoid self-reference
                convertClass(innerClassNode, compilationUnit);
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
        methodDecl
                .modifiers()
                .addAll(ast.newModifiers(convertModifiers(methodNode.getModifiers())));

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
        constructorDecl
                .modifiers()
                .addAll(ast.newModifiers(convertModifiers(constructorNode.getModifiers())));

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

    /**
     * Converts a JDT package declaration to Groovy package node.
     * @param packageDecl the JDT package declaration to convert
     * @param moduleNode the Groovy module node to set the package on
     */
    public void convertJdtPackage(PackageDeclaration packageDecl, ModuleNode moduleNode) {
        String packageName = packageDecl.getName().getFullyQualifiedName();
        moduleNode.setPackage(new PackageNode(packageName));
    }

    /**
     * Converts a JDT import declaration to Groovy import node.
     * @param importObj the JDT import object to convert
     * @param moduleNode the Groovy module node to add the import to
     */
    public void convertJdtImport(Object importObj, ModuleNode moduleNode) {
        if (!(importObj instanceof ImportDeclaration importDecl)) {
            return;
        }
        String importName = importDecl.getName().getFullyQualifiedName();

        if (importDecl.isOnDemand()) {
            // Star import
            if (importDecl.isStatic()) {
                // For static star imports, we need the class type
                ClassNode type = ClassHelper.make(importName);
                moduleNode.addStaticStarImport(importName, type);
            } else {
                // For package star imports, just pass the package name
                moduleNode.addStarImport(importName + ".");
            }
        } else {
            // Regular import
            if (importDecl.isStatic()) {
                // Extract class and member names
                int lastDot = importName.lastIndexOf('.');
                String className = importName.substring(0, lastDot);
                String memberName = importName.substring(lastDot + 1);
                ClassNode type = ClassHelper.make(className);
                moduleNode.addStaticImport(type, memberName, memberName);
            } else {
                // For regular imports, create the ClassNode and add it
                ClassNode type = ClassHelper.make(importName);
                String alias = importName.substring(importName.lastIndexOf('.') + 1);
                moduleNode.addImport(alias, type);
            }
        }
    }

    /**
     * Converts a JDT type declaration to Groovy class node.
     * @param typeObj the JDT type object to convert
     * @param moduleNode the Groovy module node to add the class to
     */
    public void convertJdtType(Object typeObj, ModuleNode moduleNode) {
        if (!(typeObj instanceof TypeDeclaration typeDecl)) {
            return;
        }

        // Create ClassNode
        String className =
                moduleNode.getPackageName() != null
                        ? moduleNode.getPackageName() + typeDecl.getName().getIdentifier()
                        : typeDecl.getName().getIdentifier();

        ClassNode classNode =
                new ClassNode(className, convertJdtModifiers(typeDecl), null, null, null);

        // TODO: Convert superclass, interfaces, members, etc.

        moduleNode.addClass(classNode);
    }

    // Helper methods

    private Name createQualifiedName(AST ast, String qualifiedName) {
        List<String> parts = Arrays.asList(qualifiedName.split("\\."));
        if (parts.size() == 1) {
            return ast.newSimpleName(parts.get(0));
        }

        Name name = ast.newSimpleName(parts.get(0));
        for (int i = 1; i < parts.size(); i++) {
            SimpleName simpleName = ast.newSimpleName(parts.get(i));
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
            ParameterizedType paramType =
                    ast.newParameterizedType(
                            ast.newSimpleType(createQualifiedName(ast, classNode.getName())));
            // TODO: Add type arguments
            return paramType;
        }

        // Regular type
        return ast.newSimpleType(createQualifiedName(ast, typeName));
    }

    private PrimitiveType.Code getPrimitiveTypeCode(String typeName) {
        return switch (typeName) {
            case "boolean" -> PrimitiveType.BOOLEAN;
            case "byte" -> PrimitiveType.BYTE;
            case "char" -> PrimitiveType.CHAR;
            case "double" -> PrimitiveType.DOUBLE;
            case "float" -> PrimitiveType.FLOAT;
            case "int" -> PrimitiveType.INT;
            case "long" -> PrimitiveType.LONG;
            case "short" -> PrimitiveType.SHORT;
            case "void" -> PrimitiveType.VOID;
            default -> throw new IllegalArgumentException("Unknown primitive type: " + typeName);
        };
    }

    private int convertModifiers(int groovyModifiers) {
        int jdtModifiers = 0;

        if ((groovyModifiers & 0x0001) != 0) {
            jdtModifiers |= Modifier.PUBLIC;
        }
        if ((groovyModifiers & 0x0002) != 0) {
            jdtModifiers |= Modifier.PRIVATE;
        }
        if ((groovyModifiers & 0x0004) != 0) {
            jdtModifiers |= Modifier.PROTECTED;
        }
        if ((groovyModifiers & 0x0008) != 0) {
            jdtModifiers |= Modifier.STATIC;
        }
        if ((groovyModifiers & 0x0010) != 0) {
            jdtModifiers |= Modifier.FINAL;
        }
        if ((groovyModifiers & 0x0400) != 0) {
            jdtModifiers |= Modifier.ABSTRACT;
        }
        if ((groovyModifiers & 0x0020) != 0) {
            jdtModifiers |= Modifier.SYNCHRONIZED;
        }
        if ((groovyModifiers & 0x0100) != 0) {
            jdtModifiers |= Modifier.NATIVE;
        }

        return jdtModifiers;
    }

    private int convertJdtModifiers(TypeDeclaration typeDecl) {
        int groovyModifiers = 0;
        List modifiers = typeDecl.modifiers();

        for (Object mod : modifiers) {
            if (mod instanceof Modifier modifier) {
                if (modifier.isPublic()) groovyModifiers |= 0x0001;
                if (modifier.isPrivate()) groovyModifiers |= 0x0002;
                if (modifier.isProtected()) groovyModifiers |= 0x0004;
                if (modifier.isStatic()) groovyModifiers |= 0x0008;
                if (modifier.isFinal()) groovyModifiers |= 0x0010;
                if (modifier.isAbstract()) groovyModifiers |= 0x0400;
            }
        }

        return groovyModifiers;
    }

    private String extractSimpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }
}
