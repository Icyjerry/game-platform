package reversi.core;

import reversi.gamehall.GamePlugin;
import reversi.gamehall.GameRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class MultiGameManager {
    private final List<GameSession> games;
    private final GameRegistry registry;
    private final int boardSize;
    private int activeIndex;

    public MultiGameManager(int boardSize) {
        this(boardSize, GameRegistry.defaultRegistry());
    }

    public MultiGameManager(int boardSize, GameRegistry registry) {
        this.boardSize = boardSize;
        this.registry = Objects.requireNonNull(registry, "registry");
        this.games = new ArrayList<>();
        for (GamePlugin plugin : registry.plugins()) {
            games.add(plugin.createGame(boardSize));
        }
        this.activeIndex = 0;
    }

    public MultiGameManager(GameSession existingGame) {
        this.boardSize = existingGame.board().size();
        this.registry = GameRegistry.defaultRegistry();
        this.games = new ArrayList<>();
        games.add(existingGame);
        this.activeIndex = 0;
    }

    public int boardSize() {
        return boardSize;
    }

    public GameRegistry registry() {
        return registry;
    }

    public GameSession activeGame() {
        return games.get(activeIndex);
    }

    public int activeIndex() {
        return activeIndex;
    }

    public int totalGames() {
        return games.size();
    }

    public List<GameSession> games() {
        return Collections.unmodifiableList(games);
    }

    public boolean switchTo(int index) {
        if (index < 0 || index >= games.size()) {
            return false;
        }
        activeIndex = index;
        return true;
    }

    public GameSession getGame(int index) {
        if (index < 0 || index >= games.size()) {
            return null;
        }
        return games.get(index);
    }

    public void addGame(GameMode mode) {
        if (mode == null) {
            return;
        }
        games.add(registry.create(mode, boardSize));
    }

    public boolean allGamesOver() {
        return games.stream().allMatch(GameSession::isOver);
    }
}
