package app.approvalapp;

import approval.domain.ApprovalAction;
import approval.domain.ApprovalEvent;
import approval.domain.ApprovalState;
import approval.domain.ApprovalStatus;
import semantic.kernel.ActionDescriptor;
import semantic.rules.NextMove;
import semantic.snapshot.SemanticSnapshot;

public record ApprovalSimulationStep(
    int stepIndex,
    String requestedActionName,
    ActionDescriptor descriptor,
    ApprovalEvent event,
    SemanticSnapshot<ApprovalState, NextMove<ApprovalStatus>, ApprovalAction> snapshot
) {
    public ApprovalSimulationStep {
        if (stepIndex < 0) {
            throw new IllegalArgumentException("stepIndex must be non-negative");
        }
        if (requestedActionName == null || requestedActionName.isBlank()) {
            throw new IllegalArgumentException("requestedActionName must not be blank");
        }
        if (descriptor == null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot must not be null");
        }
    }
}
