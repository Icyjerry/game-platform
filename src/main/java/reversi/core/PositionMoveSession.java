package reversi.core;

import reversi.core.model.*;

import java.util.Set;

public interface PositionMoveSession extends GameSession {
    Set<Position> validMoves();

    ActionResult tryMove(Position p);
}
