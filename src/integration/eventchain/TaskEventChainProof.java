package integration.eventchain;

import java.io.IOException;
import java.nio.file.Path;
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
import task.domain.TaskEvent;
import task.domain.TaskProjector;
import task.domain.TaskState;
import task.domain.TaskStatus;

public final class TaskEventChainProof {

    public static void main(String[] args) throws IOException {
        VerifiedFieldEventSource source = new VerifiedFieldEventSource();
        List<VerifiedFieldEvent> verifiedEvents =
            source.load(Path.of("samples/eventchain/task-history.verified"));

        TaskEventChainDecoder decoder = new TaskEventChainDecoder();
        List<TaskEvent> taskEvents = decoder.decode(verifiedEvents);

        SemanticKernel<TaskState, TaskEvent, NextMove<TaskStatus>, TaskAction> kernel =
            new DefaultSemanticKernel<>(
                new TaskProjector(),
                TaskState::status,
                transitionTable(),
                new TaskActionAdapter());

        SemanticSnapshot<TaskState, NextMove<TaskStatus>, TaskAction> snapshot = kernel.analyze(taskEvents);

        assertEquals(new TaskState("task-1", TaskStatus.IN_PROGRESS), snapshot.state(), "state");
        assertEquals(
            List.of(new NextMove<>("complete", TaskStatus.COMPLETED, List.of())),
            snapshot.nextMoves(),
            "nextMoves");
        assertEquals(
            List.of(new TaskAction("complete", TaskStatus.COMPLETED)),
            snapshot.actions(),
            "actions");

        System.out.println("verifiedEvents=" + verifiedEvents.size());
        System.out.println("decodedEvents=" + taskEvents);
        System.out.println("state=" + snapshot.state());
        System.out.println("nextMoves=" + snapshot.nextMoves());
        System.out.println("actions=" + snapshot.actions());
    }

    private static TransitionTable<TaskStatus> transitionTable() {
        return new TransitionTable<>(List.of(
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
                true)));
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }
}
