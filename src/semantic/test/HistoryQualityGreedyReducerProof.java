package semantic.test;

import accounttask.domain.AccountTaskAction;
import accounttask.domain.AccountTaskDomainKernel;
import accounttask.domain.AccountTaskEvent;
import accounttask.domain.AccountTaskSemanticState;
import app.historycompare.HistoryAnalysisResult;
import app.historycompare.HistorySnapshotSummary;
import app.historyquality.GreedyHistoryReducer;
import app.historyquality.GreedyReductionResult;
import app.historyquality.TaskFlowQuality;
import app.historyquality.TaskFlowQualityAnalyzer;
import app.taskcli.TaskHistoryFile;
import app.accounttaskcli.AccountTaskHistoryFile;
import integration.eventchain.AccountTaskEventChainDecoder;
import integration.eventchain.TaskEventChainDecoder;
import integration.eventchain.VerifiedFieldEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import task.domain.TaskAction;
import task.domain.TaskDomainKernel;
import task.domain.TaskEvent;
import task.domain.TaskState;

public final class HistoryQualityGreedyReducerProof {

    public static void main(String[] args) throws IOException {
        taskStableHistoryRemainsLocallyMinimal();
        repairCycleIsLocallyMinimalButFlowUnstable();
        accountTaskNotesAreGreedilyRemovable();
        greedyReductionIsIdempotent();
        explanationsExposeRepairsAndReductions();
        System.out.println("History quality greedy reducer proof passed.");
    }

    private static void taskStableHistoryRemainsLocallyMinimal() throws IOException {
        List<TaskEvent> events = decodeTask(Path.of("semantic-kernel", "samples", "eventchain", "real-task-stable.verified"));
        GreedyReductionResult<TaskEvent, TaskState, TaskAction> result =
            GreedyHistoryReducer.reduce(events, HistoryQualityGreedyReducerProof::analyzeTask);

        assertEquals(3, result.originalLength(), "task originalLength");
        assertEquals(3, result.reducedLength(), "task reducedLength");
        assertEquals(List.of(), result.removedOriginalIndices(), "task removed indices");
        assertEquals(events, result.reducedHistory(), "task reduced history");
    }

    private static void repairCycleIsLocallyMinimalButFlowUnstable() throws IOException {
        List<TaskEvent> events = decodeTask(
            Path.of("semantic-kernel", "samples", "eventchain", "real-task-repair-cycle-b.verified")
        );
        GreedyReductionResult<TaskEvent, TaskState, TaskAction> result =
            GreedyHistoryReducer.reduce(events, HistoryQualityGreedyReducerProof::analyzeTask);
        TaskFlowQuality flowQuality = TaskFlowQualityAnalyzer.analyze(events);

        assertEquals(events.size(), result.reducedLength(), "repair-cycle reducedLength");
        assertEquals(true, result.locallyMinimal(), "repair-cycle locallyMinimal");
        assertEquals(2, flowQuality.repairCount(), "repair-cycle repairCount");
        assertEquals(false, flowQuality.flowStable(), "repair-cycle flowStable");
        assertEquals(2, flowQuality.repairs().size(), "repair-cycle repairs size");
        assertEquals(3, flowQuality.repairs().get(0).index(), "repair-cycle first repair index");
        assertEquals(5, flowQuality.repairs().get(1).index(), "repair-cycle second repair index");
    }

    private static void accountTaskNotesAreGreedilyRemovable() throws IOException {
        List<AccountTaskEvent> events = decodeAccountTask(
            Path.of("semantic-kernel", "samples", "eventchain", "accounttask", "accounttask-session.verified")
        );
        GreedyReductionResult<AccountTaskEvent, AccountTaskSemanticState, AccountTaskAction> result =
            GreedyHistoryReducer.reduce(events, HistoryQualityGreedyReducerProof::analyzeAccountTask);

        assertEquals(events.size(), result.originalLength(), "accounttask originalLength");
        assertEquals(events.size() - 6, result.reducedLength(), "accounttask reducedLength");
        assertEquals(false, result.locallyMinimal(), "accounttask locallyMinimal");
        assertEquals(List.of(8, 9, 10, 11, 12, 17), result.removedOriginalIndices(), "accounttask removed indices");
    }

    private static void greedyReductionIsIdempotent() throws IOException {
        List<AccountTaskEvent> events = decodeAccountTask(
            Path.of("semantic-kernel", "samples", "eventchain", "accounttask", "accounttask-session.verified")
        );
        GreedyReductionResult<AccountTaskEvent, AccountTaskSemanticState, AccountTaskAction> first =
            GreedyHistoryReducer.reduce(events, HistoryQualityGreedyReducerProof::analyzeAccountTask);
        GreedyReductionResult<AccountTaskEvent, AccountTaskSemanticState, AccountTaskAction> second =
            GreedyHistoryReducer.reduce(first.reducedHistory(), HistoryQualityGreedyReducerProof::analyzeAccountTask);

        assertEquals(first.reducedHistory(), second.reducedHistory(), "idempotent reduced history");
        assertEquals(List.of(), second.removedOriginalIndices(), "idempotent removed indices");
    }

    private static void explanationsExposeRepairsAndReductions() throws IOException {
        List<TaskEvent> stableEvents = decodeTask(Path.of("semantic-kernel", "samples", "eventchain", "real-task-stable.verified"));
        TaskFlowQuality stableFlow = TaskFlowQualityAnalyzer.analyze(stableEvents);
        GreedyReductionResult<TaskEvent, TaskState, TaskAction> stableReduction =
            GreedyHistoryReducer.reduce(stableEvents, HistoryQualityGreedyReducerProof::analyzeTask);
        assertEquals(List.of(), stableFlow.repairs(), "stable repairs");
        assertEquals(List.of(), stableReduction.reductionSteps(), "stable reduction steps");

        List<TaskEvent> repairEvents = decodeTask(
            Path.of("semantic-kernel", "samples", "eventchain", "real-task-repair-cycle-b.verified")
        );
        TaskFlowQuality repairFlow = TaskFlowQualityAnalyzer.analyze(repairEvents);
        GreedyReductionResult<TaskEvent, TaskState, TaskAction> repairReduction =
            GreedyHistoryReducer.reduce(repairEvents, HistoryQualityGreedyReducerProof::analyzeTask);
        assertEquals(2, repairFlow.repairs().size(), "repair explanation count");
        assertEquals(List.of(), repairReduction.reductionSteps(), "repair reduction steps");

        List<AccountTaskEvent> noteEvents = decodeAccountTask(
            Path.of("semantic-kernel", "samples", "eventchain", "accounttask", "accounttask-session.verified")
        );
        GreedyReductionResult<AccountTaskEvent, AccountTaskSemanticState, AccountTaskAction> noteReduction =
            GreedyHistoryReducer.reduce(noteEvents, HistoryQualityGreedyReducerProof::analyzeAccountTask);
        assertEquals(true, noteReduction.reductionSteps().size() > 0, "note reduction steps");
        assertEquals(8, noteReduction.reductionSteps().get(0).removedOriginalIndex(), "first note removal index");
    }

    private static List<TaskEvent> decodeTask(Path historyPath) throws IOException {
        List<VerifiedFieldEvent> verified = new TaskHistoryFile().load(historyPath);
        return new TaskEventChainDecoder().decode(verified);
    }

    private static List<AccountTaskEvent> decodeAccountTask(Path historyPath) throws IOException {
        List<VerifiedFieldEvent> verified = new AccountTaskHistoryFile().load(historyPath);
        return new AccountTaskEventChainDecoder().decode(verified);
    }

    private static HistoryAnalysisResult<TaskState, TaskAction> analyzeTask(List<TaskEvent> events) {
        try {
            var snapshot = TaskDomainKernel.create().analyze(events);
            return new HistoryAnalysisResult.Success<>(
                new HistorySnapshotSummary<>(snapshot.state(), snapshot.actions())
            );
        } catch (IllegalStateException e) {
            return new HistoryAnalysisResult.Failure<>(message(e));
        }
    }

    private static HistoryAnalysisResult<AccountTaskSemanticState, AccountTaskAction> analyzeAccountTask(
        List<AccountTaskEvent> events
    ) {
        try {
            var snapshot = AccountTaskDomainKernel.create().analyze(events);
            return new HistoryAnalysisResult.Success<>(
                new HistorySnapshotSummary<>(snapshot.state().semanticState(), snapshot.actions())
            );
        } catch (IllegalStateException e) {
            return new HistoryAnalysisResult.Failure<>(message(e));
        }
    }

    private static String message(RuntimeException e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }
}
