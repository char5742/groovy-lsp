package com.groovy.lsp.shared.internal.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.groovy.lsp.shared.event.DomainEvent;
import com.groovy.lsp.shared.event.EventHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * GuavaEventBusのテストクラス。
 */
class GuavaEventBusTest {

    private GuavaEventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = new GuavaEventBus();
    }

    @Test
    void subscribe_shouldRegisterHandlerAndReceiveEvents() throws InterruptedException {
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
        TestEvent testEvent = new TestEvent("aggregate-1", "test-data");
        eventBus.publish(testEvent);

        // then
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(receivedEvents).hasSize(1);
        assertThat(receivedEvents.get(0)).isEqualTo(testEvent);
    }

    @Test
    void subscribe_shouldRegisterMultipleHandlers() throws InterruptedException {
        // given
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger counter = new AtomicInteger();
        EventHandler<TestEvent> handler1 =
                event -> {
                    counter.incrementAndGet();
                    latch.countDown();
                };
        EventHandler<TestEvent> handler2 =
                event -> {
                    counter.incrementAndGet();
                    latch.countDown();
                };

        // when
        eventBus.subscribe(TestEvent.class, handler1);
        eventBus.subscribe(TestEvent.class, handler2);
        eventBus.publish(new TestEvent("aggregate-1", "test"));

        // then
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(counter.get()).isEqualTo(2);
    }

    @Test
    void unsubscribe_shouldRemoveHandler() throws InterruptedException {
        // given
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger counter = new AtomicInteger();
        EventHandler<TestEvent> handler =
                event -> {
                    counter.incrementAndGet();
                    latch.countDown();
                };

        eventBus.subscribe(TestEvent.class, handler);
        eventBus.unsubscribe(TestEvent.class, handler);

        // when
        eventBus.publish(new TestEvent("aggregate-1", "test"));

        // then
        assertThat(latch.await(1, TimeUnit.SECONDS)).isFalse();
        assertThat(counter.get()).isEqualTo(0);
    }

    @Test
    void unsubscribe_shouldNotErrorWhenRemovingUnregisteredHandler() {
        // given
        EventHandler<TestEvent> handler = event -> {};

        // when/then - should not throw
        eventBus.unsubscribe(TestEvent.class, handler);
    }

    @Test
    void publish_shouldDeliverDifferentEventTypesToDifferentHandlers() throws InterruptedException {
        // given
        CountDownLatch testEventLatch = new CountDownLatch(1);
        CountDownLatch otherEventLatch = new CountDownLatch(1);
        List<TestEvent> testEvents = new ArrayList<>();
        List<OtherTestEvent> otherEvents = new ArrayList<>();

        EventHandler<TestEvent> testHandler =
                event -> {
                    testEvents.add(event);
                    testEventLatch.countDown();
                };
        EventHandler<OtherTestEvent> otherHandler =
                event -> {
                    otherEvents.add(event);
                    otherEventLatch.countDown();
                };

        // when
        eventBus.subscribe(TestEvent.class, testHandler);
        eventBus.subscribe(OtherTestEvent.class, otherHandler);

        eventBus.publish(new TestEvent("aggregate-1", "test"));
        eventBus.publish(new OtherTestEvent("aggregate-2", 42));

        // then
        assertThat(testEventLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(otherEventLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(testEvents).hasSize(1);
        assertThat(otherEvents).hasSize(1);
    }

    @Test
    void publish_shouldNotDeliverSubclassEventsToParentHandlers() throws InterruptedException {
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
        eventBus.publish(new ExtendedTestEvent("aggregate-1", "test", "extended"));

        // then
        assertThat(latch.await(1, TimeUnit.SECONDS)).isFalse();
        assertThat(receivedEvents).isEmpty();
    }

    @Test
    void publish_shouldContinueWithOtherHandlersWhenOneThrows() throws InterruptedException {
        // given
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger counter = new AtomicInteger();

        EventHandler<TestEvent> errorHandler =
                event -> {
                    throw new RuntimeException("Handler error");
                };
        EventHandler<TestEvent> normalHandler =
                event -> {
                    counter.incrementAndGet();
                    latch.countDown();
                };

        // when
        eventBus.subscribe(TestEvent.class, errorHandler);
        eventBus.subscribe(TestEvent.class, normalHandler);
        eventBus.publish(new TestEvent("aggregate-1", "test"));

        // then
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    void publish_shouldThrowErrorForNullEvent() {
        assertThatThrownBy(() -> eventBus.publish(null)).isInstanceOf(NullPointerException.class);
    }

    /**
     * テスト用のイベント。
     */
    private static class TestEvent extends DomainEvent {
        private final String data;

        public TestEvent(String aggregateId, String data) {
            super(aggregateId);
            this.data = data;
        }

        public String getData() {
            return data;
        }
    }

    /**
     * 別のテスト用イベント。
     */
    private static class OtherTestEvent extends DomainEvent {
        private final int value;

        public OtherTestEvent(String aggregateId, int value) {
            super(aggregateId);
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * TestEventを継承したイベント。
     */
    private static class ExtendedTestEvent extends TestEvent {
        private final String additionalData;

        public ExtendedTestEvent(String aggregateId, String data, String additionalData) {
            super(aggregateId, data);
            this.additionalData = additionalData;
        }

        public String getAdditionalData() {
            return additionalData;
        }
    }
}
