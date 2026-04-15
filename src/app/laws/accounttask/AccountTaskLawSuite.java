package app.laws.accounttask;

import app.laws.LawRunner;
import java.util.List;

public final class AccountTaskLawSuite {
    public static void main(String[] args) {
        LawRunner.runAll(List.of(
            new AccountTaskAdmissibleLaw(),
            new AccountTaskInvalidBoundaryLaw(),
            new AccountTaskConvergesLaw(),
            new AccountTaskOperabilityDependsOnAccountLaw()
        ));
    }
}
