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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for cross-file references functionality in ReferencesHandler
 */
public class CrossFileReferencesHandlerTest {

    private ReferencesHandler referencesHandler;
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

        referencesHandler = new ReferencesHandler(serviceRouter, documentManager);
    }

    @Test
    @DisplayName("Should find method references across multiple files via WorkspaceIndexService")
    void testCrossFileMethodReferences() throws Exception {
        // Arrange
        String currentUri = "file:///project/src/Utils.groovy";
        String sourceCode = "class Utils { static void doSomething() {} }";

        ModuleNode moduleNode = new ModuleNode((org.codehaus.groovy.control.SourceUnit) null);
        ClassNode utilsClass = new ClassNode("Utils", 0, ClassNode.SUPER);
        MethodNode methodNode =
                new MethodNode(
                        "doSomething",
                        1,
                        ClassHelper.VOID_TYPE,
                        Parameter.EMPTY_ARRAY,
                        ClassNode.EMPTY_ARRAY,
                        null);
        utilsClass.addMethod(methodNode);
        moduleNode.addClass(utilsClass);

        // Mock AST service
        when(documentManager.getDocumentContent(currentUri)).thenReturn(sourceCode);
        when(astService.parseSource(sourceCode, currentUri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 27)).thenReturn(methodNode);

        // Mock workspace index service - return multiple references
        Path mainPath = Paths.get("project/src/Main.groovy");
        Path testPath = Paths.get("project/src/Test.groovy");

        SymbolInfo reference1 = new SymbolInfo("doSomething", SymbolKind.METHOD, mainPath, 15, 20);

        SymbolInfo reference2 = new SymbolInfo("doSomething", SymbolKind.METHOD, testPath, 25, 30);

        when(workspaceIndexService.searchSymbols("doSomething"))
                .thenReturn(CompletableFuture.completedFuture(Stream.of(reference1, reference2)));

        // Act
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(currentUri),
                        new Position(0, 26),
                        new ReferenceContext(false) // Don't include declaration
                        );

        List<? extends Location> locations = referencesHandler.handleReferences(params).get();

        // Assert
        assertThat(locations).hasSize(2);

        Location location1 = locations.get(0);
        assertThat(location1.getUri()).endsWith("Main.groovy");
        assertThat(location1.getRange().getStart().getLine()).isEqualTo(14);
        assertThat(location1.getRange().getStart().getCharacter()).isEqualTo(19);

        Location location2 = locations.get(1);
        assertThat(location2.getUri()).endsWith("Test.groovy");
        assertThat(location2.getRange().getStart().getLine()).isEqualTo(24);
        assertThat(location2.getRange().getStart().getCharacter()).isEqualTo(29);
    }

    @Test
    @DisplayName("Should find class references across multiple files via WorkspaceIndexService")
    void testCrossFileClassReferences() throws Exception {
        // Arrange
        String currentUri = "file:///project/src/Model.groovy";
        String sourceCode = "class Model { String name }";

        ModuleNode moduleNode = new ModuleNode((org.codehaus.groovy.control.SourceUnit) null);
        ClassNode modelClass = new ClassNode("Model", 0, ClassNode.SUPER);
        moduleNode.addClass(modelClass);

        // Mock AST service
        when(documentManager.getDocumentContent(currentUri)).thenReturn(sourceCode);
        when(astService.parseSource(sourceCode, currentUri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 7)).thenReturn(modelClass);

        // Mock workspace index service
        Path servicePath = Paths.get("project/src/Service.groovy");
        Path controllerPath = Paths.get("project/src/Controller.groovy");

        SymbolInfo classRef1 = new SymbolInfo("Model", SymbolKind.CLASS, servicePath, 10, 15);

        SymbolInfo classRef2 = new SymbolInfo("Model", SymbolKind.CLASS, controllerPath, 20, 25);

        when(workspaceIndexService.searchSymbols("Model"))
                .thenReturn(CompletableFuture.completedFuture(Stream.of(classRef1, classRef2)));

        // Act
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(currentUri),
                        new Position(0, 6),
                        new ReferenceContext(false));

        List<? extends Location> locations = referencesHandler.handleReferences(params).get();

        // Assert
        assertThat(locations).hasSize(2);

        assertThat(locations.get(0).getUri()).endsWith("Service.groovy");
        assertThat(locations.get(1).getUri()).endsWith("Controller.groovy");
    }

    @Test
    @DisplayName("Should find property references across files via WorkspaceIndexService")
    void testCrossFilePropertyReferences() throws Exception {
        // Arrange
        String currentUri = "file:///project/src/Config.groovy";
        String sourceCode = "class Config { static String appName = 'MyApp' }";

        ModuleNode moduleNode = new ModuleNode((org.codehaus.groovy.control.SourceUnit) null);
        ClassNode configClass = new ClassNode("Config", 0, ClassNode.SUPER);
        FieldNode fieldNode =
                new FieldNode("appName", 1, ClassHelper.STRING_TYPE, configClass, null);
        PropertyNode propertyNode = new PropertyNode(fieldNode, 1, null, null);
        configClass.addProperty(propertyNode);
        moduleNode.addClass(configClass);

        // Mock AST service
        when(documentManager.getDocumentContent(currentUri)).thenReturn(sourceCode);
        when(astService.parseSource(sourceCode, currentUri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 30)).thenReturn(propertyNode);

        // Mock workspace index service
        Path appPath = Paths.get("project/src/App.groovy");
        SymbolInfo propertyRef = new SymbolInfo("appName", SymbolKind.PROPERTY, appPath, 5, 10);

        when(workspaceIndexService.searchSymbols("appName"))
                .thenReturn(CompletableFuture.completedFuture(Stream.of(propertyRef)));

        // Act
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(currentUri),
                        new Position(0, 29),
                        new ReferenceContext(false));

        List<? extends Location> locations = referencesHandler.handleReferences(params).get();

        // Assert
        assertThat(locations).hasSize(1);

        Location location = locations.get(0);
        assertThat(location.getUri()).endsWith("App.groovy");
        assertThat(location.getRange().getStart().getLine()).isEqualTo(4);
        assertThat(location.getRange().getStart().getCharacter()).isEqualTo(9);
    }

    @Test
    @DisplayName("Should filter references by symbol kind")
    void testCrossFileFilterBySymbolKind() throws Exception {
        // Arrange
        String currentUri = "file:///project/src/Utils.groovy";
        String sourceCode = "class Utils { void process() {} }";

        ModuleNode moduleNode = new ModuleNode((org.codehaus.groovy.control.SourceUnit) null);
        ClassNode utilsClass = new ClassNode("Utils", 0, ClassNode.SUPER);
        MethodNode methodNode =
                new MethodNode(
                        "process",
                        1,
                        ClassHelper.VOID_TYPE,
                        Parameter.EMPTY_ARRAY,
                        ClassNode.EMPTY_ARRAY,
                        null);
        utilsClass.addMethod(methodNode);
        moduleNode.addClass(utilsClass);

        // Mock AST service
        when(documentManager.getDocumentContent(currentUri)).thenReturn(sourceCode);
        when(astService.parseSource(sourceCode, currentUri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 20)).thenReturn(methodNode);

        // Mock workspace index service - return mixed results
        Path path1 = Paths.get("project/src/File1.groovy");
        Path path2 = Paths.get("project/src/File2.groovy");

        SymbolInfo methodRef = new SymbolInfo("process", SymbolKind.METHOD, path1, 10, 15);

        SymbolInfo fieldRef =
                new SymbolInfo(
                        "process",
                        SymbolKind.FIELD, // Different kind - should be filtered out
                        path2,
                        20,
                        25);

        when(workspaceIndexService.searchSymbols("process"))
                .thenReturn(CompletableFuture.completedFuture(Stream.of(methodRef, fieldRef)));

        // Act
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(currentUri),
                        new Position(0, 19),
                        new ReferenceContext(false));

        List<? extends Location> locations = referencesHandler.handleReferences(params).get();

        // Assert
        assertThat(locations).hasSize(1); // Only method reference
        assertThat(locations.get(0).getUri()).endsWith("File1.groovy");
    }

    @Test
    @DisplayName("Should handle workspace index service errors gracefully")
    void testCrossFileIndexServiceError() throws Exception {
        // Arrange
        String currentUri = "file:///project/src/Service.groovy";
        String sourceCode = "class Service { void handle() {} }";

        ModuleNode moduleNode = new ModuleNode((org.codehaus.groovy.control.SourceUnit) null);
        ClassNode serviceClass = new ClassNode("Service", 0, ClassNode.SUPER);
        MethodNode methodNode =
                new MethodNode(
                        "handle",
                        1,
                        ClassHelper.VOID_TYPE,
                        Parameter.EMPTY_ARRAY,
                        ClassNode.EMPTY_ARRAY,
                        null);
        serviceClass.addMethod(methodNode);
        moduleNode.addClass(serviceClass);

        // Mock AST service
        when(documentManager.getDocumentContent(currentUri)).thenReturn(sourceCode);
        when(astService.parseSource(sourceCode, currentUri)).thenReturn(moduleNode);
        when(astService.findNodeAtPosition(moduleNode, 1, 22)).thenReturn(methodNode);

        // Mock workspace index service - throws exception
        when(workspaceIndexService.searchSymbols("handle"))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Index error")));

        // Act
        ReferenceParams params =
                new ReferenceParams(
                        new TextDocumentIdentifier(currentUri),
                        new Position(0, 21),
                        new ReferenceContext(false));

        List<? extends Location> locations = referencesHandler.handleReferences(params).get();

        // Assert
        assertThat(locations).isEmpty(); // Should return empty list on error
    }
}
