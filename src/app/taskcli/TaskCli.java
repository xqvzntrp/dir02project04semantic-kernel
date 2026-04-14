package app.taskcli;

import app.taskapp.TaskView;
import app.taskapp.TaskViews;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import task.domain.TaskAction;
import task.domain.TaskActionAdapter;
import task.domain.TaskEvent;

public final class TaskCli {

    private static final Path DEFAULT_HISTORY_PATH = Path.of("samples/eventchain/task-history.verified");

    private final TaskHistoryFile historyFile = new TaskHistoryFile();
    private final TaskActionAdapter actionAdapter = new TaskActionAdapter();

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

    private void show(Path historyPath) {
        TaskView view = TaskViews.load(historyPath);
        if (view.snapshot() == null) {
            System.out.println("No verified task history.");
            return;
        }
        System.out.println("taskId=" + view.snapshot().state().id());
        System.out.println("status=" + view.snapshot().state().status());
        System.out.println("nextMoves=" + view.snapshot().nextMoves());
    }

    private void next(Path historyPath) {
        TaskView view = TaskViews.load(historyPath);
        if (view.snapshot() == null) {
            System.out.println("No verified task history.");
            return;
        }

        System.out.println("actions=" + view.snapshot().actions());
    }

    private void history(Path historyPath) {
        TaskView view = TaskViews.load(historyPath);
        System.out.println("verified=" + view.verifiedHistory());
        System.out.println("decoded=" + view.decodedEvents());
    }

    private void apply(String[] args, Path historyPath) throws IOException {
        if (args.length < 2) {
            printUsage();
            return;
        }

        String actionName = args[1];
        TaskView view = TaskViews.load(historyPath);
        if (view.snapshot() == null) {
            System.out.println("No verified task history.");
            return;
        }

        TaskAction selectedAction = view.snapshot().actions().stream()
            .filter(action -> action.eventName().equals(actionName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("unsupported action: " + actionName));

        TaskEvent newEvent = actionAdapter.toEvent(
            view.snapshot().state(),
            List.of(selectedAction),
            view.snapshot().state().id());

        historyFile.append(historyPath, newEvent);

        TaskView updatedView = TaskViews.load(historyPath);
        System.out.println("appended=" + newEvent);
        System.out.println("state=" + updatedView.snapshot().state());
        System.out.println("nextMoves=" + updatedView.snapshot().nextMoves());
    }

    private Path historyPathFor(String[] args, String command) {
        return switch (command) {
            case "apply" -> args.length >= 3 ? Path.of(args[2]) : DEFAULT_HISTORY_PATH;
            case "show", "next", "history" -> args.length >= 2 ? Path.of(args[1]) : DEFAULT_HISTORY_PATH;
            default -> DEFAULT_HISTORY_PATH;
        };
    }

    private void printUsage() {
        System.out.println("Usage:");
        System.out.println("  show [history-file]");
        System.out.println("  next [history-file]");
        System.out.println("  history [history-file]");
        System.out.println("  apply <action> [history-file]");
    }
}
