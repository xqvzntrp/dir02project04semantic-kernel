package semantic.test;

import app.historycompare.HistoryEquivalence;
import app.historycompare.HistoryEquivalenceEvaluator;
import app.historycompare.HistorySnapshotSummary;
import java.util.List;
import java.util.Set;
import semantic.kernel.ActionAdapter;
import semantic.kernel.ActionDescriptor;
import semantic.kernel.DefaultSemanticKernel;
import semantic.kernel.Projector;
import semantic.kernel.SemanticKernel;
import semantic.rules.NextMove;
import semantic.rules.TransitionRule;
import semantic.rules.TransitionTable;
import semantic.snapshot.SemanticSnapshot;

public final class HistorySensitiveRuleProbe {

    public static void main(String[] args) {
        SemanticKernel<ProbeState, ProbeEvent, NextMove<ProbeRuleState>, ProbeAction> kernel = kernel();

        SemanticSnapshot<ProbeState, NextMove<ProbeRuleState>, ProbeAction> firstCompletion =
            kernel.analyze(List.of(
                new ProbeCreated("probe-1"),
                new ProbeStarted("probe-1"),
                new ProbeCompleted("probe-1")));

        SemanticSnapshot<ProbeState, NextMove<ProbeRuleState>, ProbeAction> secondCompletion =
            kernel.analyze(List.of(
                new ProbeCreated("probe-1"),
                new ProbeStarted("probe-1"),
                new ProbeCompleted("probe-1"),
                new ProbeReopened("probe-1"),
                new ProbeCompleted("probe-1")));

        assertEquals(ProbeStatus.COMPLETED, firstCompletion.state().status(), "first completion status");
        assertEquals(ProbeStatus.COMPLETED, secondCompletion.state().status(), "second completion status");
        assertEquals(List.of(new ProbeAction("reopen")), firstCompletion.actions(), "first completion actions");
        assertEquals(List.of(), secondCompletion.actions(), "second completion actions");

        HistoryEquivalence equivalence = HistoryEquivalenceEvaluator.evaluate(
            new HistorySnapshotSummary<>(firstCompletion.state(), firstCompletion.actions()),
            new HistorySnapshotSummary<>(secondCompletion.state(), secondCompletion.actions())
        );

        if (equivalence != HistoryEquivalence.NONE) {
            throw new AssertionError("expected full-state comparison to reject STATE_EQUAL_ONLY probe but was " + equivalence);
        }

        System.out.println("firstCompletionState=" + firstCompletion.state());
        System.out.println("firstCompletionActions=" + firstCompletion.actions());
        System.out.println("secondCompletionState=" + secondCompletion.state());
        System.out.println("secondCompletionActions=" + secondCompletion.actions());
        System.out.println("equivalence=" + equivalence);
        System.out.println("result=history-sensitive rule was absorbed into full state, so STATE_EQUAL_ONLY was not observed");
    }

    private static SemanticKernel<ProbeState, ProbeEvent, NextMove<ProbeRuleState>, ProbeAction> kernel() {
        return new DefaultSemanticKernel<>(
            new ProbeProjector(),
            state -> new ProbeRuleState(state.status(), state.everReopened()),
            new TransitionTable<>(List.of(
                new TransitionRule<>(
                    "start",
                    Set.of(new ProbeRuleState(ProbeStatus.CREATED, false)),
                    new ProbeRuleState(ProbeStatus.IN_PROGRESS, false),
                    List.of(),
                    false,
                    false),
                new TransitionRule<>(
                    "complete",
                    Set.of(new ProbeRuleState(ProbeStatus.IN_PROGRESS, false)),
                    new ProbeRuleState(ProbeStatus.COMPLETED, false),
                    List.of(),
                    false,
                    true),
                new TransitionRule<>(
                    "complete",
                    Set.of(new ProbeRuleState(ProbeStatus.IN_PROGRESS, true)),
                    new ProbeRuleState(ProbeStatus.COMPLETED, true),
                    List.of(),
                    false,
                    true),
                new TransitionRule<>(
                    "reopen",
                    Set.of(new ProbeRuleState(ProbeStatus.COMPLETED, false)),
                    new ProbeRuleState(ProbeStatus.IN_PROGRESS, true),
                    List.of(),
                    false,
                    false))),
            new ProbeActionAdapter()
        );
    }

    private enum ProbeStatus {
        CREATED,
        IN_PROGRESS,
        COMPLETED
    }

    private record ProbeRuleState(ProbeStatus status, boolean everReopened) {
    }

    private record ProbeState(String id, ProbeStatus status, boolean everReopened) {
    }

    private sealed interface ProbeEvent permits ProbeCreated, ProbeStarted, ProbeCompleted, ProbeReopened {
        String id();
    }

    private record ProbeCreated(String id) implements ProbeEvent {
    }

    private record ProbeStarted(String id) implements ProbeEvent {
    }

    private record ProbeCompleted(String id) implements ProbeEvent {
    }

    private record ProbeReopened(String id) implements ProbeEvent {
    }

    private record ProbeAction(String eventName) {
    }

    private static final class ProbeProjector implements Projector<ProbeState, ProbeEvent> {
        @Override
        public ProbeState project(List<ProbeEvent> events) {
            ProbeState state = null;

            for (ProbeEvent event : events) {
                if (event instanceof ProbeCreated created) {
                    if (state != null) {
                        throw new IllegalStateException("probe already created");
                    }
                    state = new ProbeState(created.id(), ProbeStatus.CREATED, false);
                    continue;
                }

                if (state == null) {
                    throw new IllegalStateException("probe must be created first");
                }

                if (!state.id().equals(event.id())) {
                    throw new IllegalStateException("all probe events must use the same id");
                }

                if (event instanceof ProbeStarted) {
                    if (state.status() != ProbeStatus.CREATED) {
                        throw new IllegalStateException("probe can only start from CREATED");
                    }
                    state = new ProbeState(state.id(), ProbeStatus.IN_PROGRESS, state.everReopened());
                    continue;
                }

                if (event instanceof ProbeCompleted) {
                    if (state.status() != ProbeStatus.IN_PROGRESS) {
                        throw new IllegalStateException("probe can only complete from IN_PROGRESS");
                    }
                    state = new ProbeState(state.id(), ProbeStatus.COMPLETED, state.everReopened());
                    continue;
                }

                if (event instanceof ProbeReopened) {
                    if (state.status() != ProbeStatus.COMPLETED) {
                        throw new IllegalStateException("probe can only reopen from COMPLETED");
                    }
                    if (state.everReopened()) {
                        throw new IllegalStateException("probe can only reopen once");
                    }
                    state = new ProbeState(state.id(), ProbeStatus.IN_PROGRESS, true);
                    continue;
                }

                throw new IllegalStateException("unknown probe event: " + event.getClass().getName());
            }

            return state;
        }
    }

    private static final class ProbeActionAdapter
        implements ActionAdapter<ProbeState, NextMove<ProbeRuleState>, ProbeAction, ProbeEvent> {

        @Override
        public List<ProbeAction> fromMoves(List<NextMove<ProbeRuleState>> moves) {
            return moves.stream()
                .map(move -> new ProbeAction(move.eventName()))
                .toList();
        }

        @Override
        public List<ActionDescriptor> describe(List<ProbeAction> actions) {
            return actions.stream()
                .map(action -> new ActionDescriptor(action.eventName(), action.eventName(), List.of(), action.eventName()))
                .toList();
        }

        @Override
        public ProbeEvent toEvent(ProbeState state, List<ProbeAction> actions, Object request) {
            throw new UnsupportedOperationException("probe does not need request mapping");
        }
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }
}
