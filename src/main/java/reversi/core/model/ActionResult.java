package reversi.core.model;

public enum ActionResult {
    OK,
    OUT_OF_BOUNDS,
    NOT_EMPTY,
    INVALID_MOVE,
    ALREADY_REVEALED,
    FLAG_ON,
    FLAG_OFF,
    FLAGGED_CELL,
    MINE_HIT,
    CLEAR_WIN,
    GAME_OVER,
    PASS_OK,
    PASS_NOT_ALLOWED,
    CHECKMATE,
    STALEMATE
}
