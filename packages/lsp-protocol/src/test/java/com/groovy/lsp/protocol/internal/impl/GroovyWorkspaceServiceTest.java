package com.groovy.lsp.protocol.internal.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.eclipse.lsp4j.CreateFilesParams;
import org.eclipse.lsp4j.DeleteFilesParams;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.FileCreate;
import org.eclipse.lsp4j.FileDelete;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.FileRename;
import org.eclipse.lsp4j.RenameFilesParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.WorkspaceFoldersChangeEvent;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GroovyWorkspaceServiceのテストクラス。
 */
@ExtendWith(MockitoExtension.class)
class GroovyWorkspaceServiceTest {

    private GroovyWorkspaceService service;

    @Mock private LanguageClient mockClient;

    @BeforeEach
    void setUp() {
        service = new GroovyWorkspaceService();
    }

    @Test
    void connect_shouldSetClient() {
        // when
        service.connect(mockClient);

        // then - verify by calling a method that uses the client
        service.didChangeConfiguration(new DidChangeConfigurationParams(new Object()));
        // The mock client is stored but not used in current implementation
    }

    @Test
    void symbol_shouldReturnEmptySymbolList() throws Exception {
        // given
        WorkspaceSymbolParams params = new WorkspaceSymbolParams("test");

        // when
        Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>> result =
                service.symbol(params).get();

        // then
        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isEmpty();
    }

    @Test
    void symbol_shouldSearchWithEmptyQuery() throws Exception {
        // given
        WorkspaceSymbolParams params = new WorkspaceSymbolParams("");

        // when
        Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>> result =
                service.symbol(params).get();

        // then
        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isEmpty();
    }

    @Test
    void didChangeConfiguration_shouldHandleConfigurationChanges() {
        // given
        service.connect(mockClient);
        Object settings = new Object();
        DidChangeConfigurationParams params = new DidChangeConfigurationParams(settings);

        // when/then - should not throw
        service.didChangeConfiguration(params);
    }

    @Test
    void didChangeConfiguration_shouldWorkWithoutClient() {
        // given
        DidChangeConfigurationParams params = new DidChangeConfigurationParams(new Object());

        // when/then - should not throw
        service.didChangeConfiguration(params);
    }

    @Test
    void didChangeWatchedFiles_shouldHandleFileChanges() {
        // given
        FileEvent event1 = new FileEvent("file:///test1.groovy", FileChangeType.Created);
        FileEvent event2 = new FileEvent("file:///test2.groovy", FileChangeType.Changed);
        FileEvent event3 = new FileEvent("file:///test3.groovy", FileChangeType.Deleted);
        List<FileEvent> changes = Arrays.asList(event1, event2, event3);
        DidChangeWatchedFilesParams params = new DidChangeWatchedFilesParams(changes);

        // when/then - should not throw
        service.didChangeWatchedFiles(params);
    }

    @Test
    void didChangeWatchedFiles_shouldHandleEmptyChangeList() {
        // given
        DidChangeWatchedFilesParams params = new DidChangeWatchedFilesParams(Arrays.asList());

        // when/then - should not throw
        service.didChangeWatchedFiles(params);
    }

    @Test
    void executeCommand_shouldExecuteCommand() throws Exception {
        // given
        String command = "groovy.test.command";
        List<Object> arguments = Arrays.asList("arg1", "arg2");
        ExecuteCommandParams params = new ExecuteCommandParams(command, arguments);

        // when
        Object result = service.executeCommand(params).get();

        // then
        assertThat(result).isNull();
    }

    @Test
    void executeCommand_shouldExecuteCommandWithoutArguments() throws Exception {
        // given
        ExecuteCommandParams params = new ExecuteCommandParams("groovy.simple.command", null);

        // when
        Object result = service.executeCommand(params).get();

        // then
        assertThat(result).isNull();
    }

    @Test
    void didChangeWorkspaceFolders_shouldHandleFolderAddition() {
        // given
        WorkspaceFolder folder1 = new WorkspaceFolder("file:///workspace1", "workspace1");
        WorkspaceFolder folder2 = new WorkspaceFolder("file:///workspace2", "workspace2");
        WorkspaceFoldersChangeEvent event =
                new WorkspaceFoldersChangeEvent(Arrays.asList(folder1, folder2), Arrays.asList());
        DidChangeWorkspaceFoldersParams params = new DidChangeWorkspaceFoldersParams(event);

        // when/then - should not throw
        service.didChangeWorkspaceFolders(params);
    }

    @Test
    void didChangeWorkspaceFolders_shouldHandleFolderRemoval() {
        // given
        WorkspaceFolder folder = new WorkspaceFolder("file:///old-workspace", "old-workspace");
        WorkspaceFoldersChangeEvent event =
                new WorkspaceFoldersChangeEvent(Arrays.asList(), Arrays.asList(folder));
        DidChangeWorkspaceFoldersParams params = new DidChangeWorkspaceFoldersParams(event);

        // when/then - should not throw
        service.didChangeWorkspaceFolders(params);
    }

    @Test
    void didChangeWorkspaceFolders_shouldHandleFolderAdditionAndRemoval() {
        // given
        WorkspaceFolder added = new WorkspaceFolder("file:///new", "new");
        WorkspaceFolder removed = new WorkspaceFolder("file:///old", "old");
        WorkspaceFoldersChangeEvent event =
                new WorkspaceFoldersChangeEvent(Arrays.asList(added), Arrays.asList(removed));
        DidChangeWorkspaceFoldersParams params = new DidChangeWorkspaceFoldersParams(event);

        // when/then - should not throw
        service.didChangeWorkspaceFolders(params);
    }

    @Test
    void willRenameFiles_shouldReturnEmptyWorkspaceEdit() throws Exception {
        // given
        FileRename rename1 = new FileRename("file:///old1.groovy", "file:///new1.groovy");
        FileRename rename2 = new FileRename("file:///old2.groovy", "file:///new2.groovy");
        RenameFilesParams params = new RenameFilesParams(Arrays.asList(rename1, rename2));

        // when
        WorkspaceEdit result = service.willRenameFiles(params).get();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getChanges()).isNullOrEmpty();
    }

    @Test
    void didRenameFiles_shouldHandleFileRename() {
        // given
        FileRename rename = new FileRename("file:///old.groovy", "file:///new.groovy");
        RenameFilesParams params = new RenameFilesParams(Arrays.asList(rename));

        // when/then - should not throw
        service.didRenameFiles(params);
    }

    @Test
    void didRenameFiles_shouldHandleMultipleFileRenames() {
        // given
        FileRename rename1 = new FileRename("file:///a.groovy", "file:///b.groovy");
        FileRename rename2 = new FileRename("file:///c.groovy", "file:///d.groovy");
        FileRename rename3 = new FileRename("file:///e.groovy", "file:///f.groovy");
        RenameFilesParams params = new RenameFilesParams(Arrays.asList(rename1, rename2, rename3));

        // when/then - should not throw
        service.didRenameFiles(params);
    }

    @Test
    void willDeleteFiles_shouldReturnEmptyWorkspaceEdit() throws Exception {
        // given
        FileDelete delete = new FileDelete("file:///delete-me.groovy");
        DeleteFilesParams params = new DeleteFilesParams(Arrays.asList(delete));

        // when
        WorkspaceEdit result = service.willDeleteFiles(params).get();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getChanges()).isNullOrEmpty();
    }

    @Test
    void didDeleteFiles_shouldHandleFileDeletion() {
        // given
        FileDelete delete1 = new FileDelete("file:///deleted1.groovy");
        FileDelete delete2 = new FileDelete("file:///deleted2.groovy");
        DeleteFilesParams params = new DeleteFilesParams(Arrays.asList(delete1, delete2));

        // when/then - should not throw
        service.didDeleteFiles(params);
    }

    @Test
    void willCreateFiles_shouldReturnEmptyWorkspaceEdit() throws Exception {
        // given
        FileCreate create = new FileCreate("file:///new-file.groovy");
        CreateFilesParams params = new CreateFilesParams(Arrays.asList(create));

        // when
        WorkspaceEdit result = service.willCreateFiles(params).get();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getChanges()).isNullOrEmpty();
    }

    @Test
    void didCreateFiles_shouldHandleFileCreation() {
        // given
        FileCreate create1 = new FileCreate("file:///created1.groovy");
        FileCreate create2 = new FileCreate("file:///created2.groovy");
        FileCreate create3 = new FileCreate("file:///created3.groovy");
        CreateFilesParams params = new CreateFilesParams(Arrays.asList(create1, create2, create3));

        // when/then - should not throw
        service.didCreateFiles(params);
    }

    @Test
    void didCreateFiles_shouldHandleEmptyFileList() {
        // given
        CreateFilesParams params = new CreateFilesParams(Arrays.asList());

        // when/then - should not throw
        service.didCreateFiles(params);
    }
}
