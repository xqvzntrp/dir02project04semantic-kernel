package app.taskapp;

import integration.eventchain.VerifiedFieldEvent;
import java.util.List;
import task.domain.TaskEvent;

public sealed interface TaskLoadResult
    permits TaskLoadResult.EmptyHistory, TaskLoadResult.Loaded {

    record EmptyHistory(
        List<VerifiedFieldEvent> verifiedHistory,
        List<TaskEvent> decodedEvents
    ) implements TaskLoadResult {
    }

    record Loaded(TaskView view) implements TaskLoadResult {
        public Loaded {
            if (view == null) {
                throw new IllegalArgumentException("view must not be null");
            }
            if (view.snapshot() == null) {
                throw new IllegalArgumentException("loaded view must have a snapshot");
            }
        }
    }
}
