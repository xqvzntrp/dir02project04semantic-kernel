package semantic.snapshot;

import java.util.List;

public record SemanticSnapshot<S, M, A>(
    S state,
    List<M> nextMoves,
    List<A> actions
) {
    public SemanticSnapshot {
        nextMoves = List.copyOf(nextMoves);
        actions = List.copyOf(actions);
    }
}
