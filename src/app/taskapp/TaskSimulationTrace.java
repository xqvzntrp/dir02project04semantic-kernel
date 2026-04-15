package app.taskapp;

import java.util.List;
import semantic.rules.NextMove;
import semantic.snapshot.SemanticSnapshot;
import task.domain.TaskAction;
import task.domain.TaskEvent;
import task.domain.TaskState;
import task.domain.TaskStatus;

public record TaskSimulationTrace(
    List<TaskEvent> baseEvents,
    SemanticSnapshot<TaskState, NextMove<TaskStatus>, TaskAction> baseSnapshot,
    List<TaskSimulationStep> steps,
    List<TaskEvent> fullEvents,
    SemanticSnapshot<TaskState, NextMove<TaskStatus>, TaskAction> finalSnapshot
) {
    public TaskSimulationTrace {
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
