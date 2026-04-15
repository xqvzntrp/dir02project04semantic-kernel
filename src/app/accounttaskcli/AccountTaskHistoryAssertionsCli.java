package app.accounttaskcli;

import accounttask.domain.AccountTaskAction;
import accounttask.domain.AccountTaskDomainKernel;
import accounttask.domain.AccountTaskEvent;
import accounttask.domain.AccountTaskRuleState;
import accounttask.domain.AccountTaskState;
import app.accounttask.assertions.AccountTaskHistoryAssertions;
import app.historycompare.AssertionResult;
import app.historycompare.HistoryEquivalence;
import integration.eventchain.AccountTaskEventChainDecoder;
import integration.eventchain.VerifiedFieldEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import semantic.kernel.SemanticKernel;
import semantic.rules.NextMove;

public final class AccountTaskHistoryAssertionsCli {

    public static void main(String[] args) throws IOException {
        int exitCode = new AccountTaskHistoryAssertionsCli().run(args);
        System.exit(exitCode);
    }

    private int run(String[] args) throws IOException {
        if (args.length < 2 || args.length > 3) {
            printUsage();
            return 2;
        }

        String command = args[0];

        try {
            if ("admissible".equals(command) || "stable".equals(command)) {
                if (args.length != 2) {
                    printUsage();
                    return 2;
                }
                return printResult(AccountTaskHistoryAssertions.assertAdmissible(Path.of(args[1])));
            }

            if ("equivalent".equals(command)
                || "converge".equals(command)
                || "actions-equal".equals(command)
                || "state-equal".equals(command)
                || "explain".equals(command)) {
                if (args.length != 3) {
                    printUsage();
                    return 2;
                }

                Path leftPath = Path.of(args[1]);
                Path rightPath = Path.of(args[2]);

                return switch (command) {
                    case "equivalent" -> printEquivalenceCheck(leftPath, rightPath, true);
                    case "converge" -> printResult(AccountTaskHistoryAssertions.assertConverges(leftPath, rightPath));
                    case "actions-equal" -> printResult(AccountTaskHistoryAssertions.assertActionsEquivalent(leftPath, rightPath));
                    case "state-equal" -> printEquivalenceCheck(leftPath, rightPath, false);
                    case "explain" -> explain(leftPath, rightPath);
                    default -> 2;
                };
            }

            printUsage();
            return 2;
        } catch (RuntimeException e) {
            String message = e.getMessage();
            System.out.println("ERROR: " + (message == null || message.isBlank() ? e.getClass().getSimpleName() : message));
            return 2;
        }
    }

    private int printEquivalenceCheck(Path leftPath, Path rightPath, boolean requireActionsToo) throws IOException {
        HistoryEquivalence equivalence = AccountTaskHistoryAssertions.equivalence(leftPath, rightPath);
        boolean pass = requireActionsToo ? equivalence.isSemanticConvergence() : equivalence.preservesState();
        String successMessage = requireActionsToo
            ? "histories are semantically equivalent: " + equivalence
            : "histories preserve state: " + equivalence;
        String failureMessage = requireActionsToo
            ? "histories are not semantically equivalent: " + equivalence
            : "histories do not preserve state: " + equivalence;
        return printResult(new AssertionResult(pass, pass ? successMessage : failureMessage));
    }

    private int explain(Path leftPath, Path rightPath) throws IOException {
        HistoryEquivalence equivalence = AccountTaskHistoryAssertions.equivalence(leftPath, rightPath);
        var leftSnapshot = analyze(leftPath);
        var rightSnapshot = analyze(rightPath);

        System.out.println("Left: " + leftPath);
        System.out.println("Right: " + rightPath);
        System.out.println("Left semantic state: " + leftSnapshot.state().semanticState());
        System.out.println("Left note hashes: " + leftSnapshot.state().noteHashes());
        System.out.println("Left actions: " + leftSnapshot.actions());
        System.out.println("Right semantic state: " + rightSnapshot.state().semanticState());
        System.out.println("Right note hashes: " + rightSnapshot.state().noteHashes());
        System.out.println("Right actions: " + rightSnapshot.actions());
        System.out.println("Equivalence: " + equivalence);
        System.out.println("preservesState: " + equivalence.preservesState());
        System.out.println("preservesActions: " + equivalence.preservesActions());
        System.out.println("semanticConvergence: " + equivalence.isSemanticConvergence());
        return 0;
    }

    private semantic.snapshot.SemanticSnapshot<AccountTaskState, NextMove<AccountTaskRuleState>, AccountTaskAction>
    analyze(Path historyPath) throws IOException {
        List<VerifiedFieldEvent> verified = new AccountTaskHistoryFile().load(historyPath);
        List<AccountTaskEvent> decoded = new AccountTaskEventChainDecoder().decode(verified);
        if (decoded.isEmpty()) {
            throw new IllegalStateException("No verified account-task history: " + historyPath);
        }
        return kernel().analyze(decoded);
    }

    private SemanticKernel<AccountTaskState, AccountTaskEvent, NextMove<AccountTaskRuleState>, AccountTaskAction>
    kernel() {
        return AccountTaskDomainKernel.create();
    }

    private int printResult(AssertionResult result) {
        if (result.pass()) {
            System.out.println("PASS: " + result.message());
            return 0;
        }
        System.out.println("FAIL: " + result.message());
        return 1;
    }

    private void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -cp semantic-kernel/build app.accounttaskcli.AccountTaskHistoryAssertionsCli admissible <history>");
        System.out.println("  java -cp semantic-kernel/build app.accounttaskcli.AccountTaskHistoryAssertionsCli stable <history>");
        System.out.println("  java -cp semantic-kernel/build app.accounttaskcli.AccountTaskHistoryAssertionsCli equivalent <left-history> <right-history>");
        System.out.println("  java -cp semantic-kernel/build app.accounttaskcli.AccountTaskHistoryAssertionsCli converge <left-history> <right-history>");
        System.out.println("  java -cp semantic-kernel/build app.accounttaskcli.AccountTaskHistoryAssertionsCli actions-equal <left-history> <right-history>");
        System.out.println("  java -cp semantic-kernel/build app.accounttaskcli.AccountTaskHistoryAssertionsCli state-equal <left-history> <right-history>");
        System.out.println("  java -cp semantic-kernel/build app.accounttaskcli.AccountTaskHistoryAssertionsCli explain <left-history> <right-history>");
    }
}
