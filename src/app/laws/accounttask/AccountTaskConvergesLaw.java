package app.laws.accounttask;

import app.accounttask.assertions.AccountTaskHistoryAssertions;
import app.historycompare.AssertionResult;
import app.laws.Law;
import java.nio.file.Path;

public final class AccountTaskConvergesLaw implements Law {
    @Override
    public String name() {
        return "Account suspension and reactivation path must converge with stable completion";
    }

    @Override
    public AssertionResult run() throws Exception {
        return AccountTaskHistoryAssertions.assertConverges(
            Path.of("semantic-kernel", "samples", "eventchain", "accounttask", "accounttask-stable.verified"),
            Path.of("semantic-kernel", "samples", "eventchain", "accounttask", "accounttask-suspend-reactivate.verified")
        );
    }
}
