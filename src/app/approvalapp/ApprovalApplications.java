package app.approvalapp;

import app.approvalcli.ApprovalHistoryFile;
import approval.domain.ApprovalActionAdapter;
import approval.domain.ApprovalEvent;
import approval.domain.Submitted;
import java.io.IOException;
import java.nio.file.Path;

public final class ApprovalApplications {
    private ApprovalApplications() {
    }

    public static ApprovalEvent submit(Path historyFile, String approvalId) throws IOException {
        if (approvalId == null || approvalId.isBlank()) {
            throw new IllegalArgumentException("approvalId must not be blank");
        }

        ApprovalLoadResult result = ApprovalViews.load(historyFile);
        if (result instanceof ApprovalLoadResult.Loaded) {
            throw new IllegalStateException("Approval already exists in this history file.");
        }

        ApprovalEvent event = new Submitted(approvalId.trim());
        new ApprovalHistoryFile().append(historyFile, event);
        return event;
    }

    public static ApprovalEvent apply(Path historyFile, String actionName) throws IOException {
        ApprovalLoadResult result = ApprovalViews.load(historyFile);
        if (result instanceof ApprovalLoadResult.EmptyHistory) {
            throw new IllegalStateException("no verified approval history");
        }
        ApprovalView view = ((ApprovalLoadResult.Loaded) result).view();

        ApprovalEvent newEvent = new ApprovalActionAdapter().toEvent(
            view.snapshot().state(),
            view.snapshot().actions(),
            actionName
        );

        new ApprovalHistoryFile().append(historyFile, newEvent);
        return newEvent;
    }
}
