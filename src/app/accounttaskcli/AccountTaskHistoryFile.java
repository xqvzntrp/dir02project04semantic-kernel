package app.accounttaskcli;

import accounttask.domain.AccountReactivatedAffectingWork;
import accounttask.domain.AccountSuspendedAffectingWork;
import accounttask.domain.AccountTaskEvent;
import accounttask.domain.NoteRecordedForAccountTask;
import accounttask.domain.WorkItemCompletedForAccount;
import accounttask.domain.WorkItemOpenedForAccount;
import accounttask.domain.WorkItemStartedForAccount;
import integration.eventchain.VerifiedFieldEvent;
import integration.eventchain.VerifiedFieldEventSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.StandardOpenOption;

public final class AccountTaskHistoryFile {

    public record LineageMetadata(
        String forkedFrom,
        long forkedAtIndex,
        Instant forkedAt
    ) {
        public LineageMetadata {
            if (forkedFrom == null || forkedFrom.isBlank()) {
                throw new IllegalArgumentException("forkedFrom must not be blank");
            }
            if (forkedAtIndex < 1) {
                throw new IllegalArgumentException("forkedAtIndex must be at least 1");
            }
            if (forkedAt == null) {
                throw new IllegalArgumentException("forkedAt must not be null");
            }
        }
    }

    private final VerifiedFieldEventSource source = new VerifiedFieldEventSource();

    public List<VerifiedFieldEvent> load(Path historyPath) throws IOException {
        if (!Files.exists(historyPath)) {
            return List.of();
        }
        return source.load(historyPath);
    }

    public void append(Path historyPath, AccountTaskEvent event) throws IOException {
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

        Files.writeString(historyPath, formatLine(nextSequence, event), StandardOpenOption.APPEND);
    }

    public void writeAll(Path historyPath, List<AccountTaskEvent> events) throws IOException {
        writeAll(historyPath, events, null);
    }

    public void writeAll(Path historyPath, List<AccountTaskEvent> events, LineageMetadata lineageMetadata) throws IOException {
        if (historyPath.getParent() != null) {
            Files.createDirectories(historyPath.getParent());
        }

        List<String> lines = new ArrayList<>();
        if (lineageMetadata != null) {
            lines.add("# forked-from: " + lineageMetadata.forkedFrom());
            lines.add("# forked-at-index: " + lineageMetadata.forkedAtIndex());
            lines.add("# forked-at: " + lineageMetadata.forkedAt());
            lines.add("");
        }
        for (int i = 0; i < events.size(); i++) {
            lines.add(formatLine(i + 1L, events.get(i)));
        }
        Files.write(historyPath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private String formatLine(long sequence, AccountTaskEvent event) {
        StringBuilder builder = new StringBuilder();
        builder.append(sequence)
            .append("|")
            .append(eventType(event))
            .append("|accountId=").append(event.accountId())
            .append("|taskId=").append(event.taskId());
        if (event instanceof NoteRecordedForAccountTask noteRecorded) {
            builder.append("|contentHash=").append(noteRecorded.contentHash());
        }
        builder.append(System.lineSeparator());
        return builder.toString();
    }

    private String eventType(AccountTaskEvent event) {
        if (event instanceof WorkItemOpenedForAccount) {
            return "WorkItemOpenedForAccount";
        }
        if (event instanceof WorkItemStartedForAccount) {
            return "WorkItemStartedForAccount";
        }
        if (event instanceof WorkItemCompletedForAccount) {
            return "WorkItemCompletedForAccount";
        }
        if (event instanceof AccountSuspendedAffectingWork) {
            return "AccountSuspendedAffectingWork";
        }
        if (event instanceof AccountReactivatedAffectingWork) {
            return "AccountReactivatedAffectingWork";
        }
        if (event instanceof NoteRecordedForAccountTask) {
            return "NoteRecordedForAccountTask";
        }
        throw new IllegalArgumentException("unsupported account-task event: " + event.getClass().getName());
    }
}
