package task.domain;

import java.util.List;
import semantic.kernel.Projector;

public final class TaskProjector implements Projector<TaskState, TaskEvent> {

    @Override
    public TaskState project(List<TaskEvent> events) {
        TaskState state = null;

        for (TaskEvent event : events) {
            if (event instanceof TaskCreated created) {
                if (state != null) {
                    throw new IllegalStateException("task already created");
                }
                state = new TaskState(created.taskId(), TaskStatus.CREATED);
                continue;
            }

            if (state == null) {
                throw new IllegalStateException("task must be created before other events");
            }

            if (!state.id().equals(event.taskId())) {
                throw new IllegalStateException("all events must belong to the same task");
            }

            if (event instanceof TaskStarted) {
                if (state.status() != TaskStatus.CREATED) {
                    throw new IllegalStateException("task can only start from CREATED");
                }
                state = new TaskState(state.id(), TaskStatus.IN_PROGRESS);
                continue;
            }

            if (event instanceof TaskCompleted) {
                if (state.status() != TaskStatus.IN_PROGRESS) {
                    throw new IllegalStateException("task can only complete from IN_PROGRESS");
                }
                state = new TaskState(state.id(), TaskStatus.COMPLETED);
                continue;
            }

            throw new IllegalStateException("unknown task event: " + event.getClass().getName());
        }

        return state;
    }
}
