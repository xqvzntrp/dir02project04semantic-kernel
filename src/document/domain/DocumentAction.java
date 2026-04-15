package document.domain;

public record DocumentAction(String eventName, DocumentRuleState resultingState) {
}
