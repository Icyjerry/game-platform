package reversi.core.model;

import java.util.Optional;

public interface BoardView {
    int size();

    boolean isInside(Position p);

    Optional<Disc> get(Position p);

    boolean isEmpty(Position p);
}
