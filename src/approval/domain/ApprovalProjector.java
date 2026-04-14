package approval.domain;

import java.util.List;
import semantic.kernel.Projector;

public final class ApprovalProjector implements Projector<ApprovalState, ApprovalEvent> {

    @Override
    public ApprovalState project(List<ApprovalEvent> events) {
        ApprovalState state = null;

        for (ApprovalEvent event : events) {
            if (event instanceof Submitted submitted) {
                if (state != null) {
                    throw new IllegalStateException("approval already submitted");
                }
                state = new ApprovalState(submitted.approvalId(), ApprovalStatus.SUBMITTED);
                continue;
            }

            if (state == null) {
                throw new IllegalStateException("approval must be submitted before terminal decisions");
            }

            if (!state.id().equals(event.approvalId())) {
                throw new IllegalStateException("all events must belong to the same approval");
            }

            if (state.status() == ApprovalStatus.APPROVED || state.status() == ApprovalStatus.REJECTED) {
                throw new IllegalStateException("no transitions allowed after terminal state");
            }

            if (event instanceof Approved) {
                state = new ApprovalState(state.id(), ApprovalStatus.APPROVED);
                continue;
            }

            if (event instanceof Rejected) {
                state = new ApprovalState(state.id(), ApprovalStatus.REJECTED);
                continue;
            }

            throw new IllegalStateException("unknown approval event: " + event.getClass().getName());
        }

        return state;
    }
}
