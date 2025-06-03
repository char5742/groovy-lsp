package com.groovy.lsp.server.launcher.di;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.groovy.lsp.codenarc.LintEngine;
import com.groovy.lsp.formatting.service.FormattingService;
import com.groovy.lsp.groovy.core.api.ASTService;
import com.groovy.lsp.groovy.core.api.CompilerConfigurationService;
import com.groovy.lsp.groovy.core.api.TypeInferenceService;
import com.groovy.lsp.workspace.api.WorkspaceIndexService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for ServiceRouter class.
 */
class ServiceRouterTest {

    private ASTService astService;
    private CompilerConfigurationService compilerConfigurationService;
    private TypeInferenceService typeInferenceService;
    private WorkspaceIndexService workspaceIndexService;
    private FormattingService formattingService;
    private LintEngine lintEngine;

    @BeforeEach
    void setUp() {
        astService = mock(ASTService.class);
        compilerConfigurationService = mock(CompilerConfigurationService.class);
        typeInferenceService = mock(TypeInferenceService.class);
        workspaceIndexService = mock(WorkspaceIndexService.class);
        formattingService = mock(FormattingService.class);
        lintEngine = mock(LintEngine.class);
    }

    @Test
    void constructor_shouldInitializeWithAllServices() {
        // when
        ServiceRouter router =
                new ServiceRouter(
                        astService,
                        compilerConfigurationService,
                        typeInferenceService,
                        workspaceIndexService,
                        formattingService,
                        lintEngine);

        // then
        assertThat(router.getAstService()).isSameAs(astService);
        assertThat(router.getCompilerConfigurationService()).isSameAs(compilerConfigurationService);
        assertThat(router.getTypeInferenceService()).isSameAs(typeInferenceService);
        assertThat(router.getWorkspaceIndexService()).isSameAs(workspaceIndexService);
        assertThat(router.getFormattingService()).isSameAs(formattingService);
        assertThat(router.getLintEngine()).isSameAs(lintEngine);
    }

    @Test
    void constructor_shouldThrowExceptionForNullAstService() {
        // when/then
        assertThatThrownBy(
                        () ->
                                new ServiceRouter(
                                        null,
                                        compilerConfigurationService,
                                        typeInferenceService,
                                        workspaceIndexService,
                                        formattingService,
                                        lintEngine))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("ASTService must not be null");
    }

    @Test
    void constructor_shouldThrowExceptionForNullCompilerConfigurationService() {
        // when/then
        assertThatThrownBy(
                        () ->
                                new ServiceRouter(
                                        astService,
                                        null,
                                        typeInferenceService,
                                        workspaceIndexService,
                                        formattingService,
                                        lintEngine))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("CompilerConfigurationService must not be null");
    }

    @Test
    void constructor_shouldThrowExceptionForNullTypeInferenceService() {
        // when/then
        assertThatThrownBy(
                        () ->
                                new ServiceRouter(
                                        astService,
                                        compilerConfigurationService,
                                        null,
                                        workspaceIndexService,
                                        formattingService,
                                        lintEngine))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("TypeInferenceService must not be null");
    }

    @Test
    void constructor_shouldThrowExceptionForNullWorkspaceIndexService() {
        // when/then
        assertThatThrownBy(
                        () ->
                                new ServiceRouter(
                                        astService,
                                        compilerConfigurationService,
                                        typeInferenceService,
                                        null,
                                        formattingService,
                                        lintEngine))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("WorkspaceIndexService must not be null");
    }

    @Test
    void constructor_shouldThrowExceptionForNullFormattingService() {
        // when/then
        assertThatThrownBy(
                        () ->
                                new ServiceRouter(
                                        astService,
                                        compilerConfigurationService,
                                        typeInferenceService,
                                        workspaceIndexService,
                                        null,
                                        lintEngine))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("FormattingService must not be null");
    }

    @Test
    void constructor_shouldThrowExceptionForNullLintEngine() {
        // when/then
        assertThatThrownBy(
                        () ->
                                new ServiceRouter(
                                        astService,
                                        compilerConfigurationService,
                                        typeInferenceService,
                                        workspaceIndexService,
                                        formattingService,
                                        null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("LintEngine must not be null");
    }

    @Test
    void areAllServicesAvailable_shouldReturnTrueWhenAllServicesAvailable() {
        // given
        ServiceRouter router =
                new ServiceRouter(
                        astService,
                        compilerConfigurationService,
                        typeInferenceService,
                        workspaceIndexService,
                        formattingService,
                        lintEngine);

        // when
        boolean result = router.areAllServicesAvailable();

        // then
        assertThat(result).isTrue();
    }

    @Test
    void getAstService_shouldReturnService() {
        // given
        ServiceRouter router =
                new ServiceRouter(
                        astService,
                        compilerConfigurationService,
                        typeInferenceService,
                        workspaceIndexService,
                        formattingService,
                        lintEngine);

        // when
        ASTService result = router.getAstService();

        // then
        assertThat(result).isSameAs(astService);
    }

    @Test
    void getCompilerConfigurationService_shouldReturnService() {
        // given
        ServiceRouter router =
                new ServiceRouter(
                        astService,
                        compilerConfigurationService,
                        typeInferenceService,
                        workspaceIndexService,
                        formattingService,
                        lintEngine);

        // when
        CompilerConfigurationService result = router.getCompilerConfigurationService();

        // then
        assertThat(result).isSameAs(compilerConfigurationService);
    }

    @Test
    void getTypeInferenceService_shouldReturnService() {
        // given
        ServiceRouter router =
                new ServiceRouter(
                        astService,
                        compilerConfigurationService,
                        typeInferenceService,
                        workspaceIndexService,
                        formattingService,
                        lintEngine);

        // when
        TypeInferenceService result = router.getTypeInferenceService();

        // then
        assertThat(result).isSameAs(typeInferenceService);
    }

    @Test
    void getWorkspaceIndexService_shouldReturnService() {
        // given
        ServiceRouter router =
                new ServiceRouter(
                        astService,
                        compilerConfigurationService,
                        typeInferenceService,
                        workspaceIndexService,
                        formattingService,
                        lintEngine);

        // when
        WorkspaceIndexService result = router.getWorkspaceIndexService();

        // then
        assertThat(result).isSameAs(workspaceIndexService);
    }

    @Test
    void getFormattingService_shouldReturnService() {
        // given
        ServiceRouter router =
                new ServiceRouter(
                        astService,
                        compilerConfigurationService,
                        typeInferenceService,
                        workspaceIndexService,
                        formattingService,
                        lintEngine);

        // when
        FormattingService result = router.getFormattingService();

        // then
        assertThat(result).isSameAs(formattingService);
    }

    @Test
    void getLintEngine_shouldReturnService() {
        // given
        ServiceRouter router =
                new ServiceRouter(
                        astService,
                        compilerConfigurationService,
                        typeInferenceService,
                        workspaceIndexService,
                        formattingService,
                        lintEngine);

        // when
        LintEngine result = router.getLintEngine();

        // then
        assertThat(result).isSameAs(lintEngine);
    }
}
