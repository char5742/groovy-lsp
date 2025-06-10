package com.groovy.lsp.protocol.internal.document;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.lsp4j.TextDocumentItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * DocumentManagerのテストクラス。
 */
class DocumentManagerTest {

    private DocumentManager documentManager;
    private TextDocumentItem testDocument;

    @BeforeEach
    void setUp() {
        documentManager = new DocumentManager();
        testDocument = new TextDocumentItem("file:///test.groovy", "groovy", 1, "class Test {}");
    }

    @Test
    void openDocument_shouldStoreDocument() {
        // when
        documentManager.openDocument(testDocument);

        // then
        assertThat(documentManager.isDocumentOpen("file:///test.groovy")).isTrue();
        assertThat(documentManager.getDocument("file:///test.groovy")).isEqualTo(testDocument);
        assertThat(documentManager.getDocumentContent("file:///test.groovy"))
                .isEqualTo("class Test {}");
    }

    @Test
    void openDocument_shouldReplaceExistingDocument() {
        // given
        documentManager.openDocument(testDocument);
        TextDocumentItem newDocument =
                new TextDocumentItem("file:///test.groovy", "groovy", 2, "class NewTest {}");

        // when
        documentManager.openDocument(newDocument);

        // then
        assertThat(documentManager.getDocument("file:///test.groovy")).isEqualTo(newDocument);
        assertThat(documentManager.getDocumentContent("file:///test.groovy"))
                .isEqualTo("class NewTest {}");
    }

    @Test
    void updateDocument_shouldUpdateContentAndVersion() {
        // given
        documentManager.openDocument(testDocument);

        // when
        documentManager.updateDocument("file:///test.groovy", "class UpdatedTest {}", 2);

        // then
        TextDocumentItem updatedDoc = documentManager.getDocument("file:///test.groovy");
        assertThat(updatedDoc).isNotNull();
        java.util.Objects.requireNonNull(updatedDoc);
        assertThat(updatedDoc.getText()).isEqualTo("class UpdatedTest {}");
        assertThat(updatedDoc.getVersion()).isEqualTo(2);
    }

    @Test
    void updateDocument_shouldHandleNonExistentDocument() {
        // when
        documentManager.updateDocument("file:///nonexistent.groovy", "content", 1);

        // then
        assertThat(documentManager.isDocumentOpen("file:///nonexistent.groovy")).isFalse();
        assertThat(documentManager.getDocumentContent("file:///nonexistent.groovy")).isNull();
    }

    @Test
    void closeDocument_shouldRemoveDocument() {
        // given
        documentManager.openDocument(testDocument);
        assertThat(documentManager.isDocumentOpen("file:///test.groovy")).isTrue();

        // when
        documentManager.closeDocument("file:///test.groovy");

        // then
        assertThat(documentManager.isDocumentOpen("file:///test.groovy")).isFalse();
        assertThat(documentManager.getDocument("file:///test.groovy")).isNull();
        assertThat(documentManager.getDocumentContent("file:///test.groovy")).isNull();
    }

    @Test
    void closeDocument_shouldHandleNonExistentDocument() {
        // when/then - should not throw
        documentManager.closeDocument("file:///nonexistent.groovy");

        // verify state unchanged
        assertThat(documentManager.isDocumentOpen("file:///nonexistent.groovy")).isFalse();
    }

    @Test
    void getDocumentContent_shouldReturnNullForNonExistentDocument() {
        // when
        String content = documentManager.getDocumentContent("file:///nonexistent.groovy");

        // then
        assertThat(content).isNull();
    }

    @Test
    void getDocument_shouldReturnNullForNonExistentDocument() {
        // when
        TextDocumentItem document = documentManager.getDocument("file:///nonexistent.groovy");

        // then
        assertThat(document).isNull();
    }

    @Test
    void isDocumentOpen_shouldReturnFalseForNonExistentDocument() {
        // when
        boolean isOpen = documentManager.isDocumentOpen("file:///nonexistent.groovy");

        // then
        assertThat(isOpen).isFalse();
    }

    @Test
    void isDocumentOpen_shouldReturnTrueForOpenDocument() {
        // given
        documentManager.openDocument(testDocument);

        // when
        boolean isOpen = documentManager.isDocumentOpen("file:///test.groovy");

        // then
        assertThat(isOpen).isTrue();
    }

    @Test
    void multipleDocuments_shouldBeHandledCorrectly() {
        // given
        TextDocumentItem doc1 =
                new TextDocumentItem("file:///test1.groovy", "groovy", 1, "class Test1 {}");
        TextDocumentItem doc2 =
                new TextDocumentItem("file:///test2.groovy", "groovy", 1, "class Test2 {}");

        // when
        documentManager.openDocument(doc1);
        documentManager.openDocument(doc2);

        // then
        assertThat(documentManager.isDocumentOpen("file:///test1.groovy")).isTrue();
        assertThat(documentManager.isDocumentOpen("file:///test2.groovy")).isTrue();
        assertThat(documentManager.getDocumentContent("file:///test1.groovy"))
                .isEqualTo("class Test1 {}");
        assertThat(documentManager.getDocumentContent("file:///test2.groovy"))
                .isEqualTo("class Test2 {}");

        // when
        documentManager.closeDocument("file:///test1.groovy");

        // then
        assertThat(documentManager.isDocumentOpen("file:///test1.groovy")).isFalse();
        assertThat(documentManager.isDocumentOpen("file:///test2.groovy")).isTrue();
    }

    @Test
    void updateDocument_shouldPreserveLanguageIdAndUri() {
        // given
        documentManager.openDocument(testDocument);

        // when
        documentManager.updateDocument("file:///test.groovy", "updated content", 5);

        // then
        TextDocumentItem doc = documentManager.getDocument("file:///test.groovy");
        assertThat(doc).isNotNull();
        java.util.Objects.requireNonNull(doc);
        assertThat(doc.getUri()).isEqualTo("file:///test.groovy");
        assertThat(doc.getLanguageId()).isEqualTo("groovy");
        assertThat(doc.getText()).isEqualTo("updated content");
        assertThat(doc.getVersion()).isEqualTo(5);
    }
}
