package app.approvalapp;

import approval.domain.ApprovalAction;
import approval.domain.ApprovalEvent;
import approval.domain.ApprovalState;
import approval.domain.ApprovalStatus;
import semantic.rules.NextMove;
import semantic.snapshot.SemanticSnapshot;

public record ApprovalTimelineEntry(
    int eventIndex,
    ApprovalEvent event,
    SemanticSnapshot<ApprovalState, NextMove<ApprovalStatus>, ApprovalAction> snapshot
) {
    public ApprovalTimelineEntry {
        if (eventIndex < 0) {
            throw new IllegalArgumentException("eventIndex must be non-negative");
        }
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot must not be null");
        }
    }
}
