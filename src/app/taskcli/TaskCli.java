package app.taskcli;

import app.taskapp.TaskApplications;
import app.taskapp.TaskLoadResult;
import app.taskapp.TaskView;
import app.taskapp.TaskViews;
import java.io.IOException;
import java.nio.file.Path;
import task.domain.TaskEvent;

public final class TaskCli {

    private static final Path DEFAULT_HISTORY_PATH = Path.of("semantic-kernel", "samples", "eventchain", "task-history.verified");

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
            case "create" -> create(args, historyPath);
            case "show" -> show(historyPath);
            case "next" -> next(historyPath);
            case "history" -> history(historyPath);
            case "apply" -> apply(args, historyPath);
            default -> printUsage();
        }
    }

    private void create(String[] args, Path historyPath) throws IOException {
        if (args.length < 2) {
            printUsage();
            return;
        }

        String taskId = args[1];
        TaskEvent newEvent = TaskApplications.create(historyPath, taskId);
        var result = TaskViews.load(historyPath);
        if (TaskCliSupport.isEmpty(result)) {
            System.out.println("No verified task history.");
            return;
        }
        TaskView view = TaskCliSupport.requireLoaded(result);
        System.out.println("appended=" + newEvent);
        System.out.println("state=" + view.snapshot().state());
        System.out.println("nextMoves=" + view.snapshot().nextMoves());
    }

    private void show(Path historyPath) {
        var result = TaskViews.load(historyPath);
        if (TaskCliSupport.isEmpty(result)) {
            System.out.println("No verified task history.");
            return;
        }
        TaskView view = TaskCliSupport.requireLoaded(result);
        System.out.println("taskId=" + view.snapshot().state().id());
        System.out.println("status=" + view.snapshot().state().status());
        System.out.println("nextMoves=" + view.snapshot().nextMoves());
    }

    private void next(Path historyPath) {
        var result = TaskViews.load(historyPath);
        if (TaskCliSupport.isEmpty(result)) {
            System.out.println("No verified task history.");
            return;
        }
        TaskView view = TaskCliSupport.requireLoaded(result);
        System.out.println("actions=" + view.snapshot().actions());
    }

    private void history(Path historyPath) {
        var result = TaskViews.load(historyPath);
        if (TaskCliSupport.isEmpty(result)) {
            var empty = (TaskLoadResult.EmptyHistory) result;
            System.out.println("verified=" + empty.verifiedHistory());
            System.out.println("decoded=" + empty.decodedEvents());
            return;
        }
        TaskView view = TaskCliSupport.requireLoaded(result);
        System.out.println("verified=" + view.verifiedHistory());
        System.out.println("decoded=" + view.decodedEvents());
    }

    private void apply(String[] args, Path historyPath) throws IOException {
        if (args.length < 2) {
            printUsage();
            return;
        }

        String actionName = args[1];
        try {
            TaskEvent newEvent = TaskApplications.apply(historyPath, actionName);
            var result = TaskViews.load(historyPath);
            if (TaskCliSupport.isEmpty(result)) {
                System.out.println("No verified task history.");
                return;
            }
            TaskView updatedView = TaskCliSupport.requireLoaded(result);

            System.out.println("appended=" + newEvent);
            System.out.println("state=" + updatedView.snapshot().state());
            System.out.println("nextMoves=" + updatedView.snapshot().nextMoves());
        } catch (IllegalStateException e) {
            if ("no verified task history".equals(e.getMessage())) {
                System.out.println("No verified task history.");
                return;
            }
            throw e;
        }
    }

    private Path historyPathFor(String[] args, String command) {
        return switch (command) {
            case "create", "apply" -> args.length >= 3 ? Path.of(args[2]) : DEFAULT_HISTORY_PATH;
            case "show", "next", "history" -> args.length >= 2 ? Path.of(args[1]) : DEFAULT_HISTORY_PATH;
            default -> DEFAULT_HISTORY_PATH;
        };
    }

    private void printUsage() {
        System.out.println("Usage:");
        System.out.println("  create <task-id> [history-file]");
        System.out.println("  show [history-file]");
        System.out.println("  next [history-file]");
        System.out.println("  history [history-file]");
        System.out.println("  apply <action> [history-file]");
    }
}
