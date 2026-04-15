package app.task.assertions;

import app.historycompare.AssertionResult;
import app.historycompare.EventAttributionEvaluator;
import app.historycompare.HistoryAnalysisResult;
import app.historycompare.HistoryComparator;
import app.historycompare.HistoryComparisonResult;
import app.historycompare.HistoryDifferenceSummarizer;
import app.historycompare.HistoryEquivalence;
import app.historycompare.HistoryEquivalenceEvaluator;
import app.historycompare.HistoryLineageReader;
import app.historycompare.HistoryRelation;
import app.historycompare.HistorySnapshotSummary;
import app.historycompare.TimelineEquivalenceEvaluator;
import app.taskcli.TaskHistoryFile;
import integration.eventchain.TaskEventChainDecoder;
import integration.eventchain.VerifiedFieldEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import task.domain.TaskAction;
import task.domain.TaskDomainKernel;
import task.domain.TaskEvent;
import task.domain.TaskState;

public final class TaskHistoryAssertions {
    private TaskHistoryAssertions() {
    }

    public static AssertionResult assertConverges(Path left, Path right) throws IOException {
        HistoryComparisonResult<TaskEvent, TaskState, TaskAction> comparison = compare(left, right);
        if (comparison.relation() == HistoryRelation.EQUAL) {
            return new AssertionResult(true, "histories are identical and therefore converge");
        }

        if (comparison.leftAnalysis() instanceof HistoryAnalysisResult.Success<TaskState, TaskAction> leftSuccess
            && comparison.rightAnalysis() instanceof HistoryAnalysisResult.Success<TaskState, TaskAction> rightSuccess) {
            HistoryEquivalence equivalence = HistoryEquivalenceEvaluator.evaluate(
                leftSuccess.summary(),
                rightSuccess.summary()
            );
            if (equivalence == HistoryEquivalence.SEMANTICALLY_EQUAL) {
                return new AssertionResult(true, "histories converge (different structure, same meaning)");
            }
            return new AssertionResult(false, "histories do not converge; final equivalence: " + equivalence);
        }

        throw analysisFailure(comparison);
    }

    public static AssertionResult assertNoStateDivergence(Path left, Path right) throws IOException {
        List<TaskEvent> leftEvents = decode(left);
        List<TaskEvent> rightEvents = decode(right);
        int firstStateDivergence = firstStateDivergenceIndex(leftEvents, rightEvents);
        if (firstStateDivergence >= 0) {
            return new AssertionResult(false, "state divergence detected at event " + firstStateDivergence);
        }
        return new AssertionResult(true, "no state divergence detected");
    }

    public static AssertionResult assertRepairExists(Path history) throws IOException {
        HistoryLineageReader.LineageInfo lineage = HistoryLineageReader.read(history);
        Path parentDirectory = history.toAbsolutePath().getParent();
        Path parentPath = (parentDirectory == null
            ? Path.of(lineage.forkedFrom())
            : parentDirectory.resolve(lineage.forkedFrom())).normalize();

        if (!Files.exists(parentPath)) {
            throw new IllegalStateException("Parent history file not found: " + parentPath);
        }

        return assertRepairExists(parentPath, history);
    }

    public static AssertionResult assertRepairExists(Path left, Path right) throws IOException {
        List<TaskEvent> leftEvents = decode(left);
        List<TaskEvent> rightEvents = decode(right);
        HistoryComparisonResult<TaskEvent, TaskState, TaskAction> comparison = compare(leftEvents, rightEvents);
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

    public static AssertionResult assertActionsEquivalent(Path left, Path right) throws IOException {
        HistoryComparisonResult<TaskEvent, TaskState, TaskAction> comparison = compare(left, right);
        if (comparison.leftAnalysis() instanceof HistoryAnalysisResult.Success<TaskState, TaskAction> leftSuccess
            && comparison.rightAnalysis() instanceof HistoryAnalysisResult.Success<TaskState, TaskAction> rightSuccess) {
            if (HistoryEquivalenceEvaluator.sameActions(leftSuccess.summary(), rightSuccess.summary())) {
                return new AssertionResult(true, "action surfaces are equivalent");
            }
            return new AssertionResult(false, "action surfaces differ");
        }

        throw analysisFailure(comparison);
    }

    private static HistoryComparisonResult<TaskEvent, TaskState, TaskAction> compare(Path left, Path right) throws IOException {
        return compare(decode(left), decode(right));
    }

    private static HistoryComparisonResult<TaskEvent, TaskState, TaskAction> compare(
        List<TaskEvent> leftEvents,
        List<TaskEvent> rightEvents
    ) {
        return HistoryComparator.compare(leftEvents, rightEvents, TaskHistoryAssertions::analyze);
    }

    private static List<TaskEvent> decode(Path historyPath) throws IOException {
        List<VerifiedFieldEvent> verified = new TaskHistoryFile().load(historyPath);
        return new TaskEventChainDecoder().decode(verified);
    }

    private static HistoryAnalysisResult<TaskState, TaskAction> analyze(List<TaskEvent> events) {
        try {
            var snapshot = TaskDomainKernel.create().analyze(events);
            return new HistoryAnalysisResult.Success<>(
                new HistorySnapshotSummary<>(snapshot.state(), snapshot.actions())
            );
        } catch (RuntimeException e) {
            String message = e.getMessage();
            return new HistoryAnalysisResult.Failure<>(message == null || message.isBlank() ? e.getClass().getSimpleName() : message);
        }
    }

    private static int firstStateDivergenceIndex(List<TaskEvent> leftEvents, List<TaskEvent> rightEvents) {
        int max = Math.max(leftEvents.size(), rightEvents.size());
        int common = Math.min(leftEvents.size(), rightEvents.size());

        for (int i = 1; i <= common; i++) {
            var leftAnalysis = analyze(leftEvents.subList(0, i));
            var rightAnalysis = analyze(rightEvents.subList(0, i));
            if (leftAnalysis instanceof HistoryAnalysisResult.Success<TaskState, TaskAction> leftSuccess
                && rightAnalysis instanceof HistoryAnalysisResult.Success<TaskState, TaskAction> rightSuccess) {
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
                if (baseline instanceof HistoryAnalysisResult.Success<TaskState, TaskAction> baseSuccess
                    && leftAnalysis instanceof HistoryAnalysisResult.Success<TaskState, TaskAction> leftSuccess) {
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
                if (baseline instanceof HistoryAnalysisResult.Success<TaskState, TaskAction> baseSuccess
                    && rightAnalysis instanceof HistoryAnalysisResult.Success<TaskState, TaskAction> rightSuccess) {
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
        List<TaskEvent> leftEvents,
        List<TaskEvent> rightEvents,
        HistoryComparisonResult<TaskEvent, TaskState, TaskAction> comparison
    ) {
        HistoryEquivalence finalEquivalence = finalEquivalence(comparison);
        var timeline = TimelineEquivalenceEvaluator.evaluate(leftEvents, rightEvents, TaskHistoryAssertions::analyze);
        var attribution = EventAttributionEvaluator.evaluate(leftEvents, rightEvents, TaskHistoryAssertions::analyze);
        return HistoryDifferenceSummarizer.summarize(timeline, attribution, finalEquivalence);
    }

    private static HistoryEquivalence finalEquivalence(
        HistoryComparisonResult<TaskEvent, TaskState, TaskAction> comparison
    ) {
        if (comparison.relation() == HistoryRelation.EQUAL) {
            return HistoryEquivalence.EXACT;
        }

        if (comparison.leftAnalysis() instanceof HistoryAnalysisResult.Success<TaskState, TaskAction> leftSuccess
            && comparison.rightAnalysis() instanceof HistoryAnalysisResult.Success<TaskState, TaskAction> rightSuccess) {
            return HistoryEquivalenceEvaluator.evaluate(leftSuccess.summary(), rightSuccess.summary());
        }

        throw analysisFailure(comparison);
    }

    private static IllegalStateException analysisFailure(
        HistoryComparisonResult<TaskEvent, TaskState, TaskAction> comparison
    ) {
        if (comparison.leftAnalysis() instanceof HistoryAnalysisResult.Failure<TaskState, TaskAction> leftFailure) {
            return new IllegalStateException("left analysis failed: " + leftFailure.message());
        }
        HistoryAnalysisResult.Failure<TaskState, TaskAction> rightFailure =
            (HistoryAnalysisResult.Failure<TaskState, TaskAction>) comparison.rightAnalysis();
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
