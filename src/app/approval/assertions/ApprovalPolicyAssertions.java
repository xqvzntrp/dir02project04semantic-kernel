package app.approval.assertions;

import app.historycompare.AssertionResult;
import java.io.IOException;
import java.nio.file.Path;

public final class ApprovalPolicyAssertions {
    private ApprovalPolicyAssertions() {
    }

    public static AssertionResult assertRepairIsEffective(Path left, Path right) throws IOException {
        AssertionResult exists = ApprovalHistoryAssertions.assertRepairExists(left, right);
        if (!exists.pass()) {
            return new AssertionResult(false, "no repair exists to evaluate effectiveness");
        }

        AssertionResult holds = ApprovalHistoryAssertions.assertRepairHolds(left, right);
        if (holds.pass()) {
            return new AssertionResult(true, "repair exists and holds");
        }

        return new AssertionResult(false, "repair exists but does not hold");
    }

    public static AssertionResult assertRepairUnavailable(Path left, Path right) throws IOException {
        AssertionResult exists = ApprovalHistoryAssertions.assertRepairExists(left, right);
        if (exists.pass()) {
            return new AssertionResult(false, "repair exists, so repair is not unavailable");
        }
        return new AssertionResult(true, "repair is unavailable in this approval flow");
    }

    public static AssertionResult assertStableFlow(Path left, Path right) throws IOException {
        AssertionResult divergence = ApprovalHistoryAssertions.assertNoStateDivergence(left, right);
        if (divergence.pass()) {
            return new AssertionResult(true, "stable flow never diverges in state");
        }
        return new AssertionResult(false, "stable flow diverged in state");
    }

    public static AssertionResult assertEquivalentOutcomeAndActions(Path left, Path right) throws IOException {
        return switch (ApprovalHistoryAssertions.equivalence(left, right)) {
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
