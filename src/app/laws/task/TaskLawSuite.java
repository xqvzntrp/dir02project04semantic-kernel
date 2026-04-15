package app.laws.task;

import app.laws.LawRunner;
import java.util.List;

public final class TaskLawSuite {
    public static void main(String[] args) {
        LawRunner.runAll(List.of(
            new TaskConvergesLaw(),
            new TaskNoStateDivergenceLaw(),
            new TaskRepairLaw(),
            new TaskNonConvergentForkLaw(),
            new TaskStableFlowLaw(),
            new TaskRepairMustHoldLaw(),
            new TaskEquivalentOutcomeLaw()
        ));
    }
}
