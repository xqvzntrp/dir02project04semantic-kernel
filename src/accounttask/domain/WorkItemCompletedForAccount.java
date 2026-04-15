package accounttask.domain;

public record WorkItemCompletedForAccount(String accountId, String taskId) implements AccountTaskEvent {
}
