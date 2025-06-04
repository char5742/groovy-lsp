package com.groovy.lsp.shared.internal.event;

import com.google.common.eventbus.AsyncEventBus;
import com.groovy.lsp.shared.event.DomainEvent;
import com.groovy.lsp.shared.event.EventBus;
import com.groovy.lsp.shared.event.EventHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EventBus implementation using Google Guava's EventBus.
 * This implementation is thread-safe and supports asynchronous event processing.
 */
public class GuavaEventBus implements EventBus {

    private static final Logger logger = LoggerFactory.getLogger(GuavaEventBus.class);

    private final AsyncEventBus eventBus;
    private final Map<EventHandler<?>, EventHandlerAdapter<?>> adapters = new ConcurrentHashMap<>();

    public GuavaEventBus() {
        this.eventBus =
                new AsyncEventBus(
                        Executors.newCachedThreadPool(
                                r -> {
                                    Thread thread = new Thread(r, "EventBus-Handler");
                                    thread.setDaemon(true);
                                    return thread;
                                }),
                        (exception, context) -> {
                            logger.error(
                                    "Error handling event: {} in subscriber: {}",
                                    context.getEvent(),
                                    context.getSubscriber(),
                                    exception);
                        });
    }

    @Override
    public void publish(DomainEvent event) {
        logger.debug("Publishing event: {} with ID: {}", event.getEventType(), event.getEventId());
        eventBus.post(event);
    }

    @Override
    public <T extends DomainEvent> void subscribe(Class<T> eventType, EventHandler<T> handler) {
        EventHandlerAdapter<T> adapter = new EventHandlerAdapter<>(eventType, handler);
        adapters.put(handler, adapter);
        eventBus.register(adapter);
        logger.debug("Subscribed handler {} to event type: {}", handler, eventType.getSimpleName());
    }

    @Override
    public <T extends DomainEvent> void unsubscribe(Class<T> eventType, EventHandler<T> handler) {
        EventHandlerAdapter<?> adapter = adapters.remove(handler);
        if (adapter != null) {
            eventBus.unregister(adapter);
            logger.debug(
                    "Unsubscribed handler {} from event type: {}",
                    handler,
                    eventType.getSimpleName());
        }
    }

    /**
     * Adapter class to bridge between our EventHandler interface and Guava's @Subscribe annotation.
     */
    private static class EventHandlerAdapter<T extends DomainEvent> {
        private final Class<T> eventType;
        private final EventHandler<T> handler;

        EventHandlerAdapter(Class<T> eventType, EventHandler<T> handler) {
            this.eventType = eventType;
            this.handler = handler;
        }

        @com.google.common.eventbus.Subscribe
        public void handleEvent(DomainEvent event) {
            if (eventType.equals(event.getClass())) {
                handler.handle(eventType.cast(event));
            }
        }
    }
}
