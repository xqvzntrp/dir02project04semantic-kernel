package app.historyquality;

import java.util.List;
import task.domain.TaskCompleted;
import task.domain.TaskEvent;
import task.domain.TaskReopened;

public final class TaskFlowQualityAnalyzer {
    private TaskFlowQualityAnalyzer() {
    }

    public static TaskFlowQuality analyze(List<TaskEvent> events) {
        java.util.List<TaskFlowQuality.Repair> repairs = new java.util.ArrayList<>();

        for (int i = 0; i < events.size() - 1; i++) {
            if (events.get(i) instanceof TaskCompleted
                && events.get(i + 1) instanceof TaskReopened) {
                repairs.add(new TaskFlowQuality.Repair(
                    i + 1,
                    events.get(i).getClass().getSimpleName(),
                    events.get(i + 1).getClass().getSimpleName()
                ));
            }
        }

        return new TaskFlowQuality(repairs.size(), repairs.isEmpty(), repairs);
    }
}
