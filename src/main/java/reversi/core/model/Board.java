package reversi.core.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class Board implements BoardView {
    private final int size;
    private final Disc[][] cells;
    private int filledCount;

    private Board(int size, boolean withStartingPosition) {
        if (size < 4 || size % 2 != 0) {
            throw new IllegalArgumentException("Board size must be even and >= 4");
        }
        this.size = size;
        this.cells = new Disc[size][size];
        this.filledCount = 0;
        if (withStartingPosition) {
            initStartingPosition();
        }
    }

    public static Board newBoard(int size) {
        return new Board(size, true);
    }

    public static Board newEmptyBoard(int size) {
        return new Board(size, false);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isInside(Position p) {
        return p != null
                && p.row() >= 0 && p.row() < size
                && p.col() >= 0 && p.col() < size;
    }

    @Override
    public Optional<Disc> get(Position p) {
        if (!isInside(p)) {
            return Optional.empty();
        }
        return Optional.ofNullable(cells[p.row()][p.col()]);
    }

    @Override
    public boolean isEmpty(Position p) {
        return isInside(p) && cells[p.row()][p.col()] == null;
    }

    public boolean place(Position p, Disc disc) {
        Objects.requireNonNull(disc, "disc");
        if (!isEmpty(p)) {
            return false;
        }
        cells[p.row()][p.col()] = disc;
        filledCount++;
        return true;
    }

    public boolean isFull() {
        return filledCount >= size * size;
    }

    public void flip(Position p) {
        if (!isInside(p) || cells[p.row()][p.col()] == null) {
            return;
        }
        cells[p.row()][p.col()] = cells[p.row()][p.col()].opposite();
    }

    public Map<Disc, Integer> countDiscs() {
        Map<Disc, Integer> counts = new EnumMap<>(Disc.class);
        counts.put(Disc.BLACK, 0);
        counts.put(Disc.WHITE, 0);
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                Disc d = cells[r][c];
                if (d != null) {
                    counts.put(d, counts.get(d) + 1);
                }
            }
        }
        return counts;
    }

    private void initStartingPosition() {
        int mid2 = size / 2;
        int mid1 = mid2 - 1;

        setInitial(mid1, mid1, Disc.WHITE);
        setInitial(mid2, mid2, Disc.WHITE);
        setInitial(mid1, mid2, Disc.BLACK);
        setInitial(mid2, mid1, Disc.BLACK);
    }

    private void setInitial(int row, int col, Disc disc) {
        cells[row][col] = disc;
        filledCount++;
    }
}
