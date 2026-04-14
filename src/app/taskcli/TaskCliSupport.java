package app.taskcli;

import app.taskapp.TaskLoadResult;
import app.taskapp.TaskView;

final class TaskCliSupport {
    private TaskCliSupport() {
    }

    static TaskView requireLoaded(TaskLoadResult result) {
        if (result instanceof TaskLoadResult.EmptyHistory) {
            throw new IllegalStateException("No verified task history.");
        }
        return ((TaskLoadResult.Loaded) result).view();
    }

    static boolean isEmpty(TaskLoadResult result) {
        return result instanceof TaskLoadResult.EmptyHistory;
    }
}
