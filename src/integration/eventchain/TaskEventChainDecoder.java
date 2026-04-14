package integration.eventchain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import task.domain.TaskCompleted;
import task.domain.TaskCreated;
import task.domain.TaskEvent;
import task.domain.TaskReopened;
import task.domain.TaskStarted;

public final class TaskEventChainDecoder {

    public List<TaskEvent> decode(List<VerifiedFieldEvent> verifiedEvents) {
        List<TaskEvent> events = verifiedEvents.stream()
            .sorted(Comparator.comparingLong(VerifiedFieldEvent::sequence))
            .map(this::decodeEvent)
            .toList();

        return List.copyOf(new ArrayList<>(events));
    }

    private TaskEvent decodeEvent(VerifiedFieldEvent verifiedEvent) {
        String taskId = requiredField(verifiedEvent, "taskId");

        return switch (verifiedEvent.eventType()) {
            case "TaskCreated" -> new TaskCreated(taskId);
            case "TaskStarted" -> new TaskStarted(taskId);
            case "TaskCompleted" -> new TaskCompleted(taskId);
            case "TaskReopened" -> new TaskReopened(taskId);
            default -> throw new IllegalArgumentException(
                "unsupported task event type: " + verifiedEvent.eventType());
        };
    }

    private String requiredField(VerifiedFieldEvent verifiedEvent, String fieldName) {
        String value = verifiedEvent.fields().get(fieldName);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                "missing required field '" + fieldName + "' for event " + verifiedEvent.eventType());
        }
        return value;
    }
}
