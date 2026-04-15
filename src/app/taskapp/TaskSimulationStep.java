package app.taskapp;

import semantic.kernel.ActionDescriptor;
import semantic.rules.NextMove;
import semantic.snapshot.SemanticSnapshot;
import task.domain.TaskAction;
import task.domain.TaskEvent;
import task.domain.TaskState;
import task.domain.TaskStatus;

public record TaskSimulationStep(
    int stepIndex,
    String requestedActionName,
    ActionDescriptor descriptor,
    TaskEvent event,
    SemanticSnapshot<TaskState, NextMove<TaskStatus>, TaskAction> snapshot
) {
    public TaskSimulationStep {
        if (stepIndex < 0) {
            throw new IllegalArgumentException("stepIndex must be non-negative");
        }
        if (requestedActionName == null || requestedActionName.isBlank()) {
            throw new IllegalArgumentException("requestedActionName must not be blank");
        }
        if (descriptor == null) {
            throw new IllegalArgumentException("descriptor must not be null");
        }
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot must not be null");
        }
    }
}
