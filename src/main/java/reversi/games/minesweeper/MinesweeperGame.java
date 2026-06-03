package reversi.games.minesweeper;

import reversi.core.model.*;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

public final class MinesweeperGame implements MinesweeperSession {
    private final Board board;
    private final int mineCount;
    private final boolean[][] mines;
    private final boolean[][] revealed;
    private final boolean[][] flagged;
    private final Random random;
    private boolean generated;
    private boolean quit;
    private boolean lost;
    private int revealedSafeCells;
    private String lastMessage;

    private MinesweeperGame(int size, int mineCount, Random random) {
        this.board = Board.newEmptyBoard(size);
        this.mineCount = mineCount;
        this.mines = new boolean[size][size];
        this.revealed = new boolean[size][size];
        this.flagged = new boolean[size][size];
        this.random = random;
        this.generated = false;
        this.quit = false;
        this.lost = false;
        this.revealedSafeCells = 0;
        this.lastMessage = "";
    }

    public static MinesweeperGame newGame(int size, int mineCount) {
        return new MinesweeperGame(size, mineCount, new Random());
    }

    public static MinesweeperGame newGame(int size, int mineCount, Random random) {
        return new MinesweeperGame(size, mineCount, random);
    }

    @Override
    public GameMode mode() {
        return GameMode.MINESWEEPER;
    }

    @Override
    public int boardSize() {
        return board.size();
    }

    @Override
    public boolean isQuit() {
        return quit;
    }

    @Override
    public void quit() {
        quit = true;
    }

    @Override
    public boolean isOver() {
        return quit || lost || revealedSafeCells == totalSafeCells();
    }

    @Override
    public Set<Position> validMoves() {
        Set<Position> moves = new LinkedHashSet<>();
        for (int row = 0; row < board.size(); row++) {
            for (int col = 0; col < board.size(); col++) {
                Position position = new Position(row, col);
                if (!revealed[row][col] && !flagged[row][col]) {
                    moves.add(position);
                }
            }
        }
        return moves;
    }

    @Override
    public ActionResult tryMove(Position p) {
        if (isOver()) {
            return ActionResult.GAME_OVER;
        }
        if (p == null || !board.isInside(p)) {
            return ActionResult.OUT_OF_BOUNDS;
        }
        if (revealed[p.row()][p.col()]) {
            return ActionResult.ALREADY_REVEALED;
        }
        if (flagged[p.row()][p.col()]) {
            return ActionResult.FLAGGED_CELL;
        }
        if (!generated) {
            generateMines(p);
        }

        if (mines[p.row()][p.col()]) {
            lost = true;
            revealed[p.row()][p.col()] = true;
            lastMessage = "Hit a mine. Game over";
            return ActionResult.MINE_HIT;
        }

        int revealedNow = revealFrom(p);
        if (revealedSafeCells == totalSafeCells()) {
            lastMessage = "All safe cells revealed. You win";
            return ActionResult.CLEAR_WIN;
        }
        lastMessage = revealedNow <= 1
            ? "Revealed " + toLabel(p)
            : "Revealed " + toLabel(p) + " and " + (revealedNow - 1) + " more cells";
        return ActionResult.OK;
    }

    @Override
    public ActionResult tryFlag(Position p) {
        if (isOver()) {
            return ActionResult.GAME_OVER;
        }
        if (p == null || !board.isInside(p)) {
            return ActionResult.OUT_OF_BOUNDS;
        }
        if (revealed[p.row()][p.col()]) {
            return ActionResult.ALREADY_REVEALED;
        }

        flagged[p.row()][p.col()] = !flagged[p.row()][p.col()];
        if (flagged[p.row()][p.col()]) {
            lastMessage = "Flagged " + toLabel(p);
            return ActionResult.FLAG_ON;
        }

        lastMessage = "Removed flag from " + toLabel(p);
        return ActionResult.FLAG_OFF;
    }

    @Override
    public boolean isGenerated() {
        return generated;
    }

    @Override
    public int mineCount() {
        return mineCount;
    }

    @Override
    public int flagCount() {
        int count = 0;
        for (int row = 0; row < board.size(); row++) {
            for (int col = 0; col < board.size(); col++) {
                if (flagged[row][col]) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public String lastMessage() {
        return lastMessage;
    }

    @Override
    public boolean isRevealed(Position p) {
        return board.isInside(p) && revealed[p.row()][p.col()];
    }

    @Override
    public String cellDisplay(Position p) {
        int row = p.row();
        int col = p.col();
        if (!board.isInside(p)) {
            return "#";
        }
        if (isOver() && mines[row][col]) {
            return "*";
        }
        if (flagged[row][col]) {
            return "F";
        }
        if (!revealed[row][col]) {
            return "#";
        }
        int adjacent = adjacentMines(p);
        return adjacent == 0 ? "." : String.valueOf(adjacent);
    }

    private int revealFrom(Position start) {
        int opened = revealSafeCell(start);
        if (adjacentMines(start) > 0) {
            return opened;
        }

        Queue<Position> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            Position current = queue.remove();
            if (adjacentMines(current) > 0) {
                continue;
            }
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) {
                        continue;
                    }
                    Position next = new Position(current.row() + dr, current.col() + dc);
                    if (!board.isInside(next)
                        || mines[next.row()][next.col()]
                        || flagged[next.row()][next.col()]
                        || revealed[next.row()][next.col()]) {
                        continue;
                    }
                    opened += revealSafeCell(next);
                    if (adjacentMines(next) == 0) {
                        queue.add(next);
                    }
                }
            }
        }
        return opened;
    }

    private int revealSafeCell(Position p) {
        if (!revealed[p.row()][p.col()]) {
            revealed[p.row()][p.col()] = true;
            revealedSafeCells++;
            return 1;
        }
        return 0;
    }

    private void generateMines(Position firstMove) {
        int placed = 0;
        while (placed < mineCount) {
            int row = random.nextInt(board.size());
            int col = random.nextInt(board.size());
            if (mines[row][col]) {
                continue;
            }
            if (row == firstMove.row() && col == firstMove.col()) {
                continue;
            }
            mines[row][col] = true;
            placed++;
        }
        generated = true;
    }

    public boolean[][] getMines() {
        return mines;
    }

    public boolean[][] getRevealed() {
        return revealed;
    }

    private int adjacentMines(Position p) {
        int count = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) {
                    continue;
                }
                Position next = new Position(p.row() + dr, p.col() + dc);
                if (board.isInside(next) && mines[next.row()][next.col()]) {
                    count++;
                }
            }
        }
        return count;
    }

    private int totalSafeCells() {
        return board.size() * board.size() - mineCount;
    }

    private String toLabel(Position p) {
        return (p.row() + 1) + String.valueOf((char) ('A' + p.col()));
    }
}
