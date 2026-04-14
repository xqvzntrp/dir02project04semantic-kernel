package integration.eventchain;

import java.util.Map;

public record VerifiedFieldEvent(
    long sequence,
    String eventType,
    Map<String, String> fields
) {
    public VerifiedFieldEvent {
        fields = Map.copyOf(fields);
    }
}
