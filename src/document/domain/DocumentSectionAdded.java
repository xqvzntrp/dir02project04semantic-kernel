package document.domain;

public record DocumentSectionAdded(
    String documentId,
    String sectionId,
    String name,
    int position,
    String textRef
) implements DocumentEvent {
}
