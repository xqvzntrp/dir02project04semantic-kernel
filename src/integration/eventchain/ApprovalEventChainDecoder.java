package integration.eventchain;

import approval.domain.ApprovalEvent;
import approval.domain.Approved;
import approval.domain.Rejected;
import approval.domain.Submitted;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ApprovalEventChainDecoder {

    public List<ApprovalEvent> decode(List<VerifiedFieldEvent> verifiedEvents) {
        List<ApprovalEvent> events = verifiedEvents.stream()
            .sorted(Comparator.comparingLong(VerifiedFieldEvent::sequence))
            .map(this::decodeEvent)
            .toList();

        return List.copyOf(new ArrayList<>(events));
    }

    private ApprovalEvent decodeEvent(VerifiedFieldEvent verifiedEvent) {
        String approvalId = requiredField(verifiedEvent, "approvalId");

        return switch (verifiedEvent.eventType()) {
            case "ApprovalSubmitted" -> new Submitted(approvalId);
            case "ApprovalApproved" -> new Approved(approvalId);
            case "ApprovalRejected" -> new Rejected(approvalId);
            default -> throw new IllegalArgumentException(
                "unsupported approval event type: " + verifiedEvent.eventType());
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
}
