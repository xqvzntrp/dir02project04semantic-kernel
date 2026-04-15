package app.historycompare;

import java.util.List;

public record DivergencePatternResult(
    DivergencePattern pattern,
    List<Integer> repairIndices
) {
    public DivergencePatternResult {
        repairIndices = List.copyOf(repairIndices);
        if (pattern == null) {
            throw new IllegalArgumentException("pattern must not be null");
        }
    }
}
