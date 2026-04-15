package app.approvalapp;

import approval.domain.ApprovalAction;
import approval.domain.ApprovalEvent;
import approval.domain.ApprovalState;
import approval.domain.ApprovalStatus;
import integration.eventchain.VerifiedFieldEvent;
import java.util.List;
import semantic.rules.NextMove;
import semantic.snapshot.SemanticSnapshot;

public record ApprovalView(
    List<VerifiedFieldEvent> verifiedHistory,
    List<ApprovalEvent> decodedEvents,
    SemanticSnapshot<ApprovalState, NextMove<ApprovalStatus>, ApprovalAction> snapshot
) {
}
