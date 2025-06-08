package com.groovy.lsp.workspace.api.events;

import com.groovy.lsp.shared.event.DomainEvent;
import com.groovy.lsp.shared.workspace.api.dto.SymbolInfo;
import java.nio.file.Path;
import java.util.List;

/**
 * Event emitted when a single file has been indexed.
 * This event is published after parsing and indexing symbols from a file.
 */
public class FileIndexedEvent extends DomainEvent {

    private final Path filePath;
    private final List<SymbolInfo> symbols;
    private final boolean success;
    private final String errorMessage;

    /**
     * Creates a successful FileIndexedEvent.
     *
     * @param filePath the path of the indexed file
     * @param symbols the symbols found in the file
     */
    public FileIndexedEvent(Path filePath, List<SymbolInfo> symbols) {
        super(filePath.toString());
        this.filePath = filePath;
        this.symbols = List.copyOf(symbols); // Defensive copy
        this.success = true;
        this.errorMessage = ""; // Empty string for successful indexing
    }

    /**
     * Creates a failed FileIndexedEvent.
     *
     * @param filePath the path of the file that failed to index
     * @param errorMessage the error message
     */
    public FileIndexedEvent(Path filePath, String errorMessage) {
        super(filePath.toString());
        this.filePath = filePath;
        this.symbols = List.of();
        this.success = false;
        this.errorMessage = errorMessage;
    }

    /**
     * Gets the file path that was indexed.
     *
     * @return the file path
     */
    public Path getFilePath() {
        return filePath;
    }

    /**
     * Gets the symbols found in the file.
     *
     * @return the list of symbols, empty if indexing failed
     */
    public List<SymbolInfo> getSymbols() {
        return symbols;
    }

    /**
     * Checks if the indexing was successful.
     *
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Gets the error message if indexing failed.
     *
     * @return the error message, or empty string if successful
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        if (success) {
            return String.format("FileIndexedEvent{file=%s, symbols=%d}", filePath, symbols.size());
        } else {
            return String.format(
                    "FileIndexedEvent{file=%s, error=%s}",
                    filePath, errorMessage.isEmpty() ? "Unknown error" : errorMessage);
        }
    }
}
