package com.groovy.lsp.shared.event;

import java.time.Instant;
import java.util.UUID;
import org.apiguardian.api.API;

/**
 * Base class for all domain events in the system.
 * Domain events represent something that has happened in the domain.
 *
 * This class is annotated with jMolecules @DomainEvent to make the
 * architectural pattern explicit in the code.
 */
@API(status = API.Status.STABLE)
@org.jmolecules.event.annotation.DomainEvent
public abstract class DomainEvent {

    private final String eventId;
    private final Instant occurredOn;
    private final String aggregateId;

    /**
     * Creates a new domain event.
     *
     * @param aggregateId the ID of the aggregate that this event is related to
     */
    protected DomainEvent(String aggregateId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = Instant.now();
        this.aggregateId = aggregateId;
    }

    /**
     * Returns unique identifier for this event instance.
     *
     * @return unique identifier for this event instance
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Returns the timestamp when this event occurred.
     *
     * @return the timestamp when this event occurred
     */
    public Instant getOccurredOn() {
        return occurredOn;
    }

    /**
     * Returns the ID of the aggregate that this event is related to.
     *
     * @return the ID of the aggregate that this event is related to
     */
    public String getAggregateId() {
        return aggregateId;
    }

    /**
     * Returns the type name of this event.
     *
     * @return the type name of this event
     */
    public String getEventType() {
        return this.getClass().getSimpleName();
    }
}
