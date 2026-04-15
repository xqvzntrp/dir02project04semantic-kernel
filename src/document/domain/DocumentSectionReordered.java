package document.domain;

public record DocumentSectionReordered(
    String documentId,
    String sectionId,
    int targetIndex
) implements DocumentEvent {
}
