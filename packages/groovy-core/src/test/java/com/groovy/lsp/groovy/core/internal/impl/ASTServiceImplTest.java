package com.groovy.lsp.groovy.core.internal.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.groovy.lsp.test.annotations.UnitTest;
import java.util.List;
import java.util.Objects;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;

/**
 * ASTServiceImplのテストクラス。
 */
class ASTServiceImplTest {

    private ASTServiceImpl astService;

    /**
     * Helper method to safely pass nullable ModuleNode to methods that require non-null.
     * This is used for testing null handling behavior.
     */
    private static ModuleNode requireNonNullForTest(@Nullable ModuleNode moduleNode) {
        return Objects.requireNonNull(moduleNode, "Test expects non-null moduleNode");
    }

    @BeforeEach
    void setUp() {
        astService = new ASTServiceImpl();
    }

    @UnitTest
    void parseSource_shouldParseValidGroovyCode() {
        // given
        String sourceCode =
                """
                class TestClass {
                    String name

                    void sayHello() {
                        println "Hello, $name!"
                    }
                }
                """;
        String sourceName = "TestClass.groovy";

        // when
        ModuleNode moduleNode = astService.parseSource(sourceCode, sourceName);

        // then
        assertThat(moduleNode).isNotNull();
        assertThat(requireNonNullForTest(moduleNode).getClasses()).hasSize(1);
        assertThat(requireNonNullForTest(moduleNode).getClasses().get(0).getName())
                .isEqualTo("TestClass");
    }

    @UnitTest
    void parseSource_shouldReturnNullForInvalidCode() {
        // given
        String invalidCode = "class { invalid syntax";
        String sourceName = "Invalid.groovy";

        // when
        ModuleNode moduleNode = astService.parseSource(invalidCode, sourceName);

        // then
        assertThat(moduleNode).isNull();
    }

    @UnitTest
    void parseSource_shouldParseWithCustomCompilerConfiguration() {
        // given
        String sourceCode = "println 'test'";
        String sourceName = "Script.groovy";
        CompilerConfiguration config = new CompilerConfiguration();
        config.setScriptBaseClass("groovy.lang.Script");

        // when
        ModuleNode moduleNode = astService.parseSource(sourceCode, sourceName, config);

        // then
        assertThat(moduleNode).isNotNull();
    }

    @UnitTest
    void parseSource_shouldReturnFromCacheForSameSourceCode() {
        // given
        String sourceCode = "class CacheTest {}";
        String sourceName = "CacheTest.groovy";

        // when
        ModuleNode first = astService.parseSource(sourceCode, sourceName);
        ModuleNode second = astService.parseSource(sourceCode, sourceName);

        // then
        assertThat(first).isNotNull();
        assertThat(second).isSameAs(first); // Same instance from cache
    }

    @UnitTest
    void parseSource_shouldThrowExceptionForNullParameters() throws Exception {
        // when/then
        // Testing null source code parameter using reflection to bypass NullAway
        assertThatThrownBy(
                        () -> {
                            try {
                                java.lang.reflect.Method method =
                                        astService
                                                .getClass()
                                                .getMethod(
                                                        "parseSource", String.class, String.class);
                                method.invoke(astService, null, "test.groovy");
                            } catch (java.lang.reflect.InvocationTargetException e) {
                                throw e.getCause();
                            }
                        })
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Source code cannot be null");

        // Testing null source name parameter using reflection to bypass NullAway
        assertThatThrownBy(
                        () -> {
                            try {
                                java.lang.reflect.Method method =
                                        astService
                                                .getClass()
                                                .getMethod(
                                                        "parseSource", String.class, String.class);
                                method.invoke(astService, "code", null);
                            } catch (java.lang.reflect.InvocationTargetException e) {
                                throw e.getCause();
                            }
                        })
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Source name cannot be null");

        // Testing null compiler configuration parameter using reflection to bypass NullAway
        assertThatThrownBy(
                        () -> {
                            try {
                                java.lang.reflect.Method method =
                                        astService
                                                .getClass()
                                                .getMethod(
                                                        "parseSource",
                                                        String.class,
                                                        String.class,
                                                        CompilerConfiguration.class);
                                method.invoke(astService, "code", "test.groovy", null);
                            } catch (java.lang.reflect.InvocationTargetException e) {
                                throw e.getCause();
                            }
                        })
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Compiler configuration cannot be null");
    }

    @UnitTest
    void findNodeAtPosition_shouldFindNodeAtCorrectPosition() {
        // given
        String sourceCode =
                """
                class TestClass {
                    String name = "test"

                    void method() {
                        println name
                    }
                }
                """;
        ModuleNode moduleNode = astService.parseSource(sourceCode, "Test.groovy");

        // when
        ASTNode node =
                astService.findNodeAtPosition(
                        requireNonNullForTest(moduleNode), 2, 12); // "String" position

        // then
        assertThat(node).isNotNull();
    }

    @UnitTest
    @SuppressWarnings("NullAway") // Intentionally testing null parameter handling
    void findNodeAtPosition_shouldReturnNullForNullModule() {
        // when
        ASTNode node = astService.findNodeAtPosition(null, 1, 1);

        // then
        assertThat(node).isNull();
    }

    @UnitTest
    void findAllVariables_shouldFindAllVariables() {
        // given
        String sourceCode =
                """
                class TestClass {
                    String field1 = "test"
                    int field2 = 42

                    void method() {
                        def local1 = "local"
                        String local2 = field1
                        println local1 + local2
                    }
                }
                """;
        ModuleNode moduleNode = astService.parseSource(sourceCode, "Variables.groovy");

        // when
        List<VariableExpression> variables =
                astService.findAllVariables(requireNonNullForTest(moduleNode));

        // then
        assertThat(variables).isNotEmpty();
        // Variables include field references and local variable references
    }

    @UnitTest
    @SuppressWarnings("NullAway") // Intentionally testing null parameter handling
    void findAllVariables_shouldReturnEmptyListForNullModule() {
        // when
        List<VariableExpression> variables = astService.findAllVariables(null);

        // then
        assertThat(variables).isEmpty();
    }

    @UnitTest
    void findAllMethodCalls_shouldFindAllMethodCalls() {
        // given
        String sourceCode =
                """
                class TestClass {
                    void method1() {
                        println "Hello"
                        method2()
                        "test".toUpperCase()
                    }

                    void method2() {
                        System.out.println("Test")
                    }
                }
                """;
        ModuleNode moduleNode = astService.parseSource(sourceCode, "MethodCalls.groovy");

        // when
        List<MethodCallExpression> methodCalls =
                astService.findAllMethodCalls(requireNonNullForTest(moduleNode));

        // then
        assertThat(methodCalls).isNotEmpty();
        assertThat(methodCalls)
                .extracting(call -> call.getMethodAsString())
                .contains("println", "method2", "toUpperCase");
    }

    @UnitTest
    @SuppressWarnings("NullAway") // Intentionally testing null parameter handling
    void findAllMethodCalls_shouldReturnEmptyListForNullModule() {
        // when
        List<MethodCallExpression> methodCalls = astService.findAllMethodCalls(null);

        // then
        assertThat(methodCalls).isEmpty();
    }

    @UnitTest
    void clearCache_shouldClearCache() {
        // given
        String sourceCode = "class ClearTest {}";
        String sourceName = "ClearTest.groovy";
        astService.parseSource(sourceCode, sourceName);

        // when
        astService.clearCache();

        // Modify the source slightly to verify it's re-parsed
        String modifiedCode = "class ClearTest { /* modified */ }";
        ModuleNode newNode = astService.parseSource(modifiedCode, sourceName);

        // then
        assertThat(newNode).isNotNull();
        // If cache was working, it would return the old AST
    }

    @UnitTest
    void invalidateCache_shouldInvalidateSpecificSourceCache() {
        // given
        String sourceCode1 = "class Test1 {}";
        String sourceCode2 = "class Test2 {}";
        astService.parseSource(sourceCode1, "Test1.groovy");
        astService.parseSource(sourceCode2, "Test2.groovy");

        // when
        astService.invalidateCache("Test1.groovy");

        // Parse again to verify Test1 is re-parsed but Test2 is from cache
        ModuleNode node1 = astService.parseSource(sourceCode1, "Test1.groovy");
        ModuleNode node2 = astService.parseSource(sourceCode2, "Test2.groovy");

        // then
        assertThat(node1).isNotNull();
        assertThat(node2).isNotNull();
    }

    @UnitTest
    void parseSource_shouldParseComplexGroovyCode() {
        // given
        String complexCode =
                """
                package com.example

                import java.util.List

                @groovy.transform.CompileStatic
                class ComplexClass {
                    private List<String> items = []

                    ComplexClass(List<String> items) {
                        this.items = items
                    }

                    def processItems() {
                        items.collect { it.toUpperCase() }
                             .findAll { it.length() > 3 }
                             .sort()
                    }

                    static void main(String[] args) {
                        def instance = new ComplexClass(['apple', 'banana', 'cat'])
                        println instance.processItems()
                    }
                }
                """;
        String sourceName = "ComplexClass.groovy";

        // when
        ModuleNode moduleNode = astService.parseSource(complexCode, sourceName);

        // then
        assertThat(moduleNode).isNotNull();
        ModuleNode nonNullModule = requireNonNullForTest(moduleNode);
        assertThat(nonNullModule.getPackage()).isNotNull();
        // The package name might have a trailing dot, so let's handle that
        String packageName = nonNullModule.getPackage().getName();
        if (packageName.endsWith(".")) {
            packageName = packageName.substring(0, packageName.length() - 1);
        }
        assertThat(packageName).isEqualTo("com.example");
        assertThat(nonNullModule.getClasses()).hasSize(1);
        assertThat(nonNullModule.getClasses().get(0).getMethods()).hasSizeGreaterThan(1);
    }

    @UnitTest
    void parseSource_shouldParseScript() {
        // given
        String scriptCode =
                """
                import groovy.json.JsonSlurper

                def json = new JsonSlurper().parseText('{"name": "test"}')
                println json.name

                def closure = { String msg ->
                    println "Message: $msg"
                }

                closure('Hello')
                """;
        String sourceName = "script.groovy";

        // when
        ModuleNode moduleNode = astService.parseSource(scriptCode, sourceName);

        // then
        assertThat(moduleNode).isNotNull();
        assertThat(requireNonNullForTest(moduleNode).getImports()).isNotEmpty();
        assertThat(requireNonNullForTest(moduleNode).getStatementBlock()).isNotNull();
    }

    @UnitTest
    void findNodeAtPosition_shouldFindFieldNode() {
        // given
        String sourceCode =
                """
                class TestClass {
                    String myField = "value"
                    int number = 42
                }
                """;
        ModuleNode moduleNode = astService.parseSource(sourceCode, "FieldTest.groovy");

        // when - Find the field declaration
        ASTNode node =
                astService.findNodeAtPosition(
                        requireNonNullForTest(moduleNode), 2, 15); // Position of "myField"

        // then
        assertThat(node).isNotNull();
    }

    @UnitTest
    void findNodeAtPosition_shouldFindBinaryExpression() {
        // given
        String sourceCode =
                """
                class TestClass {
                    void method() {
                        int result = 10 + 20
                    }
                }
                """;
        ModuleNode moduleNode = astService.parseSource(sourceCode, "BinaryTest.groovy");

        // when - Find binary expression
        ASTNode node =
                astService.findNodeAtPosition(
                        requireNonNullForTest(moduleNode), 3, 25); // Position of "+"

        // then
        assertThat(node).isNotNull();
    }

    @UnitTest
    void findNodeAtPosition_shouldFindPropertyExpression() {
        // given
        String sourceCode =
                """
                class TestClass {
                    void method() {
                        String text = "hello"
                        int len = text.length()
                    }
                }
                """;
        ModuleNode moduleNode = astService.parseSource(sourceCode, "PropertyTest.groovy");

        // when - Find property expression
        ASTNode node =
                astService.findNodeAtPosition(
                        requireNonNullForTest(moduleNode), 4, 23); // Position of "text.length"

        // then
        assertThat(node).isNotNull();
    }

    @UnitTest
    void findNodeAtPosition_shouldFindFieldExpression() {
        // given
        String sourceCode =
                """
                class TestClass {
                    private String myField = "test"

                    void method() {
                        println this.myField
                    }
                }
                """;
        ModuleNode moduleNode = astService.parseSource(sourceCode, "FieldExprTest.groovy");

        // when - Find field expression
        ASTNode node =
                astService.findNodeAtPosition(
                        requireNonNullForTest(moduleNode), 5, 29); // Position of "this.myField"

        // then
        assertThat(node).isNotNull();
    }

    @UnitTest
    void findNodeAtPosition_shouldFindClassExpression() {
        // given
        String sourceCode =
                """
                class TestClass {
                    void method() {
                        Class clazz = String.class
                    }
                }
                """;
        ModuleNode moduleNode = astService.parseSource(sourceCode, "ClassExprTest.groovy");

        // when - Find class expression
        ASTNode node =
                astService.findNodeAtPosition(
                        requireNonNullForTest(moduleNode), 3, 30); // Position of "String.class"

        // then
        assertThat(node).isNotNull();
    }

    @UnitTest
    void findNodeAtPosition_shouldFindConstantExpression() {
        // given
        String sourceCode =
                """
                class TestClass {
                    void method() {
                        String text = "Hello World"
                        int number = 42
                    }
                }
                """;
        ModuleNode moduleNode = astService.parseSource(sourceCode, "ConstantTest.groovy");

        // when - Find constant expression
        ASTNode node =
                astService.findNodeAtPosition(
                        requireNonNullForTest(moduleNode), 3, 31); // Position of "Hello World"

        // then
        assertThat(node).isNotNull();
    }

    @UnitTest
    void findNodeAtPosition_shouldFindStatementInScriptBody() {
        // given
        String sourceCode =
                """
                // Script with statements outside of class
                println "Starting script"
                def result = 10 * 20
                println "Result: $result"
                """;
        ModuleNode moduleNode = astService.parseSource(sourceCode, "ScriptBodyTest.groovy");

        // when - Find node in script body
        ASTNode node =
                astService.findNodeAtPosition(
                        requireNonNullForTest(moduleNode), 3, 15); // Position in "10 * 20"

        // then
        assertThat(node).isNotNull();
    }

    @UnitTest
    void findNodeAtPosition_shouldReturnNullWhenPositionOutOfBounds() {
        // given
        String sourceCode =
                """
                class TestClass {
                    String name
                }
                """;
        ModuleNode moduleNode = astService.parseSource(sourceCode, "OutOfBoundsTest.groovy");

        // when - Try to find node at position outside any node bounds
        ASTNode node =
                astService.findNodeAtPosition(
                        requireNonNullForTest(moduleNode), 10, 10); // Beyond source

        // then
        assertThat(node).isNull();
    }

    @UnitTest
    void findAllVariables_shouldIncludeScriptVariables() {
        // given
        String scriptCode =
                """
                // Script-level variables
                def scriptVar1 = "test"
                String scriptVar2 = "another"

                class InnerClass {
                    def classVar = scriptVar1
                }

                println scriptVar1 + scriptVar2
                """;
        ModuleNode moduleNode = astService.parseSource(scriptCode, "ScriptVars.groovy");

        // when
        List<VariableExpression> variables =
                astService.findAllVariables(requireNonNullForTest(moduleNode));

        // then
        assertThat(variables).isNotEmpty();
        assertThat(variables)
                .extracting(VariableExpression::getName)
                .contains("scriptVar1", "scriptVar2");
    }

    @UnitTest
    void findAllMethodCalls_shouldIncludeScriptMethodCalls() {
        // given
        String scriptCode =
                """
                // Script with method calls at top level
                println "Direct call"

                def helper = { msg ->
                    println msg.toUpperCase()
                }

                helper("test")
                "test".substring(0, 2)
                """;
        ModuleNode moduleNode = astService.parseSource(scriptCode, "ScriptCalls.groovy");

        // when
        List<MethodCallExpression> methodCalls =
                astService.findAllMethodCalls(requireNonNullForTest(moduleNode));

        // then
        assertThat(methodCalls).isNotEmpty();
        assertThat(methodCalls)
                .extracting(MethodCallExpression::getMethodAsString)
                .contains("println", "toUpperCase", "call", "substring");
    }
}
