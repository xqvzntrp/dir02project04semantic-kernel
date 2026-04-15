package app.taskapp;

import java.util.ArrayList;
import java.util.List;
import semantic.kernel.ActionDescriptor;
import semantic.kernel.SemanticKernel;
import semantic.rules.NextMove;
import semantic.snapshot.SemanticSnapshot;
import task.domain.TaskAction;
import task.domain.TaskActionAdapter;
import task.domain.TaskDomainKernel;
import task.domain.TaskEvent;
import task.domain.TaskState;
import task.domain.TaskStatus;

public final class TaskExplorations {
    private TaskExplorations() {
    }

    public static TaskTimeline timeline(TaskView view) {
        List<TaskTimelineEntry> entries = new ArrayList<>();
        for (int i = 0; i < view.decodedEvents().size(); i++) {
            List<TaskEvent> prefix = view.decodedEvents().subList(0, i + 1);
            entries.add(new TaskTimelineEntry(i, view.decodedEvents().get(i), taskKernel().analyze(prefix)));
        }
        return new TaskTimeline(entries);
    }

    public static TaskSimulationTrace simulationTrace(TaskView view, int timelineIndex, List<String> actionNames) {
        TaskTimeline timeline = timeline(view);
        TaskTimelineEntry baseEntry = timeline.at(timelineIndex);
        List<TaskEvent> baseEvents = new ArrayList<>(view.decodedEvents().subList(0, timelineIndex + 1));
        List<TaskEvent> fullEvents = new ArrayList<>(baseEvents);
        List<TaskSimulationStep> steps = new ArrayList<>();
        TaskActionAdapter adapter = new TaskActionAdapter();
        SemanticSnapshot<TaskState, NextMove<TaskStatus>, TaskAction> currentSnapshot = baseEntry.snapshot();

        for (int i = 0; i < actionNames.size(); i++) {
            String actionName = actionNames.get(i);
            TaskEvent event = adapter.toEvent(
                currentSnapshot.state(),
                currentSnapshot.actions(),
                actionName
            );
            ActionDescriptor descriptor = adapter.describe(currentSnapshot.actions()).stream()
                .filter(candidate -> candidate.name().equals(actionName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unsupported action request: " + actionName));

            fullEvents.add(event);
            currentSnapshot = taskKernel().analyze(fullEvents);
            steps.add(new TaskSimulationStep(i, actionName, descriptor, event, currentSnapshot));
        }

        return new TaskSimulationTrace(baseEvents, baseEntry.snapshot(), steps, fullEvents, currentSnapshot);
    }

    private static SemanticKernel<TaskState, TaskEvent, NextMove<TaskStatus>, TaskAction> taskKernel() {
        return TaskDomainKernel.create();
    }
}
