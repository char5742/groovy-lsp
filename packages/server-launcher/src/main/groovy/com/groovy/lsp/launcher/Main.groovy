package com.groovy.lsp.launcher

import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class)
    
    static void main(String[] args) {
        logger.info("Starting Groovy Language Server...")
        
        // Create a simple language server implementation for now
        LanguageServer server = new GroovyLanguageServer()
        
        // Create and start the launcher
        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(
            server, 
            System.in, 
            System.out
        )
        
        LanguageClient client = launcher.getRemoteProxy()
        server.connect(client)
        
        logger.info("Groovy Language Server started")
        launcher.startListening().get()
    }
}