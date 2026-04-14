package app.taskcli;

import integration.eventchain.VerifiedFieldEvent;
import integration.eventchain.VerifiedFieldEventSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import task.domain.TaskCompleted;
import task.domain.TaskCreated;
import task.domain.TaskEvent;
import task.domain.TaskReopened;
import task.domain.TaskStarted;

public final class TaskHistoryFile {

    private final VerifiedFieldEventSource source = new VerifiedFieldEventSource();

    public List<VerifiedFieldEvent> load(Path historyPath) throws IOException {
        if (!Files.exists(historyPath)) {
            return List.of();
        }
        return source.load(historyPath);
    }

    public void append(Path historyPath, TaskEvent event) throws IOException {
        List<VerifiedFieldEvent> existing = load(historyPath);
        long nextSequence = existing.stream()
            .mapToLong(VerifiedFieldEvent::sequence)
            .max()
            .orElse(0L) + 1L;

        if (historyPath.getParent() != null) {
            Files.createDirectories(historyPath.getParent());
        }
        if (!Files.exists(historyPath)) {
            Files.writeString(historyPath, "", StandardOpenOption.CREATE);
        }

        Files.writeString(
            historyPath,
            formatLine(nextSequence, event),
            StandardOpenOption.APPEND);
    }

    private String formatLine(long sequence, TaskEvent event) {
        return sequence + "|" + eventType(event) + "|taskId=" + event.taskId() + System.lineSeparator();
    }

    private String eventType(TaskEvent event) {
        if (event instanceof TaskCreated) {
            return "TaskCreated";
        }
        if (event instanceof TaskStarted) {
            return "TaskStarted";
        }
        if (event instanceof TaskCompleted) {
            return "TaskCompleted";
        }
        if (event instanceof TaskReopened) {
            return "TaskReopened";
        }
        throw new IllegalArgumentException("unsupported task event: " + event.getClass().getName());
    }
}
