package semantic.kernel;

import java.util.List;

public interface ActionAdapter<S, M, A, E> {

    List<A> fromMoves(List<M> moves);

    List<ActionDescriptor> describe(List<A> actions);

    E toEvent(S state, List<A> actions, Object request);
}
