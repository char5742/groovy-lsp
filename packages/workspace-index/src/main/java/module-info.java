/**
 * Workspace indexing module for Groovy LSP.
 * Handles workspace analysis, dependency resolution, and symbol indexing.
 */
module com.groovy.lsp.workspace {
    requires com.groovy.lsp.core;
    requires com.groovy.lsp.protocol;
    
    requires org.gradle.tooling.api;
    requires org.lmdbjava;
    requires org.slf4j;
    requires java.nio.charset;
    
    exports com.groovy.lsp.workspace;
    exports com.groovy.lsp.workspace.index;
    exports com.groovy.lsp.workspace.dependency;
}