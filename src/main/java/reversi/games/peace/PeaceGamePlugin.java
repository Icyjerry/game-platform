package reversi.games.peace;

import reversi.core.GameMode;
import reversi.core.GameSession;
import reversi.core.PeaceGame;
import reversi.core.Position;
import reversi.gamehall.DemoScript;
import reversi.gamehall.GamePlugin;

import java.util.List;
import java.util.Set;

public final class PeaceGamePlugin implements GamePlugin {
    @Override
    public GameMode mode() {
        return GameMode.PEACE;
    }

    @Override
    public String id() {
        return "peace";
    }

    @Override
    public String displayName() {
        return "peace";
    }

    @Override
    public List<String> commands() {
        return List.of("peace");
    }

    @Override
    public GameSession createGame(int boardSize) {
        return PeaceGame.newGame(boardSize);
    }

    @Override
    public DemoScript createDemoScript(GameSession game) {
        return new PeaceDemoScript(game);
    }

    private static final class PeaceDemoScript implements DemoScript {
        private final GameSession game;
        private final List<Position> moves;
        private int index;

        private PeaceDemoScript(GameSession game) {
            this.game = game;
            this.moves = List.of(
                new Position(0, 0),
                new Position(0, 1),
                new Position(1, 0),
                new Position(1, 1),
                new Position(2, 0),
                new Position(2, 1)
            );
            this.index = 0;
        }

        @Override
        public String nextStep() {
            if (game.isOver()) {
                return "Peace demo: board is over";
            }
            Set<Position> legal = game.validMoves();
            if (legal.isEmpty()) {
                return "Peace demo: no moves";
            }

            for (int attempts = 0; attempts < moves.size(); attempts++) {
                Position move = moves.get(index % moves.size());
                index++;
                if (legal.contains(move)) {
                    game.tryMove(move);
                    return "Peace demo: placed at " + label(move);
                }
            }

            Position move = legal.iterator().next();
            game.tryMove(move);
            return "Peace demo: placed at " + label(move);
        }

        private String label(Position p) {
            return (p.row() + 1) + String.valueOf((char) ('a' + p.col()));
        }
    }
}
