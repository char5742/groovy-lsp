/**
 * Groovy Core module for LSP.
 * Provides AST services, compiler configuration, and type inference.
 */
module com.groovy.lsp.groovy.core {
    // Required dependencies
    requires transitive com.groovy.lsp.shared;
    requires org.apache.groovy;
    requires org.apache.groovy.json;
    requires org.apache.groovy.xml;
    requires org.apache.groovy.templates;
    requires org.slf4j;

    // Compile-only dependencies
    requires static org.apiguardian.api;

    // Exports public API
    exports com.groovy.lsp.groovy.core.api;
}
