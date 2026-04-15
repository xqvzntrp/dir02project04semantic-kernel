package app.approvalcli;

import approval.domain.ApprovalEvent;
import approval.domain.Approved;
import approval.domain.Rejected;
import approval.domain.Submitted;
import integration.eventchain.VerifiedFieldEvent;
import integration.eventchain.VerifiedFieldEventSource;
import java.io.IOException;
import java.time.Instant;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public final class ApprovalHistoryFile {

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

    public void append(Path historyPath, ApprovalEvent event) throws IOException {
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
            StandardOpenOption.APPEND
        );
    }

    public void writeAll(Path historyPath, List<ApprovalEvent> events) throws IOException {
        writeAll(historyPath, events, null);
    }

    public void writeAll(Path historyPath, List<ApprovalEvent> events, LineageMetadata lineageMetadata) throws IOException {
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

    private String formatLine(long sequence, ApprovalEvent event) {
        return sequence + "|" + eventType(event) + "|approvalId=" + event.approvalId() + System.lineSeparator();
    }

    private String eventType(ApprovalEvent event) {
        if (event instanceof Submitted) {
            return "ApprovalSubmitted";
        }
        if (event instanceof Approved) {
            return "ApprovalApproved";
        }
        if (event instanceof Rejected) {
            return "ApprovalRejected";
        }
        throw new IllegalArgumentException("unsupported approval event: " + event.getClass().getName());
    }
}
