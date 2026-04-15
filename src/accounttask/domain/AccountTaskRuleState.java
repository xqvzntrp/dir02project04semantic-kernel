package accounttask.domain;

public record AccountTaskRuleState(
    AccountTaskAccountStatus accountStatus,
    AccountTaskWorkStatus workStatus
) {
}
