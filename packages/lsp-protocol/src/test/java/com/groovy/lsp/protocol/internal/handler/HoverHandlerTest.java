package com.groovy.lsp.protocol.internal.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.groovy.core.api.TypeInferenceService;
import com.groovy.lsp.protocol.api.IServiceRouter;
import com.groovy.lsp.protocol.internal.document.DocumentManager;
import java.lang.reflect.Modifier;
import java.util.concurrent.CompletableFuture;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class HoverHandlerTest {

    @Mock private IServiceRouter serviceRouter;

    @Mock private DocumentManager documentManager;

    @Mock private ASTService astService;

    @Mock private TypeInferenceService typeInferenceService;

    private HoverHandler hoverHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(serviceRouter.getAstService()).thenReturn(astService);
        when(serviceRouter.getTypeInferenceService()).thenReturn(typeInferenceService);
        hoverHandler = new HoverHandler(serviceRouter, documentManager);
    }

    @Test
    void testHoverOnMethod() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "def hello() { return 'Hello' }";
        HoverParams params = new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 5));

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // Create a mock AST
        ModuleNode moduleNode = mock(ModuleNode.class);
        ClassNode classNode = new ClassNode("TestClass", 0, null);
        MethodNode methodNode =
                new MethodNode(
                        "hello",
                        1, // public
                        new ClassNode(String.class),
                        Parameter.EMPTY_ARRAY,
                        ClassNode.EMPTY_ARRAY,
                        new BlockStatement());
        methodNode.setLineNumber(1);
        methodNode.setLastLineNumber(1);
        methodNode.setColumnNumber(1);
        methodNode.setLastColumnNumber(30);

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(eq(moduleNode), eq(1), eq(6))).thenReturn(methodNode);

        // When
        CompletableFuture<Hover> result = hoverHandler.handleHover(params);
        Hover hover = result.get();

        // Then
        assertNotNull(hover);
        assertNotNull(hover.getContents());
        assertTrue(hover.getContents().isRight());
        MarkupContent markupContent = hover.getContents().getRight();
        assertEquals(MarkupKind.MARKDOWN, markupContent.getKind());
        String content = markupContent.getValue();
        assertTrue(content.contains("```groovy"));
        assertTrue(content.contains("hello()"));
        assertTrue(content.contains("String"));
    }

    @Test
    void testHoverOnVariable() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "String name = 'John'";
        HoverParams params = new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 7));

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // Create a mock AST
        ModuleNode moduleNode = mock(ModuleNode.class);
        VariableExpression varExpr = new VariableExpression("name", new ClassNode(String.class));
        varExpr.setLineNumber(1);
        varExpr.setLastLineNumber(1);
        varExpr.setColumnNumber(8);
        varExpr.setLastColumnNumber(12);

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(eq(moduleNode), eq(1), eq(8))).thenReturn(varExpr);
        when(typeInferenceService.inferExpressionType(varExpr, moduleNode))
                .thenReturn(new ClassNode(String.class));

        // When
        CompletableFuture<Hover> result = hoverHandler.handleHover(params);
        Hover hover = result.get();

        // Then
        assertNotNull(hover);
        assertNotNull(hover.getContents());
        assertTrue(hover.getContents().isRight());
        MarkupContent markupContent = hover.getContents().getRight();
        String content = markupContent.getValue();
        assertTrue(content.contains("String"));
        assertTrue(content.contains("Variable:") && content.contains("name"));
    }

    @Test
    void testHoverOnField() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "class Test { private String field = 'value' }";
        HoverParams params = new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 28));

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // Create a mock AST
        ModuleNode moduleNode = mock(ModuleNode.class);
        ClassNode ownerClass = new ClassNode("Test", 0, null);
        FieldNode fieldNode =
                new FieldNode(
                        "field",
                        2, // private
                        new ClassNode(String.class),
                        ownerClass,
                        new ConstantExpression("value"));
        fieldNode.setLineNumber(1);
        fieldNode.setLastLineNumber(1);
        fieldNode.setColumnNumber(28);
        fieldNode.setLastColumnNumber(33);

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(eq(moduleNode), eq(1), eq(29))).thenReturn(fieldNode);

        // When
        CompletableFuture<Hover> result = hoverHandler.handleHover(params);
        Hover hover = result.get();

        // Then
        assertNotNull(hover);
        assertNotNull(hover.getContents());
        assertTrue(hover.getContents().isRight());
        MarkupContent markupContent = hover.getContents().getRight();
        String content = markupContent.getValue();
        assertTrue(content.contains("String"));
        assertTrue(content.contains("field"));
        assertTrue(content.contains("Field"));
    }

    @Test
    void testHoverReturnsNullWhenNoDocumentFound() throws Exception {
        // Given
        String uri = "file:///notfound.groovy";
        HoverParams params = new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 0));

        when(documentManager.getDocumentContent(uri)).thenReturn(null);

        // When
        CompletableFuture<Hover> result = hoverHandler.handleHover(params);
        Hover hover = result.get();

        // Then
        assertNull(hover);
    }

    @Test
    void testHoverReturnsNullWhenParsingFails() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "invalid groovy code {{{";
        HoverParams params = new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 0));

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);
        when(astService.parseSource(sourceCode, uri)).thenReturn(null);

        // When
        CompletableFuture<Hover> result = hoverHandler.handleHover(params);
        Hover hover = result.get();

        // Then
        assertNull(hover);
    }

    @Test
    void testHoverReturnsNullWhenNoNodeAtPosition() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "def hello() { }";
        HoverParams params = new HoverParams(new TextDocumentIdentifier(uri), new Position(10, 10));

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        ModuleNode moduleNode = mock(ModuleNode.class);
        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(eq(moduleNode), eq(11), eq(11))).thenReturn(null);

        // When
        CompletableFuture<Hover> result = hoverHandler.handleHover(params);
        Hover hover = result.get();

        // Then
        assertNull(hover);
    }

    @Test
    void testHoverOnProperty() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "class Test { String name }";
        HoverParams params = new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 20));

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // Create a mock AST
        ModuleNode moduleNode = mock(ModuleNode.class);
        ClassNode declaringClass = new ClassNode("Test", 0, null);
        PropertyNode propertyNode =
                new PropertyNode(
                        "name",
                        Modifier.PUBLIC,
                        new ClassNode(String.class),
                        declaringClass,
                        null,
                        null,
                        null);
        propertyNode.setDeclaringClass(declaringClass);

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(eq(moduleNode), eq(1), eq(21))).thenReturn(propertyNode);

        // When
        CompletableFuture<Hover> result = hoverHandler.handleHover(params);
        Hover hover = result.get();

        // Then
        assertNotNull(hover);
        assertNotNull(hover.getContents());
        assertTrue(hover.getContents().isRight());
        MarkupContent markupContent = hover.getContents().getRight();
        String content = markupContent.getValue();
        assertTrue(content.contains("String"));
        assertTrue(content.contains("name"));
        assertTrue(content.contains("Property"));
    }

    @Test
    void testHoverOnClass() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "class TestClass extends Object implements Serializable { }";
        HoverParams params = new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 8));

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // Create a mock AST
        ModuleNode moduleNode = mock(ModuleNode.class);
        ClassNode classNode = mock(ClassNode.class);
        ClassNode superClass = mock(ClassNode.class);
        ClassNode interfaceNode = mock(ClassNode.class);

        when(classNode.getName()).thenReturn("TestClass");
        when(classNode.isInterface()).thenReturn(false);
        when(classNode.isEnum()).thenReturn(false);
        when(superClass.getName()).thenReturn("BaseClass");
        when(classNode.getSuperClass()).thenReturn(superClass);
        when(interfaceNode.getName()).thenReturn("java.io.Serializable");
        when(classNode.getInterfaces()).thenReturn(new ClassNode[] {interfaceNode});
        when(classNode.getPackageName()).thenReturn("com.example");

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(eq(moduleNode), eq(1), eq(9))).thenReturn(classNode);

        // When
        CompletableFuture<Hover> result = hoverHandler.handleHover(params);
        Hover hover = result.get();

        // Then
        assertNotNull(hover);
        assertNotNull(hover.getContents());
        assertTrue(hover.getContents().isRight());
        MarkupContent markupContent = hover.getContents().getRight();
        String content = markupContent.getValue();
        assertTrue(content.contains("class"));
        assertTrue(content.contains("TestClass"));
        assertTrue(content.contains("extends"));
        assertTrue(content.contains("implements"));
        assertTrue(content.contains("Package:"));
    }

    @Test
    void testHoverOnInterface() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "interface TestInterface { }";
        HoverParams params = new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 12));

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // Create a mock AST
        ModuleNode moduleNode = mock(ModuleNode.class);
        ClassNode interfaceNode =
                new ClassNode("TestInterface", Modifier.INTERFACE, new ClassNode(Object.class));

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(eq(moduleNode), eq(1), eq(13)))
                .thenReturn(interfaceNode);

        // When
        CompletableFuture<Hover> result = hoverHandler.handleHover(params);
        Hover hover = result.get();

        // Then
        assertNotNull(hover);
        assertNotNull(hover.getContents());
        assertTrue(hover.getContents().isRight());
        MarkupContent markupContent = hover.getContents().getRight();
        String content = markupContent.getValue();
        assertTrue(content.contains("interface"));
        assertTrue(content.contains("TestInterface"));
    }

    @Test
    void testHoverOnEnum() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "enum Status { ACTIVE, INACTIVE }";
        HoverParams params = new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 7));

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // Create a mock AST
        ModuleNode moduleNode = mock(ModuleNode.class);
        ClassNode enumNode =
                new ClassNode("Status", Modifier.PUBLIC, new ClassNode(Enum.class)) {
                    @Override
                    public boolean isEnum() {
                        return true;
                    }
                };

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(eq(moduleNode), eq(1), eq(8))).thenReturn(enumNode);

        // When
        CompletableFuture<Hover> result = hoverHandler.handleHover(params);
        Hover hover = result.get();

        // Then
        assertNotNull(hover);
        assertNotNull(hover.getContents());
        assertTrue(hover.getContents().isRight());
        MarkupContent markupContent = hover.getContents().getRight();
        String content = markupContent.getValue();
        assertTrue(content.contains("enum"));
        assertTrue(content.contains("Status"));
    }

    @Test
    void testHoverOnMethodCallExpression() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "println('hello')";
        HoverParams params = new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 3));

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // Create a mock AST
        ModuleNode moduleNode = mock(ModuleNode.class);
        MethodCallExpression methodCall = mock(MethodCallExpression.class);
        when(methodCall.getMethodAsString()).thenReturn("println");

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(eq(moduleNode), eq(1), eq(4))).thenReturn(methodCall);
        when(typeInferenceService.inferExpressionType(methodCall, moduleNode))
                .thenReturn(new ClassNode("void", 0, null));

        // When
        CompletableFuture<Hover> result = hoverHandler.handleHover(params);
        Hover hover = result.get();

        // Then
        assertNotNull(hover);
        assertNotNull(hover.getContents());
        assertTrue(hover.getContents().isRight());
        MarkupContent markupContent = hover.getContents().getRight();
        String content = markupContent.getValue();
        assertTrue(content.contains("void"));
        assertTrue(content.contains("Method call:"));
        assertTrue(content.contains("println"));
    }

    @Test
    void testHoverOnPropertyExpression() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "obj.property";
        HoverParams params = new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 6));

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // Create a mock AST
        ModuleNode moduleNode = mock(ModuleNode.class);
        PropertyExpression propExpr = mock(PropertyExpression.class);
        when(propExpr.getPropertyAsString()).thenReturn("property");

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(eq(moduleNode), eq(1), eq(7))).thenReturn(propExpr);
        when(typeInferenceService.inferExpressionType(propExpr, moduleNode))
                .thenReturn(new ClassNode(String.class));

        // When
        CompletableFuture<Hover> result = hoverHandler.handleHover(params);
        Hover hover = result.get();

        // Then
        assertNotNull(hover);
        assertNotNull(hover.getContents());
        assertTrue(hover.getContents().isRight());
        MarkupContent markupContent = hover.getContents().getRight();
        String content = markupContent.getValue();
        assertTrue(content.contains("String"));
        assertTrue(content.contains("Property:"));
        assertTrue(content.contains("property"));
    }

    @Test
    void testHoverOnParameter() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "def method(String param) { }";
        HoverParams params = new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 18));

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // Create a mock AST
        ModuleNode moduleNode = mock(ModuleNode.class);
        Parameter parameter = new Parameter(new ClassNode(String.class), "param");

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(eq(moduleNode), eq(1), eq(19))).thenReturn(parameter);

        // When
        CompletableFuture<Hover> result = hoverHandler.handleHover(params);
        Hover hover = result.get();

        // Then
        assertNotNull(hover);
        assertNotNull(hover.getContents());
        assertTrue(hover.getContents().isRight());
        MarkupContent markupContent = hover.getContents().getRight();
        String content = markupContent.getValue();
        assertTrue(content.contains("String"));
        assertTrue(content.contains("param"));
        assertTrue(content.contains("Parameter"));
    }

    @Test
    void testHoverOnVariableWithObjectType() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "def variable = 'value'";
        HoverParams params = new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 6));

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // Create a mock AST
        ModuleNode moduleNode = mock(ModuleNode.class);
        VariableExpression varExpr =
                new VariableExpression("variable", new ClassNode(Object.class));

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(eq(moduleNode), eq(1), eq(7))).thenReturn(varExpr);
        when(typeInferenceService.inferExpressionType(varExpr, moduleNode))
                .thenReturn(new ClassNode(Object.class));

        // When
        CompletableFuture<Hover> result = hoverHandler.handleHover(params);
        Hover hover = result.get();

        // Then
        assertNotNull(hover);
        assertNotNull(hover.getContents());
        assertTrue(hover.getContents().isRight());
        MarkupContent markupContent = hover.getContents().getRight();
        String content = markupContent.getValue();
        assertTrue(content.contains("Object"));
        assertTrue(content.contains("Variable:"));
        assertTrue(content.contains("variable"));
    }

    @Test
    void testHoverOnStaticFinalField() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "class Test { static final String CONSTANT = 'value' }";
        HoverParams params = new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 33));

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // Create a mock AST
        ModuleNode moduleNode = mock(ModuleNode.class);
        ClassNode ownerClass = new ClassNode("Test", 0, null);
        FieldNode fieldNode =
                new FieldNode(
                        "CONSTANT",
                        Modifier.STATIC | Modifier.FINAL | Modifier.PUBLIC,
                        new ClassNode(String.class),
                        ownerClass,
                        new ConstantExpression("value"));

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(eq(moduleNode), eq(1), eq(34))).thenReturn(fieldNode);

        // When
        CompletableFuture<Hover> result = hoverHandler.handleHover(params);
        Hover hover = result.get();

        // Then
        assertNotNull(hover);
        assertNotNull(hover.getContents());
        assertTrue(hover.getContents().isRight());
        MarkupContent markupContent = hover.getContents().getRight();
        String content = markupContent.getValue();
        assertTrue(content.contains("static"));
        assertTrue(content.contains("final"));
        assertTrue(content.contains("String"));
        assertTrue(content.contains("CONSTANT"));
        assertTrue(content.contains("Field"));
    }

    @Test
    void testHoverOnPrivateStaticProperty() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "class Test { private static String prop }";
        HoverParams params = new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 35));

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // Create a mock AST
        ModuleNode moduleNode = mock(ModuleNode.class);
        ClassNode declaringClass = new ClassNode("Test", 0, null);
        PropertyNode propertyNode =
                new PropertyNode(
                        "prop",
                        Modifier.PRIVATE | Modifier.STATIC,
                        new ClassNode(String.class),
                        declaringClass,
                        null,
                        null,
                        null);
        propertyNode.setDeclaringClass(declaringClass);

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(eq(moduleNode), eq(1), eq(36))).thenReturn(propertyNode);

        // When
        CompletableFuture<Hover> result = hoverHandler.handleHover(params);
        Hover hover = result.get();

        // Then
        assertNotNull(hover);
        assertNotNull(hover.getContents());
        assertTrue(hover.getContents().isRight());
        MarkupContent markupContent = hover.getContents().getRight();
        String content = markupContent.getValue();
        assertTrue(content.contains("private"));
        assertTrue(content.contains("static"));
        assertTrue(content.contains("String"));
        assertTrue(content.contains("prop"));
        assertTrue(content.contains("Property"));
    }

    @Test
    void testHoverHandlesException() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "def test() { }";
        HoverParams params = new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 5));

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);
        when(astService.parseSource(sourceCode, uri))
                .thenThrow(new RuntimeException("Parse error"));

        // When
        CompletableFuture<Hover> result = hoverHandler.handleHover(params);
        Hover hover = result.get();

        // Then
        assertNull(hover);
    }

    @Test
    void testHoverOnExpressionWithNoType() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "someExpression";
        HoverParams params = new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 5));

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // Create a mock AST
        ModuleNode moduleNode = mock(ModuleNode.class);
        VariableExpression varExpr = new VariableExpression("someExpression");

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(eq(moduleNode), eq(1), eq(6))).thenReturn(varExpr);
        when(typeInferenceService.inferExpressionType(varExpr, moduleNode)).thenReturn(null);

        // When
        CompletableFuture<Hover> result = hoverHandler.handleHover(params);
        Hover hover = result.get();

        // Then
        assertNull(hover);
    }

    @Test
    void testHoverOnProtectedMethod() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "protected String getValue(int arg1, String arg2) { }";
        HoverParams params = new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 18));

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // Create a mock AST
        ModuleNode moduleNode = mock(ModuleNode.class);
        Parameter param1 = new Parameter(new ClassNode(int.class), "arg1");
        Parameter param2 = new Parameter(new ClassNode(String.class), "arg2");
        MethodNode methodNode =
                new MethodNode(
                        "getValue",
                        Modifier.PROTECTED,
                        new ClassNode(String.class),
                        new Parameter[] {param1, param2},
                        ClassNode.EMPTY_ARRAY,
                        new BlockStatement());

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(eq(moduleNode), eq(1), eq(19))).thenReturn(methodNode);

        // When
        CompletableFuture<Hover> result = hoverHandler.handleHover(params);
        Hover hover = result.get();

        // Then
        assertNotNull(hover);
        assertNotNull(hover.getContents());
        assertTrue(hover.getContents().isRight());
        MarkupContent markupContent = hover.getContents().getRight();
        String content = markupContent.getValue();
        assertTrue(content.contains("protected"));
        assertTrue(content.contains("String"));
        assertTrue(content.contains("getValue"));
        assertTrue(content.contains("int arg1"));
        assertTrue(content.contains("String arg2"));
    }

    @Test
    void testHoverOnPrivateMethod() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "private void method() { }";
        HoverParams params = new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 13));

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // Create a mock AST
        ModuleNode moduleNode = mock(ModuleNode.class);
        MethodNode methodNode =
                new MethodNode(
                        "method",
                        Modifier.PRIVATE,
                        new ClassNode(void.class),
                        Parameter.EMPTY_ARRAY,
                        ClassNode.EMPTY_ARRAY,
                        new BlockStatement());

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(eq(moduleNode), eq(1), eq(14))).thenReturn(methodNode);

        // When
        CompletableFuture<Hover> result = hoverHandler.handleHover(params);
        Hover hover = result.get();

        // Then
        assertNotNull(hover);
        assertNotNull(hover.getContents());
        assertTrue(hover.getContents().isRight());
        MarkupContent markupContent = hover.getContents().getRight();
        String content = markupContent.getValue();
        assertTrue(content.contains("private"));
        assertTrue(content.contains("void"));
        assertTrue(content.contains("method"));
    }

    @Test
    void testHoverOnStaticFinalMethod() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "public static final String utility() { }";
        HoverParams params = new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 27));

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // Create a mock AST
        ModuleNode moduleNode = mock(ModuleNode.class);
        MethodNode methodNode =
                new MethodNode(
                        "utility",
                        Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL,
                        new ClassNode(String.class),
                        Parameter.EMPTY_ARRAY,
                        ClassNode.EMPTY_ARRAY,
                        new BlockStatement());

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(eq(moduleNode), eq(1), eq(28))).thenReturn(methodNode);

        // When
        CompletableFuture<Hover> result = hoverHandler.handleHover(params);
        Hover hover = result.get();

        // Then
        assertNotNull(hover);
        assertNotNull(hover.getContents());
        assertTrue(hover.getContents().isRight());
        MarkupContent markupContent = hover.getContents().getRight();
        String content = markupContent.getValue();
        assertTrue(content.contains("public"));
        assertTrue(content.contains("static"));
        assertTrue(content.contains("final"));
        assertTrue(content.contains("String"));
        assertTrue(content.contains("utility"));
    }

    @Test
    void testHoverOnProtectedField() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "class Test { protected int value }";
        HoverParams params = new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 26));

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // Create a mock AST
        ModuleNode moduleNode = mock(ModuleNode.class);
        ClassNode ownerClass = new ClassNode("Test", 0, null);
        FieldNode fieldNode =
                new FieldNode(
                        "value", Modifier.PROTECTED, new ClassNode(int.class), ownerClass, null);

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(eq(moduleNode), eq(1), eq(27))).thenReturn(fieldNode);

        // When
        CompletableFuture<Hover> result = hoverHandler.handleHover(params);
        Hover hover = result.get();

        // Then
        assertNotNull(hover);
        assertNotNull(hover.getContents());
        assertTrue(hover.getContents().isRight());
        MarkupContent markupContent = hover.getContents().getRight();
        String content = markupContent.getValue();
        assertTrue(content.contains("protected"));
        assertTrue(content.contains("int"));
        assertTrue(content.contains("value"));
        assertTrue(content.contains("Field"));
    }

    @Test
    void testHoverOnClassWithNoSuperClassAndNoInterfaces() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "class SimpleClass { }";
        HoverParams params = new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 8));

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // Create a mock AST
        ModuleNode moduleNode = mock(ModuleNode.class);
        ClassNode classNode = mock(ClassNode.class);
        ClassNode objectClass = mock(ClassNode.class);

        when(classNode.getName()).thenReturn("SimpleClass");
        when(classNode.isInterface()).thenReturn(false);
        when(classNode.isEnum()).thenReturn(false);
        when(objectClass.getName()).thenReturn("java.lang.Object");
        when(classNode.getSuperClass()).thenReturn(objectClass);
        when(classNode.getInterfaces()).thenReturn(new ClassNode[0]);
        when(classNode.getPackageName()).thenReturn(null);

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(eq(moduleNode), eq(1), eq(9))).thenReturn(classNode);

        // When
        CompletableFuture<Hover> result = hoverHandler.handleHover(params);
        Hover hover = result.get();

        // Then
        assertNotNull(hover);
        assertNotNull(hover.getContents());
        assertTrue(hover.getContents().isRight());
        MarkupContent markupContent = hover.getContents().getRight();
        String content = markupContent.getValue();
        assertTrue(content.contains("class"));
        assertTrue(content.contains("SimpleClass"));
        assertFalse(content.contains("extends"));
        assertFalse(content.contains("implements"));
        assertFalse(content.contains("Package:"));
    }

    @Test
    void testHoverOnVariableWithNullType() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "def nullVar = null";
        HoverParams params = new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 6));

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // Create a mock AST
        ModuleNode moduleNode = mock(ModuleNode.class);
        VariableExpression varExpr = new VariableExpression("nullVar", null);

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(eq(moduleNode), eq(1), eq(7))).thenReturn(varExpr);
        when(typeInferenceService.inferExpressionType(varExpr, moduleNode)).thenReturn(null);

        // When
        CompletableFuture<Hover> result = hoverHandler.handleHover(params);
        Hover hover = result.get();

        // Then
        assertNull(hover);
    }

    @Test
    void testHoverOnUnrecognizedNode() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "some code";
        HoverParams params = new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 3));

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // Create a mock AST with unrecognized node type
        ModuleNode moduleNode = mock(ModuleNode.class);
        ASTNode unknownNode = mock(ASTNode.class); // Not one of the recognized types

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(eq(moduleNode), eq(1), eq(4))).thenReturn(unknownNode);

        // When
        CompletableFuture<Hover> result = hoverHandler.handleHover(params);
        Hover hover = result.get();

        // Then
        assertNull(hover);
    }

    @Test
    void testHoverOnClassWithMultipleInterfaces() throws Exception {
        // Given
        String uri = "file:///test.groovy";
        String sourceCode = "class MultiInterface implements Comparable, Serializable { }";
        HoverParams params = new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 8));

        when(documentManager.getDocumentContent(uri)).thenReturn(sourceCode);

        // Create a mock AST
        ModuleNode moduleNode = mock(ModuleNode.class);
        ClassNode classNode = mock(ClassNode.class);
        ClassNode superClass = mock(ClassNode.class);
        ClassNode interface1 = mock(ClassNode.class);
        ClassNode interface2 = mock(ClassNode.class);

        when(classNode.getName()).thenReturn("MultiInterface");
        when(classNode.isInterface()).thenReturn(false);
        when(classNode.isEnum()).thenReturn(false);
        when(superClass.getName()).thenReturn("java.lang.Object");
        when(classNode.getSuperClass()).thenReturn(superClass);
        when(interface1.getName()).thenReturn("Comparable");
        when(interface2.getName()).thenReturn("Serializable");
        when(classNode.getInterfaces()).thenReturn(new ClassNode[] {interface1, interface2});
        when(classNode.getPackageName()).thenReturn("com.example");

        when(astService.parseSource(sourceCode, uri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(eq(moduleNode), eq(1), eq(9))).thenReturn(classNode);

        // When
        CompletableFuture<Hover> result = hoverHandler.handleHover(params);
        Hover hover = result.get();

        // Then
        assertNotNull(hover);
        assertNotNull(hover.getContents());
        assertTrue(hover.getContents().isRight());
        MarkupContent markupContent = hover.getContents().getRight();
        String content = markupContent.getValue();
        assertTrue(content.contains("class"));
        assertTrue(content.contains("MultiInterface"));
        assertTrue(content.contains("implements"));
        assertTrue(content.contains("Comparable"));
        assertTrue(content.contains("Serializable"));
        assertTrue(content.contains("Package:"));
    }
}
