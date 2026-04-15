package accounttask.domain;

import java.util.List;

public record AccountTaskState(
    String accountId,
    String taskId,
    AccountTaskAccountStatus accountStatus,
    AccountTaskWorkStatus workStatus,
    List<String> noteHashes
) {
    public AccountTaskState {
        noteHashes = List.copyOf(noteHashes);
    }

    public AccountTaskSemanticState semanticState() {
        return new AccountTaskSemanticState(accountId, taskId, accountStatus, workStatus);
    }
}
