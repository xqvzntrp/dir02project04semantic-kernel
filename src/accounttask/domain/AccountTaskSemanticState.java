package accounttask.domain;

public record AccountTaskSemanticState(
    String accountId,
    String taskId,
    AccountTaskAccountStatus accountStatus,
    AccountTaskWorkStatus workStatus
) {
}
