package document.domain;

public record DocumentCommand(
    String actionName,
    String documentId,
    String sectionId,
    String sectionName,
    Integer position,
    Integer targetIndex,
    Boolean collapsed,
    String textRef
) {
    public static DocumentCommand addSection(String documentId, String sectionId, String sectionName, int position, String textRef) {
        return new DocumentCommand("add_section", documentId, sectionId, sectionName, position, null, null, textRef);
    }

    public static DocumentCommand editText(String documentId, String sectionId, String textRef) {
        return new DocumentCommand("edit_text", documentId, sectionId, null, null, null, null, textRef);
    }

    public static DocumentCommand setCollapsed(String documentId, String sectionId, boolean collapsed) {
        return new DocumentCommand("set_collapsed", documentId, sectionId, null, null, null, collapsed, null);
    }

    public static DocumentCommand reorderSection(String documentId, String sectionId, int targetIndex) {
        return new DocumentCommand("reorder_section", documentId, sectionId, null, null, targetIndex, null, null);
    }
}
