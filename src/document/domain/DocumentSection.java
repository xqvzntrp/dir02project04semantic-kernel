package document.domain;

public record DocumentSection(
    String id,
    String name,
    String textRef,
    boolean collapsed,
    int order
) {
    public DocumentSection withTextRef(String nextTextRef) {
        return new DocumentSection(id, name, nextTextRef, collapsed, order);
    }

    public DocumentSection withCollapsed(boolean nextCollapsed) {
        return new DocumentSection(id, name, textRef, nextCollapsed, order);
    }

    public DocumentSection withOrder(int nextOrder) {
        return new DocumentSection(id, name, textRef, collapsed, nextOrder);
    }
}
