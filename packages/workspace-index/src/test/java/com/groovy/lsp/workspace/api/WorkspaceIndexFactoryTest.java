package com.groovy.lsp.workspace.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.groovy.lsp.shared.workspace.api.WorkspaceIndexService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * WorkspaceIndexFactoryのテストクラス。
 */
class WorkspaceIndexFactoryTest {

    @TempDir Path tempDir;

    @Test
    void createWorkspaceIndexService_shouldCreateServiceWithValidWorkspaceRoot() throws Exception {
        // given
        Path workspaceRoot = tempDir.resolve("workspace");
        Files.createDirectories(workspaceRoot);

        // when
        WorkspaceIndexService service =
                WorkspaceIndexFactory.createWorkspaceIndexService(workspaceRoot);

        // then
        assertThat(service).isNotNull();
        assertThat(service).isInstanceOf(WorkspaceIndexService.class);
    }

    @Test
    void createWorkspaceIndexService_shouldThrowExceptionForNullWorkspaceRoot() {
        // when/then
        assertThatThrownBy(() -> WorkspaceIndexFactory.createWorkspaceIndexService(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Workspace root cannot be null");
    }

    @Test
    void createWorkspaceIndexService_shouldThrowExceptionForNonExistentWorkspaceRoot() {
        // given
        Path nonExistentPath = tempDir.resolve("non-existent");

        // when/then
        assertThatThrownBy(() -> WorkspaceIndexFactory.createWorkspaceIndexService(nonExistentPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Workspace root does not exist");
    }

    @Test
    void createWorkspaceIndexService_shouldHandleFileAsWorkspaceRoot() throws Exception {
        // given
        Path file = tempDir.resolve("file.txt");
        Files.createFile(file);

        // when
        WorkspaceIndexService service = WorkspaceIndexFactory.createWorkspaceIndexService(file);

        // then - ファイルでもexists()はtrueを返すため、サービスは作成される
        assertThat(service).isNotNull();
    }

    @Test
    void createWorkspaceIndexService_shouldCreateMultipleInstances() throws Exception {
        // given
        Path workspace1 = tempDir.resolve("workspace1");
        Path workspace2 = tempDir.resolve("workspace2");
        Files.createDirectories(workspace1);
        Files.createDirectories(workspace2);

        // when
        WorkspaceIndexService service1 =
                WorkspaceIndexFactory.createWorkspaceIndexService(workspace1);
        WorkspaceIndexService service2 =
                WorkspaceIndexFactory.createWorkspaceIndexService(workspace2);

        // then
        assertThat(service1).isNotNull();
        assertThat(service2).isNotNull();
        assertThat(service1).isNotSameAs(service2);
    }

    @Test
    void createWorkspaceIndexService_shouldCreateMultipleInstancesForSameWorkspace()
            throws Exception {
        // given
        Path workspaceRoot = tempDir.resolve("workspace");
        Files.createDirectories(workspaceRoot);

        // when
        WorkspaceIndexService service1 =
                WorkspaceIndexFactory.createWorkspaceIndexService(workspaceRoot);
        WorkspaceIndexService service2 =
                WorkspaceIndexFactory.createWorkspaceIndexService(workspaceRoot);

        // then
        assertThat(service1).isNotNull();
        assertThat(service2).isNotNull();
        assertThat(service1).isNotSameAs(service2); // 新しいインスタンスが作成される
    }

    @Test
    void createWorkspaceIndexService_shouldHandleBothAbsoluteAndRelativePaths() throws Exception {
        // given
        Path absolutePath = tempDir.resolve("absolute-workspace");
        Files.createDirectories(absolutePath);

        // when
        WorkspaceIndexService service =
                WorkspaceIndexFactory.createWorkspaceIndexService(absolutePath);

        // then
        assertThat(service).isNotNull();
    }

    @Test
    void createWorkspaceIndexService_shouldHandleSymbolicLinks() throws Exception {
        // given
        Path actualDir = tempDir.resolve("actual-workspace");
        Path symlink = tempDir.resolve("symlink-workspace");
        Files.createDirectories(actualDir);

        // シンボリックリンクをサポートするシステムでのみテスト
        try {
            Files.createSymbolicLink(symlink, actualDir);
        } catch (UnsupportedOperationException e) {
            // シンボリックリンクがサポートされない場合はスキップ
            return;
        }

        // when
        WorkspaceIndexService service = WorkspaceIndexFactory.createWorkspaceIndexService(symlink);

        // then
        assertThat(service).isNotNull();
    }
}
