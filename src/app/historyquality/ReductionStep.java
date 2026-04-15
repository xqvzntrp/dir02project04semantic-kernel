package app.historyquality;

public record ReductionStep(
    int removedOriginalIndex,
    String removedEvent,
    int lengthBefore,
    int lengthAfter
) {
    public ReductionStep {
        if (removedOriginalIndex < 1) {
            throw new IllegalArgumentException("removedOriginalIndex must be at least 1");
        }
        if (removedEvent == null || removedEvent.isBlank()) {
            throw new IllegalArgumentException("removedEvent must not be blank");
        }
        if (lengthBefore < 1) {
            throw new IllegalArgumentException("lengthBefore must be at least 1");
        }
        if (lengthAfter < 0) {
            throw new IllegalArgumentException("lengthAfter must be non-negative");
        }
        if (lengthAfter >= lengthBefore) {
            throw new IllegalArgumentException("lengthAfter must be smaller than lengthBefore");
        }
    }
}
