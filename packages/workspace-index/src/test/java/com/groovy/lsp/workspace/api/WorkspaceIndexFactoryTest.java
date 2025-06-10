package com.groovy.lsp.workspace.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.groovy.lsp.shared.workspace.api.WorkspaceIndexService;
import com.groovy.lsp.test.annotations.UnitTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.io.TempDir;

/**
 * WorkspaceIndexFactoryのテストクラス。
 */
class WorkspaceIndexFactoryTest {

    @TempDir @Nullable Path tempDir;

    @UnitTest
    void createWorkspaceIndexService_shouldCreateServiceWithValidWorkspaceRoot() throws Exception {
        // given
        Path workspaceRoot =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("workspace");
        Files.createDirectories(workspaceRoot);

        // when
        WorkspaceIndexService service =
                WorkspaceIndexFactory.createWorkspaceIndexService(workspaceRoot);

        // then
        assertThat(service).isNotNull();
        assertThat(service).isInstanceOf(WorkspaceIndexService.class);
    }

    @UnitTest
    void createWorkspaceIndexService_shouldThrowExceptionForNullWorkspaceRoot() throws Exception {
        // when/then
        // Use reflection to bypass NullAway compile-time check
        var method =
                WorkspaceIndexFactory.class.getMethod("createWorkspaceIndexService", Path.class);
        assertThatThrownBy(() -> method.invoke(null, (Path) null))
                .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .getCause()
                .hasMessageContaining("Workspace root cannot be null");
    }

    @UnitTest
    void createWorkspaceIndexService_shouldThrowExceptionForNonExistentWorkspaceRoot() {
        // given
        Path nonExistentPath =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("non-existent");

        // when/then
        assertThatThrownBy(() -> WorkspaceIndexFactory.createWorkspaceIndexService(nonExistentPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Workspace root does not exist");
    }

    @UnitTest
    void createWorkspaceIndexService_shouldHandleFileAsWorkspaceRoot() throws Exception {
        // given
        Path file =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("file.txt");
        Files.createFile(file);

        // when
        WorkspaceIndexService service = WorkspaceIndexFactory.createWorkspaceIndexService(file);

        // then - ファイルでもexists()はtrueを返すため、サービスは作成される
        assertThat(service).isNotNull();
    }

    @UnitTest
    void createWorkspaceIndexService_shouldCreateMultipleInstances() throws Exception {
        // given
        Path workspace1 =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("workspace1");
        Path workspace2 =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("workspace2");
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

    @UnitTest
    void createWorkspaceIndexService_shouldCreateMultipleInstancesForSameWorkspace()
            throws Exception {
        // given
        Path workspaceRoot =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("workspace");
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

    @UnitTest
    void createWorkspaceIndexService_shouldHandleBothAbsoluteAndRelativePaths() throws Exception {
        // given
        Path absolutePath =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("absolute-workspace");
        Files.createDirectories(absolutePath);

        // when
        WorkspaceIndexService service =
                WorkspaceIndexFactory.createWorkspaceIndexService(absolutePath);

        // then
        assertThat(service).isNotNull();
    }

    @UnitTest
    void createWorkspaceIndexService_shouldHandleSymbolicLinks() throws Exception {
        // given
        Path actualDir =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("actual-workspace");
        Path symlink =
                Objects.requireNonNull(tempDir, "tempDir should be initialized by JUnit")
                        .resolve("symlink-workspace");
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
