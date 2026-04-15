package app.laws.accounttask;

import app.accounttask.assertions.AccountTaskHistoryAssertions;
import app.historycompare.AssertionResult;
import app.laws.Law;
import java.nio.file.Path;

public final class AccountTaskOperabilityDependsOnAccountLaw implements Law {
    @Override
    public String name() {
        return "Account status must change account-task operability";
    }

    @Override
    public AssertionResult run() throws Exception {
        AssertionResult result = AccountTaskHistoryAssertions.assertActionsEquivalent(
            Path.of("semantic-kernel", "samples", "eventchain", "accounttask", "accounttask-operable-parent.verified"),
            Path.of("semantic-kernel", "samples", "eventchain", "accounttask", "accounttask-operable-fork.verified")
        );
        if (result.pass()) {
            return new AssertionResult(false, "account status did not change the action surface");
        }
        return new AssertionResult(true, "account status correctly changes operability");
    }

    @Override
    public boolean expectedNegative() {
        return true;
    }
}
