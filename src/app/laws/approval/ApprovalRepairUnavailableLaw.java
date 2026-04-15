package app.laws.approval;

import app.approval.assertions.ApprovalPolicyAssertions;
import app.historycompare.AssertionResult;
import app.laws.Law;
import java.nio.file.Path;

public final class ApprovalRepairUnavailableLaw implements Law {
    @Override
    public String name() {
        return "Approval divergent branch must expose repair as unavailable";
    }

    @Override
    public AssertionResult run() throws Exception {
        return ApprovalPolicyAssertions.assertRepairUnavailable(
            Path.of("semantic-kernel", "samples", "eventchain", "approval-divergent-parent.verified"),
            Path.of("semantic-kernel", "samples", "eventchain", "approval-divergent-fork.verified")
        );
    }
}
