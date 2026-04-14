package approval.domain;

import java.util.List;
import semantic.kernel.ActionAdapter;
import semantic.rules.NextMove;

public final class ApprovalActionAdapter
    implements ActionAdapter<ApprovalState, NextMove<ApprovalStatus>, ApprovalAction, ApprovalEvent> {

    @Override
    public List<ApprovalAction> fromMoves(List<NextMove<ApprovalStatus>> moves) {
        return moves.stream()
            .map(move -> new ApprovalAction(move.eventName(), move.resultingState()))
            .toList();
    }

    @Override
    public ApprovalEvent toEvent(ApprovalState state, List<ApprovalAction> actions, Object request) {
        if (!(request instanceof String eventName)) {
            throw new IllegalArgumentException("request must be an action name String");
        }

        return actions.stream()
            .filter(action -> action.eventName().equals(eventName))
            .findFirst()
            .map(action -> toEvent(state.id(), action))
            .orElseThrow(() -> new IllegalArgumentException("unsupported action request: " + eventName));
    }

    private ApprovalEvent toEvent(String approvalId, ApprovalAction action) {
        return switch (action.eventName()) {
            case "approve" -> new Approved(approvalId);
            case "reject" -> new Rejected(approvalId);
            default -> throw new IllegalArgumentException("unsupported action: " + action.eventName());
        };
    }
}
