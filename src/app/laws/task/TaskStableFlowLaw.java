package app.laws.task;

import app.historycompare.AssertionResult;
import app.laws.Law;
import app.task.assertions.TaskPolicyAssertions;
import java.nio.file.Path;

public final class TaskStableFlowLaw implements Law {
    @Override
    public String name() {
        return "Task stable flow must never diverge";
    }

    @Override
    public AssertionResult run() throws Exception {
        return TaskPolicyAssertions.assertStableFlow(
            Path.of("semantic-kernel", "samples", "eventchain", "real-task-stable.verified"),
            Path.of("semantic-kernel", "samples", "eventchain", "real-task-stable.verified")
        );
    }
}
