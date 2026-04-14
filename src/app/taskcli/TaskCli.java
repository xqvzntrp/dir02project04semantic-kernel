package app.taskcli;

import integration.eventchain.TaskEventChainDecoder;
import integration.eventchain.VerifiedFieldEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import semantic.kernel.DefaultSemanticKernel;
import semantic.kernel.SemanticKernel;
import semantic.rules.NextMove;
import semantic.rules.TransitionRule;
import semantic.rules.TransitionTable;
import semantic.snapshot.SemanticSnapshot;
import task.domain.TaskAction;
import task.domain.TaskActionAdapter;
import task.domain.TaskEvent;
import task.domain.TaskProjector;
import task.domain.TaskState;
import task.domain.TaskStatus;

public final class TaskCli {

    private static final Path DEFAULT_HISTORY_PATH = Path.of("samples/eventchain/task-history.verified");

    private final TaskHistoryFile historyFile = new TaskHistoryFile();
    private final TaskEventChainDecoder decoder = new TaskEventChainDecoder();
    private final TaskActionAdapter actionAdapter = new TaskActionAdapter();
    private final SemanticKernel<TaskState, TaskEvent, NextMove<TaskStatus>, TaskAction> kernel =
        new DefaultSemanticKernel<>(
            new TaskProjector(),
            TaskState::status,
            transitionTable(),
            actionAdapter);

    public static void main(String[] args) throws IOException {
        new TaskCli().run(args);
    }

    private void run(String[] args) throws IOException {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String command = args[0];
        Path historyPath = historyPathFor(args, command);

        switch (command) {
            case "show" -> show(historyPath);
            case "next" -> next(historyPath);
            case "history" -> history(historyPath);
            case "apply" -> apply(args, historyPath);
            default -> printUsage();
        }
    }

    private void show(Path historyPath) throws IOException {
        CliContext context = context(historyPath);
        if (context == null) {
            System.out.println("No verified task history.");
            return;
        }

        System.out.println("taskId=" + context.snapshot().state().id());
        System.out.println("status=" + context.snapshot().state().status());
        System.out.println("nextMoves=" + context.snapshot().nextMoves());
    }

    private void next(Path historyPath) throws IOException {
        CliContext context = context(historyPath);
        if (context == null) {
            System.out.println("No verified task history.");
            return;
        }

        System.out.println("actions=" + context.snapshot().actions());
    }

    private void history(Path historyPath) throws IOException {
        List<VerifiedFieldEvent> verifiedEvents = historyFile.load(historyPath);
        List<TaskEvent> taskEvents = decoder.decode(verifiedEvents);

        System.out.println("verified=" + verifiedEvents);
        System.out.println("decoded=" + taskEvents);
    }

    private void apply(String[] args, Path historyPath) throws IOException {
        if (args.length < 2) {
            printUsage();
            return;
        }

        String actionName = args[1];
        CliContext context = context(historyPath);
        if (context == null) {
            System.out.println("No verified task history.");
            return;
        }

        TaskAction selectedAction = context.snapshot().actions().stream()
            .filter(action -> action.eventName().equals(actionName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("unsupported action: " + actionName));

        TaskEvent newEvent = actionAdapter.toEvent(
            context.snapshot().state(),
            List.of(selectedAction),
            context.snapshot().state().id());

        historyFile.append(historyPath, newEvent);

        CliContext updated = context(historyPath);
        System.out.println("appended=" + newEvent);
        System.out.println("state=" + updated.snapshot().state());
        System.out.println("nextMoves=" + updated.snapshot().nextMoves());
    }

    private CliContext context(Path historyPath) throws IOException {
        List<VerifiedFieldEvent> verifiedEvents = historyFile.load(historyPath);
        if (verifiedEvents.isEmpty()) {
            return null;
        }

        List<TaskEvent> taskEvents = decoder.decode(verifiedEvents);
        SemanticSnapshot<TaskState, NextMove<TaskStatus>, TaskAction> snapshot = kernel.analyze(taskEvents);
        return new CliContext(verifiedEvents, taskEvents, snapshot);
    }

    private Path historyPathFor(String[] args, String command) {
        return switch (command) {
            case "apply" -> args.length >= 3 ? Path.of(args[2]) : DEFAULT_HISTORY_PATH;
            case "show", "next", "history" -> args.length >= 2 ? Path.of(args[1]) : DEFAULT_HISTORY_PATH;
            default -> DEFAULT_HISTORY_PATH;
        };
    }

    private static TransitionTable<TaskStatus> transitionTable() {
        return new TransitionTable<>(List.of(
            new TransitionRule<>(
                "start",
                Set.of(TaskStatus.CREATED),
                TaskStatus.IN_PROGRESS,
                List.of(),
                false,
                false),
            new TransitionRule<>(
                "complete",
                Set.of(TaskStatus.IN_PROGRESS),
                TaskStatus.COMPLETED,
                List.of(),
                false,
                true)));
    }

    private void printUsage() {
        System.out.println("Usage:");
        System.out.println("  show [history-file]");
        System.out.println("  next [history-file]");
        System.out.println("  history [history-file]");
        System.out.println("  apply <action> [history-file]");
    }

    private record CliContext(
        List<VerifiedFieldEvent> verifiedEvents,
        List<TaskEvent> taskEvents,
        SemanticSnapshot<TaskState, NextMove<TaskStatus>, TaskAction> snapshot
    ) {
    }
}
