package app.laws.approval;

import app.laws.LawRunner;
import java.util.List;

public final class ApprovalLawSuite {
    public static void main(String[] args) {
        LawRunner.runAll(List.of(
            new ApprovalAdmissibleLaw(),
            new ApprovalStableFlowLaw(),
            new ApprovalInvalidBoundaryLaw(),
            new ApprovalRepairUnavailableLaw(),
            new ApprovalNonConvergentBranchLaw()
        ));
    }
}
