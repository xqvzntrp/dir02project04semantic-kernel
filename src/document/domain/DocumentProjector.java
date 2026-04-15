package document.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import semantic.kernel.Projector;

public final class DocumentProjector implements Projector<DocumentState, DocumentEvent> {

    private final String defaultDocumentId;

    public DocumentProjector() {
        this("document");
    }

    public DocumentProjector(String defaultDocumentId) {
        this.defaultDocumentId = defaultDocumentId;
    }

    @Override
    public DocumentState project(List<DocumentEvent> events) {
        String documentId = defaultDocumentId;
        Map<String, DocumentSection> sections = new LinkedHashMap<>();

        for (DocumentEvent event : events) {
            if (event.documentId() == null || event.documentId().isBlank()) {
                throw new IllegalStateException("document event must carry a document id");
            }
            if (documentId == null || documentId.isBlank()) {
                documentId = event.documentId();
            }
            if (!documentId.equals(event.documentId())) {
                throw new IllegalStateException("all document events must belong to the same document");
            }

            if (event instanceof DocumentSectionAdded added) {
                if (sections.containsKey(added.sectionId())) {
                    throw new IllegalStateException("section already exists: " + added.sectionId());
                }
                int nextOrder = Math.max(0, Math.min(added.position(), sections.size()));
                List<DocumentSection> ordered = sections.values().stream()
                    .sorted(Comparator.comparingInt(DocumentSection::order))
                    .toList();
                for (DocumentSection section : ordered) {
                    if (section.order() >= nextOrder) {
                        sections.put(section.id(), section.withOrder(section.order() + 1));
                    }
                }
                sections.put(added.sectionId(), new DocumentSection(
                    added.sectionId(),
                    added.name(),
                    added.textRef(),
                    false,
                    nextOrder
                ));
                continue;
            }

            if (event instanceof DocumentTextEdited edited) {
                DocumentSection existing = requireSection(sections, edited.sectionId());
                sections.put(existing.id(), existing.withTextRef(edited.textRef()));
                continue;
            }

            if (event instanceof DocumentSectionCollapsed collapsed) {
                DocumentSection existing = requireSection(sections, collapsed.sectionId());
                sections.put(existing.id(), existing.withCollapsed(collapsed.collapsed()));
                continue;
            }

            if (event instanceof DocumentSectionReordered reordered) {
                applyReorder(sections, reordered.sectionId(), reordered.targetIndex());
                continue;
            }

            throw new IllegalStateException("unknown document event: " + event.getClass().getName());
        }

        return new DocumentState(
            documentId,
            sections.values().stream()
                .sorted(Comparator.comparingInt(DocumentSection::order))
                .toList()
        );
    }

    private static void applyReorder(Map<String, DocumentSection> sections, String sectionId, int targetIndex) {
        requireSection(sections, sectionId);
        List<DocumentSection> ordered = sections.values().stream()
            .sorted(Comparator.comparingInt(DocumentSection::order))
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        int currentIndex = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).id().equals(sectionId)) {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex < 0) {
            throw new IllegalStateException("section not found during reorder: " + sectionId);
        }

        DocumentSection removed = ordered.remove(currentIndex);
        int boundedTarget = Math.max(0, Math.min(targetIndex, ordered.size()));
        ordered.add(boundedTarget, removed);
        for (int i = 0; i < ordered.size(); i++) {
            sections.put(ordered.get(i).id(), ordered.get(i).withOrder(i));
        }
    }

    private static DocumentSection requireSection(Map<String, DocumentSection> sections, String sectionId) {
        DocumentSection section = sections.get(sectionId);
        if (section == null) {
            throw new IllegalStateException("unknown section: " + sectionId);
        }
        return section;
    }
}
