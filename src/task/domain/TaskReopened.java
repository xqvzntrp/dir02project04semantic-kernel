package task.domain;

public record TaskReopened(String taskId) implements TaskEvent {
}
