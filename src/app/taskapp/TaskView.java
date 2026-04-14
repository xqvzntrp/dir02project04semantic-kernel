package app.taskapp;

import integration.eventchain.VerifiedFieldEvent;
import java.util.List;
import semantic.rules.NextMove;
import semantic.snapshot.SemanticSnapshot;
import task.domain.TaskAction;
import task.domain.TaskEvent;
import task.domain.TaskState;
import task.domain.TaskStatus;

public record TaskView(
    List<VerifiedFieldEvent> verifiedHistory,
    List<TaskEvent> decodedEvents,
    SemanticSnapshot<TaskState, NextMove<TaskStatus>, TaskAction> snapshot
) {
}
