package accounttask.domain;

public record NoteRecordedForAccountTask(
    String accountId,
    String taskId,
    String contentHash
) implements AccountTaskEvent {
    public NoteRecordedForAccountTask {
        if (contentHash == null || contentHash.isBlank()) {
            throw new IllegalArgumentException("contentHash must not be blank");
        }
    }
}
