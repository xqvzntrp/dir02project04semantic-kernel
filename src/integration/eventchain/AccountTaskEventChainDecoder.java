package integration.eventchain;

import accounttask.domain.AccountReactivatedAffectingWork;
import accounttask.domain.AccountSuspendedAffectingWork;
import accounttask.domain.AccountTaskEvent;
import accounttask.domain.NoteRecordedForAccountTask;
import accounttask.domain.WorkItemCompletedForAccount;
import accounttask.domain.WorkItemOpenedForAccount;
import accounttask.domain.WorkItemStartedForAccount;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AccountTaskEventChainDecoder {

    public List<AccountTaskEvent> decode(List<VerifiedFieldEvent> verifiedEvents) {
        List<AccountTaskEvent> events = verifiedEvents.stream()
            .sorted(Comparator.comparingLong(VerifiedFieldEvent::sequence))
            .map(this::decodeEvent)
            .toList();

        return List.copyOf(new ArrayList<>(events));
    }

    private AccountTaskEvent decodeEvent(VerifiedFieldEvent verifiedEvent) {
        String accountId = requiredField(verifiedEvent, "accountId");
        String taskId = requiredField(verifiedEvent, "taskId");

        return switch (verifiedEvent.eventType()) {
            case "WorkItemOpenedForAccount" -> new WorkItemOpenedForAccount(accountId, taskId);
            case "WorkItemStartedForAccount" -> new WorkItemStartedForAccount(accountId, taskId);
            case "WorkItemCompletedForAccount" -> new WorkItemCompletedForAccount(accountId, taskId);
            case "AccountSuspendedAffectingWork" -> new AccountSuspendedAffectingWork(accountId, taskId);
            case "AccountReactivatedAffectingWork" -> new AccountReactivatedAffectingWork(accountId, taskId);
            case "NoteRecordedForAccountTask" -> new NoteRecordedForAccountTask(
                accountId,
                taskId,
                requiredField(verifiedEvent, "contentHash")
            );
            default -> throw new IllegalArgumentException(
                "unsupported account-task event type: " + verifiedEvent.eventType());
        };
    }

    private String requiredField(VerifiedFieldEvent verifiedEvent, String fieldName) {
        String value = verifiedEvent.fields().get(fieldName);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                "missing required field '" + fieldName + "' for event " + verifiedEvent.eventType());
        }
        return value;
    }
}
