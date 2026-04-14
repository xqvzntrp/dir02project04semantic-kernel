package approval.domain;

public sealed interface ApprovalEvent permits Submitted, Approved, Rejected {

    String approvalId();
}
