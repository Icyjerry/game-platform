package reversi.gamehall;

import reversi.core.GameMode;
import reversi.core.GameSession;
import reversi.games.chess.ChessGamePlugin;
import reversi.games.minesweeper.MinesweeperGamePlugin;
import reversi.games.peace.PeaceGamePlugin;
import reversi.games.reversi.ReversiGamePlugin;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class GameRegistry {
    private final List<GamePlugin> plugins;
    private final Map<GameMode, GamePlugin> byMode;
    private final Map<String, GamePlugin> byCommand;

    private GameRegistry(List<GamePlugin> plugins) {
        this.plugins = List.copyOf(plugins);
        this.byMode = new EnumMap<>(GameMode.class);
        this.byCommand = new LinkedHashMap<>();
        for (GamePlugin plugin : plugins) {
            byMode.put(plugin.mode(), plugin);
            for (String command : plugin.commands()) {
                byCommand.put(normalize(command), plugin);
            }
        }
    }

    public static GameRegistry defaultRegistry() {
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
        return findByMode(mode).map(GamePlugin::displayName).orElse(mode.name().toLowerCase(Locale.ROOT));
    }

    public List<String> commandNames() {
        return new ArrayList<>(byCommand.keySet());
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
