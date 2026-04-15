package app.taskcli;

import app.historycompare.HistoryAnalysisResult;
import app.historycompare.HistorySnapshotSummary;
import app.historyquality.GreedyHistoryReducer;
import app.historyquality.GreedyReductionResult;
import app.historyquality.TaskFlowQuality;
import app.historyquality.TaskFlowQualityAnalyzer;
import integration.eventchain.TaskEventChainDecoder;
import integration.eventchain.VerifiedFieldEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import task.domain.TaskAction;
import task.domain.TaskDomainKernel;
import task.domain.TaskEvent;
import task.domain.TaskState;

public final class TaskHistoryQualityCli {

    public static void main(String[] args) throws IOException {
        int exitCode = new TaskHistoryQualityCli().run(args);
        System.exit(exitCode);
    }

    private int run(String[] args) throws IOException {
        if (args.length != 2) {
            printUsage();
            return 2;
        }

        String command = args[0];
        Path historyPath = Path.of(args[1]);
        try {
            List<TaskEvent> events = decode(historyPath);
            GreedyReductionResult<TaskEvent, TaskState, TaskAction> result = switch (command) {
                case "reduce-history", "analyze-quality", "explain-quality" ->
                    GreedyHistoryReducer.reduce(events, TaskHistoryQualityCli::analyze);
                default -> null;
            };
            if (result == null) {
                printUsage();
                return 2;
            }

            if ("reduce-history".equals(command)) {
                printReduction(historyPath, result);
            } else if ("analyze-quality".equals(command)) {
                printQuality(events, historyPath, result);
            } else {
                printExplanation(events, result);
            }
            return 0;
        } catch (IllegalStateException e) {
            String message = e.getMessage();
            System.out.println("ERROR: " + (message == null || message.isBlank() ? e.getClass().getSimpleName() : message));
            return 2;
        }
    }

    private void printReduction(
        Path historyPath,
        GreedyReductionResult<TaskEvent, TaskState, TaskAction> result
    ) {
        HistorySnapshotSummary<TaskState, TaskAction> summary = result.semanticSummary();
        System.out.println("History: " + historyPath);
        System.out.println("Original length: " + result.originalLength());
        System.out.println("Reduced length: " + result.reducedLength());
        System.out.println("Removed original indices: " + result.removedOriginalIndices());
        System.out.println("Locally minimal: " + result.locallyMinimal());
        System.out.println("Final semantic state: " + summary.state());
        System.out.println("Final actions: " + summary.actions());
        System.out.println("Reduction steps:");
        if (result.reductionSteps().isEmpty()) {
            System.out.println("  <none>");
        } else {
            result.reductionSteps().forEach(step -> System.out.println(
                "  remove original event " + step.removedOriginalIndex()
                    + " [" + step.removedEvent() + "]"
                    + " (" + step.lengthBefore() + " -> " + step.lengthAfter() + ")"
            ));
        }
        System.out.println("Reduced decoded history:");
        for (int i = 0; i < result.reducedHistory().size(); i++) {
            System.out.println("  " + (i + 1) + ". " + result.reducedHistory().get(i));
        }
    }

    private void printQuality(
        List<TaskEvent> events,
        Path historyPath,
        GreedyReductionResult<TaskEvent, TaskState, TaskAction> result
    ) {
        TaskFlowQuality flowQuality = TaskFlowQualityAnalyzer.analyze(events);
        System.out.println("History: " + historyPath);
        System.out.println("originalLength=" + result.originalLength());
        System.out.println("reducedLength=" + result.reducedLength());
        System.out.println("locallyMinimal=" + result.locallyMinimal());
        System.out.println("redundancy=" + (result.originalLength() - result.reducedLength()));
        System.out.println("repairCount=" + flowQuality.repairCount());
        System.out.println("flowStable=" + flowQuality.flowStable());
    }

    private void printExplanation(
        List<TaskEvent> events,
        GreedyReductionResult<TaskEvent, TaskState, TaskAction> result
    ) {
        TaskFlowQuality flowQuality = TaskFlowQualityAnalyzer.analyze(events);

        System.out.println("originalLength=" + result.originalLength());
        System.out.println("reducedLength=" + result.reducedLength());
        System.out.println("locallyMinimal=" + result.locallyMinimal());
        System.out.println("redundancy=" + (result.originalLength() - result.reducedLength()));
        System.out.println();
        System.out.println("flow:");
        System.out.println("  repairCount=" + flowQuality.repairCount());
        System.out.println("  flowStable=" + flowQuality.flowStable());
        if (flowQuality.repairs().isEmpty()) {
            System.out.println("  repairs: none");
        } else {
            System.out.println("  repairs:");
            flowQuality.repairs().forEach(repair -> System.out.println(
                "    index " + repair.index() + ": " + repair.fromEvent() + " -> " + repair.toEvent()
            ));
        }
        System.out.println();
        System.out.println("reduction:");
        if (result.reductionSteps().isEmpty()) {
            System.out.println("  removedCount=0");
            System.out.println("  steps: none (locally minimal)");
        } else {
            System.out.println("  removedCount=" + result.reductionSteps().size());
            System.out.println("  steps:");
            result.reductionSteps().forEach(step -> System.out.println(
                "    index " + step.removedOriginalIndex() + ": " + step.removedEvent()
            ));
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
        } catch (IllegalStateException e) {
            String message = e.getMessage();
            return new HistoryAnalysisResult.Failure<>(message == null || message.isBlank() ? e.getClass().getSimpleName() : message);
        }
    }

    private void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -cp semantic-kernel/build app.taskcli.TaskHistoryQualityCli reduce-history <history>");
        System.out.println("  java -cp semantic-kernel/build app.taskcli.TaskHistoryQualityCli analyze-quality <history>");
        System.out.println("  java -cp semantic-kernel/build app.taskcli.TaskHistoryQualityCli explain-quality <history>");
    }
}
