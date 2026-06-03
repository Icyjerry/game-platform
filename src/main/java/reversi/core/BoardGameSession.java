package reversi.core;

import reversi.core.model.*;

import java.util.Map;
import java.util.Set;

public interface BoardGameSession extends PositionMoveSession {
    BoardView board();

    Disc current();

    Map<Disc, Integer> counts();

    Set<Position> validMoves(Disc disc);
}
