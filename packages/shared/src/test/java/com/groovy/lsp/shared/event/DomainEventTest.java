package com.groovy.lsp.shared.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.groovy.lsp.test.annotations.UnitTest;
import java.time.Instant;
import java.util.UUID;

/**
 * DomainEventのテストクラス。
 */
class DomainEventTest {

    @UnitTest
    void constructor_shouldInitializeCorrectly() {
        // given
        String aggregateId = "test-aggregate-123";
        Instant beforeCreation = Instant.now();

        // when
        TestDomainEvent event = new TestDomainEvent(aggregateId);

        // then
        assertThat(event.getAggregateId()).isEqualTo(aggregateId);
        assertThat(event.getEventId()).isNotNull();
        assertThat(UUID.fromString(event.getEventId())).isNotNull(); // Valid UUID
        assertThat(event.getOccurredOn()).isNotNull();
        assertThat(event.getOccurredOn()).isAfterOrEqualTo(beforeCreation);
        assertThat(event.getOccurredOn()).isBeforeOrEqualTo(Instant.now());
    }

    @UnitTest
    void getEventType_shouldReturnClassName() {
        // given
        TestDomainEvent event = new TestDomainEvent("aggregate-1");

        // when
        String eventType = event.getEventType();

        // then
        assertThat(eventType).isEqualTo("TestDomainEvent");
    }

    @UnitTest
    void multipleEvents_shouldHaveDifferentIds() {
        // given
        String aggregateId = "aggregate-1";

        // when
        TestDomainEvent event1 = new TestDomainEvent(aggregateId);
        TestDomainEvent event2 = new TestDomainEvent(aggregateId);

        // then
        assertThat(event1.getEventId()).isNotEqualTo(event2.getEventId());
        assertThat(event1.getOccurredOn()).isBeforeOrEqualTo(event2.getOccurredOn());
    }

    @UnitTest
    void shouldCreateEventsWithDifferentAggregateIds() {
        // when
        TestDomainEvent event1 = new TestDomainEvent("aggregate-1");
        TestDomainEvent event2 = new TestDomainEvent("aggregate-2");

        // then
        assertThat(event1.getAggregateId()).isEqualTo("aggregate-1");
        assertThat(event2.getAggregateId()).isEqualTo("aggregate-2");
        assertThat(event1.getEventId()).isNotEqualTo(event2.getEventId());
    }

    /**
     * テスト用のDomainEvent実装。
     */
    private static class TestDomainEvent extends DomainEvent {
        public TestDomainEvent(String aggregateId) {
            super(aggregateId);
        }
    }
}
