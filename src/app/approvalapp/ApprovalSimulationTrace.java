package app.approvalapp;

import approval.domain.ApprovalAction;
import approval.domain.ApprovalEvent;
import approval.domain.ApprovalState;
import approval.domain.ApprovalStatus;
import java.util.List;
import semantic.rules.NextMove;
import semantic.snapshot.SemanticSnapshot;

public record ApprovalSimulationTrace(
    List<ApprovalEvent> baseEvents,
    SemanticSnapshot<ApprovalState, NextMove<ApprovalStatus>, ApprovalAction> baseSnapshot,
    List<ApprovalSimulationStep> steps,
    List<ApprovalEvent> fullEvents,
    SemanticSnapshot<ApprovalState, NextMove<ApprovalStatus>, ApprovalAction> finalSnapshot
) {
    public ApprovalSimulationTrace {
        baseEvents = List.copyOf(baseEvents);
        steps = List.copyOf(steps);
        fullEvents = List.copyOf(fullEvents);
        if (baseSnapshot == null) {
            throw new IllegalArgumentException("baseSnapshot must not be null");
        }
        if (finalSnapshot == null) {
            throw new IllegalArgumentException("finalSnapshot must not be null");
        }
    }
}
