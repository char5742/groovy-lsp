package com.groovy.lsp.workspace.api;

/**
 * This package now uses the WorkspaceIndexService interface from the shared module.
 * @deprecated Use {@link com.groovy.lsp.shared.workspace.api.WorkspaceIndexService} instead
 */
@Deprecated(forRemoval = true)
public interface WorkspaceIndexService
        extends com.groovy.lsp.shared.workspace.api.WorkspaceIndexService {}
