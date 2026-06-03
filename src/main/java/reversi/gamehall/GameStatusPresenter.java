package reversi.gamehall;

import reversi.core.BoardGameSession;
import reversi.core.GameSession;
import reversi.core.model.Disc;
import reversi.core.model.GameMode;
import reversi.games.chess.ChessSession;
import reversi.games.minesweeper.MinesweeperSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GameStatusPresenter {
    private GameStatusPresenter() {
    }

    public static List<String> statusLines(GameRegistry registry, GameSession game, String cursorLabel,
                                           String fallbackMessage, boolean demoMode) {
        if (demoMode) {
            List<String> base = statusLines(registry, game, cursorLabel, fallbackMessage, false);
            ArrayList<String> lines = new ArrayList<>();
            lines.add("[ DEMO MODE ]");
            lines.add("Demo: running");
            lines.addAll(base);
            return lines;
        }
        if (game instanceof MinesweeperSession ms) {
            String state = ms.isOver()
                ? (ms.lastMessage().contains("win") ? "State: Won" : "State: Lost")
                : "State: In progress";
            return List.of(
                "Mode: minesweeper",
                "Mines: " + ms.mineCount(),
                "Flags: " + ms.flagCount(),
                state,
                "Cursor: " + cursorLabel,
                "Last: " + lastMessage(fallbackMessage, ms.lastMessage()),
                "Hint: first move safe"
            );
        }
        if (game instanceof ChessSession chess) {
            if (game.isOver()) {
                Map<Disc, Map<Character, Integer>> detail = chess.pieceDetails();
                return List.of(
                    "Mode: chess",
                    "State: === GAME OVER ===",
                    chess.resultSummary(),
                    "White: " + pieceCountString(detail.get(Disc.WHITE)),
                    "Black: " + pieceCountString(detail.get(Disc.BLACK)),
                    "Cursor: " + cursorLabel,
                    "Last: " + lastMessage(fallbackMessage, chess.lastMessage())
                );
            }
            return List.of(
                "Mode: chess",
                "To move: " + GameController.chessTurnLabel(chess),
                "State: running",
                "Move: " + chess.moveCount(),
                "Cursor: " + cursorLabel,
                "Last: " + lastMessage(fallbackMessage, chess.lastMessage()),
                "Hint: select a source and target"
            );
        }
        if (!(game instanceof BoardGameSession boardGame)) {
            return List.of(
                "Mode: " + registry.displayName(game.mode()),
                "State: " + (game.isOver() ? "over" : "running"),
                "Cursor: " + cursorLabel,
                "Last: " + lastMessage(fallbackMessage, "")
            );
        }

        Map<Disc, Integer> cnt = boardGame.counts();
        int b = cnt.getOrDefault(Disc.BLACK, 0);
        int w = cnt.getOrDefault(Disc.WHITE, 0);
        if (game.mode().equals(GameMode.PEACE)) {
            return List.of(
                "Mode: peace",
                "Turn: " + discName(boardGame.current()),
                "Score B/W: " + b + " / " + w,
                "State: " + (game.isOver() ? "over" : "running"),
                "Cursor: " + cursorLabel,
                "Last: " + lastMessage(fallbackMessage, "")
            );
        }
        String passHint = (!game.isOver() && boardGame.validMoves().isEmpty())
            ? "Hint: pass available" : "Hint: choose a legal cell";
        return List.of(
            "Mode: reversi",
            "Turn: " + discName(boardGame.current()),
            "Score B/W: " + b + " / " + w,
            "Legal: " + boardGame.validMoves().size(),
            "Cursor: " + cursorLabel,
            "Last: " + lastMessage(fallbackMessage, ""),
            passHint
        );
    }

    private static String discName(Disc d) {
        return d == Disc.BLACK ? "Black (●)" : "White (○)";
    }

    private static String lastMessage(String override, String fallback) {
        if (override != null && !override.isBlank()) {
            return override;
        }
        return fallback == null ? "" : fallback;
    }

    private static String pieceCountString(Map<Character, Integer> detail) {
        if (detail == null || detail.isEmpty()) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        char[] order = {'K', 'Q', 'R', 'B', 'N', 'P'};
        for (char type : order) {
            Integer count = detail.get(type);
            if (count != null) {
                sb.append(count).append(type).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
