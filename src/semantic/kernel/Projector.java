package semantic.kernel;

import java.util.List;

public interface Projector<S, E> {

    S project(List<E> events);
}
