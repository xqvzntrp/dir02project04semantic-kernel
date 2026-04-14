package approval.domain;

public record ApprovalAction(String eventName, ApprovalStatus resultingStatus) {
}
