package reversi.gamehall;

import java.util.Objects;

public final class GameHall {
    private final GameRegistry registry;
    private final MultiGameManager manager;

    private GameHall(int boardSize, GameRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.manager = new MultiGameManager(boardSize, registry);
    }

    public static GameHall newHall(int boardSize) {
        return new GameHall(boardSize, GameRegistry.defaultRegistry());
    }

    public GameRegistry registry() {
        return registry;
    }

    public MultiGameManager manager() {
        return manager;
    }
}
