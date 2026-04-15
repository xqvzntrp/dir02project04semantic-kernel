package app.historycompare;

import java.util.List;

public record TimelineEquivalenceResult(
    int commonPrefixLength,
    List<TimelineEquivalencePoint> points,
    int firstStructuralDivergenceIndex,
    int firstSemanticMismatchIndex,
    int firstExtensionMismatchIndex
) {
    public TimelineEquivalenceResult {
        points = List.copyOf(points);
        if (commonPrefixLength < 0) {
            throw new IllegalArgumentException("commonPrefixLength must be non-negative");
        }
        if (firstStructuralDivergenceIndex < 1) {
            throw new IllegalArgumentException("firstStructuralDivergenceIndex must be at least 1");
        }
        if (firstSemanticMismatchIndex < -1) {
            throw new IllegalArgumentException("firstSemanticMismatchIndex must be -1 or greater");
        }
        if (firstExtensionMismatchIndex < -1) {
            throw new IllegalArgumentException("firstExtensionMismatchIndex must be -1 or greater");
        }
    }
}
