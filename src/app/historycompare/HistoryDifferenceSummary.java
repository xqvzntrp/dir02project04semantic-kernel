package app.historycompare;

import java.util.List;

public record HistoryDifferenceSummary(
    int firstStructuralDivergenceIndex,
    int firstStateDivergenceIndex,
    int firstActionDivergenceIndex,
    int firstStateRepairIndex,
    int firstActionRepairIndex,
    List<Integer> stateIntroducingEvents,
    List<Integer> actionIntroducingEvents,
    List<Integer> stateRepairingEvents,
    List<Integer> actionRepairingEvents,
    HistoryEquivalence finalEquivalence,
    DivergencePatternResult divergencePattern
) {
    public HistoryDifferenceSummary {
        stateIntroducingEvents = List.copyOf(stateIntroducingEvents);
        actionIntroducingEvents = List.copyOf(actionIntroducingEvents);
        stateRepairingEvents = List.copyOf(stateRepairingEvents);
        actionRepairingEvents = List.copyOf(actionRepairingEvents);
        if (firstStructuralDivergenceIndex < 1) {
            throw new IllegalArgumentException("firstStructuralDivergenceIndex must be at least 1");
        }
        if (finalEquivalence == null) {
            throw new IllegalArgumentException("finalEquivalence must not be null");
        }
        if (divergencePattern == null) {
            throw new IllegalArgumentException("divergencePattern must not be null");
        }
    }
}
