package reversi.games.chess;

import reversi.core.ActionResult;
import reversi.core.ChessGame;
import reversi.core.Disc;
import reversi.core.GameMode;
import reversi.core.GameSession;
import reversi.core.Position;
import reversi.gamehall.DemoScript;
import reversi.gamehall.GamePlugin;

import java.util.List;

public final class ChessGamePlugin implements GamePlugin {
    @Override
    public GameMode mode() {
        return GameMode.CHESS;
    }

    @Override
    public String id() {
        return "chess";
    }

    @Override
    public String displayName() {
        return "chess";
    }

    @Override
    public List<String> commands() {
        return List.of("chess");
    }

    @Override
    public GameSession createGame(int boardSize) {
        return ChessGame.newGame();
    }

    @Override
    public DemoScript createDemoScript(GameSession game) {
        return new ChessDemoScript((ChessGame) game);
    }

    private static final class ChessDemoScript implements DemoScript {
        private final ChessGame game;
        private final List<ChessMove> moves;
        private int index;

        private ChessDemoScript(ChessGame game) {
            this.game = game;
            this.moves = List.of(
                move("7e", "5e"),    // 1. e4 (Pawn)
                move("2e", "4e"),    // e5
                move("8g", "6f"),    // 2. Nf3 (Knight)
                move("1b", "3c"),    // Nc6
                move("8f", "5c"),    // 3. Bc4 (Bishop)
                move("1f", "4c"),    // Bc5
                move("7c", "6c"),    // 4. c3 (Pawn)
                move("1g", "3f"),    // Nf6 (Knight)
                move("7d", "5d"),    // 5. d4 (Pawn)
                move("4e", "5d"),    // exd4 [CAPTURE]
                move("6c", "5d"),    // 6. cxd4 [CAPTURE]
                move("4c", "3b"),    // Bb6 (Bishop)
                move("8e", "8g"),    // 7. O-O [CASTLE King-side]
                move("1e", "1g"),    // O-O [CASTLE King-side]
                move("8c", "4g"),    // 8. Bg5 (Bishop)
                move("2h", "3h"),    // h6 (Pawn)
                move("4g", "3f"),    // 9. Bxf6 [CAPTURE Knight]
                move("1d", "3f"),    // Qxf6 [CAPTURE Bishop] (Queen)
                move("8b", "7d"),    // 10. Nbd2 (Knight)
                move("2d", "4d"),    // d5 (Pawn)
                move("5e", "4d"),    // 11. exd5 [CAPTURE Pawn]
                move("1c", "5g"),    // Bg4 (Bishop)
                move("8d", "6b"),    // 12. Qb3 (Queen)
                move("5g", "6f"),    // Bxf3 [CAPTURE Knight]
                move("6b", "3b"),    // 13. Qxb6 [CAPTURE Bishop]
                move("2a", "3b"),    // axb6 [CAPTURE Queen]
                move("7g", "6f"),    // 14. gxf3 [CAPTURE Bishop]
                move("3c", "5b"),    // Nb4 (Knight)
                move("8a", "8c"),    // 15. Rac1 (Rook)
                move("1f", "1e"),    // Re8 (Rook)
                move("8f", "8d"),    // 16. Rfd1 (Rook)
                move("1e", "2e"),    // Re7 (Rook)
                move("4d", "3d"),    // 17. d6  [Pawn advance]
                move("5b", "4d"),    // Nd5 (Knight)
                move("3d", "2d"),    // 18. d7  [Pawn advance]
                move("2c", "3c"),    // c6 (Pawn)
                move("2d", "1d", "q"), // 19. d8=Q!! [PAWN PROMOTION to Queen!]
                move("1g", "2h"),    // Kh7 (King escape check)
                move("1d", "2e"),    // 20. Qxe7+! [CAPTURE Rook with check] (Queen)
                move("2h", "1g"),    // Kg8 (King)
                move("2e", "2f"),    // 21. Qxf7+ [CAPTURE Pawn with check] (Queen)
                move("1g", "1h"),    // Kh8 (King)
                move("5c", "4d"),    // 22. Bxd5! [CAPTURE Knight] (Bishop)
                move("1a", "2a"),    // Ra7 (Rook moves away)
                move("2f", "1g")     // 23. Qg8#!! [CHECKMATE!] (Queen)
            );
            this.index = 0;
        }

        @Override
        public String nextStep() {
            if (game.isOver()) {
                if (game.winner() != null) {
                    return "\u265f Checkmate! " + (game.winner() == Disc.WHITE ? "White" : "Black")
                        + " wins \u2014 a complete game with captures, castling, and promotion!";
                }
                return "\u265f Chess Demo finished.";
            }

            if (index >= moves.size()) {
                game.quit();
                return "\u265f Chess Demo complete.";
            }

            ChessMove move = moves.get(index);
            if (!canMoveFrom(move.from())) {
                index++;
                return nextStep();
            }

            ActionResult result = game.tryMove(move.from(), move.to(), move.promotion());
            if (result == ActionResult.OK || result == ActionResult.CHECKMATE) {
                String desc = describeMove(move, result);
                index++;
                return desc;
            }

            index++;
            return nextStep();
        }

        private String describeMove(ChessMove move, ActionResult result) {
            String piece = pieceEmoji(move);
            String from = label(move.from());
            String to = label(move.to());
            StringBuilder sb = new StringBuilder();
            sb.append(piece).append(" ").append(from).append(" \u2192 ").append(to);

            if (move.promotion() != null) {
                sb.append(" \u2605 Promote to Queen!");
            }

            String tag = moveTag(move);
            if (!tag.isEmpty()) {
                sb.append(" [").append(tag).append("]");
            }

            if (result == ActionResult.CHECKMATE) {
                sb.append(" \u2260 CHECKMATE!");
            }

            return sb.toString();
        }

        private String pieceEmoji(ChessMove move) {
            char piece = game.pieceAt(move.from());
            if (piece == '.') {
                piece = game.pieceAt(move.to());
            }
            return switch (Character.toLowerCase(piece)) {
                case 'k' -> "\u2654"; // King
                case 'q' -> "\u2655"; // Queen
                case 'r' -> "\u2656"; // Rook
                case 'b' -> "\u2657"; // Bishop
                case 'n' -> "\u2658"; // Knight
                case 'p' -> "\u2659"; // Pawn
                default -> "?";
            };
        }

        private String moveTag(ChessMove move) {
            int idx = moves.indexOf(move);
            if (idx < 0) return "";

            String[][] tags = {
                {"7e","5e"}, {"2e","4e"}, {"8g","6f"}, {"1b","3c"},
                {"8f","5c"}, {"1f","4c"}, {"7c","6c"}, {"1g","3f"},
                {"7d","5d"}, {"4e","5d"}, {"6c","5d"}, {"4c","3b"},
                {"8e","8g"}, {"1e","1g"}, {"8c","4g"}, {"2h","3h"},
                {"4g","3f"}, {"1d","3f"}, {"8b","7d"}, {"2d","4d"},
                {"5e","4d"}, {"1c","5g"}, {"8d","6b"}, {"5g","6f"},
                {"6b","3b"}, {"2a","3b"}, {"7g","6f"}, {"3c","5b"},
                {"8a","8c"}, {"1f","1e"}, {"8f","8d"}, {"1e","2e"},
                {"4d","3d"}, {"5b","4d"}, {"3d","2d"}, {"2c","3c"},
                {"2d","1d"}, {"1g","2h"}, {"1d","2e"}, {"2h","1g"},
                {"2e","2f"}, {"1g","1h"}, {"5c","4d"}, {"1a","2a"},
                {"2f","1g"},
            };

            String[] tagNames = {
                "Pawn", "Pawn", "Knight", "Knight",
                "Bishop", "Bishop", "Pawn", "Knight",
                "Pawn", "Capture", "Capture", "Bishop",
                "CASTLE", "CASTLE", "Bishop", "Pawn",
                "Capture", "Capture", "Knight", "Pawn",
                "Capture", "Bishop", "Queen", "Capture",
                "Capture", "Capture", "Capture", "Knight",
                "Rook", "Rook", "Rook", "Rook",
                "Advance", "Knight", "Advance", "Pawn",
                "PROMOTION!", "King", "Capture+", "King",
                "Capture+", "King", "Capture", "Rook",
                "CHECKMATE!",
            };

            if (idx < tagNames.length) {
                return tagNames[idx];
            }
            return "";
        }

        private boolean canMoveFrom(Position from) {
            char piece = game.pieceAt(from);
            if (piece == '.') {
                return false;
            }
            boolean lowercase = Character.isLowerCase(piece);
            return (game.current() == Disc.WHITE && lowercase) || (game.current() == Disc.BLACK && !lowercase);
        }

        private static ChessMove move(String from, String to) {
            return new ChessMove(Position.parse(from).orElseThrow(), Position.parse(to).orElseThrow(), null);
        }

        private static ChessMove move(String from, String to, String promotion) {
            return new ChessMove(Position.parse(from).orElseThrow(), Position.parse(to).orElseThrow(),
                promotion == null ? null : promotion.charAt(0));
        }

        private String label(Position p) {
            return (p.row() + 1) + String.valueOf((char) ('a' + p.col()));
        }
    }

    private record ChessMove(Position from, Position to, Character promotion) {
    }
}
