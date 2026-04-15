package app.historycompare;

import java.util.List;
import java.util.function.Function;

public final class HistoryComparator {
    private HistoryComparator() {
    }

    public static <E, S, A> HistoryComparisonResult<E, S, A> compare(
        List<E> left,
        List<E> right,
        Function<List<E>, HistoryAnalysisResult<S, A>> analyze
    ) {
        int commonPrefixLength = 0;
        int minLength = Math.min(left.size(), right.size());

        while (commonPrefixLength < minLength
            && left.get(commonPrefixLength).equals(right.get(commonPrefixLength))) {
            commonPrefixLength++;
        }

        HistoryRelation relation = relation(left.size(), right.size(), commonPrefixLength);
        int divergenceIndex = commonPrefixLength + 1;
        E leftDivergenceEvent = commonPrefixLength < left.size() ? left.get(commonPrefixLength) : null;
        E rightDivergenceEvent = commonPrefixLength < right.size() ? right.get(commonPrefixLength) : null;

        return new HistoryComparisonResult<>(
            commonPrefixLength,
            divergenceIndex,
            relation,
            leftDivergenceEvent,
            rightDivergenceEvent,
            analyze.apply(left),
            analyze.apply(right)
        );
    }

    private static HistoryRelation relation(int leftSize, int rightSize, int commonPrefixLength) {
        if (commonPrefixLength == leftSize && commonPrefixLength == rightSize) {
            return HistoryRelation.EQUAL;
        }
        if (commonPrefixLength == leftSize) {
            return HistoryRelation.LEFT_PREFIX;
        }
        if (commonPrefixLength == rightSize) {
            return HistoryRelation.RIGHT_PREFIX;
        }
        return HistoryRelation.DIVERGED;
    }
}
