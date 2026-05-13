package reversi.core;

import java.util.Map;
import java.util.Set;

public interface GameSession {
    GameMode mode();

    Board board();

    Disc current();

    boolean isQuit();

    void quit();

    boolean isOver();

    Map<Disc, Integer> counts();

    Set<Position> validMoves();

    Set<Position> validMoves(Disc disc);

    ActionResult tryMove(Position p);

    default ActionResult tryMove(Position from, Position to, Character promotionPiece) {
        return ActionResult.INVALID_MOVE;
    }

    default ActionResult tryFlag(Position p) {
        return ActionResult.INVALID_MOVE;
    }

    ActionResult tryPass();
}
