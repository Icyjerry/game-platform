package reversi.games.reversi;

import reversi.core.Game;
import reversi.core.GameMode;
import reversi.core.GameSession;
import reversi.core.Position;
import reversi.gamehall.DemoScript;
import reversi.gamehall.GamePlugin;

import java.util.List;
import java.util.Set;

public final class ReversiGamePlugin implements GamePlugin {
    @Override
    public GameMode mode() {
        return GameMode.REVERSI;
    }

    @Override
    public String id() {
        return "reversi";
    }

    @Override
    public String displayName() {
        return "reversi";
    }

    @Override
    public List<String> commands() {
        return List.of("reversi");
    }

    @Override
    public GameSession createGame(int boardSize) {
        return Game.newGame(boardSize);
    }

    @Override
    public DemoScript createDemoScript(GameSession game) {
        return new ReversiDemoScript(game);
    }

    private static final class ReversiDemoScript implements DemoScript {
        private final GameSession game;
        private final List<Position> preferredMoves;
        private int index;

        private ReversiDemoScript(GameSession game) {
            this.game = game;
            this.preferredMoves = List.of(
                new Position(2, 3),
                new Position(2, 2),
                new Position(2, 1),
                new Position(1, 1),
                new Position(0, 1),
                new Position(0, 0),
                new Position(4, 5),
                new Position(5, 5)
            );
            this.index = 0;
        }

        @Override
        public String nextStep() {
            if (game.isOver()) {
                return "Reversi demo: board is over";
            }
            Set<Position> legal = game.validMoves();
            if (legal.isEmpty()) {
                game.tryPass();
                return "Reversi demo: pass";
            }

            for (int attempts = 0; attempts < preferredMoves.size(); attempts++) {
                Position move = preferredMoves.get(index % preferredMoves.size());
                index++;
                if (legal.contains(move)) {
                    game.tryMove(move);
                    return "Reversi demo: move " + label(move);
                }
            }

            Position move = legal.iterator().next();
            game.tryMove(move);
            return "Reversi demo: move " + label(move);
        }

        private String label(Position p) {
            return (p.row() + 1) + String.valueOf((char) ('a' + p.col()));
        }
    }
}
