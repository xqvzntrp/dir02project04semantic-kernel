package app.documentapp;

import document.domain.DocumentAction;
import document.domain.DocumentDomainKernel;
import document.domain.DocumentEvent;
import document.domain.DocumentRuleState;
import document.domain.DocumentState;
import integration.eventchain.DocumentEventChainDecoder;
import integration.eventchain.VerifiedFieldEvent;
import java.util.List;
import semantic.kernel.SemanticKernel;
import semantic.rules.NextMove;

public final class DocumentViews {
    private DocumentViews() {
    }

    public static DocumentView view(List<VerifiedFieldEvent> verifiedHistory) {
        List<DocumentEvent> decodedEvents = decodeEvents(verifiedHistory);
        var snapshot = documentKernel().analyze(decodedEvents);
        return new DocumentView(verifiedHistory, decodedEvents, snapshot);
    }

    private static List<DocumentEvent> decodeEvents(List<VerifiedFieldEvent> verifiedHistory) {
        return new DocumentEventChainDecoder().decode(verifiedHistory);
    }

    private static SemanticKernel<DocumentState, DocumentEvent, NextMove<DocumentRuleState>, DocumentAction> documentKernel() {
        return DocumentDomainKernel.create();
    }
}
