package app.taskapp;

import app.taskcli.TaskHistoryFile;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import task.domain.TaskAction;
import task.domain.TaskActionAdapter;
import task.domain.TaskEvent;

public final class TaskApplications {
    private TaskApplications() {
    }

    public static TaskEvent apply(Path historyFile, String actionName) throws IOException {
        TaskLoadResult result = TaskViews.load(historyFile);
        if (result instanceof TaskLoadResult.EmptyHistory) {
            throw new IllegalStateException("no verified task history");
        }
        TaskView view = ((TaskLoadResult.Loaded) result).view();

        TaskAction selectedAction = view.snapshot().actions().stream()
            .filter(action -> action.eventName().equals(actionName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("unsupported action: " + actionName));

        TaskEvent newEvent = new TaskActionAdapter().toEvent(
            view.snapshot().state(),
            List.of(selectedAction),
            view.snapshot().state().id());

        new TaskHistoryFile().append(historyFile, newEvent);
        return newEvent;
    }
}
