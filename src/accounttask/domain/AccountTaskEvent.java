package accounttask.domain;

public sealed interface AccountTaskEvent
    permits WorkItemOpenedForAccount,
        WorkItemStartedForAccount,
        WorkItemCompletedForAccount,
        AccountSuspendedAffectingWork,
        AccountReactivatedAffectingWork,
        NoteRecordedForAccountTask {

    String accountId();

    String taskId();
}
