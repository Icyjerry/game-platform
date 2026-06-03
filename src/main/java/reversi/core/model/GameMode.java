package reversi.core.model;

import java.util.Objects;

public final class GameMode {
    private final String id;

    private GameMode(String id) {
        this.id = Objects.requireNonNull(id, "id").toLowerCase(java.util.Locale.ROOT);
    }

    public static final GameMode PEACE = new GameMode("peace");
    public static final GameMode REVERSI = new GameMode("reversi");
    public static final GameMode MINESWEEPER = new GameMode("minesweeper");
    public static final GameMode CHESS = new GameMode("chess");

    public static GameMode of(String id) {
        return new GameMode(id);
    }

    public String id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GameMode other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}
