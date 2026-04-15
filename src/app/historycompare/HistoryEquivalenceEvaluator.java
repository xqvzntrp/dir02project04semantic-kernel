package app.historycompare;

import java.util.Objects;
import java.util.stream.Collectors;

public final class HistoryEquivalenceEvaluator {
    private HistoryEquivalenceEvaluator() {
    }

    public static <S, A> HistoryEquivalence evaluate(
        HistorySnapshotSummary<S, A> left,
        HistorySnapshotSummary<S, A> right
    ) {
        boolean sameState = Objects.equals(left.state(), right.state());
        boolean sameActions = normalizedActions(left).equals(normalizedActions(right));

        if (sameState && sameActions) {
            return HistoryEquivalence.STATE_AND_ACTIONS_EQUAL;
        }
        if (sameState) {
            return HistoryEquivalence.STATE_EQUAL_ONLY;
        }
        if (sameActions) {
            return HistoryEquivalence.ACTIONS_EQUAL_ONLY;
        }
        return HistoryEquivalence.NONE;
    }

    public static <S, A> boolean sameState(HistorySnapshotSummary<S, A> left, HistorySnapshotSummary<S, A> right) {
        return Objects.equals(left.state(), right.state());
    }

    public static <S, A> boolean sameActions(HistorySnapshotSummary<S, A> left, HistorySnapshotSummary<S, A> right) {
        return normalizedActions(left).equals(normalizedActions(right));
    }

    private static <S, A> java.util.List<String> normalizedActions(HistorySnapshotSummary<S, A> summary) {
        return summary.actions().stream()
            .map(String::valueOf)
            .sorted()
            .collect(Collectors.toList());
    }
}
