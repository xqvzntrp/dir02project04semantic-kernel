package app.taskapp;

import java.util.List;
import semantic.rules.NextMove;
import semantic.snapshot.SemanticSnapshot;
import task.domain.TaskAction;
import task.domain.TaskEvent;
import task.domain.TaskState;
import task.domain.TaskStatus;

public record TaskSimulationResult(
    List<TaskEvent> simulatedEvents,
    TaskEvent hypotheticalEvent,
    SemanticSnapshot<TaskState, NextMove<TaskStatus>, TaskAction> snapshot
) {
    public TaskSimulationResult {
        simulatedEvents = List.copyOf(simulatedEvents);
        if (hypotheticalEvent == null) {
            throw new IllegalArgumentException("hypotheticalEvent must not be null");
        }
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot must not be null");
        }
    }
}
