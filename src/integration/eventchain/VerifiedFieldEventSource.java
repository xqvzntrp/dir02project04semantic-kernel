package integration.eventchain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VerifiedFieldEventSource {

    public List<VerifiedFieldEvent> load(Path samplePath) throws IOException {
        List<VerifiedFieldEvent> events = new ArrayList<>();

        for (String line : Files.readAllLines(samplePath)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            events.add(parse(trimmed));
        }

        return List.copyOf(events);
    }

    private VerifiedFieldEvent parse(String line) {
        String[] parts = line.split("\\|");
        if (parts.length < 2) {
            throw new IllegalArgumentException("invalid verified event line: " + line);
        }

        long sequence = Long.parseLong(parts[0]);
        String eventType = parts[1];
        Map<String, String> fields = new LinkedHashMap<>();

        for (int i = 2; i < parts.length; i++) {
            String[] keyValue = parts[i].split("=", 2);
            if (keyValue.length != 2) {
                throw new IllegalArgumentException("invalid field entry: " + parts[i]);
            }
            fields.put(keyValue[0], keyValue[1]);
        }

        return new VerifiedFieldEvent(sequence, eventType, fields);
    }
}
