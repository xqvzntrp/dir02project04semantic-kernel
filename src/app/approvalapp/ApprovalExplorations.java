package app.approvalapp;

import approval.domain.ApprovalAction;
import approval.domain.ApprovalActionAdapter;
import approval.domain.ApprovalDomainKernel;
import approval.domain.ApprovalEvent;
import approval.domain.ApprovalState;
import approval.domain.ApprovalStatus;
import java.util.ArrayList;
import java.util.List;
import semantic.kernel.ActionDescriptor;
import semantic.kernel.SemanticKernel;
import semantic.rules.NextMove;
import semantic.snapshot.SemanticSnapshot;

public final class ApprovalExplorations {
    private ApprovalExplorations() {
    }

    public static ApprovalTimeline timeline(ApprovalView view) {
        List<ApprovalTimelineEntry> entries = new ArrayList<>();
        for (int i = 0; i < view.decodedEvents().size(); i++) {
            List<ApprovalEvent> prefix = view.decodedEvents().subList(0, i + 1);
            entries.add(new ApprovalTimelineEntry(i, view.decodedEvents().get(i), approvalKernel().analyze(prefix)));
        }
        return new ApprovalTimeline(entries);
    }

    public static ApprovalSimulationTrace simulationTrace(ApprovalView view, int timelineIndex, List<String> actionNames) {
        ApprovalTimeline timeline = timeline(view);
        ApprovalTimelineEntry baseEntry = timeline.at(timelineIndex);
        List<ApprovalEvent> baseEvents = new ArrayList<>(view.decodedEvents().subList(0, timelineIndex + 1));
        List<ApprovalEvent> fullEvents = new ArrayList<>(baseEvents);
        List<ApprovalSimulationStep> steps = new ArrayList<>();
        ApprovalActionAdapter adapter = new ApprovalActionAdapter();
        SemanticSnapshot<ApprovalState, NextMove<ApprovalStatus>, ApprovalAction> currentSnapshot = baseEntry.snapshot();

        for (int i = 0; i < actionNames.size(); i++) {
            String actionName = actionNames.get(i);
            ApprovalEvent event = adapter.toEvent(
                currentSnapshot.state(),
                currentSnapshot.actions(),
                actionName
            );
            ActionDescriptor descriptor = adapter.describe(currentSnapshot.actions()).stream()
                .filter(candidate -> candidate.name().equals(actionName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unsupported action request: " + actionName));

            fullEvents.add(event);
            currentSnapshot = approvalKernel().analyze(fullEvents);
            steps.add(new ApprovalSimulationStep(i, actionName, descriptor, event, currentSnapshot));
        }

        return new ApprovalSimulationTrace(baseEvents, baseEntry.snapshot(), steps, fullEvents, currentSnapshot);
    }

    private static SemanticKernel<ApprovalState, ApprovalEvent, NextMove<ApprovalStatus>, ApprovalAction> approvalKernel() {
        return ApprovalDomainKernel.create();
    }
}
