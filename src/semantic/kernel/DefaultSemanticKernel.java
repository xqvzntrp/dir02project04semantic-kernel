package semantic.kernel;

import java.util.List;
import java.util.function.Function;
import semantic.rules.TransitionTable;

public final class DefaultSemanticKernel<S, R, E, A>
    implements SemanticKernel<S, E, semantic.rules.NextMove<R>, A> {

    private final Projector<S, E> projector;
    private final Function<S, R> ruleStateExtractor;
    private final TransitionTable<R> transitionTable;
    private final ActionAdapter<S, semantic.rules.NextMove<R>, A, E> actionAdapter;

    public DefaultSemanticKernel(
        Projector<S, E> projector,
        Function<S, R> ruleStateExtractor,
        TransitionTable<R> transitionTable,
        ActionAdapter<S, semantic.rules.NextMove<R>, A, E> actionAdapter
    ) {
        this.projector = projector;
        this.ruleStateExtractor = ruleStateExtractor;
        this.transitionTable = transitionTable;
        this.actionAdapter = actionAdapter;
    }

    @Override
    public S project(List<E> events) {
        return projector.project(events);
    }

    @Override
    public List<semantic.rules.NextMove<R>> nextMoves(S state) {
        return transitionTable.nextMoves(ruleStateExtractor.apply(state));
    }

    @Override
    public List<A> actions(S state, List<semantic.rules.NextMove<R>> moves) {
        return actionAdapter.fromMoves(moves);
    }

    @Override
    public E toEvent(S state, List<A> actions, Object request) {
        return actionAdapter.toEvent(state, actions, request);
    }
}
