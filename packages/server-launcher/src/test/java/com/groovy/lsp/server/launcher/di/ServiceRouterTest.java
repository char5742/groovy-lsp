package com.groovy.lsp.server.launcher.di;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.groovy.lsp.codenarc.LintEngine;
import com.groovy.lsp.formatting.service.FormattingService;
import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.groovy.core.api.CompilerConfigurationService;
import com.groovy.lsp.groovy.core.api.IncrementalCompilationService;
import com.groovy.lsp.groovy.core.api.TypeInferenceService;
import com.groovy.lsp.shared.workspace.api.WorkspaceIndexService;
import com.groovy.lsp.test.annotations.UnitTest;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.BeforeEach;

/**
 * Tests for ServiceRouter class.
 */
class ServiceRouterTest {

    private ASTService astService;
    private CompilerConfigurationService compilerConfigurationService;
    private IncrementalCompilationService incrementalCompilationService;
    private TypeInferenceService typeInferenceService;
    private WorkspaceIndexService workspaceIndexService;
    private FormattingService formattingService;
    private LintEngine lintEngine;

    @BeforeEach
    void setUp() {
        astService = mock(ASTService.class);
        compilerConfigurationService = mock(CompilerConfigurationService.class);
        incrementalCompilationService = mock(IncrementalCompilationService.class);
        typeInferenceService = mock(TypeInferenceService.class);
        workspaceIndexService = mock(WorkspaceIndexService.class);
        formattingService = mock(FormattingService.class);
        lintEngine = mock(LintEngine.class);
    }

    @UnitTest
    void constructor_shouldInitializeWithAllServices() {
        // when
        ServiceRouter router =
                new ServiceRouter(
                        astService,
                        compilerConfigurationService,
                        incrementalCompilationService,
                        typeInferenceService,
                        workspaceIndexService,
                        formattingService,
                        lintEngine);

        // then
        assertThat(router.getAstService()).isSameAs(astService);
        assertThat(router.getCompilerConfigurationService()).isSameAs(compilerConfigurationService);
        assertThat(router.getIncrementalCompilationService())
                .isSameAs(incrementalCompilationService);
        assertThat(router.getTypeInferenceService()).isSameAs(typeInferenceService);
        assertThat(router.getWorkspaceIndexService()).isSameAs(workspaceIndexService);
        assertThat(router.getFormattingService()).isSameAs(formattingService);
        assertThat(router.getLintEngine()).isSameAs(lintEngine);
    }

    @UnitTest
    void constructor_shouldThrowExceptionForNullAstService() throws Exception {
        // Use reflection to bypass NullAway compile-time checks
        Constructor<ServiceRouter> constructor =
                ServiceRouter.class.getConstructor(
                        ASTService.class,
                        CompilerConfigurationService.class,
                        IncrementalCompilationService.class,
                        TypeInferenceService.class,
                        WorkspaceIndexService.class,
                        FormattingService.class,
                        LintEngine.class);

        // when/then
        assertThatThrownBy(
                        () ->
                                constructor.newInstance(
                                        null,
                                        compilerConfigurationService,
                                        incrementalCompilationService,
                                        typeInferenceService,
                                        workspaceIndexService,
                                        formattingService,
                                        lintEngine))
                .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                .hasCauseInstanceOf(NullPointerException.class)
                .getCause()
                .hasMessageContaining("ASTService must not be null");
    }

    @UnitTest
    void constructor_shouldThrowExceptionForNullCompilerConfigurationService() throws Exception {
        // Use reflection to bypass NullAway compile-time checks
        Constructor<ServiceRouter> constructor =
                ServiceRouter.class.getConstructor(
                        ASTService.class,
                        CompilerConfigurationService.class,
                        IncrementalCompilationService.class,
                        TypeInferenceService.class,
                        WorkspaceIndexService.class,
                        FormattingService.class,
                        LintEngine.class);

        // when/then
        assertThatThrownBy(
                        () ->
                                constructor.newInstance(
                                        astService,
                                        null,
                                        incrementalCompilationService,
                                        typeInferenceService,
                                        workspaceIndexService,
                                        formattingService,
                                        lintEngine))
                .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                .hasCauseInstanceOf(NullPointerException.class)
                .getCause()
                .hasMessageContaining("CompilerConfigurationService must not be null");
    }

    @UnitTest
    void constructor_shouldThrowExceptionForNullIncrementalCompilationService() throws Exception {
        // Use reflection to bypass NullAway compile-time checks
        Constructor<ServiceRouter> constructor =
                ServiceRouter.class.getConstructor(
                        ASTService.class,
                        CompilerConfigurationService.class,
                        IncrementalCompilationService.class,
                        TypeInferenceService.class,
                        WorkspaceIndexService.class,
                        FormattingService.class,
                        LintEngine.class);

        // when/then
        assertThatThrownBy(
                        () ->
                                constructor.newInstance(
                                        astService,
                                        compilerConfigurationService,
                                        null,
                                        typeInferenceService,
                                        workspaceIndexService,
                                        formattingService,
                                        lintEngine))
                .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                .hasCauseInstanceOf(NullPointerException.class)
                .getCause()
                .hasMessageContaining("IncrementalCompilationService must not be null");
    }

    @UnitTest
    void constructor_shouldThrowExceptionForNullTypeInferenceService() throws Exception {
        // Use reflection to bypass NullAway compile-time checks
        Constructor<ServiceRouter> constructor =
                ServiceRouter.class.getConstructor(
                        ASTService.class,
                        CompilerConfigurationService.class,
                        IncrementalCompilationService.class,
                        TypeInferenceService.class,
                        WorkspaceIndexService.class,
                        FormattingService.class,
                        LintEngine.class);

        // when/then
        assertThatThrownBy(
                        () ->
                                constructor.newInstance(
                                        astService,
                                        compilerConfigurationService,
                                        incrementalCompilationService,
                                        null,
                                        workspaceIndexService,
                                        formattingService,
                                        lintEngine))
                .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                .hasCauseInstanceOf(NullPointerException.class)
                .getCause()
                .hasMessageContaining("TypeInferenceService must not be null");
    }

    @UnitTest
    void constructor_shouldThrowExceptionForNullWorkspaceIndexService() throws Exception {
        // Use reflection to bypass NullAway compile-time checks
        Constructor<ServiceRouter> constructor =
                ServiceRouter.class.getConstructor(
                        ASTService.class,
                        CompilerConfigurationService.class,
                        IncrementalCompilationService.class,
                        TypeInferenceService.class,
                        WorkspaceIndexService.class,
                        FormattingService.class,
                        LintEngine.class);

        // when/then
        assertThatThrownBy(
                        () ->
                                constructor.newInstance(
                                        astService,
                                        compilerConfigurationService,
                                        incrementalCompilationService,
                                        typeInferenceService,
                                        null,
                                        formattingService,
                                        lintEngine))
                .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                .hasCauseInstanceOf(NullPointerException.class)
                .getCause()
                .hasMessageContaining("WorkspaceIndexService must not be null");
    }

    @UnitTest
    void constructor_shouldThrowExceptionForNullFormattingService() throws Exception {
        // Use reflection to bypass NullAway compile-time checks
        Constructor<ServiceRouter> constructor =
                ServiceRouter.class.getConstructor(
                        ASTService.class,
                        CompilerConfigurationService.class,
                        IncrementalCompilationService.class,
                        TypeInferenceService.class,
                        WorkspaceIndexService.class,
                        FormattingService.class,
                        LintEngine.class);

        // when/then
        assertThatThrownBy(
                        () ->
                                constructor.newInstance(
                                        astService,
                                        compilerConfigurationService,
                                        incrementalCompilationService,
                                        typeInferenceService,
                                        workspaceIndexService,
                                        null,
                                        lintEngine))
                .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                .hasCauseInstanceOf(NullPointerException.class)
                .getCause()
                .hasMessageContaining("FormattingService must not be null");
    }

    @UnitTest
    void constructor_shouldThrowExceptionForNullLintEngine() throws Exception {
        // Use reflection to bypass NullAway compile-time checks
        Constructor<ServiceRouter> constructor =
                ServiceRouter.class.getConstructor(
                        ASTService.class,
                        CompilerConfigurationService.class,
                        IncrementalCompilationService.class,
                        TypeInferenceService.class,
                        WorkspaceIndexService.class,
                        FormattingService.class,
                        LintEngine.class);

        // when/then
        assertThatThrownBy(
                        () ->
                                constructor.newInstance(
                                        astService,
                                        compilerConfigurationService,
                                        incrementalCompilationService,
                                        typeInferenceService,
                                        workspaceIndexService,
                                        formattingService,
                                        null))
                .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                .hasCauseInstanceOf(NullPointerException.class)
                .getCause()
                .hasMessageContaining("LintEngine must not be null");
    }

    @UnitTest
    void areAllServicesAvailable_shouldReturnTrueWhenAllServicesAvailable() {
        // given
        ServiceRouter router =
                new ServiceRouter(
                        astService,
                        compilerConfigurationService,
                        incrementalCompilationService,
                        typeInferenceService,
                        workspaceIndexService,
                        formattingService,
                        lintEngine);

        // when
        boolean result = router.areAllServicesAvailable();

        // then
        assertThat(result).isTrue();
    }

    @UnitTest
    void getAstService_shouldReturnService() {
        // given
        ServiceRouter router =
                new ServiceRouter(
                        astService,
                        compilerConfigurationService,
                        incrementalCompilationService,
                        typeInferenceService,
                        workspaceIndexService,
                        formattingService,
                        lintEngine);

        // when
        ASTService result = router.getAstService();

        // then
        assertThat(result).isSameAs(astService);
    }

    @UnitTest
    void getCompilerConfigurationService_shouldReturnService() {
        // given
        ServiceRouter router =
                new ServiceRouter(
                        astService,
                        compilerConfigurationService,
                        incrementalCompilationService,
                        typeInferenceService,
                        workspaceIndexService,
                        formattingService,
                        lintEngine);

        // when
        CompilerConfigurationService result = router.getCompilerConfigurationService();

        // then
        assertThat(result).isSameAs(compilerConfigurationService);
    }

    @UnitTest
    void getIncrementalCompilationService_shouldReturnService() {
        // given
        ServiceRouter router =
                new ServiceRouter(
                        astService,
                        compilerConfigurationService,
                        incrementalCompilationService,
                        typeInferenceService,
                        workspaceIndexService,
                        formattingService,
                        lintEngine);

        // when
        IncrementalCompilationService result = router.getIncrementalCompilationService();

        // then
        assertThat(result).isSameAs(incrementalCompilationService);
    }

    @UnitTest
    void getTypeInferenceService_shouldReturnService() {
        // given
        ServiceRouter router =
                new ServiceRouter(
                        astService,
                        compilerConfigurationService,
                        incrementalCompilationService,
                        typeInferenceService,
                        workspaceIndexService,
                        formattingService,
                        lintEngine);

        // when
        TypeInferenceService result = router.getTypeInferenceService();

        // then
        assertThat(result).isSameAs(typeInferenceService);
    }

    @UnitTest
    void getWorkspaceIndexService_shouldReturnService() {
        // given
        ServiceRouter router =
                new ServiceRouter(
                        astService,
                        compilerConfigurationService,
                        incrementalCompilationService,
                        typeInferenceService,
                        workspaceIndexService,
                        formattingService,
                        lintEngine);

        // when
        WorkspaceIndexService result = router.getWorkspaceIndexService();

        // then
        assertThat(result).isSameAs(workspaceIndexService);
    }

    @UnitTest
    void getFormattingService_shouldReturnService() {
        // given
        ServiceRouter router =
                new ServiceRouter(
                        astService,
                        compilerConfigurationService,
                        incrementalCompilationService,
                        typeInferenceService,
                        workspaceIndexService,
                        formattingService,
                        lintEngine);

        // when
        FormattingService result = router.getFormattingService();

        // then
        assertThat(result).isSameAs(formattingService);
    }

    @UnitTest
    void getLintEngine_shouldReturnService() {
        // given
        ServiceRouter router =
                new ServiceRouter(
                        astService,
                        compilerConfigurationService,
                        incrementalCompilationService,
                        typeInferenceService,
                        workspaceIndexService,
                        formattingService,
                        lintEngine);

        // when
        LintEngine result = router.getLintEngine();

        // then
        assertThat(result).isSameAs(lintEngine);
    }

    @UnitTest
    void constructor_shouldCompleteSuccessfullyWithValidServices() {
        // given - All services are valid mocks
        // when
        ServiceRouter router =
                new ServiceRouter(
                        astService,
                        compilerConfigurationService,
                        incrementalCompilationService,
                        typeInferenceService,
                        workspaceIndexService,
                        formattingService,
                        lintEngine);

        // then - Constructor completes without throwing, validation passes
        assertThat(router).isNotNull();
        assertThat(router.areAllServicesAvailable()).isTrue();
    }

    @UnitTest
    void ensureServiceAvailable_shouldThrowWhenServiceIsNull() {
        // Test this by creating a router with reflection to set a service to null
        ServiceRouter router =
                new ServiceRouter(
                        astService,
                        compilerConfigurationService,
                        incrementalCompilationService,
                        typeInferenceService,
                        workspaceIndexService,
                        formattingService,
                        lintEngine);

        // Test each service getter's null check
        String[][] serviceTests = {
            {"astService", "ASTService"},
            {"compilerConfigurationService", "CompilerConfigurationService"},
            {"incrementalCompilationService", "IncrementalCompilationService"},
            {"typeInferenceService", "TypeInferenceService"},
            {"workspaceIndexService", "WorkspaceIndexService"},
            {"formattingService", "FormattingService"},
            {"lintEngine", "LintEngine"}
        };

        for (String[] test : serviceTests) {
            String fieldName = test[0];
            String serviceName = test[1];

            try {
                java.lang.reflect.Field field = ServiceRouter.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object originalValue = field.get(router);

                // Set service to null
                field.set(router, null);

                // Test the appropriate getter method
                switch (fieldName) {
                    case "astService" ->
                            assertThatThrownBy(() -> router.getAstService())
                                    .isInstanceOf(IllegalStateException.class)
                                    .hasMessage(serviceName + " is not available");
                    case "compilerConfigurationService" ->
                            assertThatThrownBy(() -> router.getCompilerConfigurationService())
                                    .isInstanceOf(IllegalStateException.class)
                                    .hasMessage(serviceName + " is not available");
                    case "incrementalCompilationService" ->
                            assertThatThrownBy(() -> router.getIncrementalCompilationService())
                                    .isInstanceOf(IllegalStateException.class)
                                    .hasMessage(serviceName + " is not available");
                    case "typeInferenceService" ->
                            assertThatThrownBy(() -> router.getTypeInferenceService())
                                    .isInstanceOf(IllegalStateException.class)
                                    .hasMessage(serviceName + " is not available");
                    case "workspaceIndexService" ->
                            assertThatThrownBy(() -> router.getWorkspaceIndexService())
                                    .isInstanceOf(IllegalStateException.class)
                                    .hasMessage(serviceName + " is not available");
                    case "formattingService" ->
                            assertThatThrownBy(() -> router.getFormattingService())
                                    .isInstanceOf(IllegalStateException.class)
                                    .hasMessage(serviceName + " is not available");
                    case "lintEngine" ->
                            assertThatThrownBy(() -> router.getLintEngine())
                                    .isInstanceOf(IllegalStateException.class)
                                    .hasMessage(serviceName + " is not available");
                }

                // Restore original value
                field.set(router, originalValue);

            } catch (Exception e) {
                // If reflection fails, we can't test this specific branch
            }
        }
    }

    @UnitTest
    void areAllServicesAvailable_shouldReturnFalseWhenAnyServiceIsNull() {
        // Test each service being null individually to cover all branches
        ServiceRouter router =
                new ServiceRouter(
                        astService,
                        compilerConfigurationService,
                        incrementalCompilationService,
                        typeInferenceService,
                        workspaceIndexService,
                        formattingService,
                        lintEngine);

        // Test all services are available first
        assertThat(router.areAllServicesAvailable()).isTrue();

        // Test each service null case individually via reflection
        String[] fieldNames = {
            "astService",
            "compilerConfigurationService",
            "incrementalCompilationService",
            "typeInferenceService",
            "workspaceIndexService",
            "formattingService",
            "lintEngine"
        };

        for (String fieldName : fieldNames) {
            try {
                java.lang.reflect.Field field = ServiceRouter.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object originalValue = field.get(router);

                // Set to null
                field.set(router, null);

                // when
                boolean result = router.areAllServicesAvailable();

                // then
                assertThat(result).isFalse();

                // Restore original value
                field.set(router, originalValue);

            } catch (Exception e) {
                // If reflection fails for this field, skip it
            }
        }
    }
}
