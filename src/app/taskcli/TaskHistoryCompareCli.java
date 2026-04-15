package app.taskcli;

import app.historycompare.HistoryComparator;
import app.historycompare.HistoryAnalysisResult;
import app.historycompare.HistoryComparisonResult;
import app.historycompare.HistoryDifferenceSummarizer;
import app.historycompare.HistoryEquivalence;
import app.historycompare.HistoryEquivalenceEvaluator;
import app.historycompare.HistoryLineageReader;
import app.historycompare.HistorySnapshotSummary;
import app.historycompare.EventAttributionEvaluator;
import app.historycompare.TimelineEquivalenceEvaluator;
import app.historycompare.TimelineEquivalenceResult;
import integration.eventchain.TaskEventChainDecoder;
import integration.eventchain.VerifiedFieldEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import task.domain.TaskAction;
import task.domain.TaskDomainKernel;
import task.domain.TaskEvent;
import task.domain.TaskState;

public final class TaskHistoryCompareCli {

    public static void main(String[] args) throws IOException {
        Optional<Path> overrideParent = parseParentOverride(args);
        boolean showTimeline = hasTimelineFlag(args);
        boolean showAttribution = hasAttributionFlag(args);
        List<String> positionalArgs = positionalArgs(args);

        if (positionalArgs.size() == 1) {
            compareForkToParent(Path.of(positionalArgs.get(0)), overrideParent, showTimeline, showAttribution);
            return;
        }

        if (positionalArgs.size() != 2) {
            printUsage();
            return;
        }

        Path leftPath = Path.of(positionalArgs.get(0));
        Path rightPath = Path.of(positionalArgs.get(1));

        List<TaskEvent> leftEvents = decode(leftPath);
        List<TaskEvent> rightEvents = decode(rightPath);

        HistoryComparisonResult<TaskEvent, TaskState, TaskAction> result = HistoryComparator.compare(
            leftEvents,
            rightEvents,
            TaskHistoryCompareCli::analyze
        );

        print(leftPath, rightPath, result);
        if (showTimeline) {
            printTimeline(leftEvents, rightEvents);
        }
        if (showAttribution) {
            printAttribution(leftEvents, rightEvents);
        }
        if (showTimeline || showAttribution) {
            printDifferenceSummary(leftEvents, rightEvents, result);
        }
    }

    private static void compareForkToParent(
        Path forkPath,
        Optional<Path> overrideParent,
        boolean showTimeline,
        boolean showAttribution
    ) throws IOException {
        Optional<HistoryLineageReader.LineageInfo> lineage = readLineageIfPresent(forkPath);
        Path parentPath = resolveParentPath(forkPath, overrideParent, lineage);

        if (!Files.exists(parentPath)) {
            throw new IllegalStateException("Parent history file not found: " + parentPath);
        }

        List<TaskEvent> parentEvents = decode(parentPath);
        List<TaskEvent> forkEvents = decode(forkPath);

        HistoryComparisonResult<TaskEvent, TaskState, TaskAction> result = HistoryComparator.compare(
            parentEvents,
            forkEvents,
            TaskHistoryCompareCli::analyze
        );

        print(parentPath, forkPath, result);
        printParentSource(parentPath, overrideParent, lineage);
        if (lineage.isPresent()) {
            printLineage(lineage.get(), result, overrideParent.isPresent());
        }
        if (showTimeline) {
            printTimeline(parentEvents, forkEvents);
        }
        if (showAttribution) {
            printAttribution(parentEvents, forkEvents);
        }
        if (showTimeline || showAttribution) {
            printDifferenceSummary(parentEvents, forkEvents, result);
        }
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
            return new HistoryAnalysisResult.Failure<>(e.getMessage());
        }
    }

    private static void print(
        Path leftPath,
        Path rightPath,
        HistoryComparisonResult<TaskEvent, TaskState, TaskAction> result
    ) {
        System.out.println("Left: " + leftPath);
        System.out.println("Right: " + rightPath);
        System.out.println("Relation: " + result.relation());
        System.out.println("Common prefix: " + result.commonPrefixLength() + " event(s)");
        System.out.println("Divergence at event: " + result.divergenceIndex());
        System.out.println("Left divergence event: " + formatEvent(result.leftDivergenceEvent()));
        System.out.println("Right divergence event: " + formatEvent(result.rightDivergenceEvent()));
        printEquivalence(result);
        printAnalysis("Left", result.leftAnalysis());
        printAnalysis("Right", result.rightAnalysis());
    }

    private static void printLineage(
        HistoryLineageReader.LineageInfo lineage,
        HistoryComparisonResult<TaskEvent, TaskState, TaskAction> result,
        boolean usedOverride
    ) {
        boolean ok = result.commonPrefixLength() == lineage.forkedAtIndex();

        System.out.println("Lineage:");
        System.out.println("  forked-from: " + lineage.forkedFrom());
        System.out.println("  forked-at-index: " + lineage.forkedAtIndex());
        System.out.println("  forked-at: " + lineage.forkedAt());
        System.out.println("Lineage validation:");
        System.out.println("  expected prefix: " + lineage.forkedAtIndex());
        System.out.println("  actual prefix:   " + result.commonPrefixLength());
        System.out.println("  status: " + (ok ? "OK" : "MISMATCH"));
        if (usedOverride) {
            System.out.println("  source: override parent path");
        }
    }

    private static void printParentSource(
        Path parentPath,
        Optional<Path> overrideParent,
        Optional<HistoryLineageReader.LineageInfo> lineage
    ) {
        System.out.println("Parent source:");
        if (overrideParent.isPresent()) {
            System.out.println("  override: " + parentPath);
        } else {
            System.out.println("  override: <none>");
        }
        if (lineage.isPresent()) {
            System.out.println("  lineage: " + lineage.get().forkedFrom());
        } else {
            System.out.println("  lineage: <none>");
        }
    }

    private static String formatEvent(TaskEvent event) {
        return event == null ? "<none>" : event.toString();
    }

    private static void printAnalysis(String label, HistoryAnalysisResult<TaskState, TaskAction> analysis) {
        if (analysis instanceof HistoryAnalysisResult.Success<TaskState, TaskAction> success) {
            System.out.println(label + " final state: " + success.summary().state());
            System.out.println(label + " available actions: " + success.summary().actions());
            return;
        }

        HistoryAnalysisResult.Failure<TaskState, TaskAction> failure =
            (HistoryAnalysisResult.Failure<TaskState, TaskAction>) analysis;
        System.out.println(label + " analysis error: " + failure.message());
    }

    private static void printEquivalence(HistoryComparisonResult<TaskEvent, TaskState, TaskAction> result) {
        if (result.relation() == app.historycompare.HistoryRelation.EQUAL) {
            System.out.println("Semantic equivalence: " + HistoryEquivalence.EXACT);
            return;
        }

        if (result.leftAnalysis() instanceof HistoryAnalysisResult.Success<TaskState, TaskAction> leftSuccess
            && result.rightAnalysis() instanceof HistoryAnalysisResult.Success<TaskState, TaskAction> rightSuccess) {
            HistoryEquivalence equivalence = HistoryEquivalenceEvaluator.evaluate(
                leftSuccess.summary(),
                rightSuccess.summary()
            );
            System.out.println("Semantic equivalence: " + equivalence);
            if (result.relation() == app.historycompare.HistoryRelation.DIVERGED
                && equivalence.isSemanticConvergence()) {
                System.out.println("Note: Different histories converge to identical state and action surface.");
            }
            return;
        }

        System.out.println("Semantic equivalence: UNAVAILABLE");
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -cp out app.taskcli.TaskHistoryCompareCli <fork-history>");
        System.out.println("  java -cp out app.taskcli.TaskHistoryCompareCli <fork-history> --parent <parent-history>");
        System.out.println("  java -cp out app.taskcli.TaskHistoryCompareCli <left-history> <right-history>");
        System.out.println("  java -cp out app.taskcli.TaskHistoryCompareCli [args...] --timeline");
        System.out.println("  java -cp out app.taskcli.TaskHistoryCompareCli [args...] --attribution");
    }

    private static Optional<Path> parseParentOverride(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("--parent".equals(args[i])) {
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("Missing value for --parent");
                }
                return Optional.of(Path.of(args[i + 1]));
            }
        }
        return Optional.empty();
    }

    private static List<String> positionalArgs(String[] args) {
        java.util.ArrayList<String> positional = new java.util.ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            if ("--parent".equals(args[i])) {
                i++;
                continue;
            }
            if ("--timeline".equals(args[i])) {
                continue;
            }
            if ("--attribution".equals(args[i])) {
                continue;
            }
            positional.add(args[i]);
        }
        return List.copyOf(positional);
    }

    private static boolean hasTimelineFlag(String[] args) {
        for (String arg : args) {
            if ("--timeline".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAttributionFlag(String[] args) {
        for (String arg : args) {
            if ("--attribution".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static Optional<HistoryLineageReader.LineageInfo> readLineageIfPresent(Path forkPath) throws IOException {
        try {
            return Optional.of(HistoryLineageReader.read(forkPath));
        } catch (IllegalStateException e) {
            return Optional.empty();
        }
    }

    private static Path resolveParentPath(
        Path forkPath,
        Optional<Path> overrideParent,
        Optional<HistoryLineageReader.LineageInfo> lineage
    ) {
        if (overrideParent.isPresent()) {
            return overrideParent.get().toAbsolutePath().normalize();
        }
        if (lineage.isPresent()) {
            Path parentDirectory = forkPath.toAbsolutePath().getParent();
            return (parentDirectory == null
                ? Path.of(lineage.get().forkedFrom())
                : parentDirectory.resolve(lineage.get().forkedFrom()))
                .normalize();
        }
        throw new IllegalStateException("No parent override provided and no valid lineage header found");
    }

    private static void printTimeline(List<TaskEvent> leftEvents, List<TaskEvent> rightEvents) {
        TimelineEquivalenceResult timeline = TimelineEquivalenceEvaluator.evaluate(
            leftEvents,
            rightEvents,
            TaskHistoryCompareCli::analyze
        );
        System.out.println("Timeline equivalence:");
        for (var point : timeline.points()) {
            String prefix = point.side() == app.historycompare.TimelineSide.BOTH ? "" : "[" + point.side() + "] ";
            System.out.println("  " + point.index() + ": " + prefix + point.equivalence());
        }
        System.out.println("First structural divergence: " + timeline.firstStructuralDivergenceIndex());
        System.out.println("First semantic mismatch: "
            + (timeline.firstSemanticMismatchIndex() < 0 ? "<none>" : timeline.firstSemanticMismatchIndex()));
        System.out.println("First extension mismatch: "
            + (timeline.firstExtensionMismatchIndex() < 0 ? "<none>" : timeline.firstExtensionMismatchIndex()));
    }

    private static void printAttribution(List<TaskEvent> leftEvents, List<TaskEvent> rightEvents) {
        System.out.println("Event attribution:");
        for (var point : EventAttributionEvaluator.evaluate(leftEvents, rightEvents, TaskHistoryCompareCli::analyze)) {
            String prefix = point.side() == app.historycompare.TimelineSide.BOTH ? "" : "[" + point.side() + "] ";
            System.out.println("  " + point.index() + ": " + prefix + point.event() + " -> " + point.effect());
        }
    }

    private static void printDifferenceSummary(
        List<TaskEvent> leftEvents,
        List<TaskEvent> rightEvents,
        HistoryComparisonResult<TaskEvent, TaskState, TaskAction> result
    ) {
        java.util.Optional<HistoryEquivalence> finalEquivalence = finalEquivalence(result);
        if (finalEquivalence.isEmpty()) {
            System.out.println("Difference summary:");
            System.out.println("  unavailable: final equivalence could not be determined");
            return;
        }

        var timeline = TimelineEquivalenceEvaluator.evaluate(leftEvents, rightEvents, TaskHistoryCompareCli::analyze);
        var attribution = EventAttributionEvaluator.evaluate(leftEvents, rightEvents, TaskHistoryCompareCli::analyze);
        var summary = HistoryDifferenceSummarizer.summarize(timeline, attribution, finalEquivalence.get());

        System.out.println("Difference summary:");
        System.out.println("  first structural divergence: " + summary.firstStructuralDivergenceIndex());
        System.out.println("  first state divergence: " + displayIndex(summary.firstStateDivergenceIndex()));
        System.out.println("  first action divergence: " + displayIndex(summary.firstActionDivergenceIndex()));
        System.out.println("  first state repair: " + displayIndex(summary.firstStateRepairIndex()));
        System.out.println("  first action repair: " + displayIndex(summary.firstActionRepairIndex()));
        System.out.println("  state-introducing events: " + summary.stateIntroducingEvents());
        System.out.println("  action-introducing events: " + summary.actionIntroducingEvents());
        System.out.println("  state-repairing events: " + summary.stateRepairingEvents());
        System.out.println("  action-repairing events: " + summary.actionRepairingEvents());
        System.out.println("  final equivalence: " + summary.finalEquivalence());
        System.out.println("  divergence pattern: " + formatPattern(summary.divergencePattern()));
    }

    private static java.util.Optional<HistoryEquivalence> finalEquivalence(
        HistoryComparisonResult<TaskEvent, TaskState, TaskAction> result
    ) {
        if (result.relation() == app.historycompare.HistoryRelation.EQUAL) {
            return java.util.Optional.of(HistoryEquivalence.EXACT);
        }
        if (result.leftAnalysis() instanceof HistoryAnalysisResult.Success<TaskState, TaskAction> leftSuccess
            && result.rightAnalysis() instanceof HistoryAnalysisResult.Success<TaskState, TaskAction> rightSuccess) {
            return java.util.Optional.of(HistoryEquivalenceEvaluator.evaluate(
                leftSuccess.summary(),
                rightSuccess.summary()
            ));
        }
        return java.util.Optional.empty();
    }

    private static String displayIndex(int index) {
        return index < 0 ? "<none>" : String.valueOf(index);
    }

    private static String formatPattern(app.historycompare.DivergencePatternResult result) {
        if (result.repairIndices().isEmpty()) {
            return result.pattern().toString();
        }
        if (result.repairIndices().size() == 1) {
            return result.pattern() + " (repaired at " + result.repairIndices().get(0) + ")";
        }
        return result.pattern() + " (repairs at " + result.repairIndices() + ")";
    }
}
