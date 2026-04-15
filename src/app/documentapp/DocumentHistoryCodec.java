package app.documentapp;

import document.domain.DocumentEvent;
import document.domain.DocumentSectionAdded;
import document.domain.DocumentSectionCollapsed;
import document.domain.DocumentSectionReordered;
import document.domain.DocumentTextEdited;
import integration.eventchain.VerifiedFieldEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DocumentHistoryCodec {
    private DocumentHistoryCodec() {
    }

    public static List<VerifiedFieldEvent> encode(List<DocumentEvent> events) {
        return java.util.stream.IntStream.range(0, events.size())
            .mapToObj(index -> encode(index + 1L, events.get(index)))
            .toList();
    }

    public static VerifiedFieldEvent encode(long sequence, DocumentEvent event) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("documentId", event.documentId());

        if (event instanceof DocumentSectionAdded added) {
            fields.put("sectionId", added.sectionId());
            fields.put("sectionName", added.name());
            fields.put("position", Integer.toString(added.position()));
            fields.put("textRef", added.textRef());
            return new VerifiedFieldEvent(sequence, "DocumentSectionAdded", fields);
        }
        if (event instanceof DocumentTextEdited edited) {
            fields.put("sectionId", edited.sectionId());
            fields.put("textRef", edited.textRef());
            return new VerifiedFieldEvent(sequence, "DocumentTextEdited", fields);
        }
        if (event instanceof DocumentSectionCollapsed collapsed) {
            fields.put("sectionId", collapsed.sectionId());
            fields.put("collapsed", Boolean.toString(collapsed.collapsed()));
            return new VerifiedFieldEvent(sequence, "DocumentSectionCollapsed", fields);
        }
        if (event instanceof DocumentSectionReordered reordered) {
            fields.put("sectionId", reordered.sectionId());
            fields.put("targetIndex", Integer.toString(reordered.targetIndex()));
            return new VerifiedFieldEvent(sequence, "DocumentSectionReordered", fields);
        }
        throw new IllegalArgumentException("unsupported document event: " + event.getClass().getName());
    }
}
