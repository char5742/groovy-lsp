package com.groovy.lsp.workspace.api.events;

import com.groovy.lsp.shared.event.DomainEvent;
import java.nio.file.Path;

/**
 * Event emitted when the workspace indexing is completed.
 * This event is published after the initial workspace scan and index building.
 */
public class WorkspaceIndexedEvent extends DomainEvent {
    
    private final Path workspacePath;
    private final int totalFiles;
    private final int totalSymbols;
    private final long indexingDurationMs;
    
    /**
     * Creates a new WorkspaceIndexedEvent.
     * 
     * @param workspacePath the root path of the indexed workspace
     * @param totalFiles the total number of files indexed
     * @param totalSymbols the total number of symbols found
     * @param indexingDurationMs the time taken to index in milliseconds
     */
    public WorkspaceIndexedEvent(Path workspacePath, int totalFiles, int totalSymbols, long indexingDurationMs) {
        super(workspacePath.toString());
        this.workspacePath = workspacePath;
        this.totalFiles = totalFiles;
        this.totalSymbols = totalSymbols;
        this.indexingDurationMs = indexingDurationMs;
    }
    
    /**
     * Gets the workspace path that was indexed.
     * 
     * @return the workspace path
     */
    public Path getWorkspacePath() {
        return workspacePath;
    }
    
    /**
     * Gets the total number of files indexed.
     * 
     * @return the number of files
     */
    public int getTotalFiles() {
        return totalFiles;
    }
    
    /**
     * Gets the total number of symbols found.
     * 
     * @return the number of symbols
     */
    public int getTotalSymbols() {
        return totalSymbols;
    }
    
    /**
     * Gets the duration of the indexing operation.
     * 
     * @return the duration in milliseconds
     */
    public long getIndexingDurationMs() {
        return indexingDurationMs;
    }
    
    @Override
    public String toString() {
        return String.format("WorkspaceIndexedEvent{workspacePath=%s, files=%d, symbols=%d, duration=%dms}",
            workspacePath, totalFiles, totalSymbols, indexingDurationMs);
    }
}