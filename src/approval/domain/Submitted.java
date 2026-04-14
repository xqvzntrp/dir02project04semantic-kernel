package approval.domain;

public record Submitted(String approvalId) implements ApprovalEvent {
}
