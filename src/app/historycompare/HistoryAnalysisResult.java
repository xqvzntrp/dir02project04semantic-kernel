package app.historycompare;

public sealed interface HistoryAnalysisResult<S, A>
    permits HistoryAnalysisResult.Success, HistoryAnalysisResult.Failure {

    record Success<S, A>(HistorySnapshotSummary<S, A> summary) implements HistoryAnalysisResult<S, A> {
        public Success {
            if (summary == null) {
                throw new IllegalArgumentException("summary must not be null");
            }
        }
    }

    record Failure<S, A>(String message) implements HistoryAnalysisResult<S, A> {
        public Failure {
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("message must not be blank");
            }
        }
    }
}
