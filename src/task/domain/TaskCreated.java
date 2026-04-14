package task.domain;

public record TaskCreated(String taskId) implements TaskEvent {
}
