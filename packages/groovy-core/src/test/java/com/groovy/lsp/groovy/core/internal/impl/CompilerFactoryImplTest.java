package com.groovy.lsp.groovy.core.internal.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.groovy.lsp.test.annotations.UnitTest;
import java.util.List;
import java.util.Map;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

class CompilerFactoryImplTest {

    private CompilerFactoryImpl factory;

    @BeforeEach
    void setUp() {
        factory = new CompilerFactoryImpl();
    }

    @Nested
    @DisplayName("Default configuration")
    class DefaultConfiguration {

        @UnitTest
        @DisplayName("Should create configuration with UTF-8 encoding")
        void shouldHaveUtf8Encoding() {
            CompilerConfiguration config = factory.createDefaultConfiguration();

            assertThat(config.getSourceEncoding()).isEqualTo("UTF-8");
        }

        @UnitTest
        @DisplayName("Should enable verbose error reporting")
        void shouldEnableVerboseErrors() {
            CompilerConfiguration config = factory.createDefaultConfiguration();

            assertThat(config.getVerbose()).isTrue();
        }

        @UnitTest
        @DisplayName("Should enable Parrot parser")
        void shouldEnableParrotParser() {
            CompilerConfiguration config = factory.createDefaultConfiguration();
            Map<String, Boolean> optimizations = config.getOptimizationOptions();

            assertThat(optimizations).containsEntry("parrot", true);
        }

        @UnitTest
        @DisplayName("Should enable invokedynamic")
        void shouldEnableInvokedynamic() {
            CompilerConfiguration config = factory.createDefaultConfiguration();
            Map<String, Boolean> optimizations = config.getOptimizationOptions();

            assertThat(optimizations).containsEntry("indy", true);
        }

        @UnitTest
        @DisplayName("Should preserve groovydoc")
        void shouldPreserveGroovydoc() {
            CompilerConfiguration config = factory.createDefaultConfiguration();
            Map<String, Boolean> optimizations = config.getOptimizationOptions();

            assertThat(optimizations).containsEntry("groovydoc", true);
        }

        @UnitTest
        @DisplayName("Should disable int optimization for debugging")
        void shouldDisableIntOptimization() {
            CompilerConfiguration config = factory.createDefaultConfiguration();
            Map<String, Boolean> optimizations = config.getOptimizationOptions();

            assertThat(optimizations).containsEntry("int", false);
        }

        @UnitTest
        @DisplayName("Should have import customizers")
        void shouldHaveImportCustomizers() {
            CompilerConfiguration config = factory.createDefaultConfiguration();

            assertThat(config.getCompilationCustomizers()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Configuration with classpath")
    class ConfigurationWithClasspath {

        @UnitTest
        @DisplayName("Should create configuration with provided classpath")
        void shouldSetClasspath() {
            List<String> classpath = List.of("/path/to/lib1.jar", "/path/to/lib2.jar");

            CompilerConfiguration config = factory.createConfigurationWithClasspath(classpath);

            assertThat(config.getClasspath()).containsAll(classpath);
        }

        @UnitTest
        @DisplayName("Should inherit default configuration settings")
        void shouldInheritDefaultSettings() {
            List<String> classpath = List.of("/path/to/lib.jar");

            CompilerConfiguration config = factory.createConfigurationWithClasspath(classpath);

            assertThat(config.getSourceEncoding()).isEqualTo("UTF-8");
            assertThat(config.getVerbose()).isTrue();
            assertThat(config.getOptimizationOptions()).containsEntry("parrot", true);
        }

        @UnitTest
        @DisplayName("Should throw exception for null classpath")
        @SuppressWarnings("NullAway") // Intentionally testing null handling
        void shouldThrowForNullClasspath() {
            List<String> nullClasspath = null;
            assertThatThrownBy(() -> factory.createConfigurationWithClasspath(nullClasspath))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Classpath cannot be null");
        }
    }

    @Nested
    @DisplayName("Script configuration")
    class ScriptConfiguration {

        @UnitTest
        @DisplayName("Should set script base class")
        void shouldSetScriptBaseClass() {
            CompilerConfiguration config = factory.createScriptConfiguration();

            assertThat(config.getScriptBaseClass()).isEqualTo("groovy.lang.Script");
        }

        @UnitTest
        @DisplayName("Should enable script extensions")
        void shouldEnableScriptExtensions() {
            CompilerConfiguration config = factory.createScriptConfiguration();

            assertThat(config.getScriptExtensions()).contains("groovy", "gvy", "gy", "gsh");
        }

        @UnitTest
        @DisplayName("Should inherit default configuration settings")
        void shouldInheritDefaultSettings() {
            CompilerConfiguration config = factory.createScriptConfiguration();

            assertThat(config.getSourceEncoding()).isEqualTo("UTF-8");
            assertThat(config.getOptimizationOptions()).containsEntry("parrot", true);
        }
    }

    @Nested
    @DisplayName("Type checking configuration")
    class TypeCheckingConfiguration {

        @UnitTest
        @DisplayName("Should enable static type checking when requested")
        void shouldEnableStaticTypeChecking() {
            CompilerConfiguration config = factory.createTypeCheckingConfiguration(true);

            // Verify that compilation customizers include type checking transformation
            assertThat(config.getCompilationCustomizers())
                    .anySatisfy(
                            customizer ->
                                    assertThat(customizer.getClass().getName())
                                            .contains("ASTTransformationCustomizer"));
        }

        @UnitTest
        @DisplayName("Should not add type checking when disabled")
        void shouldNotAddTypeCheckingWhenDisabled() {
            CompilerConfiguration config = factory.createTypeCheckingConfiguration(false);

            // The configuration should still have the default customizers (import customizer)
            // but not the type checking transformation
            long astTransformCount =
                    config.getCompilationCustomizers().stream()
                            .filter(
                                    c ->
                                            c.getClass()
                                                    .getName()
                                                    .contains("ASTTransformationCustomizer"))
                            .count();

            assertThat(astTransformCount).isEqualTo(0);
        }

        @UnitTest
        @DisplayName("Should inherit default configuration settings")
        void shouldInheritDefaultSettings() {
            CompilerConfiguration config = factory.createTypeCheckingConfiguration(true);

            assertThat(config.getSourceEncoding()).isEqualTo("UTF-8");
            assertThat(config.getOptimizationOptions()).containsEntry("parrot", true);
        }
    }

    @Nested
    @DisplayName("Static factory methods")
    class StaticFactoryMethods {

        @UnitTest
        @DisplayName("Should provide static method for default configuration")
        void shouldProvideStaticDefaultConfiguration() {
            CompilerConfiguration config = CompilerFactoryImpl.createDefaultConfigurationStatic();

            assertThat(config).isNotNull();
            assertThat(config.getSourceEncoding()).isEqualTo("UTF-8");
            assertThat(config.getOptimizationOptions()).containsEntry("parrot", true);
        }

        @UnitTest
        @DisplayName("Should provide static method for classpath configuration")
        void shouldProvideStaticClasspathConfiguration() {
            List<String> classpath = List.of("/path/to/lib.jar");
            CompilerConfiguration config =
                    CompilerFactoryImpl.createConfigurationWithClasspathStatic(classpath);

            assertThat(config).isNotNull();
            assertThat(config.getClasspath()).containsAll(classpath);
        }

        @UnitTest
        @DisplayName("Should provide static method for script configuration")
        void shouldProvideStaticScriptConfiguration() {
            CompilerConfiguration config = CompilerFactoryImpl.createScriptConfigurationStatic();

            assertThat(config).isNotNull();
            assertThat(config.getScriptBaseClass()).isEqualTo("groovy.lang.Script");
        }

        @UnitTest
        @DisplayName("Should provide static method for type checking configuration")
        void shouldProvideStaticTypeCheckingConfiguration() {
            CompilerConfiguration config =
                    CompilerFactoryImpl.createTypeCheckingConfigurationStatic(true);

            assertThat(config).isNotNull();
            assertThat(config.getCompilationCustomizers()).isNotEmpty();
        }
    }
}
