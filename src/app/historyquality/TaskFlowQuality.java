package app.historyquality;

import java.util.List;

public record TaskFlowQuality(
    int repairCount,
    boolean flowStable,
    List<Repair> repairs
) {
    public TaskFlowQuality {
        if (repairCount < 0) {
            throw new IllegalArgumentException("repairCount must be non-negative");
        }
        repairs = List.copyOf(repairs);
    }

    public record Repair(
        int index,
        String fromEvent,
        String toEvent
    ) {
        public Repair {
            if (index < 1) {
                throw new IllegalArgumentException("index must be at least 1");
            }
            if (fromEvent == null || fromEvent.isBlank()) {
                throw new IllegalArgumentException("fromEvent must not be blank");
            }
            if (toEvent == null || toEvent.isBlank()) {
                throw new IllegalArgumentException("toEvent must not be blank");
            }
        }
    }
}
