package com.groovy.lsp.shared.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.groovy.lsp.test.annotations.UnitTest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;

/**
 * EventBusインターフェースのテストクラス。
 * EventBusFactoryを使用して実装をテストする。
 */
class EventBusTest {

    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = EventBusFactory.create();
    }

    @UnitTest
    void interfaceMethods_shouldWorkCorrectly() throws InterruptedException {
        // given
        CountDownLatch latch = new CountDownLatch(1);
        List<TestEvent> receivedEvents = new ArrayList<>();
        EventHandler<TestEvent> handler =
                event -> {
                    receivedEvents.add(event);
                    latch.countDown();
                };

        // when
        eventBus.subscribe(TestEvent.class, handler);
        TestEvent event = new TestEvent("aggregate-1");
        eventBus.publish(event);

        // then
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedEvents).hasSize(1);
        assertThat(receivedEvents.get(0).getAggregateId()).isEqualTo("aggregate-1");
    }

    @UnitTest
    void handler_shouldBeDefinableWithLambda() throws InterruptedException {
        // given
        CountDownLatch latch = new CountDownLatch(1);
        List<String> aggregateIds = new ArrayList<>();

        // when
        eventBus.subscribe(
                TestEvent.class,
                event -> {
                    aggregateIds.add(event.getAggregateId());
                    latch.countDown();
                });
        eventBus.publish(new TestEvent("test-id"));

        // then
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(aggregateIds).containsExactly("test-id");
    }

    @UnitTest
    void handler_shouldBeDefinableWithMethodReference() throws InterruptedException {
        // given
        CountDownLatch latch = new CountDownLatch(1);
        TestEventCollector collector = new TestEventCollector(latch);

        // when
        eventBus.subscribe(TestEvent.class, collector::collect);
        eventBus.publish(new TestEvent("method-ref-test"));

        // then
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(collector.getEvents()).hasSize(1);
        assertThat(collector.getEvents().get(0).getAggregateId()).isEqualTo("method-ref-test");
    }

    /**
     * テスト用のイベント。
     */
    private static class TestEvent extends DomainEvent {
        public TestEvent(String aggregateId) {
            super(aggregateId);
        }
    }

    /**
     * テスト用のイベントコレクター。
     */
    private static class TestEventCollector {
        private final List<TestEvent> events = new ArrayList<>();
        private final CountDownLatch latch;

        public TestEventCollector(CountDownLatch latch) {
            this.latch = latch;
        }

        public void collect(TestEvent event) {
            events.add(event);
            latch.countDown();
        }

        public List<TestEvent> getEvents() {
            return new ArrayList<>(events);
        }
    }
}
