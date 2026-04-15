package approval.domain;

import java.util.List;
import java.util.Set;
import semantic.kernel.DefaultSemanticKernel;
import semantic.kernel.SemanticKernel;
import semantic.rules.NextMove;
import semantic.rules.TransitionRule;
import semantic.rules.TransitionTable;

public final class ApprovalDomainKernel {
    private ApprovalDomainKernel() {
    }

    public static SemanticKernel<ApprovalState, ApprovalEvent, NextMove<ApprovalStatus>, ApprovalAction> create() {
        return new DefaultSemanticKernel<>(
            new ApprovalProjector(),
            ApprovalState::status,
            transitionTable(),
            new ApprovalActionAdapter()
        );
    }

    private static TransitionTable<ApprovalStatus> transitionTable() {
        return new TransitionTable<>(List.of(
            new TransitionRule<>(
                "approve",
                Set.of(ApprovalStatus.SUBMITTED),
                ApprovalStatus.APPROVED,
                List.of(),
                false,
                true),
            new TransitionRule<>(
                "reject",
                Set.of(ApprovalStatus.SUBMITTED),
                ApprovalStatus.REJECTED,
                List.of(),
                false,
                true)
        ));
    }
}
