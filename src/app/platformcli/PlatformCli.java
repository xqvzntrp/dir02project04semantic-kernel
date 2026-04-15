package app.platformcli;

import app.taskcli.TaskCli;

import java.io.IOException;
import java.util.Arrays;

/**
 * PlatformCli
 *
 * Operator shell over stable subsystems.
 *
 * Responsibilities:
 * - dispatch commands
 * - keep boundaries visible
 * - delegate real meaning to existing layers
 *
 * Non-responsibilities:
 * - no workflow interpretation
 * - no rule evaluation
 * - no projector logic
 * - no kernel changes
 */
public final class PlatformCli {
    private PlatformCli() {
    }

    public static void main(String[] args) {
        int exitCode = run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public static int run(String[] args) {
        if (args.length == 0 || isHelp(args[0])) {
            printUsage();
            return 0;
        }

        String area = args[0];

        return switch (area) {
            case "task" -> runTaskCommand(Arrays.copyOfRange(args, 1, args.length));
            default -> {
                System.err.println("Unknown area: " + area);
                printUsage();
                yield 1;
            }
        };
    }

    private static int runTaskCommand(String[] args) {
        if (args.length == 0 || isHelp(args[0])) {
            printTaskUsage();
            return 0;
        }

        String command = args[0];

        try {
            return switch (command) {
                case "show" -> delegateToTaskCli("show", args);
                case "next" -> delegateToTaskCli("next", args);
                case "history" -> delegateToTaskCli("history", args);

                // refresh is an execution convenience, not a semantic operation.
                // It simply re-reads history and shows the latest interpretation.
                case "refresh" -> delegateToTaskCli("show", replaceFirst(args, "show"));

                case "apply" -> delegateToTaskCli("apply", args);

                // Stubs for later operator capabilities.
                case "verify" -> notYetImplemented(
                    "task verify",
                    "Keep verification in the history/integration layer, not in the kernel."
                );
                case "compare" -> notYetImplemented(
                    "task compare",
                    "Keep history comparison in the integration layer, e.g. Merkle/prefix comparison."
                );

                default -> {
                    System.err.println("Unknown task command: " + command);
                    printTaskUsage();
                    yield 1;
                }
            };
        } catch (Exception e) {
            System.err.println("platform-cli error: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Delegates task operations to the existing thin task consumer.
     *
     * This preserves the architecture:
     * history -> verified field events -> task decoder -> semantic-kernel -> output
     */
    private static int delegateToTaskCli(String expectedCommand, String[] args) throws IOException {
        String[] forwarded;

        if (!args[0].equals(expectedCommand)) {
            forwarded = replaceFirst(args, expectedCommand);
        } else {
            forwarded = args;
        }

        // Assumes app.taskcli.TaskCli has a standard main(String[]) entry point.
        // This keeps platform-cli as a shell, not a second implementation.
        TaskCli.main(forwarded);
        return 0;
    }

    private static int notYetImplemented(String command, String guidance) {
        System.err.println("Not yet implemented: " + command);
        System.err.println(guidance);
        return 1;
    }

    private static boolean isHelp(String value) {
        return "help".equals(value) || "--help".equals(value) || "-h".equals(value);
    }

    private static String[] replaceFirst(String[] args, String newFirst) {
        String[] replaced = Arrays.copyOf(args, args.length);
        replaced[0] = newFirst;
        return replaced;
    }

    private static void printUsage() {
        System.out.println("Platform CLI");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  platform-cli <area> <command> [args]");
        System.out.println();
        System.out.println("Areas:");
        System.out.println("  task");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  platform-cli task show <history-file>");
        System.out.println("  platform-cli task next <history-file>");
        System.out.println("  platform-cli task history <history-file>");
        System.out.println("  platform-cli task refresh <history-file>");
        System.out.println("  platform-cli task apply <action> <history-file>");
    }

    private static void printTaskUsage() {
        System.out.println("Task commands:");
        System.out.println("  platform-cli task show <history-file>");
        System.out.println("  platform-cli task next <history-file>");
        System.out.println("  platform-cli task history <history-file>");
        System.out.println("  platform-cli task refresh <history-file>");
        System.out.println("  platform-cli task apply <action> <history-file>");
        System.out.println("  platform-cli task verify <history-file>      # stub");
        System.out.println("  platform-cli task compare <left> <right>     # stub");
    }
}
