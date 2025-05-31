/**
 * Shared components for Groovy LSP modular architecture.
 * Provides common infrastructure including event bus and DDD patterns.
 */
module com.groovy.lsp.shared {
    // Required dependencies
    requires transitive org.jmolecules.ddd;
    requires transitive org.jmolecules.event;
    requires transitive org.jspecify;
    requires com.google.common;
    requires org.slf4j;
    
    // Compile-only dependencies
    requires static org.apiguardian.api;
    
    // Exports
    exports com.groovy.lsp.shared.event;
    
    // Opens for reflection (if needed by Guava EventBus)
    opens com.groovy.lsp.shared.internal.event to com.google.common;
}