package com.groovy.lsp.protocol.internal.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.protocol.api.IServiceRouter;
import com.groovy.lsp.protocol.internal.document.DocumentManager;
import com.groovy.lsp.shared.workspace.api.WorkspaceIndexService;
import com.groovy.lsp.shared.workspace.api.dto.SymbolInfo;
import com.groovy.lsp.shared.workspace.api.dto.SymbolKind;
import com.groovy.lsp.test.annotations.UnitTest;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

/**
 * Tests for cross-file definition functionality in DefinitionHandler
 */
public class CrossFileDefinitionHandlerTest {

    private DefinitionHandler definitionHandler;
    private IServiceRouter serviceRouter;
    private DocumentManager documentManager;
    private ASTService astService;
    private WorkspaceIndexService workspaceIndexService;

    @BeforeEach
    void setUp() {
        serviceRouter = mock(IServiceRouter.class);
        documentManager = mock(DocumentManager.class);
        astService = mock(ASTService.class);
        workspaceIndexService = mock(WorkspaceIndexService.class);

        when(serviceRouter.getAstService()).thenReturn(astService);
        when(serviceRouter.getWorkspaceIndexService()).thenReturn(workspaceIndexService);

        definitionHandler = new DefinitionHandler(serviceRouter, documentManager);
    }

    @UnitTest
    @DisplayName("Should find method definition in external file via WorkspaceIndexService")
    void testCrossFileMethodDefinition() throws Exception {
        // Arrange
        String currentUri = "file:///project/src/main.groovy";
        String sourceCode = "class Main { void test() { Utils.doSomething() } }";

        ModuleNode moduleNode = new ModuleNode((org.codehaus.groovy.control.SourceUnit) null);
        ClassNode mainClass = new ClassNode("Main", 0, ClassNode.SUPER);
        moduleNode.addClass(mainClass);

        MethodCallExpression methodCall =
                new MethodCallExpression(
                        new VariableExpression("Utils"),
                        "doSomething",
                        new org.codehaus.groovy.ast.expr.ArgumentListExpression());

        // Mock AST service
        when(documentManager.getDocumentContent(currentUri)).thenReturn(sourceCode);
        when(astService.parseSource(sourceCode, currentUri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 41)).thenReturn(methodCall);

        // Mock workspace index service
        Path targetPath = Paths.get("project/src/utils.groovy");
        SymbolInfo methodSymbol =
                new SymbolInfo("doSomething", SymbolKind.METHOD, targetPath, 10, 5);

        when(workspaceIndexService.searchSymbols("doSomething"))
                .thenReturn(CompletableFuture.completedFuture(Stream.of(methodSymbol)));

        // Act
        DefinitionParams params =
                new DefinitionParams(new TextDocumentIdentifier(currentUri), new Position(0, 40));

        Either<List<? extends Location>, List<? extends org.eclipse.lsp4j.LocationLink>> result =
                definitionHandler.handleDefinition(params).get();

        // Assert
        assertThat(result.isLeft()).isTrue();
        List<? extends Location> locations = result.getLeft();
        assertThat(locations).hasSize(1);

        Location location = locations.get(0);
        assertThat(location.getUri()).endsWith("utils.groovy");
        assertThat(location.getRange().getStart().getLine()).isEqualTo(9); // 0-based
        assertThat(location.getRange().getStart().getCharacter()).isEqualTo(4); // 0-based
    }

    @UnitTest
    @DisplayName("Should find class definition in external file via WorkspaceIndexService")
    void testCrossFileClassDefinition() throws Exception {
        // Arrange
        String currentUri = "file:///project/src/main.groovy";
        String sourceCode = "class Main { Utils utils = new Utils() }";

        ModuleNode moduleNode = new ModuleNode((org.codehaus.groovy.control.SourceUnit) null);
        ClassNode mainClass = new ClassNode("Main", 0, ClassNode.SUPER);
        ClassNode utilsType = new ClassNode("Utils", 0, ClassNode.SUPER);
        moduleNode.addClass(mainClass);

        // Create ClassExpression instead of ClassNode for the test
        ClassExpression classExpr = new ClassExpression(utilsType);

        // Mock AST service
        when(documentManager.getDocumentContent(currentUri)).thenReturn(sourceCode);
        when(astService.parseSource(sourceCode, currentUri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 14)).thenReturn(classExpr);

        // Mock workspace index service
        Path targetPath = Paths.get("project/src/Utils.groovy");
        SymbolInfo classSymbol = new SymbolInfo("Utils", SymbolKind.CLASS, targetPath, 1, 1);

        when(workspaceIndexService.searchSymbols("Utils"))
                .thenReturn(CompletableFuture.completedFuture(Stream.of(classSymbol)));

        // Act
        DefinitionParams params =
                new DefinitionParams(new TextDocumentIdentifier(currentUri), new Position(0, 13));

        Either<List<? extends Location>, List<? extends org.eclipse.lsp4j.LocationLink>> result =
                definitionHandler.handleDefinition(params).get();

        // Assert
        assertThat(result.isLeft()).isTrue();
        List<? extends Location> locations = result.getLeft();
        assertThat(locations).hasSize(1);

        Location location = locations.get(0);
        assertThat(location.getUri()).endsWith("Utils.groovy");
        assertThat(location.getRange().getStart().getLine()).isEqualTo(0);
        assertThat(location.getRange().getStart().getCharacter()).isEqualTo(0);
    }

    @UnitTest
    @DisplayName("Should find property definition in external file via WorkspaceIndexService")
    void testCrossFilePropertyDefinition() throws Exception {
        // Arrange
        String currentUri = "file:///project/src/main.groovy";
        String sourceCode = "class Main { void test() { def config = Utils.config } }";

        ModuleNode moduleNode = new ModuleNode((org.codehaus.groovy.control.SourceUnit) null);
        ClassNode mainClass = new ClassNode("Main", 0, ClassNode.SUPER);
        moduleNode.addClass(mainClass);

        PropertyExpression propExpr =
                new PropertyExpression(
                        new VariableExpression("Utils"), new ConstantExpression("config"));

        // Mock AST service
        when(documentManager.getDocumentContent(currentUri)).thenReturn(sourceCode);
        when(astService.parseSource(sourceCode, currentUri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 48)).thenReturn(propExpr);

        // Mock workspace index service
        Path targetPath = Paths.get("project/src/Utils.groovy");
        SymbolInfo propertySymbol =
                new SymbolInfo("config", SymbolKind.PROPERTY, targetPath, 5, 10);

        when(workspaceIndexService.searchSymbols("config"))
                .thenReturn(CompletableFuture.completedFuture(Stream.of(propertySymbol)));

        // Act
        DefinitionParams params =
                new DefinitionParams(new TextDocumentIdentifier(currentUri), new Position(0, 47));

        Either<List<? extends Location>, List<? extends org.eclipse.lsp4j.LocationLink>> result =
                definitionHandler.handleDefinition(params).get();

        // Assert
        assertThat(result.isLeft()).isTrue();
        List<? extends Location> locations = result.getLeft();
        assertThat(locations).hasSize(1);

        Location location = locations.get(0);
        assertThat(location.getUri()).endsWith("Utils.groovy");
        assertThat(location.getRange().getStart().getLine()).isEqualTo(4);
        assertThat(location.getRange().getStart().getCharacter()).isEqualTo(9);
    }

    @UnitTest
    @DisplayName("Should return empty list when workspace index returns no results")
    void testCrossFileNoResults() throws Exception {
        // Arrange
        String currentUri = "file:///project/src/main.groovy";
        String sourceCode = "class Main { void test() { Unknown.method() } }";

        ModuleNode moduleNode = new ModuleNode((org.codehaus.groovy.control.SourceUnit) null);
        ClassNode mainClass = new ClassNode("Main", 0, ClassNode.SUPER);
        moduleNode.addClass(mainClass);

        MethodCallExpression methodCall =
                new MethodCallExpression(
                        new VariableExpression("Unknown"),
                        "method",
                        new org.codehaus.groovy.ast.expr.ArgumentListExpression());

        // Mock AST service
        when(documentManager.getDocumentContent(currentUri)).thenReturn(sourceCode);
        when(astService.parseSource(sourceCode, currentUri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 36)).thenReturn(methodCall);

        // Mock workspace index service - returns empty stream
        when(workspaceIndexService.searchSymbols("method"))
                .thenReturn(CompletableFuture.completedFuture(Stream.empty()));

        // Act
        DefinitionParams params =
                new DefinitionParams(new TextDocumentIdentifier(currentUri), new Position(0, 35));

        Either<List<? extends Location>, List<? extends org.eclipse.lsp4j.LocationLink>> result =
                definitionHandler.handleDefinition(params).get();

        // Assert
        assertThat(result.isLeft()).isTrue();
        List<? extends Location> locations = result.getLeft();
        assertThat(locations).isEmpty();
    }

    @UnitTest
    @DisplayName("Should handle workspace index service errors gracefully")
    void testCrossFileIndexServiceError() throws Exception {
        // Arrange
        String currentUri = "file:///project/src/main.groovy";
        String sourceCode = "class Main { void test() { Utils.doSomething() } }";

        ModuleNode moduleNode = new ModuleNode((org.codehaus.groovy.control.SourceUnit) null);
        ClassNode mainClass = new ClassNode("Main", 0, ClassNode.SUPER);
        moduleNode.addClass(mainClass);

        MethodCallExpression methodCall =
                new MethodCallExpression(
                        new VariableExpression("Utils"),
                        "doSomething",
                        new org.codehaus.groovy.ast.expr.ArgumentListExpression());

        // Mock AST service
        when(documentManager.getDocumentContent(currentUri)).thenReturn(sourceCode);
        when(astService.parseSource(sourceCode, currentUri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 41)).thenReturn(methodCall);

        // Mock workspace index service - throws exception
        when(workspaceIndexService.searchSymbols("doSomething"))
                .thenReturn(
                        CompletableFuture.failedFuture(
                                new RuntimeException("Index service error")));

        // Act
        DefinitionParams params =
                new DefinitionParams(new TextDocumentIdentifier(currentUri), new Position(0, 40));

        Either<List<? extends Location>, List<? extends org.eclipse.lsp4j.LocationLink>> result =
                definitionHandler.handleDefinition(params).get();

        // Assert
        assertThat(result.isLeft()).isTrue();
        List<? extends Location> locations = result.getLeft();
        assertThat(locations).isEmpty(); // Should return empty list on error
    }
}
