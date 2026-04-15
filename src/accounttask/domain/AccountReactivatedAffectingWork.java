package accounttask.domain;

public record AccountReactivatedAffectingWork(String accountId, String taskId) implements AccountTaskEvent {
}
