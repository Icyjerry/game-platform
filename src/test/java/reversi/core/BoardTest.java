package reversi.core;

import reversi.core.model.*;
import reversi.games.chess.ChessGame;
import reversi.games.minesweeper.MinesweeperGame;
import reversi.games.reversi.ReversiGame;

import org.junit.jupiter.api.Test;
import reversi.command.CommandParser;
import reversi.command.CommandType;
import reversi.command.ParsedCommand;
import reversi.gamehall.DemoController;
import reversi.gamehall.GameController;
import reversi.gamehall.GameRegistry;
import reversi.gamehall.MultiGameManager;
import reversi.gamehall.UiRegistry;

import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardTest {
    @Test
    void initialPositionHasTwoEach() {
        Board board = Board.newBoard(8);
        Map<Disc, Integer> counts = board.countDiscs();
        assertEquals(2, counts.get(Disc.BLACK));
        assertEquals(2, counts.get(Disc.WHITE));
    }

    @Test
    void reversiMoveFlipsCapturedDisc() {
        ReversiGame game = ReversiGame.newGame(8);

        ActionResult result = game.tryMove(new Position(2, 3));

        assertEquals(ActionResult.OK, result);
        assertEquals(Disc.BLACK, game.board().get(new Position(3, 3)).orElseThrow());
        assertEquals(4, game.counts().get(Disc.BLACK));
        assertEquals(1, game.counts().get(Disc.WHITE));
    }

    @Test
    void multiGameStartsWithPeaceReversiAndMinesweeper() {
        MultiGameManager manager = new MultiGameManager(8);

        assertEquals(4, manager.totalGames());
        assertEquals(GameMode.PEACE, manager.activeGame().mode());
        assertEquals(GameMode.REVERSI, manager.getGame(1).mode());
        assertEquals(GameMode.MINESWEEPER, manager.getGame(2).mode());
        assertEquals(GameMode.CHESS, manager.getGame(3).mode());
    }

    @Test
    void defaultRegistryExposesGamesInHallOrder() {
        GameRegistry registry = GameRegistry.defaultRegistry();

        assertEquals(4, registry.plugins().size());
        assertEquals(GameMode.PEACE, registry.plugins().get(0).mode());
        assertEquals(GameMode.REVERSI, registry.plugins().get(1).mode());
        assertEquals(GameMode.MINESWEEPER, registry.plugins().get(2).mode());
        assertEquals(GameMode.CHESS, registry.plugins().get(3).mode());
        assertEquals(GameMode.CHESS, registry.findByCommand("chess").orElseThrow().mode());
    }

    @Test
    void managerCanResetCurrentGameWithoutChangingSlot() {
        MultiGameManager manager = new MultiGameManager(8);
        manager.switchTo(1);
        ReversiGame before = (ReversiGame) manager.activeGame();
        assertEquals(ActionResult.OK, before.tryMove(new Position(2, 3)));

        assertTrue(manager.resetActiveGame());

        assertEquals(4, manager.totalGames());
        assertEquals(1, manager.activeIndex());
        assertEquals(GameMode.REVERSI, manager.activeGame().mode());
        ReversiGame after = (ReversiGame) manager.activeGame();
        assertEquals(2, after.counts().get(Disc.BLACK));
        assertEquals(2, after.counts().get(Disc.WHITE));
    }

    @Test
    void passIsRejectedWhenCurrentPlayerHasLegalMoves() {
        ReversiGame game = ReversiGame.newGame(8);

        ActionResult result = game.tryPass();

        assertEquals(ActionResult.PASS_NOT_ALLOWED, result);
        assertEquals(Disc.BLACK, game.current());
    }

    @Test
    void passIsAllowedWhenCurrentPlayerHasNoLegalMoves() {
        ReversiGame game = ReversiGame.newGame(4);
        Board board = game.board();

        board.place(new Position(0, 1), Disc.BLACK);
        board.place(new Position(0, 2), Disc.WHITE);
        board.place(new Position(0, 3), Disc.WHITE);
        board.place(new Position(1, 0), Disc.WHITE);
        board.place(new Position(1, 3), Disc.WHITE);
        board.place(new Position(2, 0), Disc.WHITE);
        board.place(new Position(2, 3), Disc.WHITE);
        board.place(new Position(3, 0), Disc.WHITE);
        board.place(new Position(3, 1), Disc.WHITE);
        board.place(new Position(3, 2), Disc.WHITE);
        board.place(new Position(3, 3), Disc.WHITE);

        assertTrue(game.validMoves().isEmpty());
        assertFalse(game.validMoves(Disc.WHITE).isEmpty());
        assertEquals("No legal moves. Type pass",
            GameController.resultMessage(game, ActionResult.INVALID_MOVE));

        ActionResult result = game.tryPass();

        assertEquals(ActionResult.PASS_OK, result);
        assertEquals(Disc.WHITE, game.current());
        assertTrue(game.validMoves().contains(new Position(0, 0)));
    }

    @Test
    void gameEndsWhenNeitherPlayerHasLegalMoves() {
        ReversiGame game = ReversiGame.newGame(4);
        Board board = game.board();

        board.flip(new Position(1, 2));
        board.flip(new Position(2, 1));
        board.place(new Position(0, 1), Disc.WHITE);
        board.place(new Position(0, 2), Disc.WHITE);
        board.place(new Position(0, 3), Disc.WHITE);
        board.place(new Position(1, 0), Disc.WHITE);
        board.place(new Position(1, 3), Disc.WHITE);
        board.place(new Position(2, 0), Disc.WHITE);
        board.place(new Position(2, 3), Disc.WHITE);
        board.place(new Position(3, 0), Disc.WHITE);
        board.place(new Position(3, 1), Disc.WHITE);
        board.place(new Position(3, 2), Disc.WHITE);
        board.place(new Position(3, 3), Disc.WHITE);

        assertTrue(game.validMoves(Disc.BLACK).isEmpty());
        assertTrue(game.validMoves(Disc.WHITE).isEmpty());
        assertTrue(game.isOver());
        assertEquals(ActionResult.GAME_OVER, game.tryMove(new Position(0, 0)));
        assertEquals(ActionResult.GAME_OVER, game.tryPass());
    }

    @Test
    void positionSupportsLetterFirstAndDigitFirst() {
        assertEquals(new Position(0, 0), Position.parse("a1").orElseThrow());
        assertEquals(new Position(0, 0), Position.parse("1a").orElseThrow());
        assertTrue(Position.parse("abc").isEmpty());
    }

    @Test
    void parserUnderstandsFlagAndSwitchCommands() {
        ParsedCommand flag = CommandParser.parse("f 1a");
        ParsedCommand move = CommandParser.parse("a1");
        ParsedCommand switchBoard = CommandParser.parse("switch 3");
        ParsedCommand chessMove = CommandParser.parse("m 7a 5a");

        assertEquals(CommandType.FLAG, flag.type());
        assertEquals(new Position(0, 0), flag.position());
        assertEquals(CommandType.MOVE, move.type());
        assertEquals(CommandType.SWITCH, switchBoard.type());
        assertEquals(3, switchBoard.boardIndex());
        assertEquals(new Position(6, 0), chessMove.from());
        assertEquals(new Position(4, 0), chessMove.to());
    }

    @Test
    void parserUnderstandsDemoAndStopCommands() {
        assertEquals(CommandType.DEMO, CommandParser.parse("demo").type());
        assertEquals(CommandType.STOP, CommandParser.parse("stop").type());
    }

    @Test
    void uiRegistryRejectsUnknownUiName() {
        assertThrows(IllegalArgumentException.class, () -> UiRegistry.discover("missing-ui"));
    }

    @Test
    void demoControllerExecutesScriptAndSwitchesGames() {
        MultiGameManager manager = new MultiGameManager(8);
        DemoController controller = new DemoController(manager);
        controller.reset(0);

        String firstMessage = controller.tick(0);
        assertTrue(firstMessage.contains("DEMO MODE"));
        BoardGameSession active = (BoardGameSession) manager.activeGame();
        assertTrue(active.counts().values().stream().mapToInt(Integer::intValue).sum() > 0);

        // Run demo until first game completes
        int maxSteps = 200;
        for (int i = 0; i < maxSteps && manager.activeIndex() == 0; i++) {
            controller.tick(i * 1000L);
        }

        // Should have switched to next game after completion
        assertEquals(1, manager.activeIndex());
        assertEquals(GameMode.REVERSI, manager.activeGame().mode());
    }

    @Test
    void chessStartsWithStandardPositionAndWhiteFirst() {
        ChessGame game = ChessGame.newGame();

        assertEquals(Disc.WHITE, game.current());
        assertEquals('R', game.pieceAt(new Position(0, 0)));
        assertEquals('K', game.pieceAt(new Position(0, 4)));
        assertEquals('p', game.pieceAt(new Position(6, 0)));
        assertEquals('k', game.pieceAt(new Position(7, 4)));
        assertEquals('.', game.pieceAt(new Position(3, 3)));
    }

    @Test
    void chessAcceptsBasicOpeningMoves() {
        ChessGame game = ChessGame.newGame();

        assertEquals(ActionResult.OK, move(game, "7a", "5a"));
        assertEquals('p', game.pieceAt(new Position(4, 0)));
        assertEquals(ActionResult.OK, move(game, "1b", "3c"));
        assertEquals('N', game.pieceAt(new Position(2, 2)));
    }

    @Test
    void chessRejectsIllegalMoveWithoutChangingBoard() {
        ChessGame game = ChessGame.newGame();

        assertEquals(ActionResult.INVALID_MOVE, move(game, "8a", "6a"));
        assertEquals('r', game.pieceAt(new Position(7, 0)));
        assertEquals('.', game.pieceAt(new Position(5, 0)));
        assertEquals(Disc.WHITE, game.current());
    }

    @Test
    void chessSupportsEnPassantOnNextMove() {
        ChessGame game = ChessGame.newGame();

        assertEquals(ActionResult.OK, move(game, "7e", "5e"));
        assertEquals(ActionResult.OK, move(game, "2a", "3a"));
        assertEquals(ActionResult.OK, move(game, "5e", "4e"));
        assertEquals(ActionResult.OK, move(game, "2d", "4d"));
        assertEquals(ActionResult.OK, move(game, "4e", "3d"));

        assertEquals('p', game.pieceAt(new Position(2, 3)));
        assertEquals('.', game.pieceAt(new Position(3, 3)));
    }

    @Test
    void chessPromotesPawnToQueenByDefault() {
        ChessGame game = ChessGame.newGame();

        assertEquals(ActionResult.OK, move(game, "7a", "5a"));
        assertEquals(ActionResult.OK, move(game, "2h", "3h"));
        assertEquals(ActionResult.OK, move(game, "5a", "4a"));
        assertEquals(ActionResult.OK, move(game, "3h", "4h"));
        assertEquals(ActionResult.OK, move(game, "4a", "3a"));
        assertEquals(ActionResult.OK, move(game, "4h", "5h"));
        assertEquals(ActionResult.OK, move(game, "3a", "2b"));
        assertEquals(ActionResult.OK, move(game, "5h", "6h"));
        assertEquals(ActionResult.OK, move(game, "2b", "1a"));

        assertEquals('q', game.pieceAt(new Position(0, 0)));
    }

    @Test
    void chessSupportsKingSideAndQueenSideCastling() {
        ChessGame kingSide = ChessGame.newGame();

        assertEquals(ActionResult.OK, move(kingSide, "7g", "6g"));
        assertEquals(ActionResult.OK, move(kingSide, "2a", "3a"));
        assertEquals(ActionResult.OK, move(kingSide, "8f", "7g"));
        assertEquals(ActionResult.OK, move(kingSide, "2b", "3b"));
        assertEquals(ActionResult.OK, move(kingSide, "8g", "6f"));
        assertEquals(ActionResult.OK, move(kingSide, "2c", "3c"));
        assertEquals(ActionResult.OK, move(kingSide, "8e", "8g"));
        assertEquals('k', kingSide.pieceAt(new Position(7, 6)));
        assertEquals('r', kingSide.pieceAt(new Position(7, 5)));

        ChessGame queenSide = ChessGame.newGame();
        assertEquals(ActionResult.OK, move(queenSide, "7d", "5d"));
        assertEquals(ActionResult.OK, move(queenSide, "2a", "3a"));
        assertEquals(ActionResult.OK, move(queenSide, "8c", "5f"));
        assertEquals(ActionResult.OK, move(queenSide, "3a", "4a"));
        assertEquals(ActionResult.OK, move(queenSide, "8d", "7d"));
        assertEquals(ActionResult.OK, move(queenSide, "4a", "5a"));
        assertEquals(ActionResult.OK, move(queenSide, "8b", "6c"));
        assertEquals(ActionResult.OK, move(queenSide, "5a", "6a"));
        assertEquals(ActionResult.OK, move(queenSide, "8e", "8c"));
        assertEquals('k', queenSide.pieceAt(new Position(7, 2)));
        assertEquals('r', queenSide.pieceAt(new Position(7, 3)));
    }

    @Test
    void minesweeperFirstMoveIsAlwaysSafe() {
        MinesweeperGame game = MinesweeperGame.newGame(8, 10, new SequenceRandom());

        ActionResult result = game.tryMove(new Position(0, 0));

        assertNotEquals(ActionResult.MINE_HIT, result);
        assertTrue(game.isGenerated());
        assertNotEquals("*", game.cellDisplay(new Position(0, 0)));
    }

    @Test
    void minesweeperFlagBlocksOpenUntilRemoved() {
        MinesweeperGame game = MinesweeperGame.newGame(8, 10, new SequenceRandom());
        Position cell = new Position(0, 1);

        assertEquals(ActionResult.FLAG_ON, game.tryFlag(cell));
        assertEquals(ActionResult.FLAGGED_CELL, game.tryMove(cell));
        assertEquals(ActionResult.FLAG_OFF, game.tryFlag(cell));
    }

    @Test
    void chessDemoGameIsFullyLegal() {
        ChessGame game = ChessGame.newGame();
        String[][] moves = {
            {"7e", "5e"},    // 1. e4
            {"2e", "4e"},    // e5
            {"8g", "6f"},    // 2. Nf3
            {"1b", "3c"},    // Nc6
            {"8f", "5c"},    // 3. Bc4
            {"1f", "4c"},    // Bc5
            {"7c", "6c"},    // 4. c3
            {"1g", "3f"},    // Nf6
            {"7d", "5d"},    // 5. d4
            {"4e", "5d"},    // exd4 (CAPTURE)
            {"6c", "5d"},    // 6. cxd4 (CAPTURE)
            {"4c", "3b"},    // Bb6
            {"8e", "8g"},    // 7. O-O (CASTLE)
            {"1e", "1g"},    // O-O (CASTLE)
            {"8c", "4g"},    // 8. Bg5
            {"2h", "3h"},    // h6
            {"4g", "3f"},    // 9. Bxf6 (CAPTURE)
            {"1d", "3f"},    // Qxf6 (CAPTURE)
            {"8b", "7d"},    // 10. Nbd2
            {"2d", "4d"},    // d5
            {"5e", "4d"},    // 11. exd5 (CAPTURE)
            {"1c", "5g"},    // Bg4
            {"8d", "6b"},    // 12. Qb3
            {"5g", "6f"},    // Bxf3 (CAPTURE)
            {"6b", "3b"},    // 13. Qxb6 (CAPTURE)
            {"2a", "3b"},    // axb6 (CAPTURE)
            {"7g", "6f"},    // 14. gxf3 (CAPTURE)
            {"3c", "5b"},    // Nb4
            {"8a", "8c"},    // 15. Rac1
            {"1f", "1e"},    // Re8
            {"8f", "8d"},    // 16. Rfd1
            {"1e", "2e"},    // Re7
            {"4d", "3d"},    // 17. d6  (PAWN ADVANCE)
            {"5b", "4d"},    // Nd5 (knight moves away)
            {"3d", "2d"},    // 18. d7  (PAWN ADVANCE)
            {"2c", "3c"},    // c6 (clear c7)
            {"2d", "1d", "q"}, // 19. d8=Q!! (PROMOTION! + CHECK)
            {"1g", "2h"},    // Kh7 (escape check, only safe square!)
            {"1d", "2e"},    // 20. Qxe7+! (CAPTURE rook + CHECK)
            {"2h", "1g"},    // Kg8
            {"2e", "2f"},    // 21. Qxf7+ (CAPTURE pawn + CHECK)
            {"1g", "1h"},    // Kh8
            {"5c", "4d"},    // 22. Bxd5! (CAPTURE knight, clears diagonal for mate)
            {"1a", "2a"},    // Ra7 (move a8 rook away so it can't capture g8!)
            {"2f", "1g"},    // 23. Qg8#!! (QUEEN CHECKMATE! Bishop on d5 protects queen)
        };

        for (int i = 0; i < moves.length; i++) {
            String from = moves[i][0];
            String to = moves[i][1];
            Character promo = moves[i].length > 2 ? moves[i][2].charAt(0) : null;
            ActionResult r = game.tryMove(
                Position.parse(from).orElseThrow(),
                Position.parse(to).orElseThrow(),
                promo);
            boolean ok = r == ActionResult.OK || r == ActionResult.CHECKMATE;
            assertTrue(ok,
                "Move " + (i+1) + " " + from + "->" + to
                + " should be legal. Got " + r + ": " + game.lastMessage());
        }
        assertTrue(game.isOver(), "Game should be over after checkmate");
        System.out.println("SUCCESS: All " + moves.length + " moves played! Final: " + game.lastMessage());
    }

    @Test
    void chessCheckmateDetectionWorks() {
        ChessGame game = ChessGame.newGame();
        String[][] setup = {
            {"7e", "5e"}, {"2e", "4e"}, {"8g", "6f"}, {"1b", "3c"},
            {"8f", "5c"}, {"1f", "4c"}, {"7c", "6c"}, {"1g", "3f"},
            {"7d", "5d"}, {"4e", "5d"}, {"6c", "5d"}, {"4c", "3b"},
            {"8e", "8g"}, {"1e", "1g"}, {"8c", "4g"}, {"2h", "3h"},
            {"4g", "3f"}, {"1d", "3f"}, {"8b", "7d"}, {"2d", "4d"},
            {"5e", "4d"}, {"1c", "5g"}, {"8d", "6b"}, {"5g", "6f"},
            {"6b", "3b"}, {"2a", "3b"}, {"7g", "6f"}, {"3c", "5b"},
            {"8a", "8c"}, {"1f", "1e"}, {"8f", "8d"}, {"1e", "2e"},
            {"4d", "3d"}, {"5b", "4d"}, {"3d", "2d"}, {"2c", "3c"},
            {"2d", "1d", "q"}, {"1g", "2h"}, {"1d", "2e"}, {"2h", "1g"},
            {"2e", "2f"}, {"1g", "1h"}, {"5c", "4d"}, {"1a", "2a"},
            {"2f", "1g"},
        };
        ActionResult last = null;
        for (String[] m : setup) {
            last = game.tryMove(
                Position.parse(m[0]).orElseThrow(),
                Position.parse(m[1]).orElseThrow(),
                m.length > 2 ? m[2].charAt(0) : null);
        }
        assertTrue(game.isOver(), "Should be checkmate");
        assertEquals(ActionResult.CHECKMATE, last, "Should return CHECKMATE");
    }

    private static final class SequenceRandom extends Random {
        private final int[] values = {
            0, 0, 0, 1, 0, 2, 0, 3, 0, 4,
            0, 5, 0, 6, 0, 7, 1, 0, 1, 1,
            1, 2
        };
        private int index = 0;

        @Override
        public int nextInt(int bound) {
            int value = values[index % values.length];
            index++;
            return value % bound;
        }
    }

    private static ActionResult move(ChessGame game, String from, String to) {
        return game.tryMove(Position.parse(from).orElseThrow(), Position.parse(to).orElseThrow(), null);
    }
}
