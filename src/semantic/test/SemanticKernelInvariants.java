package semantic.test;

import approval.domain.ApprovalAction;
import approval.domain.ApprovalActionAdapter;
import approval.domain.ApprovalEvent;
import approval.domain.ApprovalProjector;
import approval.domain.ApprovalState;
import approval.domain.ApprovalStatus;
import approval.domain.Approved;
import approval.domain.Submitted;
import java.util.List;
import java.util.Set;
import semantic.kernel.DefaultSemanticKernel;
import semantic.kernel.SemanticKernel;
import semantic.rules.NextMove;
import semantic.rules.TransitionRule;
import semantic.rules.TransitionTable;
import semantic.snapshot.SemanticSnapshot;
import task.domain.TaskAction;
import task.domain.TaskActionAdapter;
import task.domain.TaskCompleted;
import task.domain.TaskCreated;
import task.domain.TaskEvent;
import task.domain.TaskProjector;
import task.domain.TaskStarted;
import task.domain.TaskState;
import task.domain.TaskStatus;

public final class SemanticKernelInvariants {

    public static void main(String[] args) {
        sameEventListAlwaysYieldsSameSnapshot();
        taskLinearProofRemainsStable();
        approvalBranchingProofReturnsBothNextMoves();
        terminalStatesYieldEmptyMovesAndActions();
        projectorErrorsRemainDomainOwned();
        actionRequestsAreValidatedAtTheAdapterBoundary();

        System.out.println("Semantic kernel invariants passed.");
    }

    private static void sameEventListAlwaysYieldsSameSnapshot() {
        SemanticKernel<TaskState, TaskEvent, NextMove<TaskStatus>, TaskAction> kernel = taskKernel();
        List<TaskEvent> events = List.of(
            new TaskCreated("task-1"),
            new TaskStarted("task-1"));

        SemanticSnapshot<TaskState, NextMove<TaskStatus>, TaskAction> first = kernel.analyze(events);
        SemanticSnapshot<TaskState, NextMove<TaskStatus>, TaskAction> second = kernel.analyze(events);

        assertEquals(first, second, "deterministic snapshot");
    }

    private static void taskLinearProofRemainsStable() {
        SemanticKernel<TaskState, TaskEvent, NextMove<TaskStatus>, TaskAction> kernel = taskKernel();

        SemanticSnapshot<TaskState, NextMove<TaskStatus>, TaskAction> snapshot =
            kernel.analyze(List.of(
                new TaskCreated("task-1"),
                new TaskStarted("task-1")));

        assertEquals(new TaskState("task-1", TaskStatus.IN_PROGRESS), snapshot.state(), "task state");
        assertEquals(
            List.of(new NextMove<>("complete", TaskStatus.COMPLETED, List.of())),
            snapshot.nextMoves(),
            "task nextMoves");
        assertEquals(
            List.of(new TaskAction("complete", TaskStatus.COMPLETED)),
            snapshot.actions(),
            "task actions");
    }

    private static void approvalBranchingProofReturnsBothNextMoves() {
        SemanticKernel<ApprovalState, ApprovalEvent, NextMove<ApprovalStatus>, ApprovalAction> kernel =
            approvalKernel();

        SemanticSnapshot<ApprovalState, NextMove<ApprovalStatus>, ApprovalAction> snapshot =
            kernel.analyze(List.of(new Submitted("approval-1")));

        assertEquals(new ApprovalState("approval-1", ApprovalStatus.SUBMITTED), snapshot.state(), "approval state");
        assertEquals(
            Set.of(
                new NextMove<>("approve", ApprovalStatus.APPROVED, List.of()),
                new NextMove<>("reject", ApprovalStatus.REJECTED, List.of())),
            Set.copyOf(snapshot.nextMoves()),
            "approval nextMoves");
        assertEquals(
            Set.of(
                new ApprovalAction("approve", ApprovalStatus.APPROVED),
                new ApprovalAction("reject", ApprovalStatus.REJECTED)),
            Set.copyOf(snapshot.actions()),
            "approval actions");
    }

    private static void terminalStatesYieldEmptyMovesAndActions() {
        SemanticKernel<ApprovalState, ApprovalEvent, NextMove<ApprovalStatus>, ApprovalAction> kernel =
            approvalKernel();

        SemanticSnapshot<ApprovalState, NextMove<ApprovalStatus>, ApprovalAction> snapshot =
            kernel.analyze(List.of(
                new Submitted("approval-1"),
                new Approved("approval-1")));

        assertEquals(new ApprovalState("approval-1", ApprovalStatus.APPROVED), snapshot.state(), "terminal state");
        assertEquals(List.of(), snapshot.nextMoves(), "terminal nextMoves");
        assertEquals(List.of(), snapshot.actions(), "terminal actions");
    }

    private static void projectorErrorsRemainDomainOwned() {
        SemanticKernel<TaskState, TaskEvent, NextMove<TaskStatus>, TaskAction> kernel = taskKernel();

        try {
            kernel.analyze(List.of(new TaskCompleted("task-1")));
            throw new AssertionError("expected projector to reject invalid task history");
        } catch (IllegalStateException expected) {
            assertEquals(
                "task must be created before other events",
                expected.getMessage(),
                "projector error");
        }
    }

    private static void actionRequestsAreValidatedAtTheAdapterBoundary() {
        TaskActionAdapter adapter = new TaskActionAdapter();
        SemanticSnapshot<TaskState, NextMove<TaskStatus>, TaskAction> snapshot =
            taskKernel().analyze(List.of(
                new TaskCreated("task-1"),
                new TaskStarted("task-1")));

        try {
            adapter.toEvent(snapshot.state(), snapshot.actions(), "reopen");
            throw new AssertionError("expected adapter to reject unsupported action request");
        } catch (IllegalArgumentException expected) {
            assertEquals(
                "unsupported action request: reopen",
                expected.getMessage(),
                "adapter boundary error");
        }
    }

    private static SemanticKernel<TaskState, TaskEvent, NextMove<TaskStatus>, TaskAction> taskKernel() {
        return new DefaultSemanticKernel<>(
            new TaskProjector(),
            TaskState::status,
            new TransitionTable<>(List.of(
                new TransitionRule<>(
                    "start",
                    Set.of(TaskStatus.CREATED),
                    TaskStatus.IN_PROGRESS,
                    List.of(),
                    false,
                    false),
                new TransitionRule<>(
                    "complete",
                    Set.of(TaskStatus.IN_PROGRESS),
                    TaskStatus.COMPLETED,
                    List.of(),
                    false,
                    true))),
            new TaskActionAdapter());
    }

    private static SemanticKernel<ApprovalState, ApprovalEvent, NextMove<ApprovalStatus>, ApprovalAction>
    approvalKernel() {
        return new DefaultSemanticKernel<>(
            new ApprovalProjector(),
            ApprovalState::status,
            new TransitionTable<>(List.of(
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
                    true))),
            new ApprovalActionAdapter());
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }
}
