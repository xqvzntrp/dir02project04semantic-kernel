package task.domain;

public sealed interface TaskEvent permits TaskCreated, TaskStarted, TaskCompleted, TaskReopened {

    String taskId();
}
