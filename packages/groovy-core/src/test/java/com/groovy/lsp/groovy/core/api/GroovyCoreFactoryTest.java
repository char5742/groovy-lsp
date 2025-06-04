package com.groovy.lsp.groovy.core.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * GroovyCoreFactoryのテストクラス。
 */
class GroovyCoreFactoryTest {

    @Test
    void getInstance_shouldReturnSingletonInstance() {
        // when
        GroovyCoreFactory instance1 = GroovyCoreFactory.getInstance();
        GroovyCoreFactory instance2 = GroovyCoreFactory.getInstance();

        // then
        assertThat(instance1).isNotNull();
        assertThat(instance2).isNotNull();
        assertThat(instance1).isSameAs(instance2);
    }

    @Test
    void createASTService_shouldCreateNewInstance() {
        // given
        GroovyCoreFactory factory = GroovyCoreFactory.getInstance();

        // when
        ASTService service1 = factory.createASTService();
        ASTService service2 = factory.createASTService();

        // then
        assertThat(service1).isNotNull();
        assertThat(service2).isNotNull();
        assertThat(service1).isNotSameAs(service2);
    }

    @Test
    void createCompilerConfigurationService_shouldCreateNewInstance() {
        // given
        GroovyCoreFactory factory = GroovyCoreFactory.getInstance();

        // when
        CompilerConfigurationService service1 = factory.createCompilerConfigurationService();
        CompilerConfigurationService service2 = factory.createCompilerConfigurationService();

        // then
        assertThat(service1).isNotNull();
        assertThat(service2).isNotNull();
        assertThat(service1).isNotSameAs(service2);
    }

    @Test
    void createTypeInferenceService_shouldCreateNewInstanceWithASTService() {
        // given
        GroovyCoreFactory factory = GroovyCoreFactory.getInstance();
        ASTService astService = factory.createASTService();

        // when
        TypeInferenceService service1 = factory.createTypeInferenceService(astService);
        TypeInferenceService service2 = factory.createTypeInferenceService(astService);

        // then
        assertThat(service1).isNotNull();
        assertThat(service2).isNotNull();
        assertThat(service1).isNotSameAs(service2);
    }

    @Test
    void createIncrementalCompilationService_shouldCreateNewInstance() {
        // given
        GroovyCoreFactory factory = GroovyCoreFactory.getInstance();

        // when
        IncrementalCompilationService service1 = factory.createIncrementalCompilationService();
        IncrementalCompilationService service2 = factory.createIncrementalCompilationService();

        // then
        assertThat(service1).isNotNull();
        assertThat(service2).isNotNull();
        assertThat(service1).isNotSameAs(service2);
    }

    @Test
    void getASTService_shouldReturnSharedInstance() {
        // given
        GroovyCoreFactory factory = GroovyCoreFactory.getInstance();

        // when
        ASTService service1 = factory.getASTService();
        ASTService service2 = factory.getASTService();

        // then
        assertThat(service1).isNotNull();
        assertThat(service2).isNotNull();
        assertThat(service1).isSameAs(service2);
    }

    @Test
    void getCompilerConfigurationService_shouldReturnSharedInstance() {
        // given
        GroovyCoreFactory factory = GroovyCoreFactory.getInstance();

        // when
        CompilerConfigurationService service1 = factory.getCompilerConfigurationService();
        CompilerConfigurationService service2 = factory.getCompilerConfigurationService();

        // then
        assertThat(service1).isNotNull();
        assertThat(service2).isNotNull();
        assertThat(service1).isSameAs(service2);
    }

    @Test
    void getTypeInferenceService_shouldReturnSharedInstance() {
        // given
        GroovyCoreFactory factory = GroovyCoreFactory.getInstance();

        // when
        TypeInferenceService service1 = factory.getTypeInferenceService();
        TypeInferenceService service2 = factory.getTypeInferenceService();

        // then
        assertThat(service1).isNotNull();
        assertThat(service2).isNotNull();
        assertThat(service1).isSameAs(service2);
    }

    @Test
    void getIncrementalCompilationService_shouldReturnSharedInstance() {
        // given
        GroovyCoreFactory factory = GroovyCoreFactory.getInstance();

        // when
        IncrementalCompilationService service1 = factory.getIncrementalCompilationService();
        IncrementalCompilationService service2 = factory.getIncrementalCompilationService();

        // then
        assertThat(service1).isNotNull();
        assertThat(service2).isNotNull();
        assertThat(service1).isSameAs(service2);
    }

    @Test
    void getInstance_shouldReturnSameInstanceInMultithreadedEnvironment()
            throws InterruptedException {
        // given
        GroovyCoreFactory[] instances = new GroovyCoreFactory[10];
        Thread[] threads = new Thread[10];

        // when
        for (int i = 0; i < threads.length; i++) {
            int index = i;
            threads[i] =
                    new Thread(
                            () -> {
                                instances[index] = GroovyCoreFactory.getInstance();
                            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // then
        GroovyCoreFactory firstInstance = instances[0];
        for (GroovyCoreFactory instance : instances) {
            assertThat(instance).isSameAs(firstInstance);
        }
    }

    @Test
    void factoryMethods_shouldReturnCorrectImplementationClasses() {
        // given
        GroovyCoreFactory factory = GroovyCoreFactory.getInstance();

        // when/then
        assertThat(factory.createASTService()).isInstanceOf(ASTService.class);
        assertThat(factory.createCompilerConfigurationService())
                .isInstanceOf(CompilerConfigurationService.class);
        assertThat(factory.createTypeInferenceService(factory.createASTService()))
                .isInstanceOf(TypeInferenceService.class);
        assertThat(factory.createIncrementalCompilationService())
                .isInstanceOf(IncrementalCompilationService.class);
    }
}
