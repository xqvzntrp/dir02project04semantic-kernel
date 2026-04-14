package semantic.rules;

import java.util.List;

public record NextMove<S>(
    String eventName,
    S resultingState,
    List<String> requiredFields
) {
    public NextMove {
        requiredFields = List.copyOf(requiredFields);
    }
}
