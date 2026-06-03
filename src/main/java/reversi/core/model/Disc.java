package reversi.core.model;

public enum Disc {
    BLACK,
    WHITE;

    public Disc opposite() {
        return this == BLACK ? WHITE : BLACK;
    }
}
