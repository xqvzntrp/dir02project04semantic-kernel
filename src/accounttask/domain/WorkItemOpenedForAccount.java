package accounttask.domain;

public record WorkItemOpenedForAccount(String accountId, String taskId) implements AccountTaskEvent {
}
