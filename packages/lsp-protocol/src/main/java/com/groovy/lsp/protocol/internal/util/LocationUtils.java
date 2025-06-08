package com.groovy.lsp.protocol.internal.util;

import org.codehaus.groovy.ast.ASTNode;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.jspecify.annotations.Nullable;

/**
 * Utility class for creating LSP Location objects from AST nodes.
 */
public final class LocationUtils {

    private LocationUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Creates a Location from an AST node.
     *
     * @param uri The URI of the document containing the node
     * @param node The AST node to create a location for
     * @return A Location object, or null if the node has invalid position information
     */
    public static @Nullable Location createLocation(String uri, ASTNode node) {
        if (node.getLineNumber() < 0 || node.getColumnNumber() < 0) {
            return null;
        }

        Range range =
                new Range(
                        new Position(node.getLineNumber() - 1, node.getColumnNumber() - 1),
                        new Position(node.getLastLineNumber() - 1, node.getLastColumnNumber() - 1));

        return new Location(uri, range);
    }
}
