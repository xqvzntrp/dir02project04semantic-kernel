package app.laws.task;

import app.historycompare.AssertionResult;
import app.laws.Law;
import app.task.assertions.TaskHistoryAssertions;
import java.nio.file.Path;

public final class TaskConvergesLaw implements Law {
    @Override
    public String name() {
        return "Task alternate completion paths must converge";
    }

    @Override
    public AssertionResult run() throws Exception {
        return TaskHistoryAssertions.assertConverges(
            Path.of("semantic-kernel", "samples", "eventchain", "task-converge-a.verified"),
            Path.of("semantic-kernel", "samples", "eventchain", "task-converge-b.verified")
        );
    }
}
