package app.laws.task;

import app.historycompare.AssertionResult;
import app.laws.Law;
import app.task.assertions.TaskHistoryAssertions;
import java.nio.file.Path;

public final class TaskRepairLaw implements Law {
    @Override
    public String name() {
        return "Task repairable scenario must exhibit repair";
    }

    @Override
    public AssertionResult run() throws Exception {
        return TaskHistoryAssertions.assertRepairExists(
            Path.of("semantic-kernel", "samples", "eventchain", "task-repairable.verified")
        );
    }
}
