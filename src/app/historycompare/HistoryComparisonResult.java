package app.historycompare;

public record HistoryComparisonResult<E, S, A>(
    int commonPrefixLength,
    int divergenceIndex,
    HistoryRelation relation,
    E leftDivergenceEvent,
    E rightDivergenceEvent,
    HistoryAnalysisResult<S, A> leftAnalysis,
    HistoryAnalysisResult<S, A> rightAnalysis
) {
    public HistoryComparisonResult {
        if (commonPrefixLength < 0) {
            throw new IllegalArgumentException("commonPrefixLength must be non-negative");
        }
        if (divergenceIndex < 1) {
            throw new IllegalArgumentException("divergenceIndex must be at least 1");
        }
        if (relation == null) {
            throw new IllegalArgumentException("relation must not be null");
        }
        if (leftAnalysis == null || rightAnalysis == null) {
            throw new IllegalArgumentException("analyses must not be null");
        }
    }
}
