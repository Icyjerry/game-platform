package reversi.games.minesweeper;

import reversi.core.GameMode;
import reversi.core.GameSession;
import reversi.core.MinesweeperGame;
import reversi.core.Position;
import reversi.gamehall.DemoScript;
import reversi.gamehall.GamePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class MinesweeperGamePlugin implements GamePlugin {
    private static final int SIZE = 8;
    private static final int MINE_COUNT = 10;
    private static final long DEMO_SEED = 42L;

    @Override
    public GameMode mode() {
        return GameMode.MINESWEEPER;
    }

    @Override
    public String id() {
        return "minesweeper";
    }

    @Override
    public String displayName() {
        return "minesweeper";
    }

    @Override
    public List<String> commands() {
        return List.of("minesweeper");
    }

    @Override
    public GameSession createGame(int boardSize) {
        return MinesweeperGame.newGame(SIZE, MINE_COUNT, new Random(DEMO_SEED));
    }

    @Override
    public DemoScript createDemoScript(GameSession game) {
        if (!(game instanceof MinesweeperGame)) {
            return () -> "Minesweeper demo: invalid game type";
        }
        return new MinesweeperDemoScript((MinesweeperGame) game);
    }

    private static final class MinesweeperDemoScript implements DemoScript {
        private final MinesweeperGame game;
        private final List<Position> safeSequence;
        private int stepIndex;

        private MinesweeperDemoScript(MinesweeperGame game) {
            this.game = game;
            this.safeSequence = generateSafeSequence();
            this.stepIndex = 0;
        }

        private List<Position> generateSafeSequence() {
            // Pre-compute mine positions using the same seed
            Random testRandom = new Random(DEMO_SEED);
            boolean[][] testMines = new boolean[SIZE][SIZE];
            Position firstMove = new Position(0, 0);

            // Simulate mine generation (same logic as MinesweeperGame)
            int placed = 0;
            while (placed < MINE_COUNT) {
                int row = testRandom.nextInt(SIZE);
                int col = testRandom.nextInt(SIZE);
                if (testMines[row][col]) {
                    continue;
                }
                if (row == firstMove.row() && col == firstMove.col()) {
                    continue;
                }
                testMines[row][col] = true;
                placed++;
            }

            // Build sequence of safe positions
            List<Position> sequence = new ArrayList<>();
            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    if (!testMines[row][col]) {
                        sequence.add(new Position(row, col));
                    }
                }
            }
            return sequence;
        }

        @Override
        public String nextStep() {
            if (game.isOver()) {
                return "Minesweeper demo complete - Victory!";
            }

            if (stepIndex >= safeSequence.size()) {
                return "Minesweeper demo: all safe cells revealed";
            }

            Position move = safeSequence.get(stepIndex);
            stepIndex++;

            boolean[][] revealed = game.getRevealed();
            if (revealed[move.row()][move.col()]) {
                return "Minesweeper demo: skip already revealed " + label(move);
            }

            game.tryMove(move);

            if (!game.isGenerated()) {
                return "Minesweeper demo: first move " + label(move);
            }

            return "Minesweeper demo: reveal " + label(move);
        }

        private String label(Position p) {
            return (p.row() + 1) + String.valueOf((char) ('a' + p.col()));
        }
    }
}
