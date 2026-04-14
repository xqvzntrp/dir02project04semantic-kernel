package task.domain;

import java.util.List;
import java.util.Set;
import semantic.kernel.DefaultSemanticKernel;
import semantic.kernel.SemanticKernel;
import semantic.rules.NextMove;
import semantic.rules.TransitionRule;
import semantic.rules.TransitionTable;

public final class TaskDomainKernel {
    private TaskDomainKernel() {
    }

    public static SemanticKernel<TaskState, TaskEvent, NextMove<TaskStatus>, TaskAction> create() {
        return new DefaultSemanticKernel<>(
            new TaskProjector(),
            TaskState::status,
            transitionTable(),
            new TaskActionAdapter()
        );
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
                true),
            new TransitionRule<>(
                "reopen",
                Set.of(TaskStatus.COMPLETED),
                TaskStatus.IN_PROGRESS,
                List.of(),
                false,
                false)
        ));
    }
}
