package reversi.games.minesweeper;

import reversi.core.FlaggableSession;
import reversi.core.PositionMoveSession;
import reversi.core.model.*;

public interface MinesweeperSession extends PositionMoveSession, FlaggableSession {
    int mineCount();

    int flagCount();

    String lastMessage();

    boolean isGenerated();

    boolean isRevealed(Position p);
}
