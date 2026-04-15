package app.historycompare;

import java.util.List;

public record HistorySnapshotSummary<S, A>(
    S state,
    List<A> actions
) {
    public HistorySnapshotSummary {
        actions = List.copyOf(actions);
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
    }
}
