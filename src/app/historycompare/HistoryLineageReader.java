package app.historycompare;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public final class HistoryLineageReader {

    public record LineageInfo(
        String forkedFrom,
        int forkedAtIndex,
        Instant forkedAt
    ) {
        public LineageInfo {
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

    private HistoryLineageReader() {
    }

    public static LineageInfo read(Path file) throws IOException {
        String forkedFrom = null;
        Integer forkedAtIndex = null;
        Instant forkedAt = null;

        for (String line : Files.readAllLines(file)) {
            if (!line.startsWith("#")) {
                break;
            }

            String body = line.substring(1).trim();
            int separator = body.indexOf(':');
            if (separator < 0) {
                continue;
            }

            String key = body.substring(0, separator).trim();
            String value = body.substring(separator + 1).trim();

            switch (key) {
                case "forked-from" -> forkedFrom = value;
                case "forked-at-index" -> forkedAtIndex = Integer.parseInt(value);
                case "forked-at" -> forkedAt = Instant.parse(value);
                default -> {
                }
            }
        }

        if (forkedFrom == null || forkedAtIndex == null || forkedAt == null) {
            throw new IllegalStateException("Missing or malformed lineage header");
        }

        return new LineageInfo(forkedFrom, forkedAtIndex, forkedAt);
    }
}
