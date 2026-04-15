package integration.eventchain;

import accounttask.domain.AccountTaskAction;
import accounttask.domain.AccountTaskDomainKernel;
import accounttask.domain.AccountTaskEvent;
import accounttask.domain.AccountTaskRuleState;
import accounttask.domain.AccountTaskState;
import accounttask.domain.AccountTaskWorkStatus;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import semantic.kernel.SemanticKernel;
import semantic.rules.NextMove;
import semantic.snapshot.SemanticSnapshot;

public final class AccountTaskEventChainProof {

    public static void main(String[] args) throws IOException {
        VerifiedFieldEventSource source = new VerifiedFieldEventSource();
        List<VerifiedFieldEvent> verifiedEvents =
            source.load(Path.of("semantic-kernel", "samples", "eventchain", "accounttask", "accounttask-suspend-reactivate.verified"));

        AccountTaskEventChainDecoder decoder = new AccountTaskEventChainDecoder();
        List<AccountTaskEvent> accountTaskEvents = decoder.decode(verifiedEvents);

        SemanticKernel<AccountTaskState, AccountTaskEvent, NextMove<AccountTaskRuleState>, AccountTaskAction> kernel =
            AccountTaskDomainKernel.create();

        SemanticSnapshot<AccountTaskState, NextMove<AccountTaskRuleState>, AccountTaskAction> snapshot =
            kernel.analyze(accountTaskEvents);

        assertEquals(AccountTaskWorkStatus.COMPLETED, snapshot.state().workStatus(), "work status");
        assertEquals("account-10", snapshot.state().accountId(), "account id");
        assertEquals("work-10", snapshot.state().taskId(), "task id");
        assertEquals(List.of(), snapshot.state().noteHashes(), "note hashes");

        System.out.println("verifiedEvents=" + verifiedEvents.size());
        System.out.println("decodedEvents=" + accountTaskEvents);
        System.out.println("state=" + snapshot.state());
        System.out.println("nextMoves=" + snapshot.nextMoves());
        System.out.println("actions=" + snapshot.actions());
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }
}
