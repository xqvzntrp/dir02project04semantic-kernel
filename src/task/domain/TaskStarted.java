package task.domain;

public record TaskStarted(String taskId) implements TaskEvent {
}
