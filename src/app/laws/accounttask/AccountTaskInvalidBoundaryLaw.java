package app.laws.accounttask;

import app.accounttask.assertions.AccountTaskHistoryAssertions;
import app.historycompare.AssertionResult;
import app.laws.Law;
import java.nio.file.Path;

public final class AccountTaskInvalidBoundaryLaw implements Law {
    @Override
    public String name() {
        return "Account-task invalid history must fail before semantics";
    }

    @Override
    public AssertionResult run() throws Exception {
        return AccountTaskHistoryAssertions.assertRejected(
            Path.of("semantic-kernel", "samples", "eventchain", "invalid", "accounttask-invalid.verified")
        );
    }
}
