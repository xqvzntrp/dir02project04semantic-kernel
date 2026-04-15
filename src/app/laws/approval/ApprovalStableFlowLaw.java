package app.laws.approval;

import app.approval.assertions.ApprovalPolicyAssertions;
import app.historycompare.AssertionResult;
import app.laws.Law;
import java.nio.file.Path;

public final class ApprovalStableFlowLaw implements Law {
    @Override
    public String name() {
        return "Approval stable history must remain stable";
    }

    @Override
    public AssertionResult run() throws Exception {
        return ApprovalPolicyAssertions.assertStableFlow(
            Path.of("semantic-kernel", "samples", "eventchain", "approval-stable.verified"),
            Path.of("semantic-kernel", "samples", "eventchain", "approval-stable.verified")
        );
    }
}
