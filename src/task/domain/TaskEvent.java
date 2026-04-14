package task.domain;

public sealed interface TaskEvent permits TaskCreated, TaskStarted, TaskCompleted {

    String taskId();
}
