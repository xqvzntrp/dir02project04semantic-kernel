package semantic.rules;

import java.util.List;
import java.util.Set;

public record TransitionRule<S>(
    String eventName,
    Set<S> allowedPriorStates,
    S resultingState,
    List<String> requiredFields,
    boolean createsAggregate,
    boolean terminal
) {
    public TransitionRule {
        allowedPriorStates = Set.copyOf(allowedPriorStates);
        requiredFields = List.copyOf(requiredFields);
    }
}
