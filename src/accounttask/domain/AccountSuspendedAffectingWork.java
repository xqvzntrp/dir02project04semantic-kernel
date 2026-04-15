package accounttask.domain;

public record AccountSuspendedAffectingWork(String accountId, String taskId) implements AccountTaskEvent {
}
