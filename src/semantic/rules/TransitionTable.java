package semantic.rules;

import java.util.List;

public final class TransitionTable<S> {

    private final List<TransitionRule<S>> rules;

    public TransitionTable(List<TransitionRule<S>> rules) {
        this.rules = List.copyOf(rules);
    }

    public List<TransitionRule<S>> rules() {
        return rules;
    }

    public List<NextMove<S>> nextMoves(S currentState) {
        return rules.stream()
            .filter(rule -> rule.allowedPriorStates().contains(currentState))
            .map(rule -> new NextMove<>(
                rule.eventName(),
                rule.resultingState(),
                rule.requiredFields()))
            .toList();
    }
}
