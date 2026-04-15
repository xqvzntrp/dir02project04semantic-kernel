package app.approvalapp;

import app.approvalcli.ApprovalHistoryFile;
import approval.domain.ApprovalAction;
import approval.domain.ApprovalDomainKernel;
import approval.domain.ApprovalEvent;
import approval.domain.ApprovalState;
import approval.domain.ApprovalStatus;
import integration.eventchain.ApprovalEventChainDecoder;
import integration.eventchain.VerifiedFieldEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import semantic.kernel.SemanticKernel;
import semantic.rules.NextMove;

public final class ApprovalViews {
    private ApprovalViews() {
    }

    public static ApprovalLoadResult load(Path historyFile) {
        try {
            List<VerifiedFieldEvent> verifiedHistory = loadVerifiedHistory(historyFile);
            List<ApprovalEvent> decodedEvents = decodeApprovalEvents(verifiedHistory);
            if (decodedEvents.isEmpty()) {
                return new ApprovalLoadResult.EmptyHistory(verifiedHistory, decodedEvents);
            }
            var snapshot = approvalKernel().analyze(decodedEvents);
            return new ApprovalLoadResult.Loaded(new ApprovalView(verifiedHistory, decodedEvents, snapshot));
        } catch (IOException e) {
            throw new IllegalStateException("failed to read history file: " + historyFile, e);
        }
    }

    private static List<VerifiedFieldEvent> loadVerifiedHistory(Path historyFile) throws IOException {
        return new ApprovalHistoryFile().load(historyFile);
    }

    private static List<ApprovalEvent> decodeApprovalEvents(List<VerifiedFieldEvent> verifiedHistory) {
        return new ApprovalEventChainDecoder().decode(verifiedHistory);
    }

    private static SemanticKernel<ApprovalState, ApprovalEvent, NextMove<ApprovalStatus>, ApprovalAction> approvalKernel() {
        return ApprovalDomainKernel.create();
    }
}
