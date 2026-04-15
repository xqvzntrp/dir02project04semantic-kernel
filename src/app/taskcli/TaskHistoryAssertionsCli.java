package app.taskcli;

import app.historycompare.AssertionResult;
import app.task.assertions.TaskHistoryAssertions;
import java.io.IOException;
import java.nio.file.Path;

public final class TaskHistoryAssertionsCli {

    public static void main(String[] args) throws IOException {
        int exitCode = new TaskHistoryAssertionsCli().run(args);
        System.exit(exitCode);
    }

    private int run(String[] args) throws IOException {
        if (args.length < 2 || args.length > 3) {
            printUsage();
            return 2;
        }

        String command = args[0];

        try {
            if ("assert-repair-exists".equals(command) && args.length == 2) {
                return runRepairExistsAgainstParent(Path.of(args[1]));
            }

            if (args.length != 3) {
                printUsage();
                return 2;
            }

            Path leftPath = Path.of(args[1]);
            Path rightPath = Path.of(args[2]);

            AssertionResult result = switch (command) {
                case "assert-converges" -> TaskHistoryAssertions.assertConverges(leftPath, rightPath);
                case "assert-no-state-divergence" -> TaskHistoryAssertions.assertNoStateDivergence(leftPath, rightPath);
                case "assert-repair-exists" -> TaskHistoryAssertions.assertRepairExists(leftPath, rightPath);
                case "assert-actions-equivalent" -> TaskHistoryAssertions.assertActionsEquivalent(leftPath, rightPath);
                default -> {
                    printUsage();
                    yield new AssertionResult(false, "unknown command");
                }
            };

            if ("assert-converges".equals(command)
                || "assert-no-state-divergence".equals(command)
                || "assert-repair-exists".equals(command)
                || "assert-actions-equivalent".equals(command)) {
                if (result.pass()) {
                    System.out.println("PASS: " + result.message());
                    return 0;
                }
                System.out.println("FAIL: " + result.message());
                return 1;
            }

            return 2;
        } catch (RuntimeException e) {
            String message = e.getMessage();
            System.out.println("ERROR: " + (message == null || message.isBlank() ? e.getClass().getSimpleName() : message));
            return 2;
        }
    }

    private int runRepairExistsAgainstParent(Path historyPath) throws IOException {
        AssertionResult result = TaskHistoryAssertions.assertRepairExists(historyPath);
        if (result.pass()) {
            System.out.println("PASS: " + result.message());
            return 0;
        }
        System.out.println("FAIL: " + result.message());
        return 1;
    }

    private void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -cp out app.taskcli.TaskHistoryAssertionsCli assert-converges <left-history> <right-history>");
        System.out.println("  java -cp out app.taskcli.TaskHistoryAssertionsCli assert-no-state-divergence <left-history> <right-history>");
        System.out.println("  java -cp out app.taskcli.TaskHistoryAssertionsCli assert-repair-exists <history>");
        System.out.println("  java -cp out app.taskcli.TaskHistoryAssertionsCli assert-repair-exists <left-history> <right-history>");
        System.out.println("  java -cp out app.taskcli.TaskHistoryAssertionsCli assert-actions-equivalent <left-history> <right-history>");
    }
}
