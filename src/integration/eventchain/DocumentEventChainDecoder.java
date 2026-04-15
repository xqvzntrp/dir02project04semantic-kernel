package integration.eventchain;

import document.domain.DocumentEvent;
import document.domain.DocumentSectionAdded;
import document.domain.DocumentSectionCollapsed;
import document.domain.DocumentSectionReordered;
import document.domain.DocumentTextEdited;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class DocumentEventChainDecoder {

    public List<DocumentEvent> decode(List<VerifiedFieldEvent> verifiedEvents) {
        List<DocumentEvent> events = verifiedEvents.stream()
            .sorted(Comparator.comparingLong(VerifiedFieldEvent::sequence))
            .map(this::decodeEvent)
            .toList();

        return List.copyOf(new ArrayList<>(events));
    }

    private DocumentEvent decodeEvent(VerifiedFieldEvent verifiedEvent) {
        String documentId = requiredField(verifiedEvent, "documentId");
        return switch (verifiedEvent.eventType()) {
            case "DocumentSectionAdded" -> new DocumentSectionAdded(
                documentId,
                requiredField(verifiedEvent, "sectionId"),
                requiredField(verifiedEvent, "sectionName"),
                requiredInt(verifiedEvent, "position"),
                requiredField(verifiedEvent, "textRef")
            );
            case "DocumentTextEdited" -> new DocumentTextEdited(
                documentId,
                requiredField(verifiedEvent, "sectionId"),
                requiredField(verifiedEvent, "textRef")
            );
            case "DocumentSectionCollapsed" -> new DocumentSectionCollapsed(
                documentId,
                requiredField(verifiedEvent, "sectionId"),
                requiredBoolean(verifiedEvent, "collapsed")
            );
            case "DocumentSectionReordered" -> new DocumentSectionReordered(
                documentId,
                requiredField(verifiedEvent, "sectionId"),
                requiredInt(verifiedEvent, "targetIndex")
            );
            default -> throw new IllegalArgumentException(
                "unsupported document event type: " + verifiedEvent.eventType());
        };
    }

    private String requiredField(VerifiedFieldEvent verifiedEvent, String fieldName) {
        String value = verifiedEvent.fields().get(fieldName);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                "missing required field '" + fieldName + "' for event " + verifiedEvent.eventType());
        }
        return value;
    }

    private int requiredInt(VerifiedFieldEvent verifiedEvent, String fieldName) {
        return Integer.parseInt(requiredField(verifiedEvent, fieldName));
    }

    private boolean requiredBoolean(VerifiedFieldEvent verifiedEvent, String fieldName) {
        return Boolean.parseBoolean(requiredField(verifiedEvent, fieldName));
    }
}
