package accounttask.domain;

import java.util.List;
import semantic.kernel.ActionAdapter;
import semantic.kernel.ActionDescriptor;
import semantic.rules.NextMove;

public final class AccountTaskActionAdapter
    implements ActionAdapter<AccountTaskState, NextMove<AccountTaskRuleState>, AccountTaskAction, AccountTaskEvent> {

    @Override
    public List<AccountTaskAction> fromMoves(List<NextMove<AccountTaskRuleState>> moves) {
        return moves.stream()
            .map(move -> new AccountTaskAction(move.eventName(), move.resultingState()))
            .toList();
    }

    @Override
    public List<ActionDescriptor> describe(List<AccountTaskAction> actions) {
        return actions.stream()
            .map(this::describe)
            .toList();
    }

    @Override
    public AccountTaskEvent toEvent(AccountTaskState state, List<AccountTaskAction> actions, Object request) {
        if (!(request instanceof String actionName)) {
            throw new IllegalArgumentException("request must be an action name String");
        }

        return actions.stream()
            .filter(action -> action.eventName().equals(actionName))
            .findFirst()
            .map(action -> toEvent(state.accountId(), state.taskId(), action))
            .orElseThrow(() -> new IllegalArgumentException("unsupported action request: " + actionName));
    }

    private AccountTaskEvent toEvent(String accountId, String taskId, AccountTaskAction action) {
        return switch (action.eventName()) {
            case "start-work" -> new WorkItemStartedForAccount(accountId, taskId);
            case "complete-work" -> new WorkItemCompletedForAccount(accountId, taskId);
            case "suspend-account" -> new AccountSuspendedAffectingWork(accountId, taskId);
            case "reactivate-account" -> new AccountReactivatedAffectingWork(accountId, taskId);
            default -> throw new IllegalArgumentException("unsupported action: " + action.eventName());
        };
    }

    private ActionDescriptor describe(AccountTaskAction action) {
        return switch (action.eventName()) {
            case "start-work" -> new ActionDescriptor(
                "start-work",
                "Start Work Item",
                List.of(),
                "Starts work only when the account is active."
            );
            case "complete-work" -> new ActionDescriptor(
                "complete-work",
                "Complete Work Item",
                List.of(),
                "Completes work only when the account remains active."
            );
            case "suspend-account" -> new ActionDescriptor(
                "suspend-account",
                "Suspend Account",
                List.of(),
                "Suspends the account and blocks further work actions."
            );
            case "reactivate-account" -> new ActionDescriptor(
                "reactivate-account",
                "Reactivate Account",
                List.of(),
                "Restores the account so work actions become available again."
            );
            default -> throw new IllegalArgumentException("unsupported action: " + action.eventName());
        };
    }
}
