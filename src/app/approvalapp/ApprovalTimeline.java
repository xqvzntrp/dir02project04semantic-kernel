package app.approvalapp;

import java.util.List;

public record ApprovalTimeline(List<ApprovalTimelineEntry> entries) {
    public ApprovalTimeline {
        entries = List.copyOf(entries);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("entries must not be empty");
        }
    }

    public ApprovalTimelineEntry at(int index) {
        return entries.get(index);
    }
}
