package reversi.gamehall;

import reversi.core.GameSession;
import reversi.core.MultiGameManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class DemoController {
    private static final long TICK_MILLIS = 700;
    private static final long PAUSE_MILLIS = 1200;

    private final MultiGameManager manager;
    private final Map<Integer, DemoScript> scripts;
    private long nextTickAt;
    private String lastMessage;
    private boolean waitingForSwitch;

    public DemoController(MultiGameManager manager) {
        this.manager = Objects.requireNonNull(manager, "manager");
        this.scripts = new HashMap<>();
        this.waitingForSwitch = false;
        reset(System.currentTimeMillis());
    }

    public void reset(long now) {
        scripts.clear();
        nextTickAt = now;
        lastMessage = "Demo running...";
        waitingForSwitch = false;
    }

    public boolean shouldTick(long now) {
        return now >= nextTickAt;
    }

    public String tick(long now) {
        if (waitingForSwitch) {
            waitingForSwitch = false;
            switchToNextGame();
            nextTickAt = now + TICK_MILLIS;
            return lastMessage;
        }

        GameSession game = manager.activeGame();
        int currentIndex = manager.activeIndex();
        DemoScript script = scripts.computeIfAbsent(currentIndex, ignored -> createScript(game));
        String message = script.nextStep();
        if (message == null || message.isBlank()) {
            message = "Demo tick";
        }
        lastMessage = "[ DEMO MODE ] " + message;
        nextTickAt = now + TICK_MILLIS;

        if (game.isOver()) {
            scripts.remove(currentIndex);
            waitingForSwitch = true;
            nextTickAt = now + PAUSE_MILLIS;
        }

        return lastMessage;
    }

    public String lastMessage() {
        return lastMessage;
    }

    private DemoScript createScript(GameSession game) {
        return manager.registry()
            .findByMode(game.mode())
            .map(plugin -> plugin.createDemoScript(game))
            .orElse(() -> "No demo script for this game");
    }

    private void switchToNextGame() {
        if (manager.totalGames() == 0) {
            return;
        }
        int next = (manager.activeIndex() + 1) % manager.totalGames();
        manager.switchTo(next);
    }
}
