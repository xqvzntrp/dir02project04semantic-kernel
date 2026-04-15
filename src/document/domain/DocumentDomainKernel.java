package document.domain;

import java.util.List;
import java.util.Set;
import semantic.kernel.DefaultSemanticKernel;
import semantic.kernel.SemanticKernel;
import semantic.rules.NextMove;
import semantic.rules.TransitionRule;
import semantic.rules.TransitionTable;

public final class DocumentDomainKernel {
    private DocumentDomainKernel() {
    }

    public static SemanticKernel<DocumentState, DocumentEvent, NextMove<DocumentRuleState>, DocumentAction> create() {
        return new DefaultSemanticKernel<>(
            new DocumentProjector(),
            DocumentState::ruleState,
            transitionTable(),
            new DocumentActionAdapter()
        );
    }

    private static TransitionTable<DocumentRuleState> transitionTable() {
        return new TransitionTable<>(List.of(
            new TransitionRule<>(
                "add_section",
                Set.of(DocumentRuleState.EMPTY, DocumentRuleState.HAS_SECTIONS),
                DocumentRuleState.HAS_SECTIONS,
                List.of(),
                false,
                false
            ),
            new TransitionRule<>(
                "edit_text",
                Set.of(DocumentRuleState.HAS_SECTIONS),
                DocumentRuleState.HAS_SECTIONS,
                List.of(),
                false,
                false
            ),
            new TransitionRule<>(
                "set_collapsed",
                Set.of(DocumentRuleState.HAS_SECTIONS),
                DocumentRuleState.HAS_SECTIONS,
                List.of(),
                false,
                false
            ),
            new TransitionRule<>(
                "reorder_section",
                Set.of(DocumentRuleState.HAS_SECTIONS),
                DocumentRuleState.HAS_SECTIONS,
                List.of(),
                false,
                false
            )
        ));
    }
}
