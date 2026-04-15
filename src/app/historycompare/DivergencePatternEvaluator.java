package app.historycompare;

import java.util.ArrayList;
import java.util.List;

public final class DivergencePatternEvaluator {
    private DivergencePatternEvaluator() {
    }

    public static <E> DivergencePatternResult evaluate(
        TimelineEquivalenceResult timeline,
        List<EventAttributionPoint<E>> attribution
    ) {
        if (timeline.commonPrefixLength() == timeline.points().size()
            && timeline.firstSemanticMismatchIndex() < 0
            && timeline.firstExtensionMismatchIndex() < 0) {
            return new DivergencePatternResult(DivergencePattern.NO_DIVERGENCE, List.of());
        }

        boolean inDivergence = false;
        int cycles = 0;
        List<Integer> repairIndices = new ArrayList<>();

        for (EventAttributionPoint<E> point : attribution) {
            switch (point.effect()) {
                case INTRODUCES_STATE_DIFFERENCE,
                    INTRODUCES_ACTION_DIFFERENCE,
                    INTRODUCES_STATE_AND_ACTION_DIFFERENCE -> {
                    if (!inDivergence) {
                        inDivergence = true;
                    }
                }
                case REPAIRS_STATE_DIFFERENCE,
                    REPAIRS_ACTION_DIFFERENCE,
                    REPAIRS_STATE_AND_ACTION_DIFFERENCE -> {
                    if (inDivergence) {
                        inDivergence = false;
                        cycles++;
                        repairIndices.add(point.index());
                    }
                }
                default -> {
                }
            }
        }

        DivergencePattern pattern;
        if (cycles == 0) {
            pattern = DivergencePattern.PERMANENT_DIVERGENCE;
        } else if (cycles == 1 && !inDivergence) {
            pattern = DivergencePattern.TEMPORARY_DIVERGENCE;
        } else {
            pattern = DivergencePattern.MULTIPLE_DIVERGENCE_CYCLES;
        }

        return new DivergencePatternResult(pattern, repairIndices);
    }
}
