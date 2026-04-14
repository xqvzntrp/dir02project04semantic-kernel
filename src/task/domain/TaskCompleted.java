package task.domain;

public record TaskCompleted(String taskId) implements TaskEvent {
}
