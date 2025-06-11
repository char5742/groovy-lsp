package com.groovy.lsp.groovy.core.internal.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.groovy.lsp.test.annotations.UnitTest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.junit.jupiter.api.BeforeEach;

/**
 * Additional tests for CompilerFactoryImpl to improve branch coverage.
 */
class CompilerFactoryImplAdditionalTest {

    private CompilerFactoryImpl factory;

    @BeforeEach
    void setUp() {
        factory = new CompilerFactoryImpl();
    }

    @UnitTest
    void createConfigurationWithClasspath_shouldHandleEmptyClasspath() {
        // given
        List<String> emptyClasspath = Collections.emptyList();

        // when
        CompilerConfiguration config = factory.createConfigurationWithClasspath(emptyClasspath);

        // then
        assertThat(config.getClasspath()).isEmpty();
        // Should still have default settings
        assertThat(config.getSourceEncoding()).isEqualTo("UTF-8");
    }

    @UnitTest
    void createConfigurationWithClasspath_shouldHandleImmutableList() {
        // given
        List<String> immutableClasspath = List.of("/lib1.jar", "/lib2.jar");

        // when
        CompilerConfiguration config = factory.createConfigurationWithClasspath(immutableClasspath);

        // then
        assertThat(config.getClasspath()).containsAll(immutableClasspath);
    }

    @UnitTest
    void createConfigurationWithClasspath_shouldHandleMutableList() {
        // given
        List<String> mutableClasspath = new ArrayList<>();
        mutableClasspath.add("/lib1.jar");
        mutableClasspath.add("/lib2.jar");

        // when
        CompilerConfiguration config = factory.createConfigurationWithClasspath(mutableClasspath);

        // then
        assertThat(config.getClasspath()).containsAll(mutableClasspath);
    }

    @UnitTest
    void createConfigurationWithClasspath_shouldHandleClasspathWithDuplicates() {
        // given
        List<String> classpathWithDuplicates = Arrays.asList("/lib.jar", "/lib.jar", "/other.jar");

        // when
        CompilerConfiguration config =
                factory.createConfigurationWithClasspath(classpathWithDuplicates);

        // then
        assertThat(config.getClasspath()).containsAll(classpathWithDuplicates);
    }

    @UnitTest
    void createScriptConfiguration_shouldSetupCorrectly() {
        // when
        CompilerConfiguration config = factory.createScriptConfiguration();

        // then
        assertThat(config.getScriptBaseClass()).isEqualTo("groovy.lang.Script");
        assertThat(config.getScriptExtensions()).isNotEmpty();
        // Verify default imports are preserved
        assertThat(config.getCompilationCustomizers()).isNotEmpty();
    }

    @UnitTest
    void createTypeCheckingConfiguration_shouldHandleBothTrueAndFalse() {
        // when - true case
        CompilerConfiguration configTrue = factory.createTypeCheckingConfiguration(true);

        // then
        assertThat(configTrue.getCompilationCustomizers()).hasSizeGreaterThan(1);

        // when - false case
        CompilerConfiguration configFalse = factory.createTypeCheckingConfiguration(false);

        // then
        assertThat(configFalse.getCompilationCustomizers()).hasSize(1); // Only import customizer
    }

    @UnitTest
    void defaultConfiguration_shouldHaveCorrectOptimizationOptions() {
        // when
        CompilerConfiguration config = factory.createDefaultConfiguration();

        // then
        assertThat(config.getOptimizationOptions())
                .containsEntry("parrot", true)
                .containsEntry("indy", true)
                .containsEntry("groovydoc", true)
                .containsEntry("int", false);
    }

    @UnitTest
    void defaultConfiguration_shouldHaveImportCustomizer() {
        // when
        CompilerConfiguration config = factory.createDefaultConfiguration();

        // then
        assertThat(config.getCompilationCustomizers()).hasSize(1);
        CompilationCustomizer customizer = config.getCompilationCustomizers().get(0);
        assertThat(customizer.getClass().getName()).contains("ImportCustomizer");
    }

    @UnitTest
    void createConfigurationWithClasspath_shouldPreserveCustomizers() {
        // given
        List<String> classpath = List.of("/lib.jar");

        // when
        CompilerConfiguration config = factory.createConfigurationWithClasspath(classpath);

        // then
        assertThat(config.getCompilationCustomizers()).isNotEmpty();
    }

    @UnitTest
    void staticFactoryMethods_shouldProduceEquivalentConfigurations() {
        // Test that static methods produce configurations equivalent to instance methods

        // Default configuration
        CompilerConfiguration instanceDefault = factory.createDefaultConfiguration();
        CompilerConfiguration staticDefault =
                CompilerFactoryImpl.createDefaultConfigurationStatic();

        assertThat(staticDefault.getSourceEncoding())
                .isEqualTo(instanceDefault.getSourceEncoding());
        assertThat(staticDefault.getVerbose()).isEqualTo(instanceDefault.getVerbose());
        assertThat(staticDefault.getOptimizationOptions())
                .isEqualTo(instanceDefault.getOptimizationOptions());

        // Script configuration
        CompilerConfiguration instanceScript = factory.createScriptConfiguration();
        CompilerConfiguration staticScript = CompilerFactoryImpl.createScriptConfigurationStatic();

        assertThat(staticScript.getScriptBaseClass())
                .isEqualTo(instanceScript.getScriptBaseClass());
        assertThat(staticScript.getScriptExtensions())
                .isEqualTo(instanceScript.getScriptExtensions());
    }

    @UnitTest
    void createConfigurationWithClasspath_shouldHandlePathsWithSpaces() {
        // given
        List<String> pathsWithSpaces =
                List.of("/path with spaces/lib.jar", "/another path/with spaces/lib2.jar");

        // when
        CompilerConfiguration config = factory.createConfigurationWithClasspath(pathsWithSpaces);

        // then
        assertThat(config.getClasspath()).containsAll(pathsWithSpaces);
    }

    @UnitTest
    void createConfigurationWithClasspath_shouldHandleRelativePaths() {
        // given
        List<String> relativePaths = List.of("./lib/lib1.jar", "../libs/lib2.jar", "lib3.jar");

        // when
        CompilerConfiguration config = factory.createConfigurationWithClasspath(relativePaths);

        // then
        assertThat(config.getClasspath()).containsAll(relativePaths);
    }

    @UnitTest
    void multipleConfigurations_shouldBeIndependent() {
        // when - create multiple configurations
        CompilerConfiguration config1 = factory.createDefaultConfiguration();
        CompilerConfiguration config2 = factory.createDefaultConfiguration();

        // Modify one configuration
        config1.setSourceEncoding("ISO-8859-1");

        // then - other configuration should not be affected
        assertThat(config2.getSourceEncoding()).isEqualTo("UTF-8");
    }

    @UnitTest
    void createScriptConfiguration_shouldHandleScriptExtensions() {
        // when
        CompilerConfiguration config = factory.createScriptConfiguration();

        // then
        assertThat(config.getScriptExtensions())
                .contains("groovy", "gvy", "gy", "gsh")
                .hasSizeGreaterThanOrEqualTo(4);
    }

    @UnitTest
    void allConfigurations_shouldBeNonNull() {
        // Test that all factory methods return non-null configurations
        assertThatCode(
                        () -> {
                            assertThat(factory.createDefaultConfiguration()).isNotNull();
                            assertThat(factory.createConfigurationWithClasspath(List.of()))
                                    .isNotNull();
                            assertThat(factory.createScriptConfiguration()).isNotNull();
                            assertThat(factory.createTypeCheckingConfiguration(true)).isNotNull();
                            assertThat(factory.createTypeCheckingConfiguration(false)).isNotNull();
                        })
                .doesNotThrowAnyException();
    }
}
