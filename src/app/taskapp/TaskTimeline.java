package app.taskapp;

import java.util.List;

public record TaskTimeline(List<TaskTimelineEntry> entries) {
    public TaskTimeline {
        entries = List.copyOf(entries);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("entries must not be empty");
        }
    }

    public TaskTimelineEntry latest() {
        return entries.get(entries.size() - 1);
    }

    public TaskTimelineEntry at(int index) {
        return entries.get(index);
    }
}
