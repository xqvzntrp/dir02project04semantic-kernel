package approval.domain;

import java.util.List;
import java.util.Set;
import semantic.kernel.DefaultSemanticKernel;
import semantic.kernel.SemanticKernel;
import semantic.rules.NextMove;
import semantic.rules.TransitionRule;
import semantic.rules.TransitionTable;
import semantic.snapshot.SemanticSnapshot;

public final class ApprovalDomainProof {

    public static void main(String[] args) {
        SemanticKernel<ApprovalState, ApprovalEvent, NextMove<ApprovalStatus>, ApprovalAction> kernel =
            new DefaultSemanticKernel<>(
                new ApprovalProjector(),
                ApprovalState::status,
                transitionTable(),
                new ApprovalActionAdapter());

        SemanticSnapshot<ApprovalState, NextMove<ApprovalStatus>, ApprovalAction> openSnapshot =
            kernel.analyze(List.of(new Submitted("approval-1")));

        assertEquals(new ApprovalState("approval-1", ApprovalStatus.SUBMITTED), openSnapshot.state(), "open state");
        assertEquals(
            Set.of(
                new NextMove<>("approve", ApprovalStatus.APPROVED, List.of()),
                new NextMove<>("reject", ApprovalStatus.REJECTED, List.of())),
            Set.copyOf(openSnapshot.nextMoves()),
            "open nextMoves");
        assertEquals(
            Set.of(
                new ApprovalAction("approve", ApprovalStatus.APPROVED),
                new ApprovalAction("reject", ApprovalStatus.REJECTED)),
            Set.copyOf(openSnapshot.actions()),
            "open actions");

        SemanticSnapshot<ApprovalState, NextMove<ApprovalStatus>, ApprovalAction> terminalSnapshot =
            kernel.analyze(List.of(
                new Submitted("approval-1"),
                new Approved("approval-1")));

        assertEquals(
            new ApprovalState("approval-1", ApprovalStatus.APPROVED),
            terminalSnapshot.state(),
            "terminal state");
        assertEquals(List.of(), terminalSnapshot.nextMoves(), "terminal nextMoves");
        assertEquals(List.of(), terminalSnapshot.actions(), "terminal actions");

        System.out.println("openState=" + openSnapshot.state());
        System.out.println("openNextMoves=" + openSnapshot.nextMoves());
        System.out.println("openActions=" + openSnapshot.actions());
        System.out.println("terminalState=" + terminalSnapshot.state());
        System.out.println("terminalNextMoves=" + terminalSnapshot.nextMoves());
        System.out.println("terminalActions=" + terminalSnapshot.actions());
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
                true)));
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }
}
