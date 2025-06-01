/**
 * Shared components for Groovy LSP modular architecture.
 * Provides common infrastructure including event bus and DDD patterns.
 */
module com.groovy.lsp.shared {
    // Required dependencies
    // Note: org.jmolecules.ddd and org.jmolecules.event are automatic modules,
    // so we avoid using 'transitive' with them to suppress warnings
    requires org.jmolecules.ddd;
    requires org.jmolecules.event;
    requires transitive org.jspecify;
    requires com.google.common;
    requires org.slf4j;

    // Compile-only dependencies
    requires static org.apiguardian.api;

    // Exports
    exports com.groovy.lsp.shared.event;

    // Opens for reflection (if needed by Guava EventBus)
    opens com.groovy.lsp.shared.internal.event to
            com.google.common;
}
