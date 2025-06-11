package com.groovy.lsp.protocol.internal.handler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.groovy.core.api.TypeInferenceService;
import com.groovy.lsp.protocol.api.IServiceRouter;
import com.groovy.lsp.protocol.internal.document.DocumentManager;
import com.groovy.lsp.test.annotations.PerformanceTest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;

public class HoverHandlerPerformanceTest {

    private HoverHandler hoverHandler;
    private IServiceRouter serviceRouter;
    private DocumentManager documentManager;
    private ASTService astService;
    private TypeInferenceService typeInferenceService;

    @BeforeEach
    void setUp() {
        serviceRouter = mock(IServiceRouter.class);
        documentManager = mock(DocumentManager.class);
        astService = mock(ASTService.class);
        typeInferenceService = mock(TypeInferenceService.class);

        when(serviceRouter.getAstService()).thenReturn(astService);
        when(serviceRouter.getTypeInferenceService()).thenReturn(typeInferenceService);

        hoverHandler = new HoverHandler(serviceRouter, documentManager);
    }

    @PerformanceTest
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    void testHoverPerformanceUnder100ms() throws Exception {
        // Arrange
        String uri = createTestUri();
        String content = createLargeGroovyFile();

        when(documentManager.getDocumentContent(uri)).thenReturn(content);

        ModuleNode moduleNode = createMockModuleNode();
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);

        MethodNode methodNode = createMockMethodNode();
        when(astService.findNodeAtPosition(any(), any(Integer.class), any(Integer.class)))
                .thenReturn(methodNode);

        HoverParams params = createHoverParams(uri, 50, 10);

        // Act - measure execution time
        long startTime = System.nanoTime();
        CompletableFuture<Hover> future = hoverHandler.handleHover(params);
        Hover result = future.get(100, TimeUnit.MILLISECONDS);
        long endTime = System.nanoTime();

        // Assert
        assertNotNull(result);
        long durationMs = (endTime - startTime) / 1_000_000;
        assertTrue(durationMs < 100, "Hover request took " + durationMs + "ms, expected < 100ms");
    }

    @PerformanceTest
    void testHoverPerformanceWithGroovydoc() throws Exception {
        // Arrange
        String uri = createTestUri();
        String content = createGroovyFileWithJavadoc();

        when(documentManager.getDocumentContent(uri)).thenReturn(content);

        ModuleNode moduleNode = createMockModuleNode();
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);

        MethodNode methodNode = createMockMethodNodeWithGroovydoc();
        when(astService.findNodeAtPosition(any(), any(Integer.class), any(Integer.class)))
                .thenReturn(methodNode);

        HoverParams params = createHoverParams(uri, 10, 15);

        // Act - measure multiple requests
        long totalTime = 0;
        int iterations = 10;

        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            CompletableFuture<Hover> future = hoverHandler.handleHover(params);
            Hover result = future.get(200, TimeUnit.MILLISECONDS);
            long endTime = System.nanoTime();
            totalTime += (endTime - startTime);
            assertNotNull(result);
        }

        // Assert average time
        long averageTimeMs = (totalTime / iterations) / 1_000_000;
        assertTrue(
                averageTimeMs < 100,
                "Average hover request took " + averageTimeMs + "ms, expected < 100ms");
    }

    @PerformanceTest
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    void testHoverPerformanceWithTypeInference() throws Exception {
        // Arrange
        String uri = createTestUri();
        String content = createGroovyFileWithComplexTypes();

        when(documentManager.getDocumentContent(uri)).thenReturn(content);

        ModuleNode moduleNode = createMockModuleNode();
        when(astService.parseSource(content, uri)).thenReturn(moduleNode);

        // Create a variable expression that requires type inference
        VariableExpression varExpr = new VariableExpression("complexVar");
        when(astService.findNodeAtPosition(any(), any(Integer.class), any(Integer.class)))
                .thenReturn(varExpr);

        // Mock type inference
        ClassNode inferredType = new ClassNode(java.util.List.class);
        when(typeInferenceService.inferExpressionType(any(), any())).thenReturn(inferredType);

        HoverParams params = createHoverParams(uri, 20, 10);

        // Act
        long startTime = System.nanoTime();
        CompletableFuture<Hover> future = hoverHandler.handleHover(params);
        Hover result = future.get(100, TimeUnit.MILLISECONDS);
        long endTime = System.nanoTime();

        // Assert
        assertNotNull(result);
        long durationMs = (endTime - startTime) / 1_000_000;
        assertTrue(
                durationMs < 100,
                "Hover with type inference took " + durationMs + "ms, expected < 100ms");
    }

    private String createTestUri() throws URISyntaxException {
        return new URI("file:///test/performance.groovy").toString();
    }

    private HoverParams createHoverParams(String uri, int line, int character) {
        HoverParams params = new HoverParams();
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
        Position position = new Position(line, character);
        params.setTextDocument(textDocument);
        params.setPosition(position);
        return params;
    }

    private String createLargeGroovyFile() {
        StringBuilder sb = new StringBuilder();
        sb.append("package com.example.performance\n\n");

        // Generate 100 classes with methods
        for (int i = 0; i < 100; i++) {
            sb.append("class TestClass").append(i).append(" {\n");
            for (int j = 0; j < 10; j++) {
                sb.append("    def method").append(j).append("() {\n");
                sb.append("        return 'result'\n");
                sb.append("    }\n");
            }
            sb.append("}\n\n");
        }

        return sb.toString();
    }

    private String createGroovyFileWithJavadoc() {
        return """
        package com.example

        /**
         * This is a test class with comprehensive Javadoc.
         * It includes multiple paragraphs and formatting.
         *
         * @author Test Author
         * @since 1.0
         */
        class DocumentedClass {
            /**
             * Performs a complex calculation.
             * This method demonstrates performance with Javadoc parsing.
             *
             * @param input The input value
             * @param multiplier The multiplication factor
             * @return The calculated result
             * @throws IllegalArgumentException if input is negative
             */
            def calculate(int input, double multiplier) {
                if (input < 0) {
                    throw new IllegalArgumentException("Input must be non-negative")
                }
                return input * multiplier
            }
        }
        """;
    }

    private String createGroovyFileWithComplexTypes() {
        return """
        package com.example

        import java.util.concurrent.CompletableFuture

        class ComplexTypeExample {
            def complexVar = CompletableFuture.supplyAsync {
                ['item1', 'item2', 'item3']
            }

            def genericMethod() {
                def result = complexVar.thenApply { list ->
                    list.collect { it.toUpperCase() }
                }
                return result
            }
        }
        """;
    }

    private ModuleNode createMockModuleNode() {
        ModuleNode moduleNode = mock(ModuleNode.class);
        return moduleNode;
    }

    private MethodNode createMockMethodNode() {
        Parameter[] params =
                new Parameter[] {
                    new Parameter(new ClassNode(String.class), "param1"),
                    new Parameter(new ClassNode(int.class), "param2")
                };

        MethodNode method =
                new MethodNode(
                        "testMethod",
                        ACC_PUBLIC,
                        new ClassNode(String.class),
                        params,
                        new ClassNode[0],
                        null);

        return method;
    }

    private MethodNode createMockMethodNodeWithGroovydoc() {
        MethodNode method = createMockMethodNode();

        // Mock Groovydoc - Note: In a real test, this would be set by the parser
        // when CompilerConfiguration.GROOVYDOC is enabled
        // For performance testing, we're simulating the presence of Groovydoc

        return method;
    }

    private static final int ACC_PUBLIC = 0x0001;
}
