package reversi.games.minesweeper;

import reversi.core.model.*;

import org.junit.jupiter.api.Test;
import reversi.games.minesweeper.MinesweeperGame;
import reversi.core.model.Position;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MinesweeperDemoTest {
    private static final long DEMO_SEED = 42L;
    private static final int SIZE = 8;
    private static final int MINE_COUNT = 10;

    @Test
    void testDeterministicBoardGeneration() {
        MinesweeperGame game1 = MinesweeperGame.newGame(SIZE, MINE_COUNT, new Random(DEMO_SEED));
        MinesweeperGame game2 = MinesweeperGame.newGame(SIZE, MINE_COUNT, new Random(DEMO_SEED));

        Position firstMove = new Position(0, 0);
        game1.tryMove(firstMove);
        game2.tryMove(firstMove);

        boolean[][] mines1 = game1.getMines();
        boolean[][] mines2 = game2.getMines();

        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                assertEquals(mines1[row][col], mines2[row][col],
                    "Mine positions should be identical at (" + row + "," + col + ")");
            }
        }
    }

    @Test
    void testDemoCanWin() {
        MinesweeperGame game = MinesweeperGame.newGame(SIZE, MINE_COUNT, new Random(DEMO_SEED));

        game.tryMove(new Position(0, 0));
        assertTrue(game.isGenerated(), "Board should be generated after first move");

        boolean[][] mines = game.getMines();
        int totalSafeCells = SIZE * SIZE - MINE_COUNT;

        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                Position pos = new Position(row, col);
                if (!mines[row][col] && !game.getRevealed()[row][col]) {
                    game.tryMove(pos);
                }
            }
        }

        int safeCellsRevealed = countRevealed(game.getRevealed());
        assertTrue(game.isOver(), "Game should be over");
        assertFalse(game.isQuit(), "Game should not be quit");
        assertEquals(totalSafeCells, safeCellsRevealed, "All safe cells should be revealed");
    }

    @Test
    void testBlankCellExpandsSafeRegion() {
        MinesweeperGame game = MinesweeperGame.newGame(4, 1, new FixedRandom(3, 3));

        ActionResult result = game.tryMove(new Position(0, 0));

        assertEquals(ActionResult.CLEAR_WIN, result, "A blank corner should expand to all safe cells");
        assertEquals(".", game.cellDisplay(new Position(0, 0)));
        assertEquals("1", game.cellDisplay(new Position(2, 2)));
        assertTrue(game.isOver(), "The board has one mine, so expansion should reveal every safe cell");
    }

    @Test
    void testBlankExpansionDoesNotOpenFlaggedCells() {
        MinesweeperGame game = MinesweeperGame.newGame(4, 1, new FixedRandom(3, 3));
        Position flaggedSafeCell = new Position(2, 2);

        assertEquals(ActionResult.FLAG_ON, game.tryFlag(flaggedSafeCell));
        assertEquals(ActionResult.OK, game.tryMove(new Position(0, 0)));

        assertFalse(game.isRevealed(flaggedSafeCell), "BFS should not open a flagged cell");
        assertEquals("F", game.cellDisplay(flaggedSafeCell));
        assertFalse(game.isOver(), "One flagged safe cell is still unrevealed");

        assertEquals(ActionResult.FLAG_OFF, game.tryFlag(flaggedSafeCell));
        assertEquals(ActionResult.CLEAR_WIN, game.tryMove(flaggedSafeCell));
    }

    @Test
    void testFirstMoveNeverHitsMine() {
        MinesweeperGame game = MinesweeperGame.newGame(SIZE, MINE_COUNT, new Random(DEMO_SEED));
        Position firstMove = new Position(0, 0);

        game.tryMove(firstMove);

        boolean[][] mines = game.getMines();
        assertFalse(mines[firstMove.row()][firstMove.col()],
            "First move position should never have a mine");
    }

    private static int countRevealed(boolean[][] revealed) {
        int count = 0;
        for (boolean[] row : revealed) {
            for (boolean cell : row) {
                if (cell) {
                    count++;
                }
            }
        }
        return count;
    }

    private static final class FixedRandom extends Random {
        private final int[] values;
        private int index;

        private FixedRandom(int... values) {
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            int value = values[index % values.length];
            index++;
            return value % bound;
        }
    }
}
