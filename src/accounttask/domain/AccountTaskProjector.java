package accounttask.domain;

import java.util.ArrayList;
import java.util.List;
import semantic.kernel.Projector;

public final class AccountTaskProjector implements Projector<AccountTaskState, AccountTaskEvent> {

    @Override
    public AccountTaskState project(List<AccountTaskEvent> events) {
        AccountTaskState state = null;

        for (AccountTaskEvent event : events) {
            if (event instanceof WorkItemOpenedForAccount opened) {
                if (state != null) {
                    throw new IllegalStateException("work item already opened for account");
                }
                state = new AccountTaskState(
                    opened.accountId(),
                    opened.taskId(),
                    AccountTaskAccountStatus.ACTIVE,
                    AccountTaskWorkStatus.CREATED,
                    List.of()
                );
                continue;
            }

            if (state == null) {
                throw new IllegalStateException("work item must be opened before follow-up account or task events");
            }

            ensureSameAggregate(state, event);

            if (event instanceof WorkItemStartedForAccount) {
                if (state.accountStatus() != AccountTaskAccountStatus.ACTIVE) {
                    throw new IllegalStateException("work item can only start while the account is active");
                }
                if (state.workStatus() != AccountTaskWorkStatus.CREATED) {
                    throw new IllegalStateException("work item can only start from CREATED");
                }
                state = new AccountTaskState(
                    state.accountId(),
                    state.taskId(),
                    state.accountStatus(),
                    AccountTaskWorkStatus.IN_PROGRESS,
                    state.noteHashes()
                );
                continue;
            }

            if (event instanceof WorkItemCompletedForAccount) {
                if (state.accountStatus() != AccountTaskAccountStatus.ACTIVE) {
                    throw new IllegalStateException("work item can only complete while the account is active");
                }
                if (state.workStatus() != AccountTaskWorkStatus.IN_PROGRESS) {
                    throw new IllegalStateException("work item can only complete from IN_PROGRESS");
                }
                state = new AccountTaskState(
                    state.accountId(),
                    state.taskId(),
                    state.accountStatus(),
                    AccountTaskWorkStatus.COMPLETED,
                    state.noteHashes()
                );
                continue;
            }

            if (event instanceof AccountSuspendedAffectingWork) {
                if (state.accountStatus() != AccountTaskAccountStatus.ACTIVE) {
                    throw new IllegalStateException("account is already suspended");
                }
                state = new AccountTaskState(
                    state.accountId(),
                    state.taskId(),
                    AccountTaskAccountStatus.SUSPENDED,
                    state.workStatus(),
                    state.noteHashes()
                );
                continue;
            }

            if (event instanceof AccountReactivatedAffectingWork) {
                if (state.accountStatus() != AccountTaskAccountStatus.SUSPENDED) {
                    throw new IllegalStateException("account can only reactivate from SUSPENDED");
                }
                state = new AccountTaskState(
                    state.accountId(),
                    state.taskId(),
                    AccountTaskAccountStatus.ACTIVE,
                    state.workStatus(),
                    state.noteHashes()
                );
                continue;
            }

            if (event instanceof NoteRecordedForAccountTask noteRecorded) {
                List<String> noteHashes = new ArrayList<>(state.noteHashes());
                noteHashes.add(noteRecorded.contentHash());
                state = new AccountTaskState(
                    state.accountId(),
                    state.taskId(),
                    state.accountStatus(),
                    state.workStatus(),
                    noteHashes
                );
                continue;
            }

            throw new IllegalStateException("unknown account-task event: " + event.getClass().getName());
        }

        return state;
    }

    private void ensureSameAggregate(AccountTaskState state, AccountTaskEvent event) {
        if (!state.accountId().equals(event.accountId())) {
            throw new IllegalStateException("all events must belong to the same account");
        }
        if (!state.taskId().equals(event.taskId())) {
            throw new IllegalStateException("all events must belong to the same work item");
        }
    }
}
