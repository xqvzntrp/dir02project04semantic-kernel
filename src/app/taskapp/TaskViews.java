package app.taskapp;

import app.taskcli.TaskHistoryFile;
import integration.eventchain.TaskEventChainDecoder;
import integration.eventchain.VerifiedFieldEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import semantic.kernel.SemanticKernel;
import semantic.rules.NextMove;
import task.domain.TaskAction;
import task.domain.TaskDomainKernel;
import task.domain.TaskEvent;
import task.domain.TaskState;
import task.domain.TaskStatus;

public final class TaskViews {
    private TaskViews() {
    }

    public static TaskView load(Path historyFile) {
        try {
            List<VerifiedFieldEvent> verifiedHistory = loadVerifiedHistory(historyFile);
            List<TaskEvent> decodedEvents = decodeTaskEvents(verifiedHistory);
            if (decodedEvents.isEmpty()) {
                return new TaskView(verifiedHistory, decodedEvents, null);
            }
            var snapshot = taskKernel().analyze(decodedEvents);
            return new TaskView(verifiedHistory, decodedEvents, snapshot);
        } catch (IOException e) {
            throw new IllegalStateException("failed to read history file: " + historyFile, e);
        }
    }

    private static List<VerifiedFieldEvent> loadVerifiedHistory(Path historyFile) throws IOException {
        return new TaskHistoryFile().load(historyFile);
    }

    private static List<TaskEvent> decodeTaskEvents(List<VerifiedFieldEvent> verifiedHistory) {
        return new TaskEventChainDecoder().decode(verifiedHistory);
    }

    private static SemanticKernel<TaskState, TaskEvent, NextMove<TaskStatus>, TaskAction> taskKernel() {
        return TaskDomainKernel.create();
    }
}
