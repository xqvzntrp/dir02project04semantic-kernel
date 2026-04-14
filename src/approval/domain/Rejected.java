package approval.domain;

public record Rejected(String approvalId) implements ApprovalEvent {
}
