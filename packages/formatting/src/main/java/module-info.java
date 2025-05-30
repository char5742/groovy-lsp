/**
 * Groovy LSP Formatting Module
 * 
 * Provides code formatting capabilities for Groovy source code
 * based on Google Java Format with Groovy-specific extensions.
 */
module com.groovy.lsp.formatting {
    requires com.groovy.lsp.core;
    requires com.groovy.lsp.protocol;
    
    requires com.google.googlejavaformat;
    requires org.apache.groovy;
    requires org.slf4j;
    
    exports com.groovy.lsp.formatting;
    exports com.groovy.lsp.formatting.service;
    exports com.groovy.lsp.formatting.options;
}