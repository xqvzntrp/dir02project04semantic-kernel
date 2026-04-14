package task.domain;

public record TaskAction(String eventName, TaskStatus resultingStatus) {
}
