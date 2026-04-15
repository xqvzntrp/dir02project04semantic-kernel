package app.laws.accounttask;

import app.accounttask.assertions.AccountTaskHistoryAssertions;
import app.historycompare.AssertionResult;
import app.laws.Law;
import java.nio.file.Path;

public final class AccountTaskAdmissibleLaw implements Law {
    @Override
    public String name() {
        return "Account-task stable history must be admissible";
    }

    @Override
    public AssertionResult run() throws Exception {
        return AccountTaskHistoryAssertions.assertAdmissible(
            Path.of("semantic-kernel", "samples", "eventchain", "accounttask", "accounttask-stable.verified")
        );
    }
}
