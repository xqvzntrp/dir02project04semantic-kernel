package app.historyquality;

import app.historycompare.HistoryAnalysisResult;
import app.historycompare.HistoryEquivalenceEvaluator;
import app.historycompare.HistorySnapshotSummary;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class GreedyHistoryReducer {
    private GreedyHistoryReducer() {
    }

    /**
     * Greedy reduction scans candidates left-to-right and restarts from the beginning
     * after each successful deletion. Only admissibility rejections should be encoded
     * as analysis failures; unexpected runtime defects should surface to the caller.
     */
    public static <E, S, A> GreedyReductionResult<E, S, A> reduce(
        List<E> history,
        Function<List<E>, HistoryAnalysisResult<S, A>> analyze
    ) {
        HistorySnapshotSummary<S, A> targetSummary = analyzeSuccess(history, analyze, "input history");
        List<IndexedEvent<E>> current = indexed(history);
        List<Integer> removedOriginalIndices = new ArrayList<>();
        List<ReductionStep> reductionSteps = new ArrayList<>();

        boolean removedSomething;
        do {
            removedSomething = false;
            for (int i = 0; i < current.size(); i++) {
                List<IndexedEvent<E>> candidate = removeIndex(current, i);
                HistoryAnalysisResult<S, A> candidateAnalysis = analyze.apply(events(candidate));
                if (!(candidateAnalysis instanceof HistoryAnalysisResult.Success<S, A> success)) {
                    continue;
                }
                if (!sameSummary(targetSummary, success.summary())) {
                    continue;
                }

                int removedOriginalIndex = current.get(i).originalIndex();
                reductionSteps.add(new ReductionStep(
                    removedOriginalIndex,
                    String.valueOf(current.get(i).event()),
                    current.size(),
                    candidate.size()
                ));
                removedOriginalIndices.add(removedOriginalIndex);
                current = candidate;
                removedSomething = true;
                break;
            }
        } while (removedSomething);

        return new GreedyReductionResult<>(
            history.size(),
            current.size(),
            removedOriginalIndices,
            reductionSteps,
            events(current),
            targetSummary,
            history.size() == current.size()
        );
    }

    private static <E, S, A> HistorySnapshotSummary<S, A> analyzeSuccess(
        List<E> history,
        Function<List<E>, HistoryAnalysisResult<S, A>> analyze,
        String label
    ) {
        HistoryAnalysisResult<S, A> analysis = analyze.apply(history);
        if (analysis instanceof HistoryAnalysisResult.Success<S, A> success) {
            return success.summary();
        }
        HistoryAnalysisResult.Failure<S, A> failure = (HistoryAnalysisResult.Failure<S, A>) analysis;
        throw new IllegalStateException(label + " is not admissible: " + failure.message());
    }

    private static <S, A> boolean sameSummary(
        HistorySnapshotSummary<S, A> left,
        HistorySnapshotSummary<S, A> right
    ) {
        return HistoryEquivalenceEvaluator.sameState(left, right)
            && HistoryEquivalenceEvaluator.sameActions(left, right);
    }

    private static <E> List<IndexedEvent<E>> indexed(List<E> history) {
        List<IndexedEvent<E>> indexed = new ArrayList<>(history.size());
        for (int i = 0; i < history.size(); i++) {
            indexed.add(new IndexedEvent<>(i + 1, history.get(i)));
        }
        return List.copyOf(indexed);
    }

    private static <E> List<IndexedEvent<E>> removeIndex(List<IndexedEvent<E>> current, int removalIndex) {
        List<IndexedEvent<E>> candidate = new ArrayList<>(current.size() - 1);
        for (int i = 0; i < current.size(); i++) {
            if (i != removalIndex) {
                candidate.add(current.get(i));
            }
        }
        return List.copyOf(candidate);
    }

    private static <E> List<E> events(List<IndexedEvent<E>> indexed) {
        return indexed.stream()
            .map(IndexedEvent::event)
            .toList();
    }

    private record IndexedEvent<E>(int originalIndex, E event) {
    }
}
