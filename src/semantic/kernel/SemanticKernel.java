package semantic.kernel;

import java.util.List;
import semantic.snapshot.SemanticSnapshot;

public interface SemanticKernel<S, E, M, A> {

    S project(List<E> events);

    List<M> nextMoves(S state);

    List<A> actions(S state, List<M> moves);

    E toEvent(S state, List<A> actions, Object request);

    default SemanticSnapshot<S, M, A> analyze(List<E> events) {
        S state = project(events);
        List<M> moves = nextMoves(state);
        List<A> availableActions = actions(state, moves);
        return new SemanticSnapshot<>(state, moves, availableActions);
    }
}
