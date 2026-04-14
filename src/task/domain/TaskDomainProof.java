package task.domain;

import java.util.List;
import semantic.kernel.DefaultSemanticKernel;
import semantic.kernel.SemanticKernel;
import semantic.rules.NextMove;
import semantic.rules.TransitionRule;
import semantic.rules.TransitionTable;
import semantic.snapshot.SemanticSnapshot;

public final class TaskDomainProof {

    public static void main(String[] args) {
        SemanticKernel<TaskState, TaskEvent, NextMove<TaskStatus>, TaskAction> kernel =
            new DefaultSemanticKernel<>(
                new TaskProjector(),
                TaskState::status,
                transitionTable(),
                new TaskActionAdapter());

        List<TaskEvent> events = List.of(
            new TaskCreated("task-1"),
            new TaskStarted("task-1"));

        SemanticSnapshot<TaskState, NextMove<TaskStatus>, TaskAction> snapshot = kernel.analyze(events);

        assertEquals(new TaskState("task-1", TaskStatus.IN_PROGRESS), snapshot.state(), "state");
        assertEquals(
            List.of(new NextMove<>("complete", TaskStatus.COMPLETED, List.of())),
            snapshot.nextMoves(),
            "nextMoves");
        assertEquals(
            List.of(new TaskAction("complete", TaskStatus.COMPLETED)),
            snapshot.actions(),
            "actions");

        System.out.println("state=" + snapshot.state());
        System.out.println("nextMoves=" + snapshot.nextMoves());
        System.out.println("actions=" + snapshot.actions());
    }

    private static TransitionTable<TaskStatus> transitionTable() {
        return new TransitionTable<>(List.of(
            new TransitionRule<>(
                "start",
                java.util.Set.of(TaskStatus.CREATED),
                TaskStatus.IN_PROGRESS,
                List.of(),
                false,
                false),
            new TransitionRule<>(
                "complete",
                java.util.Set.of(TaskStatus.IN_PROGRESS),
                TaskStatus.COMPLETED,
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
