/**
 * JDT Adapter module for Groovy Language Server.
 * This module bridges Groovy AST and Eclipse JDT for enhanced IDE features.
 */
module com.groovy.lsp.jdt.adapter {
    requires transitive com.groovy.lsp.protocol;
    requires transitive com.groovy.lsp.core;
    
    requires org.eclipse.jdt.core;
    requires org.eclipse.core.runtime;
    requires org.eclipse.core.resources;
    requires org.eclipse.text;
    
    requires groovy;
    requires groovy.json;
    requires org.slf4j;
    
    exports com.groovy.lsp.jdt.adapter;
    exports com.groovy.lsp.jdt.adapter.ast;
    exports com.groovy.lsp.jdt.adapter.type;
}