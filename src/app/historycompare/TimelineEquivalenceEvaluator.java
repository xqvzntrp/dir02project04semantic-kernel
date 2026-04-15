package app.historycompare;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class TimelineEquivalenceEvaluator {
    private TimelineEquivalenceEvaluator() {
    }

    public static <E, S, A> TimelineEquivalenceResult evaluate(
        List<E> left,
        List<E> right,
        Function<List<E>, HistoryAnalysisResult<S, A>> analyze
    ) {
        int minLength = Math.min(left.size(), right.size());
        List<TimelineEquivalencePoint> points = new ArrayList<>();

        int commonPrefixLength = 0;
        for (int i = 0; i < minLength; i++) {
            if (left.get(i).equals(right.get(i))) {
                commonPrefixLength++;
            } else {
                break;
            }
        }

        int firstSemanticMismatchIndex = -1;
        int firstExtensionMismatchIndex = -1;

        for (int i = 0; i < minLength; i++) {
            HistoryEquivalence equivalence = equivalenceAtPrefix(
                analyze.apply(left.subList(0, i + 1)),
                analyze.apply(right.subList(0, i + 1)),
                i < commonPrefixLength
            );

            if (firstSemanticMismatchIndex < 0 && equivalence != HistoryEquivalence.SEMANTICALLY_EQUAL
                && equivalence != HistoryEquivalence.EXACT) {
                firstSemanticMismatchIndex = i + 1;
            }

            points.add(new TimelineEquivalencePoint(i + 1, TimelineSide.BOTH, equivalence));
        }

        HistoryAnalysisResult<S, A> baseline = commonPrefixLength > 0
            ? analyze.apply(left.subList(0, commonPrefixLength))
            : null;

        if (left.size() > minLength) {
            for (int i = minLength; i < left.size(); i++) {
                HistoryEquivalence equivalence = extensionEquivalence(
                    analyze.apply(left.subList(0, i + 1)),
                    baseline
                );
                if (firstExtensionMismatchIndex < 0 && equivalence != HistoryEquivalence.SEMANTICALLY_EQUAL) {
                    firstExtensionMismatchIndex = i + 1;
                }
                points.add(new TimelineEquivalencePoint(i + 1, TimelineSide.LEFT_ONLY, equivalence));
            }
        } else if (right.size() > minLength) {
            for (int i = minLength; i < right.size(); i++) {
                HistoryEquivalence equivalence = extensionEquivalence(
                    analyze.apply(right.subList(0, i + 1)),
                    baseline
                );
                if (firstExtensionMismatchIndex < 0 && equivalence != HistoryEquivalence.SEMANTICALLY_EQUAL) {
                    firstExtensionMismatchIndex = i + 1;
                }
                points.add(new TimelineEquivalencePoint(i + 1, TimelineSide.RIGHT_ONLY, equivalence));
            }
        }

        return new TimelineEquivalenceResult(
            commonPrefixLength,
            points,
            commonPrefixLength + 1,
            firstSemanticMismatchIndex,
            firstExtensionMismatchIndex
        );
    }

    private static <S, A> HistoryEquivalence equivalenceAtPrefix(
        HistoryAnalysisResult<S, A> left,
        HistoryAnalysisResult<S, A> right,
        boolean exactPrefix
    ) {
        if (left instanceof HistoryAnalysisResult.Success<S, A> leftSuccess
            && right instanceof HistoryAnalysisResult.Success<S, A> rightSuccess) {
            HistoryEquivalence equivalence = HistoryEquivalenceEvaluator.evaluate(
                leftSuccess.summary(),
                rightSuccess.summary()
            );
            if (exactPrefix && equivalence == HistoryEquivalence.SEMANTICALLY_EQUAL) {
                return HistoryEquivalence.EXACT;
            }
            return equivalence;
        }
        return HistoryEquivalence.DIFFERENT;
    }

    private static <S, A> HistoryEquivalence extensionEquivalence(
        HistoryAnalysisResult<S, A> extension,
        HistoryAnalysisResult<S, A> baseline
    ) {
        if (baseline == null) {
            return HistoryEquivalence.DIFFERENT;
        }
        return equivalenceAtPrefix(extension, baseline, false);
    }
}
