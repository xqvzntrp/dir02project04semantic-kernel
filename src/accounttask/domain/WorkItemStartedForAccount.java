package accounttask.domain;

public record WorkItemStartedForAccount(String accountId, String taskId) implements AccountTaskEvent {
}
