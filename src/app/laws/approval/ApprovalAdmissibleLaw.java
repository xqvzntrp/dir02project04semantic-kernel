package app.laws.approval;

import app.approval.assertions.ApprovalHistoryAssertions;
import app.historycompare.AssertionResult;
import app.laws.Law;
import java.nio.file.Path;

public final class ApprovalAdmissibleLaw implements Law {
    @Override
    public String name() {
        return "Approval stable history must be admissible";
    }

    @Override
    public AssertionResult run() throws Exception {
        return ApprovalHistoryAssertions.assertAdmissible(
            Path.of("semantic-kernel", "samples", "eventchain", "approval-stable.verified")
        );
    }
}
