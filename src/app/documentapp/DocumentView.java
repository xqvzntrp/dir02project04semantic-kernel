package app.documentapp;

import document.domain.DocumentAction;
import document.domain.DocumentEvent;
import document.domain.DocumentRuleState;
import document.domain.DocumentState;
import integration.eventchain.VerifiedFieldEvent;
import java.util.List;
import semantic.rules.NextMove;
import semantic.snapshot.SemanticSnapshot;

public record DocumentView(
    List<VerifiedFieldEvent> verifiedHistory,
    List<DocumentEvent> decodedEvents,
    SemanticSnapshot<DocumentState, NextMove<DocumentRuleState>, DocumentAction> snapshot
) {
}
