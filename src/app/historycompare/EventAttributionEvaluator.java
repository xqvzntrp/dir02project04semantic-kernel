package app.historycompare;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class EventAttributionEvaluator {
    private record EquivalenceSignals(
        HistoryEquivalence equivalence,
        boolean stateEqual,
        boolean actionsEqual
    ) {
    }

    private EventAttributionEvaluator() {
    }

    public static <E, S, A> List<EventAttributionPoint<E>> evaluate(
        List<E> left,
        List<E> right,
        Function<List<E>, HistoryAnalysisResult<S, A>> analyze
    ) {
        TimelineEquivalenceResult timeline = TimelineEquivalenceEvaluator.evaluate(left, right, analyze);
        List<EventAttributionPoint<E>> points = new ArrayList<>();

        EquivalenceSignals before = new EquivalenceSignals(HistoryEquivalence.EXACT, true, true);
        for (TimelineEquivalencePoint point : timeline.points()) {
            E event = eventAt(left, right, point);
            EquivalenceSignals after = signalsAt(left, right, analyze, point, timeline.commonPrefixLength());
            points.add(new EventAttributionPoint<>(
                point.index(),
                point.side(),
                event,
                before.equivalence(),
                after.equivalence(),
                classify(before, after)
            ));
            before = after;
        }

        return List.copyOf(points);
    }

    private static <E> E eventAt(List<E> left, List<E> right, TimelineEquivalencePoint point) {
        return switch (point.side()) {
            case BOTH -> {
                int index = point.index() - 1;
                yield index < right.size() ? right.get(index) : left.get(index);
            }
            case LEFT_ONLY -> left.get(point.index() - 1);
            case RIGHT_ONLY -> right.get(point.index() - 1);
        };
    }

    private static <E, S, A> EquivalenceSignals signalsAt(
        List<E> left,
        List<E> right,
        java.util.function.Function<List<E>, HistoryAnalysisResult<S, A>> analyze,
        TimelineEquivalencePoint point,
        int commonPrefixLength
    ) {
        HistoryAnalysisResult<S, A> leftAnalysis;
        HistoryAnalysisResult<S, A> rightAnalysis;

        switch (point.side()) {
            case BOTH -> {
                leftAnalysis = analyze.apply(left.subList(0, point.index()));
                rightAnalysis = analyze.apply(right.subList(0, point.index()));
            }
            case LEFT_ONLY -> {
                leftAnalysis = analyze.apply(left.subList(0, point.index()));
                rightAnalysis = baselineAnalysis(left, analyze, commonPrefixLength);
            }
            case RIGHT_ONLY -> {
                leftAnalysis = baselineAnalysis(right, analyze, commonPrefixLength);
                rightAnalysis = analyze.apply(right.subList(0, point.index()));
            }
            default -> throw new IllegalStateException("Unsupported timeline side: " + point.side());
        }

        if (leftAnalysis instanceof HistoryAnalysisResult.Success<S, A> leftSuccess
            && rightAnalysis instanceof HistoryAnalysisResult.Success<S, A> rightSuccess) {
            HistorySnapshotSummary<S, A> leftSummary = leftSuccess.summary();
            HistorySnapshotSummary<S, A> rightSummary = rightSuccess.summary();
            return new EquivalenceSignals(
                point.equivalence(),
                HistoryEquivalenceEvaluator.sameState(leftSummary, rightSummary),
                HistoryEquivalenceEvaluator.sameActions(leftSummary, rightSummary)
            );
        }

        return new EquivalenceSignals(point.equivalence(), false, false);
    }

    private static <E, S, A> HistoryAnalysisResult<S, A> baselineAnalysis(
        List<E> reference,
        java.util.function.Function<List<E>, HistoryAnalysisResult<S, A>> analyze,
        int commonPrefixLength
    ) {
        if (commonPrefixLength <= 0) {
            return new HistoryAnalysisResult.Failure<>("no shared baseline");
        }
        return analyze.apply(reference.subList(0, commonPrefixLength));
    }

    private static AttributionEffect classify(EquivalenceSignals before, EquivalenceSignals after) {
        boolean stateIntroduced = before.stateEqual() && !after.stateEqual();
        boolean stateRepaired = !before.stateEqual() && after.stateEqual();
        boolean actionIntroduced = before.actionsEqual() && !after.actionsEqual();
        boolean actionRepaired = !before.actionsEqual() && after.actionsEqual();
        boolean stateDifferentNow = !after.stateEqual();
        boolean actionDifferentNow = !after.actionsEqual();

        if (stateIntroduced || actionIntroduced) {
            if (stateIntroduced && actionIntroduced) {
                return AttributionEffect.INTRODUCES_STATE_AND_ACTION_DIFFERENCE;
            }
            if (stateIntroduced) {
                return AttributionEffect.INTRODUCES_STATE_DIFFERENCE;
            }
            return AttributionEffect.INTRODUCES_ACTION_DIFFERENCE;
        }

        if (stateRepaired || actionRepaired) {
            if (stateRepaired && actionRepaired) {
                return AttributionEffect.REPAIRS_STATE_AND_ACTION_DIFFERENCE;
            }
            if (stateRepaired) {
                return AttributionEffect.REPAIRS_STATE_DIFFERENCE;
            }
            return AttributionEffect.REPAIRS_ACTION_DIFFERENCE;
        }

        if (stateDifferentNow || actionDifferentNow) {
            return AttributionEffect.PRESERVES_DIFFERENCE;
        }
        return AttributionEffect.NO_SEMANTIC_CHANGE;
    }
}
