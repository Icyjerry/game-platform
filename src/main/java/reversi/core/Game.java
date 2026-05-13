package reversi.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class Game implements GameSession {
    private final Board board;
    private Disc current;
    private boolean quit;

    private Game(Board board) {
        this.board = Objects.requireNonNull(board, "board");
        this.current = Disc.BLACK;
        this.quit = false;
    }

    public static Game newGame(int boardSize) {
        return new Game(Board.newBoard(boardSize));
    }

    @Override
    public GameMode mode() {
        return GameMode.REVERSI;
    }

    @Override
    public Board board() {
        return board;
    }

    @Override
    public Disc current() {
        return current;
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
        if (quit || board.isFull()) {
            return true;
        }
        return !hasValidMoves(Disc.BLACK) && !hasValidMoves(Disc.WHITE);
    }

    @Override
    public Set<Position> validMoves() {
        return validMoves(current);
    }

    @Override
    public Set<Position> validMoves(Disc disc) {
        if (disc == null) {
            return Set.of();
        }
        Set<Position> moves = new LinkedHashSet<>();
        for (int r = 0; r < board.size(); r++) {
            for (int c = 0; c < board.size(); c++) {
                Position p = new Position(r, c);
                if (board.isEmpty(p) && !getFlippableDiscs(p, disc).isEmpty()) {
                    moves.add(p);
                }
            }
        }
        return moves;
    }

    private List<Position> getFlippableDiscs(Position p, Disc disc) {
        List<Position> flippable = new ArrayList<>();
        if (!board.isEmpty(p)) {
            return flippable;
        }
        int[][] dirs = { {-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1} };
        Disc opponent = disc.opposite();
        for (int[] d : dirs) {
            int r = p.row() + d[0];
            int c = p.col() + d[1];
            List<Position> temp = new ArrayList<>();
            // First check whether the adjacent cell contains an opponent disc.
            Position first = new Position(r, c);
            if (!board.isInside(first) || board.get(first).orElse(null) != opponent) {
                continue;
            }
            // Continue scanning along the current direction.
            while (board.isInside(first)) {
                Optional<Disc> opt = board.get(first);
                if (opt.isEmpty()) {
                    break;
                }
                if (opt.get() == disc) {
                    flippable.addAll(temp);
                    break;
                }
                temp.add(first);
                r += d[0];
                c += d[1];
                first = new Position(r, c);
            }
        }
        return flippable;
    }

    private boolean hasValidMoves(Disc disc) {
        for (int r = 0; r < board.size(); r++) {
            for (int c = 0; c < board.size(); c++) {
                Position p = new Position(r, c);
                if (board.isEmpty(p) && !getFlippableDiscs(p, disc).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public ActionResult tryMove(Position p) {
        if (isOver()) {
            return ActionResult.GAME_OVER;
        }
        if (p == null || !board.isInside(p)) {
            return ActionResult.OUT_OF_BOUNDS;
        }
        if (!board.isEmpty(p)) {
            return ActionResult.NOT_EMPTY;
        }

        List<Position> flippable = getFlippableDiscs(p, current);
        if (flippable.isEmpty()) {
            return ActionResult.INVALID_MOVE;
        }

        boolean placed = board.place(p, current);
        if (!placed) {
            return ActionResult.NOT_EMPTY;
        }

        for (Position flipPos : flippable) {
            board.flip(flipPos);
        }

        current = current.opposite();
        return ActionResult.OK;
    }

    @Override
    public Map<Disc, Integer> counts() {
        return board.countDiscs();
    }

    @Override
    public ActionResult tryPass() {
        if (isOver()) {
            return ActionResult.GAME_OVER;
        }
        if (!validMoves(current).isEmpty()) {
            return ActionResult.PASS_NOT_ALLOWED;
        }
        current = current.opposite();
        return ActionResult.PASS_OK;
    }
}
