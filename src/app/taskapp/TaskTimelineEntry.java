package app.taskapp;

import semantic.rules.NextMove;
import semantic.snapshot.SemanticSnapshot;
import task.domain.TaskAction;
import task.domain.TaskEvent;
import task.domain.TaskState;
import task.domain.TaskStatus;

public record TaskTimelineEntry(
    int eventIndex,
    TaskEvent event,
    SemanticSnapshot<TaskState, NextMove<TaskStatus>, TaskAction> snapshot
) {
    public TaskTimelineEntry {
        if (eventIndex < 0) {
            throw new IllegalArgumentException("eventIndex must be non-negative");
        }
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot must not be null");
        }
    }
}
