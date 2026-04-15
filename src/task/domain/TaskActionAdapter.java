package task.domain;

import java.util.List;
import semantic.kernel.ActionDescriptor;
import semantic.kernel.ActionAdapter;
import semantic.kernel.InputField;
import semantic.rules.NextMove;

public final class TaskActionAdapter
    implements ActionAdapter<TaskState, NextMove<TaskStatus>, TaskAction, TaskEvent> {

    @Override
    public List<TaskAction> fromMoves(List<NextMove<TaskStatus>> moves) {
        return moves.stream()
            .map(move -> new TaskAction(move.eventName(), move.resultingState()))
            .toList();
    }

    @Override
    public List<ActionDescriptor> describe(List<TaskAction> actions) {
        return actions.stream()
            .map(this::describe)
            .toList();
    }

    @Override
    public TaskEvent toEvent(TaskState state, List<TaskAction> actions, Object request) {
        if (!(request instanceof String actionName)) {
            throw new IllegalArgumentException("request must be an action name String");
        }

        return actions.stream()
            .filter(action -> action.eventName().equals(actionName))
            .findFirst()
            .map(action -> toEvent(state.id(), action))
            .orElseThrow(() -> new IllegalArgumentException("unsupported action request: " + actionName));
    }

    private TaskEvent toEvent(String taskId, TaskAction action) {
        return switch (action.eventName()) {
            case "start" -> new TaskStarted(taskId);
            case "complete" -> new TaskCompleted(taskId);
            case "reopen" -> new TaskReopened(taskId);
            default -> throw new IllegalArgumentException("unsupported action: " + action.eventName());
        };
    }

    private ActionDescriptor describe(TaskAction action) {
        return switch (action.eventName()) {
            case "start" -> new ActionDescriptor(
                "start",
                "Start Task",
                List.of(),
                "Moves the task from created to in-progress."
            );
            case "complete" -> new ActionDescriptor(
                "complete",
                "Complete Task",
                List.of(),
                "Marks the task as completed."
            );
            case "reopen" -> new ActionDescriptor(
                "reopen",
                "Reopen Task",
                List.of(),
                "Moves the task from completed back to in-progress."
            );
            default -> throw new IllegalArgumentException("unsupported action: " + action.eventName());
        };
    }
}
