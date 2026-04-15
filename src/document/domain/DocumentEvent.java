package document.domain;

public sealed interface DocumentEvent
    permits DocumentSectionAdded, DocumentSectionCollapsed, DocumentSectionReordered, DocumentTextEdited {

    String documentId();
}
