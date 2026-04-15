package app.laws.task;

import app.historycompare.AssertionResult;
import app.laws.Law;
import app.task.assertions.TaskPolicyAssertions;
import java.nio.file.Path;

public final class TaskEquivalentOutcomeLaw implements Law {
    @Override
    public String name() {
        return "Task repaired flow must match baseline meaning and actions";
    }

    @Override
    public AssertionResult run() throws Exception {
        return TaskPolicyAssertions.assertEquivalentOutcomeAndActions(
            Path.of("semantic-kernel", "samples", "eventchain", "real-task-repairable-parent.verified"),
            Path.of("semantic-kernel", "samples", "eventchain", "real-task-repairable.verified")
        );
    }
}
