package app.historycompare;

public record TimelineEquivalencePoint(
    int index,
    TimelineSide side,
    HistoryEquivalence equivalence
) {
    public TimelineEquivalencePoint {
        if (index < 1) {
            throw new IllegalArgumentException("index must be at least 1");
        }
        if (side == null) {
            throw new IllegalArgumentException("side must not be null");
        }
        if (equivalence == null) {
            throw new IllegalArgumentException("equivalence must not be null");
        }
    }
}
