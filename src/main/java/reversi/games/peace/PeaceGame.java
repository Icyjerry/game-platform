package reversi.games.peace;

import reversi.core.BoardGameSession;
import reversi.core.model.*;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class PeaceGame implements BoardGameSession {
    private final Board board;
    private Disc current;
    private boolean quit;

    private PeaceGame(Board board) {
        this.board = Objects.requireNonNull(board, "board");
        this.current = Disc.BLACK;
        this.quit = false;
    }

    public static PeaceGame newGame(int boardSize) {
        return new PeaceGame(Board.newBoard(boardSize));
    }

    @Override
    public GameMode mode() {
        return GameMode.PEACE;
    }

    @Override
    public int boardSize() {
        return board.size();
    }

    @Override
    public Board board() {
        return board;
    }

    @Override
    public String cellDisplay(Position p) {
        return board.get(p)
            .map(disc -> disc == Disc.BLACK ? "●" : "○")
            .orElse("·");
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
        return quit || board.isFull();
    }

    @Override
    public Map<Disc, Integer> counts() {
        return board.countDiscs();
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
                if (board.isEmpty(p)) {
                    moves.add(p);
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
        if (!board.isEmpty(p)) {
            return ActionResult.NOT_EMPTY;
        }

        boolean placed = board.place(p, current);
        if (!placed) {
            return ActionResult.NOT_EMPTY;
        }

        current = current.opposite();
        return ActionResult.OK;
    }

}
