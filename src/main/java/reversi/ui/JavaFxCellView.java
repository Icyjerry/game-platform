package reversi.ui;

import javafx.scene.paint.Color;
import reversi.core.BoardGameSession;
import reversi.core.GameSession;
import reversi.core.model.Disc;
import reversi.core.model.Position;
import reversi.games.chess.ChessSession;
import reversi.games.minesweeper.MinesweeperSession;

import java.util.Optional;
import java.util.Set;

final class JavaFxCellView {
    private static final Color BOARD_LIGHT = Color.web("#3f8f46");
    private static final Color BOARD_DARK = Color.web("#2f7537");
    private static final Color CHESS_LIGHT = Color.web("#f0d9b5");
    private static final Color CHESS_DARK = Color.web("#b58863");
    private static final Color MINE_HIDDEN = Color.web("#c7c7c7");
    private static final Color MINE_HIDDEN_DARK = Color.web("#b5b5b5");
    private static final Color MINE_OPEN = Color.web("#8f8f8f");
    private static final Color COORD = Color.web("#5f6d5f");
    private static final Color FG = Color.web("#f6f1e6");
    private static final Color BLK_PC = Color.BLACK;
    private static final Color WHT_PC = Color.WHITE;
    private static final Color MINE_CLR = Color.web("#b00020");
    private static final Color FLAG_CLR = Color.web("#b00020");
    private static final Color LEGAL_MARK = Color.web("#f2d16b");
    private static final String BLACK_OUTLINE = "-fx-effect: dropshadow(gaussian, white, 2.4, 0.95, 0, 0);";
    private static final String WHITE_OUTLINE = "-fx-effect: dropshadow(gaussian, black, 2.4, 0.95, 0, 0);";

    private JavaFxCellView() {
    }

    static CellView of(GameSession game, Position p, Set<Position> legal,
                       Position selected, Position lastClicked) {
        String text = text(game, p, legal);
        return new CellView(
            text,
            textColor(game, p, text),
            style(game, p, legal, selected, lastClicked),
            tip(game, p),
            textStyle(game, p)
        );
    }

    private static String text(GameSession game, Position p, Set<Position> legal) {
        if (game instanceof MinesweeperSession) {
            String display = game.cellDisplay(p);
            return switch (display) {
                case "#" -> "";
                case "." -> "·";
                case "F" -> "⚑";
                case "*" -> "✹";
                default -> display;
            };
        }
        if (game instanceof ChessSession chess) {
            return chessSymbol(chess.pieceAt(p));
        }
        if (game instanceof BoardGameSession boardGame) {
            Optional<Disc> disc = boardGame.board().get(p);
            if (disc.isPresent()) {
                return disc.get() == Disc.BLACK ? "●" : "○";
            }
            return legal.contains(p) ? "•" : "";
        }
        return game.cellDisplay(p);
    }

    private static Color textColor(GameSession game, Position p, String text) {
        if (game instanceof MinesweeperSession) {
            return switch (text) {
                case "⚑" -> FLAG_CLR;
                case "✹" -> MINE_CLR;
                case "·" -> COORD;
                case "1" -> Color.CYAN;
                case "2" -> Color.LIGHTGREEN;
                case "3" -> Color.YELLOW;
                case "4" -> Color.web("#add8e6");
                case "5" -> Color.MAGENTA;
                case "6" -> Color.AQUAMARINE;
                case "7" -> Color.ORANGERED;
                case "8" -> Color.WHITE;
                default -> FG;
            };
        }
        if (game instanceof ChessSession chess) {
            char piece = chess.pieceAt(p);
            if (piece == '.') {
                return COORD;
            }
            return Character.isUpperCase(piece) ? BLK_PC : WHT_PC;
        }
        if (game instanceof BoardGameSession boardGame) {
            Optional<Disc> disc = boardGame.board().get(p);
            if (disc.isPresent()) {
                return disc.get() == Disc.BLACK ? BLK_PC : WHT_PC;
            }
        }
        return "•".equals(text) ? LEGAL_MARK : FG;
    }

    private static String style(GameSession game, Position p, Set<Position> legal,
                                Position selected, Position lastClicked) {
        boolean isSelected = selected != null && selected.equals(p);
        boolean isLast = lastClicked != null && lastClicked.equals(p);
        boolean light = (p.row() + p.col()) % 2 == 0;
        Color base = light ? BOARD_LIGHT : BOARD_DARK;
        String border = "#1b4f23";
        int radius = 0;
        if (game instanceof MinesweeperSession) {
            String display = game.cellDisplay(p);
            base = switch (display) {
                case "#", "F" -> light ? MINE_HIDDEN : MINE_HIDDEN_DARK;
                default -> MINE_OPEN;
            };
            border = switch (display) {
                case "#", "F" -> "#f2f2f2 #6f6f6f #6f6f6f #f2f2f2";
                default -> "#777777";
            };
            radius = 2;
        } else if (game instanceof ChessSession) {
            base = light ? CHESS_LIGHT : CHESS_DARK;
            border = light ? "#d8bc8f" : "#8c623d";
        }
        if (legal.contains(p) && !(game instanceof MinesweeperSession)) {
            base = game instanceof ChessSession ? Color.web("#cfb06f") : Color.web("#4d9e4e");
        }
        border = isSelected ? "#d07a00" : (isLast ? "#6b4a1f" : border);
        int width = isSelected || isLast ? 2 : 1;
        return "-fx-background-color: " + hex(base) + ";"
            + "-fx-border-color: " + border + ";"
            + "-fx-border-width: " + width + ";"
            + "-fx-background-radius: " + radius + ";"
            + "-fx-border-radius: " + radius + ";";
    }

    private static String tip(GameSession game, Position p) {
        String label = (p.row() + 1) + String.valueOf((char) ('a' + p.col()));
        if (game instanceof MinesweeperSession) {
            return label + ": left click open, right click flag";
        }
        if (game instanceof ChessSession) {
            return label + ": select or move";
        }
        return label + ": play here";
    }

    private static String textStyle(GameSession game, Position p) {
        if (game instanceof ChessSession chess) {
            char piece = chess.pieceAt(p);
            if (piece == '.') {
                return "";
            }
            return Character.isUpperCase(piece) ? BLACK_OUTLINE : WHITE_OUTLINE;
        }
        if (game instanceof BoardGameSession boardGame) {
            Optional<Disc> disc = boardGame.board().get(p);
            if (disc.isPresent()) {
                return disc.get() == Disc.BLACK ? BLACK_OUTLINE : WHITE_OUTLINE;
            }
        }
        return "";
    }

    private static String chessSymbol(char piece) {
        return switch (piece) {
            case 'K' -> "♚";
            case 'Q' -> "♛";
            case 'R' -> "♜";
            case 'B' -> "♝";
            case 'N' -> "♞";
            case 'P' -> "♟";
            case 'k' -> "♔";
            case 'q' -> "♕";
            case 'r' -> "♖";
            case 'b' -> "♗";
            case 'n' -> "♘";
            case 'p' -> "♙";
            default -> "";
        };
    }

    private static String hex(Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }

    record CellView(String text, Color textColor, String style, String tip, String textStyle) {
    }
}
