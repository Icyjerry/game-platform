package reversi.games.minesweeper;

import org.junit.jupiter.api.Test;
import reversi.core.MinesweeperGame;
import reversi.core.Position;

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
        int safeCellsRevealed = 0;
        int totalSafeCells = SIZE * SIZE - MINE_COUNT;

        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                Position pos = new Position(row, col);
                if (!mines[row][col] && !game.getRevealed()[row][col]) {
                    game.tryMove(pos);
                    if (game.getRevealed()[row][col]) {
                        safeCellsRevealed++;
                    }
                }
            }
        }

        assertTrue(game.isOver(), "Game should be over");
        assertFalse(game.isQuit(), "Game should not be quit");
        assertTrue(safeCellsRevealed >= totalSafeCells - 10, "Most safe cells should be revealed");
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
}
