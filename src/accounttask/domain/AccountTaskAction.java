package accounttask.domain;

public record AccountTaskAction(String eventName, AccountTaskRuleState resultingState) {
}
