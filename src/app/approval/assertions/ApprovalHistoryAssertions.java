package app.approval.assertions;

import app.historycompare.AssertionResult;
import app.historycompare.EventAttributionEvaluator;
import app.historycompare.HistoryAnalysisResult;
import app.historycompare.HistoryComparator;
import app.historycompare.HistoryComparisonResult;
import app.historycompare.HistoryDifferenceSummarizer;
import app.historycompare.HistoryEquivalence;
import app.historycompare.HistoryEquivalenceEvaluator;
import app.historycompare.HistorySnapshotSummary;
import app.historycompare.TimelineEquivalenceEvaluator;
import app.approvalcli.ApprovalHistoryFile;
import approval.domain.ApprovalAction;
import approval.domain.ApprovalDomainKernel;
import approval.domain.ApprovalEvent;
import approval.domain.ApprovalState;
import integration.eventchain.ApprovalEventChainDecoder;
import integration.eventchain.VerifiedFieldEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class ApprovalHistoryAssertions {
    private ApprovalHistoryAssertions() {
    }

    public static AssertionResult assertAdmissible(Path history) throws IOException {
        try {
            analyzeSuccess(history);
            return new AssertionResult(true, "history is admissible and yields semantic meaning");
        } catch (IllegalStateException e) {
            return new AssertionResult(false, "history is not admissible: " + e.getMessage());
        }
    }

    public static AssertionResult assertRejected(Path history) throws IOException {
        try {
            analyzeSuccess(history);
            return new AssertionResult(false, "history was admissible");
        } catch (IllegalStateException e) {
            return new AssertionResult(true, "history rejected before semantic meaning: " + e.getMessage());
        }
    }

    public static AssertionResult assertConverges(Path left, Path right) throws IOException {
        HistoryEquivalence equivalence = equivalence(left, right);
        if (equivalence == HistoryEquivalence.EXACT) {
            return new AssertionResult(true, "histories are identical and therefore converge");
        }
        if (equivalence.isSemanticConvergence()) {
            return new AssertionResult(true, "histories converge (different structure, same meaning)");
        }
        return new AssertionResult(false, "histories do not converge; final equivalence: " + equivalence);
    }

    public static AssertionResult assertNoStateDivergence(Path left, Path right) throws IOException {
        List<ApprovalEvent> leftEvents = decode(left);
        List<ApprovalEvent> rightEvents = decode(right);
        int firstStateDivergence = firstStateDivergenceIndex(leftEvents, rightEvents);
        if (firstStateDivergence >= 0) {
            return new AssertionResult(false, "state divergence detected at event " + firstStateDivergence);
        }
        return new AssertionResult(true, "no state divergence detected");
    }

    public static AssertionResult assertRepairExists(Path left, Path right) throws IOException {
        List<ApprovalEvent> leftEvents = decode(left);
        List<ApprovalEvent> rightEvents = decode(right);
        HistoryComparisonResult<ApprovalEvent, ApprovalState, ApprovalAction> comparison = compare(leftEvents, rightEvents);
        var summary = summarize(leftEvents, rightEvents, comparison);

        boolean hasRepair = summary.firstStateRepairIndex() >= 0 || summary.firstActionRepairIndex() >= 0;
        boolean hadDivergence = summary.firstStateDivergenceIndex() >= 0 || summary.firstActionDivergenceIndex() >= 0;

        if (!hadDivergence) {
            return new AssertionResult(true, "no divergence detected (nothing to repair)");
        }

        if (hasRepair) {
            return new AssertionResult(true, "repair detected at event " + firstNonNegative(
                summary.firstStateRepairIndex(),
                summary.firstActionRepairIndex()
            ));
        }

        return new AssertionResult(false, "divergence not repaired; first divergence: " + firstNonNegative(
            summary.firstStateDivergenceIndex(),
            summary.firstActionDivergenceIndex()
        ));
    }

    public static AssertionResult assertRepairHolds(Path left, Path right) throws IOException {
        List<ApprovalEvent> leftEvents = decode(left);
        List<ApprovalEvent> rightEvents = decode(right);
        HistoryComparisonResult<ApprovalEvent, ApprovalState, ApprovalAction> comparison = compare(leftEvents, rightEvents);
        var summary = summarize(leftEvents, rightEvents, comparison);

        boolean hadDivergence = summary.firstStateDivergenceIndex() >= 0 || summary.firstActionDivergenceIndex() >= 0;
        if (!hadDivergence) {
            return new AssertionResult(true, "no divergence detected (nothing needed repair)");
        }

        if (finalEquivalence(comparison).isSemanticConvergence()) {
            return new AssertionResult(true, "repair holds in final effect");
        }

        return new AssertionResult(false, "repair does not hold; final equivalence: " + finalEquivalence(comparison));
    }

    public static AssertionResult assertActionsEquivalent(Path left, Path right) throws IOException {
        HistoryEquivalence equivalence = equivalence(left, right);
        if (equivalence.preservesActions()) {
            return new AssertionResult(true, "action surfaces are equivalent");
        }
        return new AssertionResult(false, "action surfaces differ");
    }

    public static HistoryEquivalence equivalence(Path left, Path right) throws IOException {
        HistoryComparisonResult<ApprovalEvent, ApprovalState, ApprovalAction> comparison = compare(left, right);
        return finalEquivalence(comparison);
    }

    private static HistoryComparisonResult<ApprovalEvent, ApprovalState, ApprovalAction> compare(
        Path left,
        Path right
    ) throws IOException {
        return compare(decode(left), decode(right));
    }

    private static HistoryComparisonResult<ApprovalEvent, ApprovalState, ApprovalAction> compare(
        List<ApprovalEvent> leftEvents,
        List<ApprovalEvent> rightEvents
    ) {
        return HistoryComparator.compare(leftEvents, rightEvents, ApprovalHistoryAssertions::analyze);
    }

    private static List<ApprovalEvent> decode(Path historyPath) throws IOException {
        List<VerifiedFieldEvent> verified = new ApprovalHistoryFile().load(historyPath);
        return new ApprovalEventChainDecoder().decode(verified);
    }

    private static HistoryAnalysisResult<ApprovalState, ApprovalAction> analyze(List<ApprovalEvent> events) {
        try {
            var snapshot = ApprovalDomainKernel.create().analyze(events);
            return new HistoryAnalysisResult.Success<>(
                new HistorySnapshotSummary<>(snapshot.state(), snapshot.actions())
            );
        } catch (RuntimeException e) {
            String message = e.getMessage();
            return new HistoryAnalysisResult.Failure<>(message == null || message.isBlank() ? e.getClass().getSimpleName() : message);
        }
    }

    private static HistorySnapshotSummary<ApprovalState, ApprovalAction> analyzeSuccess(Path history) throws IOException {
        HistoryAnalysisResult<ApprovalState, ApprovalAction> analysis = analyze(decode(history));
        if (analysis instanceof HistoryAnalysisResult.Success<ApprovalState, ApprovalAction> success) {
            return success.summary();
        }

        HistoryAnalysisResult.Failure<ApprovalState, ApprovalAction> failure =
            (HistoryAnalysisResult.Failure<ApprovalState, ApprovalAction>) analysis;
        throw new IllegalStateException(failure.message());
    }

    private static int firstStateDivergenceIndex(List<ApprovalEvent> leftEvents, List<ApprovalEvent> rightEvents) {
        int max = Math.max(leftEvents.size(), rightEvents.size());
        int common = Math.min(leftEvents.size(), rightEvents.size());

        for (int i = 1; i <= common; i++) {
            var leftAnalysis = analyze(leftEvents.subList(0, i));
            var rightAnalysis = analyze(rightEvents.subList(0, i));
            if (leftAnalysis instanceof HistoryAnalysisResult.Success<ApprovalState, ApprovalAction> leftSuccess
                && rightAnalysis instanceof HistoryAnalysisResult.Success<ApprovalState, ApprovalAction> rightSuccess) {
                if (!HistoryEquivalenceEvaluator.sameState(leftSuccess.summary(), rightSuccess.summary())) {
                    return i;
                }
            } else {
                return i;
            }
        }

        if (leftEvents.size() > common) {
            var baseline = analyze(leftEvents.subList(0, common));
            for (int i = common + 1; i <= max; i++) {
                var leftAnalysis = analyze(leftEvents.subList(0, i));
                if (baseline instanceof HistoryAnalysisResult.Success<ApprovalState, ApprovalAction> baseSuccess
                    && leftAnalysis instanceof HistoryAnalysisResult.Success<ApprovalState, ApprovalAction> leftSuccess) {
                    if (!HistoryEquivalenceEvaluator.sameState(baseSuccess.summary(), leftSuccess.summary())) {
                        return i;
                    }
                } else {
                    return i;
                }
            }
        } else if (rightEvents.size() > common) {
            var baseline = analyze(rightEvents.subList(0, common));
            for (int i = common + 1; i <= max; i++) {
                var rightAnalysis = analyze(rightEvents.subList(0, i));
                if (baseline instanceof HistoryAnalysisResult.Success<ApprovalState, ApprovalAction> baseSuccess
                    && rightAnalysis instanceof HistoryAnalysisResult.Success<ApprovalState, ApprovalAction> rightSuccess) {
                    if (!HistoryEquivalenceEvaluator.sameState(baseSuccess.summary(), rightSuccess.summary())) {
                        return i;
                    }
                } else {
                    return i;
                }
            }
        }

        return -1;
    }

    private static app.historycompare.HistoryDifferenceSummary summarize(
        List<ApprovalEvent> leftEvents,
        List<ApprovalEvent> rightEvents,
        HistoryComparisonResult<ApprovalEvent, ApprovalState, ApprovalAction> comparison
    ) {
        HistoryEquivalence finalEquivalence = finalEquivalence(comparison);
        var timeline = TimelineEquivalenceEvaluator.evaluate(leftEvents, rightEvents, ApprovalHistoryAssertions::analyze);
        var attribution = EventAttributionEvaluator.evaluate(leftEvents, rightEvents, ApprovalHistoryAssertions::analyze);
        return HistoryDifferenceSummarizer.summarize(timeline, attribution, finalEquivalence);
    }

    private static HistoryEquivalence finalEquivalence(
        HistoryComparisonResult<ApprovalEvent, ApprovalState, ApprovalAction> comparison
    ) {
        if (comparison.relation() == app.historycompare.HistoryRelation.EQUAL) {
            return HistoryEquivalence.EXACT;
        }

        if (comparison.leftAnalysis() instanceof HistoryAnalysisResult.Success<ApprovalState, ApprovalAction> leftSuccess
            && comparison.rightAnalysis() instanceof HistoryAnalysisResult.Success<ApprovalState, ApprovalAction> rightSuccess) {
            return HistoryEquivalenceEvaluator.evaluate(leftSuccess.summary(), rightSuccess.summary());
        }

        throw analysisFailure(comparison);
    }

    private static IllegalStateException analysisFailure(
        HistoryComparisonResult<ApprovalEvent, ApprovalState, ApprovalAction> comparison
    ) {
        if (comparison.leftAnalysis() instanceof HistoryAnalysisResult.Failure<ApprovalState, ApprovalAction> leftFailure) {
            return new IllegalStateException("left analysis failed: " + leftFailure.message());
        }
        HistoryAnalysisResult.Failure<ApprovalState, ApprovalAction> rightFailure =
            (HistoryAnalysisResult.Failure<ApprovalState, ApprovalAction>) comparison.rightAnalysis();
        return new IllegalStateException("right analysis failed: " + rightFailure.message());
    }

    private static int firstNonNegative(int first, int second) {
        if (first >= 0 && second >= 0) {
            return Math.min(first, second);
        }
        if (first >= 0) {
            return first;
        }
        return second;
    }
}
