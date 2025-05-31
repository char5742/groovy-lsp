package com.groovy.lsp.integration;

import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.groovy.core.api.CompilerConfigurationService;
import com.groovy.lsp.groovy.core.api.GroovyCoreFactory;
import com.groovy.lsp.codenarc.LintEngine;
import com.groovy.lsp.formatting.GroovyFormatter;
import com.groovy.lsp.workspace.api.WorkspaceIndexer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 各モジュール間の統合をテスト
 */
@Tag("integration")
class ModuleIntegrationTest {
    
    @TempDir
    Path tempDir;
    
    private GroovyCoreFactory groovyCoreFactory;
    private LintEngine lintEngine;
    private GroovyFormatter formatter;
    private WorkspaceIndexer indexer;
    
    @BeforeEach
    void setUp() {
        groovyCoreFactory = GroovyCoreFactory.getInstance();
        lintEngine = new LintEngine();
        formatter = new GroovyFormatter();
        indexer = new WorkspaceIndexer(tempDir.toString());
    }
    
    @Test
    @DisplayName("Groovy Core と Lint Engine の統合")
    void testGroovyCoreAndLintIntegration() throws Exception {
        // Groovyファイルを作成
        Path groovyFile = tempDir.resolve("TestClass.groovy");
        String content = """
            class TestClass {
                def unusedVariable = 42  // This should trigger a lint warning
                
                def methodWithIssues() {
                    def x = 1
                    def y = 2
                    // Missing return statement
                }
            }
            """;
        Files.writeString(groovyFile, content);
        
        // Groovy Coreでパース
        ASTService astService = groovyCoreFactory.createASTService();
        CompletableFuture<Object> astFuture = astService.parseFile(groovyFile.toString());
        Object ast = astFuture.get();
        assertThat(ast).isNotNull();
        
        // Lint Engineで解析
        CompletableFuture<List<Object>> lintFuture = lintEngine.analyzeFile(groovyFile.toString());
        List<Object> violations = lintFuture.get();
        
        // 違反が検出されることを確認
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream()
            .map(Object::toString)
            .anyMatch(s -> s.contains("unused")))
            .isTrue();
    }
    
    @Test
    @DisplayName("Formatter と Groovy Core の統合")
    void testFormatterAndGroovyCoreIntegration() throws Exception {
        // フォーマット前のコード
        String unformattedCode = """
            class    BadlyFormatted    {
              def    method( param1,param2 )   {
                 println    "Hello"
                   return    param1+param2
              }
            }
            """;
        
        // フォーマット実行
        CompletableFuture<String> formatFuture = formatter.format(unformattedCode);
        String formattedCode = formatFuture.get();
        
        // フォーマット後のコードをパース可能か確認
        Path formattedFile = tempDir.resolve("Formatted.groovy");
        Files.writeString(formattedFile, formattedCode);
        
        ASTService astService = groovyCoreFactory.createASTService();
        CompletableFuture<Object> astFuture = astService.parseFile(formattedFile.toString());
        Object ast = astFuture.get();
        
        // パース成功を確認
        assertThat(ast).isNotNull();
        
        // フォーマットが適用されていることを確認
        assertThat(formattedCode)
            .doesNotContain("    BadlyFormatted    ")
            .contains("class BadlyFormatted {");
    }
    
    @Test
    @DisplayName("Workspace Indexer と複数モジュールの統合")
    void testWorkspaceIndexerIntegration() throws Exception {
        // 複数のGroovyファイルを作成
        createGroovyFile("src/main/groovy/com/example/Service.groovy", """
            package com.example
            
            class Service {
                def process(String input) {
                    return input.toUpperCase()
                }
            }
            """);
        
        createGroovyFile("src/main/groovy/com/example/Repository.groovy", """
            package com.example
            
            class Repository {
                def save(entity) {
                    // Save logic
                }
            }
            """);
        
        createGroovyFile("src/test/groovy/com/example/ServiceTest.groovy", """
            package com.example
            
            import spock.lang.Specification
            
            class ServiceTest extends Specification {
                def "test service"() {
                    given:
                    def service = new Service()
                    
                    when:
                    def result = service.process("hello")
                    
                    then:
                    result == "HELLO"
                }
            }
            """);
        
        // インデックス作成
        CompletableFuture<Void> indexFuture = indexer.indexWorkspace(tempDir.toString());
        indexFuture.get();
        
        // シンボル検索
        CompletableFuture<List<Object>> searchFuture = indexer.findSymbol("Service");
        List<Object> symbols = searchFuture.get();
        
        assertThat(symbols).hasSize(2); // Service class and ServiceTest
        
        // 参照検索
        CompletableFuture<List<Object>> refsFuture = indexer.findReferences("com.example.Service");
        List<Object> references = refsFuture.get();
        
        assertThat(references).isNotEmpty();
    }
    
    @Test
    @DisplayName("エンドツーエンド: コード変更からLint、フォーマット、インデックス更新まで")
    void testEndToEndWorkflow() throws Exception {
        // 初期コード
        Path sourceFile = createGroovyFile("src/main/groovy/Workflow.groovy", """
            class   Workflow   {
                def    unusedVar = 123
                
                def    process(  input  )   {
                    println   "Processing: ${input}"
                    return   input.toLowerCase(  )
                }
            }
            """);
        
        // 1. Lint実行
        CompletableFuture<List<Object>> lintFuture = lintEngine.analyzeFile(sourceFile.toString());
        List<Object> violations = lintFuture.get();
        assertThat(violations).isNotEmpty();
        
        // 2. フォーマット実行
        String originalContent = Files.readString(sourceFile);
        CompletableFuture<String> formatFuture = formatter.format(originalContent);
        String formattedContent = formatFuture.get();
        Files.writeString(sourceFile, formattedContent);
        
        // 3. インデックス更新
        CompletableFuture<Void> indexFuture = indexer.updateFile(sourceFile.toString());
        indexFuture.get();
        
        // 4. 結果確認
        assertThat(formattedContent)
            .contains("class Workflow {")
            .doesNotContain("class   Workflow   {");
        
        // 再度Lint実行（フォーマット後）
        CompletableFuture<List<Object>> lintAfterFormat = lintEngine.analyzeFile(sourceFile.toString());
        List<Object> violationsAfter = lintAfterFormat.get();
        
        // 未使用変数の警告は残るが、フォーマット関連の問題は解決
        assertThat(violationsAfter.size()).isLessThanOrEqualTo(violations.size());
    }
    
    private Path createGroovyFile(String relativePath, String content) throws Exception {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }
}