package app.laws.task;

import app.historycompare.AssertionResult;
import app.laws.Law;
import app.task.assertions.TaskPolicyAssertions;
import java.nio.file.Path;

public final class TaskRepairMustHoldLaw implements Law {
    @Override
    public String name() {
        return "Task repairable flow must hold once repaired";
    }

    @Override
    public AssertionResult run() throws Exception {
        return TaskPolicyAssertions.assertRepairIsEffective(
            Path.of("semantic-kernel", "samples", "eventchain", "real-task-repairable-parent.verified"),
            Path.of("semantic-kernel", "samples", "eventchain", "real-task-repairable.verified")
        );
    }
}
