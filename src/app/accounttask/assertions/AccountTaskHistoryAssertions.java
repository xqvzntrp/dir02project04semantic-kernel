package app.accounttask.assertions;

import accounttask.domain.AccountTaskAction;
import accounttask.domain.AccountTaskDomainKernel;
import accounttask.domain.AccountTaskEvent;
import accounttask.domain.AccountTaskSemanticState;
import app.accounttaskcli.AccountTaskHistoryFile;
import app.historycompare.AssertionResult;
import app.historycompare.HistoryAnalysisResult;
import app.historycompare.HistoryComparator;
import app.historycompare.HistoryComparisonResult;
import app.historycompare.HistoryEquivalence;
import app.historycompare.HistoryEquivalenceEvaluator;
import app.historycompare.HistorySnapshotSummary;
import integration.eventchain.AccountTaskEventChainDecoder;
import integration.eventchain.VerifiedFieldEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class AccountTaskHistoryAssertions {
    private AccountTaskHistoryAssertions() {
    }

    public static AssertionResult assertAdmissible(Path history) throws IOException {
        try {
            analyzeSuccess(history);
            return new AssertionResult(true, "history is admissible and yields composite meaning");
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
            return new AssertionResult(true, "histories converge (different structure, same composite meaning)");
        }
        return new AssertionResult(false, "histories do not converge; final equivalence: " + equivalence);
    }

    public static AssertionResult assertActionsEquivalent(Path left, Path right) throws IOException {
        HistoryEquivalence equivalence = equivalence(left, right);
        if (equivalence.preservesActions()) {
            return new AssertionResult(true, "action surfaces are equivalent");
        }
        return new AssertionResult(false, "action surfaces differ");
    }

    public static HistoryEquivalence equivalence(Path left, Path right) throws IOException {
        HistoryComparisonResult<AccountTaskEvent, AccountTaskSemanticState, AccountTaskAction> comparison = compare(left, right);
        if (comparison.relation() == app.historycompare.HistoryRelation.EQUAL) {
            return HistoryEquivalence.EXACT;
        }
        if (comparison.leftAnalysis() instanceof HistoryAnalysisResult.Success<AccountTaskSemanticState, AccountTaskAction> leftSuccess
            && comparison.rightAnalysis() instanceof HistoryAnalysisResult.Success<AccountTaskSemanticState, AccountTaskAction> rightSuccess) {
            return HistoryEquivalenceEvaluator.evaluate(leftSuccess.summary(), rightSuccess.summary());
        }
        throw analysisFailure(comparison);
    }

    private static HistoryComparisonResult<AccountTaskEvent, AccountTaskSemanticState, AccountTaskAction> compare(
        Path left,
        Path right
    ) throws IOException {
        return compare(decode(left), decode(right));
    }

    private static HistoryComparisonResult<AccountTaskEvent, AccountTaskSemanticState, AccountTaskAction> compare(
        List<AccountTaskEvent> leftEvents,
        List<AccountTaskEvent> rightEvents
    ) {
        return HistoryComparator.compare(leftEvents, rightEvents, AccountTaskHistoryAssertions::analyze);
    }

    private static List<AccountTaskEvent> decode(Path historyPath) throws IOException {
        List<VerifiedFieldEvent> verified = new AccountTaskHistoryFile().load(historyPath);
        return new AccountTaskEventChainDecoder().decode(verified);
    }

    private static HistoryAnalysisResult<AccountTaskSemanticState, AccountTaskAction> analyze(List<AccountTaskEvent> events) {
        try {
            var snapshot = AccountTaskDomainKernel.create().analyze(events);
            return new HistoryAnalysisResult.Success<>(
                new HistorySnapshotSummary<AccountTaskSemanticState, AccountTaskAction>(
                    snapshot.state().semanticState(),
                    snapshot.actions()
                )
            );
        } catch (RuntimeException e) {
            String message = e.getMessage();
            return new HistoryAnalysisResult.Failure<>(message == null || message.isBlank() ? e.getClass().getSimpleName() : message);
        }
    }

    private static HistorySnapshotSummary<AccountTaskSemanticState, AccountTaskAction> analyzeSuccess(Path history) throws IOException {
        HistoryAnalysisResult<AccountTaskSemanticState, AccountTaskAction> analysis = analyze(decode(history));
        if (analysis instanceof HistoryAnalysisResult.Success<AccountTaskSemanticState, AccountTaskAction> success) {
            return success.summary();
        }

        HistoryAnalysisResult.Failure<AccountTaskSemanticState, AccountTaskAction> failure =
            (HistoryAnalysisResult.Failure<AccountTaskSemanticState, AccountTaskAction>) analysis;
        throw new IllegalStateException(failure.message());
    }

    private static IllegalStateException analysisFailure(
        HistoryComparisonResult<AccountTaskEvent, AccountTaskSemanticState, AccountTaskAction> comparison
    ) {
        if (comparison.leftAnalysis() instanceof HistoryAnalysisResult.Failure<AccountTaskSemanticState, AccountTaskAction> leftFailure) {
            return new IllegalStateException("left analysis failed: " + leftFailure.message());
        }
        HistoryAnalysisResult.Failure<AccountTaskSemanticState, AccountTaskAction> rightFailure =
            (HistoryAnalysisResult.Failure<AccountTaskSemanticState, AccountTaskAction>) comparison.rightAnalysis();
        return new IllegalStateException("right analysis failed: " + rightFailure.message());
    }
}
