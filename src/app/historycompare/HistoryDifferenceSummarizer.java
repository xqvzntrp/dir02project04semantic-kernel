package app.historycompare;

import java.util.ArrayList;
import java.util.List;

public final class HistoryDifferenceSummarizer {
    private HistoryDifferenceSummarizer() {
    }

    public static <E> HistoryDifferenceSummary summarize(
        TimelineEquivalenceResult timeline,
        List<EventAttributionPoint<E>> attribution,
        HistoryEquivalence finalEquivalence
    ) {
        int firstStateDivergenceIndex = -1;
        int firstActionDivergenceIndex = -1;
        int firstStateRepairIndex = -1;
        int firstActionRepairIndex = -1;

        List<Integer> stateIntroducingEvents = new ArrayList<>();
        List<Integer> actionIntroducingEvents = new ArrayList<>();
        List<Integer> stateRepairingEvents = new ArrayList<>();
        List<Integer> actionRepairingEvents = new ArrayList<>();

        for (EventAttributionPoint<E> point : attribution) {
            int index = point.index();
            switch (point.effect()) {
                case INTRODUCES_STATE_DIFFERENCE -> {
                    if (firstStateDivergenceIndex < 0) {
                        firstStateDivergenceIndex = index;
                    }
                    stateIntroducingEvents.add(index);
                }
                case INTRODUCES_ACTION_DIFFERENCE -> {
                    if (firstActionDivergenceIndex < 0) {
                        firstActionDivergenceIndex = index;
                    }
                    actionIntroducingEvents.add(index);
                }
                case INTRODUCES_STATE_AND_ACTION_DIFFERENCE -> {
                    if (firstStateDivergenceIndex < 0) {
                        firstStateDivergenceIndex = index;
                    }
                    if (firstActionDivergenceIndex < 0) {
                        firstActionDivergenceIndex = index;
                    }
                    stateIntroducingEvents.add(index);
                    actionIntroducingEvents.add(index);
                }
                case REPAIRS_STATE_DIFFERENCE -> {
                    if (firstStateRepairIndex < 0) {
                        firstStateRepairIndex = index;
                    }
                    stateRepairingEvents.add(index);
                }
                case REPAIRS_ACTION_DIFFERENCE -> {
                    if (firstActionRepairIndex < 0) {
                        firstActionRepairIndex = index;
                    }
                    actionRepairingEvents.add(index);
                }
                case REPAIRS_STATE_AND_ACTION_DIFFERENCE -> {
                    if (firstStateRepairIndex < 0) {
                        firstStateRepairIndex = index;
                    }
                    if (firstActionRepairIndex < 0) {
                        firstActionRepairIndex = index;
                    }
                    stateRepairingEvents.add(index);
                    actionRepairingEvents.add(index);
                }
                default -> {
                }
            }
        }

        return new HistoryDifferenceSummary(
            timeline.firstStructuralDivergenceIndex(),
            firstStateDivergenceIndex,
            firstActionDivergenceIndex,
            firstStateRepairIndex,
            firstActionRepairIndex,
            stateIntroducingEvents,
            actionIntroducingEvents,
            stateRepairingEvents,
            actionRepairingEvents,
            finalEquivalence,
            DivergencePatternEvaluator.evaluate(timeline, attribution)
        );
    }
}
