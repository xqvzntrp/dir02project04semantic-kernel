package app.laws.approval;

import app.approval.assertions.ApprovalHistoryAssertions;
import app.historycompare.AssertionResult;
import app.laws.Law;
import java.nio.file.Path;

public final class ApprovalInvalidBoundaryLaw implements Law {
    @Override
    public String name() {
        return "Approval invalid history must fail before semantics";
    }

    @Override
    public AssertionResult run() throws Exception {
        return ApprovalHistoryAssertions.assertRejected(
            Path.of("semantic-kernel", "samples", "eventchain", "invalid", "approval-invalid.verified")
        );
    }
}
