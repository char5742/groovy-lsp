package com.groovy.lsp.shared.event;

import org.apiguardian.api.API;

/**
 * Handler interface for processing domain events.
 * 
 * @param <T> the type of event this handler can process
 */
@API(status = API.Status.STABLE)
@FunctionalInterface
public interface EventHandler<T extends DomainEvent> {
    
    /**
     * Handles the given event.
     * 
     * @param event the event to handle
     */
    void handle(T event);
}