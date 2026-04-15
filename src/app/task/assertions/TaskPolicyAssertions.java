package app.task.assertions;

import app.historycompare.AssertionResult;
import java.io.IOException;
import java.nio.file.Path;

public final class TaskPolicyAssertions {
    private TaskPolicyAssertions() {
    }

    public static AssertionResult assertRepairIsEffective(Path left, Path right) throws IOException {
        AssertionResult exists = TaskHistoryAssertions.assertRepairExists(left, right);
        if (!exists.pass()) {
            return new AssertionResult(false, "no repair exists to evaluate effectiveness");
        }

        AssertionResult holds = TaskHistoryAssertions.assertRepairHolds(left, right);
        if (holds.pass()) {
            return new AssertionResult(true, "repair exists and holds");
        }

        return new AssertionResult(false, "repair exists but does not hold");
    }

    public static AssertionResult assertStableFlow(Path left, Path right) throws IOException {
        AssertionResult divergence = TaskHistoryAssertions.assertNoStateDivergence(left, right);
        if (divergence.pass()) {
            return new AssertionResult(true, "stable flow never diverges in state");
        }
        return new AssertionResult(false, "stable flow diverged in state");
    }

    public static AssertionResult assertEquivalentOutcomeAndActions(Path left, Path right) throws IOException {
        return switch (TaskHistoryAssertions.equivalence(left, right)) {
            case EXACT, STATE_AND_ACTIONS_EQUAL ->
                new AssertionResult(true, "outcomes converge and action surfaces agree");
            case ACTIONS_EQUAL_ONLY ->
                new AssertionResult(false, "action surfaces agree but states differ");
            case STATE_EQUAL_ONLY ->
                new AssertionResult(false, "states agree but action surfaces differ");
            case NONE ->
                new AssertionResult(false, "neither state nor actions match");
        };
    }
}
