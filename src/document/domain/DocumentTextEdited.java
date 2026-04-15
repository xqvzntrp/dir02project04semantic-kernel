package document.domain;

public record DocumentTextEdited(
    String documentId,
    String sectionId,
    String textRef
) implements DocumentEvent {
}
