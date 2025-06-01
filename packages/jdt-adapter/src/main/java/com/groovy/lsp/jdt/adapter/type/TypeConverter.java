package com.groovy.lsp.jdt.adapter.type;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.GenericsType;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.*;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts between Groovy types and Eclipse JDT types.
 * This class handles type resolution and mapping between the two type systems.
 */
public class TypeConverter {
    private static final Logger logger = LoggerFactory.getLogger(TypeConverter.class);

    // Cache for frequently used type conversions
    private final Map<String, ITypeBinding> typeBindingCache = new HashMap<>();
    private final Map<String, ClassNode> classNodeCache = new HashMap<>();

    /**
     * Default constructor for TypeConverter.
     */
    public TypeConverter() {
        // Default constructor
    }

    // Primitive type mappings
    private static final Map<String, String> PRIMITIVE_TYPE_MAP = new HashMap<>();

    static {
        PRIMITIVE_TYPE_MAP.put("boolean", "Z");
        PRIMITIVE_TYPE_MAP.put("byte", "B");
        PRIMITIVE_TYPE_MAP.put("char", "C");
        PRIMITIVE_TYPE_MAP.put("double", "D");
        PRIMITIVE_TYPE_MAP.put("float", "F");
        PRIMITIVE_TYPE_MAP.put("int", "I");
        PRIMITIVE_TYPE_MAP.put("long", "J");
        PRIMITIVE_TYPE_MAP.put("short", "S");
        PRIMITIVE_TYPE_MAP.put("void", "V");
    }

    /**
     * Converts a Groovy ClassNode to a JDT type signature.
     *
     * @param classNode the Groovy class node
     * @return the JDT type signature
     */
    public String toJdtSignature(@Nullable ClassNode classNode) {
        if (classNode == null) {
            return Signature.SIG_VOID;
        }

        // Handle primitive types
        if (classNode.isPrimaryClassNode()) {
            String primitiveSig = PRIMITIVE_TYPE_MAP.get(classNode.getName());
            if (primitiveSig != null) {
                return primitiveSig;
            }
        }

        // Handle array types
        if (classNode.isArray()) {
            return "[" + toJdtSignature(classNode.getComponentType());
        }

        // Handle parameterized types
        if (classNode.isUsingGenerics() && classNode.getGenericsTypes() != null) {
            return createParameterizedSignature(classNode);
        }

        // Regular object type
        return "L" + classNode.getName().replace('.', '/') + ";";
    }

    /**
     * Converts a JDT type signature to a Groovy ClassNode.
     *
     * @param signature the JDT type signature
     * @return the Groovy class node
     */
    public ClassNode fromJdtSignature(@Nullable String signature) {
        if (signature == null || signature.isEmpty()) {
            return ClassHelper.OBJECT_TYPE;
        }

        // Check cache first
        ClassNode cached = classNodeCache.get(signature);
        if (cached != null) {
            return cached;
        }

        ClassNode result = parseJdtSignature(signature);
        classNodeCache.put(signature, result);
        return result;
    }

    /**
     * Converts a JDT Type to a Groovy ClassNode.
     *
     * @param type the JDT type
     * @return the Groovy class node
     */
    public ClassNode fromJdtType(@Nullable Type type) {
        if (type == null) {
            return ClassHelper.OBJECT_TYPE;
        }

        if (type.isPrimitiveType()) {
            PrimitiveType primitiveType = (PrimitiveType) type;
            return fromPrimitiveType(primitiveType.getPrimitiveTypeCode());
        }

        if (type.isArrayType()) {
            ArrayType arrayType = (ArrayType) type;
            ClassNode componentType = fromJdtType(arrayType.getElementType());
            return componentType.makeArray();
        }

        if (type.isParameterizedType()) {
            ParameterizedType paramType = (ParameterizedType) type;
            return fromParameterizedType(paramType);
        }

        if (type.isSimpleType()) {
            SimpleType simpleType = (SimpleType) type;
            String typeName = simpleType.getName().getFullyQualifiedName();
            return ClassHelper.make(typeName);
        }

        if (type.isQualifiedType()) {
            QualifiedType qualifiedType = (QualifiedType) type;
            return ClassHelper.make(qualifiedType.getName().getFullyQualifiedName());
        }

        if (type.isWildcardType()) {
            // Wildcards are represented as Object in Groovy
            return ClassHelper.OBJECT_TYPE;
        }

        logger.warn("Unknown JDT type: {}", type.getClass());
        return ClassHelper.OBJECT_TYPE;
    }

    /**
     * Converts a JDT IType to a Groovy ClassNode.
     *
     * @param iType the JDT IType
     * @return the Groovy class node
     * @throws JavaModelException if type information cannot be accessed
     */
    public ClassNode fromJdtIType(@Nullable IType iType) throws JavaModelException {
        if (iType == null) {
            return ClassHelper.OBJECT_TYPE;
        }

        String fullyQualifiedName = iType.getFullyQualifiedName();

        // Check cache
        ClassNode cached = classNodeCache.get(fullyQualifiedName);
        if (cached != null) {
            return cached;
        }

        // Create new ClassNode
        ClassNode classNode = ClassHelper.make(fullyQualifiedName);

        // Set interface flag
        if (iType.isInterface()) {
            classNode.setModifiers(classNode.getModifiers() | 0x0200); // ACC_INTERFACE
        }

        // Cache and return
        classNodeCache.put(fullyQualifiedName, classNode);
        return classNode;
    }

    /**
     * Converts a Groovy ClassNode to a JDT Type AST node.
     *
     * @param ast the AST to create nodes in
     * @param classNode the Groovy class node
     * @return the JDT type
     */
    public Type toJdtType(AST ast, @Nullable ClassNode classNode) {
        if (classNode == null) {
            return ast.newSimpleType(ast.newSimpleName("Object"));
        }

        // Handle primitive types
        if (classNode.isPrimaryClassNode()) {
            PrimitiveType.Code code = getPrimitiveCode(classNode.getName());
            if (code != null) {
                return ast.newPrimitiveType(code);
            }
        }

        // Handle array types
        if (classNode.isArray()) {
            Type componentType = toJdtType(ast, classNode.getComponentType());
            return ast.newArrayType(componentType);
        }

        // Handle parameterized types
        if (classNode.isUsingGenerics() && classNode.getGenericsTypes() != null) {
            return createParameterizedType(ast, classNode);
        }

        // Regular type
        return ast.newSimpleType(createTypeName(ast, classNode.getName()));
    }

    /**
     * Resolves type parameters for a generic type.
     *
     * @param classNode the generic class node
     * @param typeArguments the type arguments
     * @return resolved class node with type arguments applied
     */
    public ClassNode resolveGenerics(ClassNode classNode, @Nullable ClassNode[] typeArguments) {
        if (!classNode.isUsingGenerics() || typeArguments == null || typeArguments.length == 0) {
            return classNode;
        }

        ClassNode resolvedNode = classNode.getPlainNodeReference();
        GenericsType[] genericsTypes = classNode.getGenericsTypes();

        if (genericsTypes != null && typeArguments.length == genericsTypes.length) {
            GenericsType[] resolvedGenerics = new GenericsType[genericsTypes.length];
            for (int i = 0; i < genericsTypes.length; i++) {
                resolvedGenerics[i] = new GenericsType(typeArguments[i]);
            }
            resolvedNode.setGenericsTypes(resolvedGenerics);
        }

        return resolvedNode;
    }

    // Helper methods

    private String createParameterizedSignature(ClassNode classNode) {
        StringBuilder sig = new StringBuilder();
        sig.append("L").append(classNode.getName().replace('.', '/'));

        GenericsType[] genericsTypes = classNode.getGenericsTypes();
        if (genericsTypes != null && genericsTypes.length > 0) {
            sig.append("<");
            for (GenericsType gt : genericsTypes) {
                if (gt.isWildcard()) {
                    if (gt.getUpperBounds() != null && gt.getUpperBounds().length > 0) {
                        sig.append("+").append(toJdtSignature(gt.getUpperBounds()[0]));
                    } else if (gt.getLowerBound() != null) {
                        sig.append("-").append(toJdtSignature(gt.getLowerBound()));
                    } else {
                        sig.append("*");
                    }
                } else {
                    sig.append(toJdtSignature(gt.getType()));
                }
            }
            sig.append(">");
        }

        sig.append(";");
        return sig.toString();
    }

    private ClassNode parseJdtSignature(String signature) {
        if (signature.length() == 1) {
            // Primitive type
            for (Map.Entry<String, String> entry : PRIMITIVE_TYPE_MAP.entrySet()) {
                if (entry.getValue().equals(signature)) {
                    return ClassHelper.make(entry.getKey());
                }
            }
        }

        if (signature.startsWith("[")) {
            // Array type
            ClassNode componentType = parseJdtSignature(signature.substring(1));
            return componentType.makeArray();
        }

        if (signature.startsWith("L") && signature.endsWith(";")) {
            // Object type
            String className = signature.substring(1, signature.length() - 1).replace('/', '.');

            // Check for generics
            int genericStart = className.indexOf('<');
            if (genericStart > 0) {
                String baseClass = className.substring(0, genericStart);
                // TODO: Parse generic type arguments
                return ClassHelper.make(baseClass);
            }

            return ClassHelper.make(className);
        }

        logger.warn("Unable to parse JDT signature: {}", signature);
        return ClassHelper.OBJECT_TYPE;
    }

    private ClassNode fromPrimitiveType(PrimitiveType.Code code) {
        if (code == PrimitiveType.BOOLEAN) {
            return ClassHelper.boolean_TYPE;
        } else if (code == PrimitiveType.BYTE) {
            return ClassHelper.byte_TYPE;
        } else if (code == PrimitiveType.CHAR) {
            return ClassHelper.char_TYPE;
        } else if (code == PrimitiveType.DOUBLE) {
            return ClassHelper.double_TYPE;
        } else if (code == PrimitiveType.FLOAT) {
            return ClassHelper.float_TYPE;
        } else if (code == PrimitiveType.INT) {
            return ClassHelper.int_TYPE;
        } else if (code == PrimitiveType.LONG) {
            return ClassHelper.long_TYPE;
        } else if (code == PrimitiveType.SHORT) {
            return ClassHelper.short_TYPE;
        } else if (code == PrimitiveType.VOID) {
            return ClassHelper.VOID_TYPE;
        } else {
            return ClassHelper.OBJECT_TYPE;
        }
    }

    private ClassNode fromParameterizedType(ParameterizedType paramType) {
        Type rawType = paramType.getType();
        ClassNode baseType = fromJdtType(rawType);

        // TODO: Handle type arguments

        return baseType;
    }

    private PrimitiveType.@Nullable Code getPrimitiveCode(String typeName) {
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
            default -> null;
        };
    }

    private Type createParameterizedType(AST ast, ClassNode classNode) {
        SimpleType rawType = ast.newSimpleType(createTypeName(ast, classNode.getName()));
        ParameterizedType paramType = ast.newParameterizedType(rawType);

        GenericsType[] genericsTypes = classNode.getGenericsTypes();
        if (genericsTypes != null) {
            for (GenericsType gt : genericsTypes) {
                if (gt.isWildcard()) {
                    WildcardType wildcard = ast.newWildcardType();
                    // TODO: Set bounds
                    paramType.typeArguments().add(wildcard);
                } else {
                    Type typeArg = toJdtType(ast, gt.getType());
                    paramType.typeArguments().add(typeArg);
                }
            }
        }

        return paramType;
    }

    private Name createTypeName(AST ast, String typeName) {
        List<String> parts = Arrays.asList(typeName.split("\\."));
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

    /**
     * Clears the type conversion caches.
     */
    public void clearCache() {
        typeBindingCache.clear();
        classNodeCache.clear();
    }
}
