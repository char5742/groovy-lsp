package com.groovy.lsp.protocol.internal.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.groovy.core.api.TypeInferenceService;
import com.groovy.lsp.protocol.api.IServiceRouter;
import com.groovy.lsp.protocol.internal.document.DocumentManager;
import java.util.concurrent.CompletableFuture;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ConstantExpression;
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
}
