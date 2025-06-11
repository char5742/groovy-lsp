package com.groovy.lsp.shared.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.groovy.lsp.test.annotations.UnitTest;

/**
 * EventBusFactoryのテストクラス。
 */
class EventBusFactoryTest {

    @UnitTest
    void create_shouldCreateNewEventBusInstance() {
        // when
        EventBus eventBus1 = EventBusFactory.create();
        EventBus eventBus2 = EventBusFactory.create();

        // then
        assertThat(eventBus1).isNotNull();
        assertThat(eventBus2).isNotNull();
        assertThat(eventBus1).isNotSameAs(eventBus2);
    }

    @UnitTest
    void getInstance_shouldReturnSingletonInstance() {
        // when
        EventBus instance1 = EventBusFactory.getInstance();
        EventBus instance2 = EventBusFactory.getInstance();

        // then
        assertThat(instance1).isNotNull();
        assertThat(instance2).isNotNull();
        assertThat(instance1).isSameAs(instance2);
    }

    @UnitTest
    void getInstance_shouldReturnSameInstanceInMultithreadedEnvironment()
            throws InterruptedException {
        // given
        EventBus[] instances = new EventBus[10];
        Thread[] threads = new Thread[10];

        // when
        for (int i = 0; i < threads.length; i++) {
            int index = i;
            threads[i] =
                    new Thread(
                            () -> {
                                instances[index] = EventBusFactory.getInstance();
                            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // then
        EventBus firstInstance = instances[0];
        for (EventBus instance : instances) {
            assertThat(instance).isSameAs(firstInstance);
        }
    }
}
