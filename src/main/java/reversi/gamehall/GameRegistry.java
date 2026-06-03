package reversi.gamehall;

import reversi.core.model.GameMode;
import reversi.core.GameSession;
import reversi.games.chess.ChessGamePlugin;
import reversi.games.minesweeper.MinesweeperGamePlugin;
import reversi.games.peace.PeaceGamePlugin;
import reversi.games.reversi.ReversiGamePlugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

public final class GameRegistry {
    private final List<GamePlugin> plugins;
    private final Map<GameMode, GamePlugin> byMode;
    private final Map<String, GamePlugin> byCommand;

    private GameRegistry(List<GamePlugin> plugins) {
        this.plugins = List.copyOf(plugins);
        this.byMode = new LinkedHashMap<>();
        this.byCommand = new LinkedHashMap<>();
        for (GamePlugin plugin : plugins) {
            byMode.put(plugin.mode(), plugin);
            for (String command : plugin.commands()) {
                byCommand.put(normalize(command), plugin);
            }
        }
    }

    public static GameRegistry defaultRegistry() {
        List<GamePlugin> discovered = ServiceLoader.load(GamePlugin.class)
            .stream()
            .map(ServiceLoader.Provider::get)
            .toList();
        if (!discovered.isEmpty()) {
            return new GameRegistry(discovered);
        }
        return builtInRegistry();
    }

    private static GameRegistry builtInRegistry() {
        return new GameRegistry(List.of(
            new PeaceGamePlugin(),
            new ReversiGamePlugin(),
            new MinesweeperGamePlugin(),
            new ChessGamePlugin()
        ));
    }

    public List<GamePlugin> plugins() {
        return plugins;
    }

    public Optional<GamePlugin> findByMode(GameMode mode) {
        return Optional.ofNullable(byMode.get(mode));
    }

    public Optional<GamePlugin> findByCommand(String command) {
        if (command == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byCommand.get(normalize(command)));
    }

    public GameSession create(GameMode mode, int boardSize) {
        return findByMode(mode)
            .orElseThrow(() -> new IllegalArgumentException("No game plugin registered for " + mode))
            .createGame(boardSize);
    }

    public String displayName(GameMode mode) {
        return findByMode(mode).map(GamePlugin::displayName).orElse(mode.id());
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
