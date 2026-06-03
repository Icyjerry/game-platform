package reversi.core;

import reversi.core.model.*;

public interface GameSession {
    GameMode mode();

    int boardSize();

    String cellDisplay(Position p);

    boolean isQuit();

    void quit();

    boolean isOver();
}
