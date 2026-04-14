package task.domain;

import java.util.List;
import semantic.kernel.ActionAdapter;
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
    public TaskEvent toEvent(TaskState state, List<TaskAction> actions, Object request) {
        if (!(request instanceof String taskId)) {
            throw new IllegalArgumentException("request must be a task id String");
        }

        return actions.stream()
            .findFirst()
            .map(action -> toEvent(taskId, action))
            .orElseThrow(() -> new IllegalArgumentException("no available actions"));
    }

    private TaskEvent toEvent(String taskId, TaskAction action) {
        return switch (action.eventName()) {
            case "start" -> new TaskStarted(taskId);
            case "complete" -> new TaskCompleted(taskId);
            case "reopen" -> new TaskReopened(taskId);
            default -> throw new IllegalArgumentException("unsupported action: " + action.eventName());
        };
    }
}
