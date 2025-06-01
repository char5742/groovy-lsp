package com.groovy.lsp.shared.event;

import com.groovy.lsp.shared.internal.event.GuavaEventBus;
import org.apiguardian.api.API;
import org.jmolecules.ddd.annotation.Factory;
import org.jspecify.annotations.Nullable;

/**
 * Factory for creating EventBus instances.
 * This factory hides the implementation details and allows for future changes
 * without affecting client code.
 *
 * Follows the Factory pattern to ensure proper instantiation and configuration
 * of the event bus infrastructure used for inter-module communication.
 */
@API(status = API.Status.STABLE)
@Factory
public final class EventBusFactory {

    private static volatile @Nullable EventBus instance;

    private EventBusFactory() {
        // Prevent instantiation
    }

    /**
     * Creates a new EventBus instance.
     *
     * @return a new EventBus instance
     */
    public static EventBus create() {
        return new GuavaEventBus();
    }

    /**
     * Gets the singleton EventBus instance for the application.
     * This method is thread-safe and uses double-checked locking.
     *
     * @return the singleton EventBus instance
     */
    public static EventBus getInstance() {
        if (instance == null) {
            synchronized (EventBusFactory.class) {
                if (instance == null) {
                    instance = create();
                }
            }
        }
        return instance;
    }
}
