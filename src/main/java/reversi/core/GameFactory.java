package reversi.core;

import reversi.gamehall.GameRegistry;

import java.util.Objects;

public final class GameFactory {
    private GameFactory() {
    }

    public static GameSession create(GameMode mode, int boardSize) {
        Objects.requireNonNull(mode, "mode");
        return GameRegistry.defaultRegistry().create(mode, boardSize);
    }
}
