package reversi.gamehall;

import reversi.core.GameMode;
import reversi.core.GameSession;

import java.util.List;

public interface GamePlugin {
    GameMode mode();

    String id();

    String displayName();

    List<String> commands();

    GameSession createGame(int boardSize);

    default DemoScript createDemoScript(GameSession game) {
        return () -> displayName() + " demo idle";
    }

    default void updateDemo() {
    }
}
