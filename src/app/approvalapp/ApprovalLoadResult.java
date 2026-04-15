package app.approvalapp;

import approval.domain.ApprovalEvent;
import integration.eventchain.VerifiedFieldEvent;
import java.util.List;

public sealed interface ApprovalLoadResult
    permits ApprovalLoadResult.EmptyHistory, ApprovalLoadResult.Loaded {

    record EmptyHistory(
        List<VerifiedFieldEvent> verifiedHistory,
        List<ApprovalEvent> decodedEvents
    ) implements ApprovalLoadResult {
    }

    record Loaded(ApprovalView view) implements ApprovalLoadResult {
        public Loaded {
            if (view == null) {
                throw new IllegalArgumentException("view must not be null");
            }
            if (view.snapshot() == null) {
                throw new IllegalArgumentException("loaded view must have a snapshot");
            }
        }
    }
}
