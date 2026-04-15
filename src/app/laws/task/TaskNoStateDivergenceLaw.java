package app.laws.task;

import app.historycompare.AssertionResult;
import app.laws.Law;
import app.task.assertions.TaskHistoryAssertions;
import java.nio.file.Path;

public final class TaskNoStateDivergenceLaw implements Law {
    @Override
    public String name() {
        return "Task stable scenario must not diverge in state";
    }

    @Override
    public AssertionResult run() throws Exception {
        return TaskHistoryAssertions.assertNoStateDivergence(
            Path.of("semantic-kernel", "samples", "eventchain", "task-stable.verified"),
            Path.of("semantic-kernel", "samples", "eventchain", "task-stable.verified")
        );
    }
}
