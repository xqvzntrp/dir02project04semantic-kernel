package app.laws.approval;

import app.approval.assertions.ApprovalHistoryAssertions;
import app.historycompare.AssertionResult;
import app.laws.Law;
import java.nio.file.Path;

public final class ApprovalNonConvergentBranchLaw implements Law {
    @Override
    public String name() {
        return "Approval terminal branches must not converge semantically";
    }

    @Override
    public AssertionResult run() throws Exception {
        AssertionResult converges = ApprovalHistoryAssertions.assertConverges(
            Path.of("semantic-kernel", "samples", "eventchain", "approval-divergent-parent.verified"),
            Path.of("semantic-kernel", "samples", "eventchain", "approval-divergent-fork.verified")
        );
        if (!converges.pass()) {
            return new AssertionResult(true, converges.message());
        }
        return new AssertionResult(false, "terminal branches unexpectedly converged");
    }
}
