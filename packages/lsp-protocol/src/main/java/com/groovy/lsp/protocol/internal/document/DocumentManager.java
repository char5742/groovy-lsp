package com.groovy.lsp.protocol.internal.document;

import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.lsp4j.TextDocumentItem;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages text documents in memory for the LSP server.
 *
 * This service tracks opened documents and their content,
 * allowing other services to retrieve document content by URI.
 */
@Singleton
public class DocumentManager {

    private static final Logger logger = LoggerFactory.getLogger(DocumentManager.class);

    private final ConcurrentHashMap<String, TextDocumentItem> documents = new ConcurrentHashMap<>();

    @Inject
    public DocumentManager() {
        // Default constructor for dependency injection
    }

    /**
     * Opens a document and stores its content.
     *
     * @param document The document to open
     */
    public void openDocument(TextDocumentItem document) {
        logger.debug("Opening document: {}", document.getUri());
        documents.put(document.getUri(), document);
    }

    /**
     * Updates the content of an opened document.
     *
     * @param uri The document URI
     * @param newContent The new content
     * @param version The new version number
     */
    public void updateDocument(String uri, String newContent, int version) {
        logger.debug("Updating document: {} to version {}", uri, version);
        TextDocumentItem doc = documents.get(uri);
        if (doc != null) {
            doc.setText(newContent);
            doc.setVersion(version);
        } else {
            logger.warn("Attempted to update non-existent document: {}", uri);
        }
    }

    /**
     * Closes a document and removes it from memory.
     *
     * @param uri The document URI
     */
    public void closeDocument(String uri) {
        logger.debug("Closing document: {}", uri);
        documents.remove(uri);
    }

    /**
     * Gets the content of a document.
     *
     * @param uri The document URI
     * @return The document content, or null if not found
     */
    public @Nullable String getDocumentContent(String uri) {
        TextDocumentItem doc = documents.get(uri);
        return doc != null ? doc.getText() : null;
    }

    /**
     * Gets the full document item.
     *
     * @param uri The document URI
     * @return The document item, or null if not found
     */
    public @Nullable TextDocumentItem getDocument(String uri) {
        return documents.get(uri);
    }

    /**
     * Checks if a document is currently open.
     *
     * @param uri The document URI
     * @return true if the document is open, false otherwise
     */
    public boolean isDocumentOpen(String uri) {
        return documents.containsKey(uri);
    }
}
