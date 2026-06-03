package reversi.gamehall;

import reversi.command.ParsedCommand;
import reversi.core.model.ActionResult;
import reversi.core.BoardGameSession;
import reversi.games.chess.ChessSession;
import reversi.core.model.Disc;
import reversi.core.FlaggableSession;
import reversi.core.model.GameMode;
import reversi.core.GameSession;
import reversi.core.PassableSession;
import reversi.core.model.Position;
import reversi.core.PositionMoveSession;

import java.util.List;
import java.util.Set;

public final class GameController {
    private GameController() {
    }

    public static String dispatch(MultiGameManager manager, GameSession game, ParsedCommand cmd) {
        return switch (cmd.type()) {
            case NEW_GAME -> {
                manager.addGame(cmd.gameMode());
                yield "Added " + manager.registry().displayName(cmd.gameMode()) + " game " + manager.totalGames();
            }
            case SWITCH -> manager.switchTo(cmd.boardIndex() - 1)
                ? "Switched to board " + cmd.boardIndex()
                : "Invalid board number";
            case PASS -> {
                if (game instanceof PassableSession passable) {
                    yield resultMessage(game, passable.tryPass());
                }
                yield "Pass is not supported in this mode";
            }
            case FLAG -> {
                if (game instanceof FlaggableSession flaggable) {
                    yield resultMessage(game, flaggable.tryFlag(cmd.position()));
                }
                yield "Flag is only available in minesweeper";
            }
            case MOVE -> {
                if (game instanceof ChessSession chess) {
                    if (cmd.from() == null || cmd.to() == null) yield "坐标越界或格式错误";
                    chess.tryMove(cmd.from(), cmd.to(), cmd.promotionPiece());
                    yield chess.lastMessage();
                }
                if (game.isOver()) yield "This board is over";
                if (game instanceof PositionMoveSession mover) {
                    yield resultMessage(game, mover.tryMove(cmd.position()));
                }
                yield "This mode requires a different move command";
            }
            case DEMO  -> "Demo mode started";
            case STOP  -> "Demo is not running";
            case QUIT  -> "";
            case INVALID -> "Invalid input. Use a coordinate, move 1a 2a, f <coord>, switch <n>, demo, stop, or quit";
        };
    }

    public static String resultMessage(GameSession game, ActionResult result) {
        return switch (result) {
            case OK               -> "";
            case OUT_OF_BOUNDS    -> "Out of bounds";
            case NOT_EMPTY        -> "Cell is not empty";
            case INVALID_MOVE     -> (game.mode().equals(GameMode.REVERSI) && validMovesFor(game).isEmpty())
                    ? "No legal moves. Type pass" : "Invalid move";
            case ALREADY_REVEALED -> game.mode().equals(GameMode.MINESWEEPER) ? "Cell already revealed" : "Cell is already occupied";
            case FLAG_ON          -> "Flag placed";
            case FLAG_OFF         -> "Flag removed";
            case FLAGGED_CELL     -> "Cell is flagged. Remove the flag first";
            case MINE_HIT         -> "Hit a mine. Game over";
            case CLEAR_WIN        -> "All safe cells revealed. You win";
            case GAME_OVER        -> "This board is over";
            case PASS_OK          -> "Turn passed";
            case PASS_NOT_ALLOWED -> game.mode().equals(GameMode.REVERSI)
                    ? "Pass is not allowed because legal moves exist" : "Pass is not supported in this mode";
            case CHECKMATE        -> "Checkmate! Game over";
            case STALEMATE        -> "Stalemate! Game is a draw";
        };
    }

    public static List<String> commandHelp(GameSession game) {
        if (game.mode().equals(GameMode.MINESWEEPER)) {
            return List.of("1a       open", "f 1a     flag", "s 2      switch",
                "minesweeper new", "demo     auto", "stop     manual", "quit");
        }
        if (game.mode().equals(GameMode.CHESS)) {
            return List.of("m 1a 2a  move", "m 7a 8a q prom", "s 2      switch",
                "chess    new game", "demo     auto", "stop     manual", "quit");
        }
        if (game instanceof PassableSession) {
            return List.of("1a       move", "s 2      switch", "reversi  new game",
                "pass", "demo     auto", "stop     manual", "quit");
        }
        return List.of("1a       move", "s 2      switch", "peace    new game",
            "demo     auto", "stop     manual", "quit");
    }

    public static String chessTurnLabel(GameSession game) {
        if (game.isOver()) return "-";
        return game instanceof ChessSession chess && chess.current() == Disc.WHITE
            ? "White pieces (lowercase)" : "Black pieces (UPPERCASE)";
    }

    public static Set<Position> validMovesFor(GameSession game) {
        if (!game.isOver() && game instanceof BoardGameSession boardGame
                && game.mode().equals(GameMode.REVERSI)) {
            return boardGame.validMoves();
        }
        return Set.of();
    }

}
