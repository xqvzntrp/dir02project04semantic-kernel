package document.domain;

public record DocumentSectionCollapsed(
    String documentId,
    String sectionId,
    boolean collapsed
) implements DocumentEvent {
}
