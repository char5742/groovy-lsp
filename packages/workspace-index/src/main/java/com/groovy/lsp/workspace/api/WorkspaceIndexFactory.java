package com.groovy.lsp.workspace.api;

import com.groovy.lsp.workspace.internal.impl.WorkspaceIndexerImpl;
import org.jmolecules.ddd.annotation.Factory;
import java.nio.file.Path;

/**
 * Factory for creating WorkspaceIndexService instances.
 * This is the main entry point for external modules to access workspace indexing functionality.
 * 
 * Follows the Factory pattern to maintain proper module boundaries and
 * encapsulate the creation of workspace indexing services.
 */
@Factory
public class WorkspaceIndexFactory {
    
    /**
     * Creates a new WorkspaceIndexService for the given workspace root.
     * 
     * @param workspaceRoot the root path of the workspace to index
     * @return a new WorkspaceIndexService instance
     * @throws IllegalArgumentException if workspaceRoot is null or doesn't exist
     */
    public static WorkspaceIndexService createWorkspaceIndexService(Path workspaceRoot) {
        if (workspaceRoot == null) {
            throw new IllegalArgumentException("Workspace root cannot be null");
        }
        if (!workspaceRoot.toFile().exists()) {
            throw new IllegalArgumentException("Workspace root does not exist: " + workspaceRoot);
        }
        
        return new WorkspaceIndexerImpl(workspaceRoot);
    }
}