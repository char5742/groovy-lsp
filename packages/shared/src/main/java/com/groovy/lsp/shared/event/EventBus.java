package com.groovy.lsp.shared.event;

import org.apiguardian.api.API;
import org.jmolecules.ddd.annotation.Service;

/**
 * Event bus for publishing and subscribing to domain events in a modular monolith architecture.
 * This enables loose coupling between modules through event-driven communication.
 *
 * This is a domain service that enables event-driven communication
 * across module boundaries.
 */
@API(status = API.Status.STABLE)
@Service
public interface EventBus {

    /**
     * Publishes a domain event to all registered subscribers.
     *
     * @param event the event to publish
     */
    void publish(DomainEvent event);

    /**
     * Subscribes to events of a specific type.
     *
     * @param <T> the type of event
     * @param eventType the class of the event type to subscribe to
     * @param handler the handler to process events
     */
    <T extends DomainEvent> void subscribe(Class<T> eventType, EventHandler<T> handler);

    /**
     * Unsubscribes a handler from events of a specific type.
     *
     * @param <T> the type of event
     * @param eventType the class of the event type to unsubscribe from
     * @param handler the handler to remove
     */
    <T extends DomainEvent> void unsubscribe(Class<T> eventType, EventHandler<T> handler);
}
