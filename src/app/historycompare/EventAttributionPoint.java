package app.historycompare;

public record EventAttributionPoint<E>(
    int index,
    TimelineSide side,
    E event,
    HistoryEquivalence before,
    HistoryEquivalence after,
    AttributionEffect effect
) {
    public EventAttributionPoint {
        if (index < 1) {
            throw new IllegalArgumentException("index must be at least 1");
        }
        if (side == null) {
            throw new IllegalArgumentException("side must not be null");
        }
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (before == null || after == null) {
            throw new IllegalArgumentException("equivalence values must not be null");
        }
        if (effect == null) {
            throw new IllegalArgumentException("effect must not be null");
        }
    }
}
