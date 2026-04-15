package app.historyquality;

import app.historycompare.HistorySnapshotSummary;
import java.util.List;

public record GreedyReductionResult<E, S, A>(
    int originalLength,
    int reducedLength,
    List<Integer> removedOriginalIndices,
    List<ReductionStep> reductionSteps,
    List<E> reducedHistory,
    HistorySnapshotSummary<S, A> semanticSummary,
    boolean locallyMinimal
) {
    public GreedyReductionResult {
        removedOriginalIndices = List.copyOf(removedOriginalIndices);
        reductionSteps = List.copyOf(reductionSteps);
        reducedHistory = List.copyOf(reducedHistory);
        if (originalLength < 0) {
            throw new IllegalArgumentException("originalLength must be non-negative");
        }
        if (reducedLength < 0) {
            throw new IllegalArgumentException("reducedLength must be non-negative");
        }
        if (reducedLength > originalLength) {
            throw new IllegalArgumentException("reducedLength must not exceed originalLength");
        }
        if (reducedLength != reducedHistory.size()) {
            throw new IllegalArgumentException("reducedLength must match reducedHistory size");
        }
        if (semanticSummary == null) {
            throw new IllegalArgumentException("semanticSummary must not be null");
        }
    }
}
