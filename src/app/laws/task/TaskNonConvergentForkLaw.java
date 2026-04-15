package app.laws.task;

import app.historycompare.AssertionResult;
import app.laws.Law;
import app.task.assertions.TaskHistoryAssertions;
import java.nio.file.Path;

public final class TaskNonConvergentForkLaw implements Law {
    @Override
    public String name() {
        return "Task fork sample must not converge back to parent";
    }

    @Override
    public AssertionResult run() throws Exception {
        AssertionResult result = TaskHistoryAssertions.assertConverges(
            Path.of("semantic-kernel", "samples", "eventchain", "task-nonconvergent-parent.verified"),
            Path.of("semantic-kernel", "samples", "eventchain", "task-nonconvergent-fork.verified")
        );
        if (result.pass()) {
            return new AssertionResult(false, "unexpected convergence");
        }
        return new AssertionResult(true, "correctly did not converge");
    }

    @Override
    public boolean expectedNegative() {
        return true;
    }
}
