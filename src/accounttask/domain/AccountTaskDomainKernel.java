package accounttask.domain;

import java.util.List;
import java.util.Set;
import semantic.kernel.DefaultSemanticKernel;
import semantic.kernel.SemanticKernel;
import semantic.rules.NextMove;
import semantic.rules.TransitionRule;
import semantic.rules.TransitionTable;

public final class AccountTaskDomainKernel {
    private AccountTaskDomainKernel() {
    }

    public static SemanticKernel<AccountTaskState, AccountTaskEvent, NextMove<AccountTaskRuleState>, AccountTaskAction>
    create() {
        return new DefaultSemanticKernel<>(
            new AccountTaskProjector(),
            state -> new AccountTaskRuleState(state.accountStatus(), state.workStatus()),
            transitionTable(),
            new AccountTaskActionAdapter()
        );
    }

    private static TransitionTable<AccountTaskRuleState> transitionTable() {
        return new TransitionTable<>(List.of(
            new TransitionRule<>(
                "start-work",
                Set.of(new AccountTaskRuleState(AccountTaskAccountStatus.ACTIVE, AccountTaskWorkStatus.CREATED)),
                new AccountTaskRuleState(AccountTaskAccountStatus.ACTIVE, AccountTaskWorkStatus.IN_PROGRESS),
                List.of(),
                false,
                false),
            new TransitionRule<>(
                "complete-work",
                Set.of(new AccountTaskRuleState(AccountTaskAccountStatus.ACTIVE, AccountTaskWorkStatus.IN_PROGRESS)),
                new AccountTaskRuleState(AccountTaskAccountStatus.ACTIVE, AccountTaskWorkStatus.COMPLETED),
                List.of(),
                false,
                false),
            new TransitionRule<>(
                "suspend-account",
                Set.of(new AccountTaskRuleState(AccountTaskAccountStatus.ACTIVE, AccountTaskWorkStatus.CREATED)),
                new AccountTaskRuleState(AccountTaskAccountStatus.SUSPENDED, AccountTaskWorkStatus.CREATED),
                List.of(),
                false,
                false),
            new TransitionRule<>(
                "suspend-account",
                Set.of(new AccountTaskRuleState(AccountTaskAccountStatus.ACTIVE, AccountTaskWorkStatus.IN_PROGRESS)),
                new AccountTaskRuleState(AccountTaskAccountStatus.SUSPENDED, AccountTaskWorkStatus.IN_PROGRESS),
                List.of(),
                false,
                false),
            new TransitionRule<>(
                "suspend-account",
                Set.of(new AccountTaskRuleState(AccountTaskAccountStatus.ACTIVE, AccountTaskWorkStatus.COMPLETED)),
                new AccountTaskRuleState(AccountTaskAccountStatus.SUSPENDED, AccountTaskWorkStatus.COMPLETED),
                List.of(),
                false,
                false),
            new TransitionRule<>(
                "reactivate-account",
                Set.of(new AccountTaskRuleState(AccountTaskAccountStatus.SUSPENDED, AccountTaskWorkStatus.CREATED)),
                new AccountTaskRuleState(AccountTaskAccountStatus.ACTIVE, AccountTaskWorkStatus.CREATED),
                List.of(),
                false,
                false),
            new TransitionRule<>(
                "reactivate-account",
                Set.of(new AccountTaskRuleState(AccountTaskAccountStatus.SUSPENDED, AccountTaskWorkStatus.IN_PROGRESS)),
                new AccountTaskRuleState(AccountTaskAccountStatus.ACTIVE, AccountTaskWorkStatus.IN_PROGRESS),
                List.of(),
                false,
                false),
            new TransitionRule<>(
                "reactivate-account",
                Set.of(new AccountTaskRuleState(AccountTaskAccountStatus.SUSPENDED, AccountTaskWorkStatus.COMPLETED)),
                new AccountTaskRuleState(AccountTaskAccountStatus.ACTIVE, AccountTaskWorkStatus.COMPLETED),
                List.of(),
                false,
                false)
        ));
    }
}
