/**
 * Groovy Language Server Launcher Module
 * 
 * This module provides the main entry point for the Groovy Language Server.
 * It sets up the LSP4J launcher and initializes the server with dependency injection.
 */
module com.groovy.lsp.server.launcher {
    // Required modules
    requires com.groovy.lsp.protocol;
    requires com.groovy.lsp.groovy.core;
    requires com.groovy.lsp.document.manager;
    requires com.groovy.lsp.completion.engine;
    requires com.groovy.lsp.diagnostic.engine;
    requires com.groovy.lsp.symbol.table;
    requires com.groovy.lsp.language.server;
    
    // LSP4J modules
    requires org.eclipse.lsp4j;
    requires org.eclipse.lsp4j.jsonrpc;
    
    // Guice for DI
    requires com.google.inject;
    
    // Logging
    requires org.slf4j;
    requires ch.qos.logback.classic;
    
    // Java modules
    requires java.logging;
    
    // Export launcher package
    exports com.groovy.lsp.server.launcher;
    
    // Open for Guice reflection
    opens com.groovy.lsp.server.launcher to com.google.inject;
}