package accounttask.domain;

import java.util.List;
import java.util.Set;
import semantic.kernel.SemanticKernel;
import semantic.rules.NextMove;
import semantic.snapshot.SemanticSnapshot;

public final class AccountTaskDomainProof {

    public static void main(String[] args) {
        SemanticKernel<AccountTaskState, AccountTaskEvent, NextMove<AccountTaskRuleState>, AccountTaskAction> kernel =
            AccountTaskDomainKernel.create();

        SemanticSnapshot<AccountTaskState, NextMove<AccountTaskRuleState>, AccountTaskAction> opened =
            kernel.analyze(List.of(new WorkItemOpenedForAccount("account-1", "task-1")));

        assertEquals(
            new AccountTaskState("account-1", "task-1", AccountTaskAccountStatus.ACTIVE, AccountTaskWorkStatus.CREATED, List.of()),
            opened.state(),
            "opened state");
        assertEquals(
            Set.of(
                new NextMove<>(
                    "start-work",
                    new AccountTaskRuleState(AccountTaskAccountStatus.ACTIVE, AccountTaskWorkStatus.IN_PROGRESS),
                    List.of()
                ),
                new NextMove<>(
                    "suspend-account",
                    new AccountTaskRuleState(AccountTaskAccountStatus.SUSPENDED, AccountTaskWorkStatus.CREATED),
                    List.of()
                )
            ),
            Set.copyOf(opened.nextMoves()),
            "opened nextMoves");

        SemanticSnapshot<AccountTaskState, NextMove<AccountTaskRuleState>, AccountTaskAction> suspended =
            kernel.analyze(List.of(
                new WorkItemOpenedForAccount("account-1", "task-1"),
                new AccountSuspendedAffectingWork("account-1", "task-1")));

        assertEquals(
            new AccountTaskState("account-1", "task-1", AccountTaskAccountStatus.SUSPENDED, AccountTaskWorkStatus.CREATED, List.of()),
            suspended.state(),
            "suspended state");
        assertEquals(
            List.of(
                new NextMove<>(
                    "reactivate-account",
                    new AccountTaskRuleState(AccountTaskAccountStatus.ACTIVE, AccountTaskWorkStatus.CREATED),
                    List.of()
                )
            ),
            suspended.nextMoves(),
            "suspended nextMoves");

        SemanticSnapshot<AccountTaskState, NextMove<AccountTaskRuleState>, AccountTaskAction> completed =
            kernel.analyze(List.of(
                new WorkItemOpenedForAccount("account-1", "task-1"),
                new AccountSuspendedAffectingWork("account-1", "task-1"),
                new AccountReactivatedAffectingWork("account-1", "task-1"),
                new WorkItemStartedForAccount("account-1", "task-1"),
                new WorkItemCompletedForAccount("account-1", "task-1")));

        assertEquals(
            new AccountTaskState("account-1", "task-1", AccountTaskAccountStatus.ACTIVE, AccountTaskWorkStatus.COMPLETED, List.of()),
            completed.state(),
            "completed state");
        assertEquals(
            List.of(
                new NextMove<>(
                    "suspend-account",
                    new AccountTaskRuleState(AccountTaskAccountStatus.SUSPENDED, AccountTaskWorkStatus.COMPLETED),
                    List.of()
                )
            ),
            completed.nextMoves(),
            "completed nextMoves");

        System.out.println("openedState=" + opened.state());
        System.out.println("openedActions=" + opened.actions());
        System.out.println("suspendedState=" + suspended.state());
        System.out.println("suspendedActions=" + suspended.actions());
        System.out.println("completedState=" + completed.state());
        System.out.println("completedActions=" + completed.actions());
        System.out.println("result=account state constrains task actions inside a composite domain");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }
}
