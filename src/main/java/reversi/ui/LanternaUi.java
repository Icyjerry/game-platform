package reversi.ui;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import reversi.command.CommandParser;
import reversi.command.CommandType;
import reversi.command.ParsedCommand;
import reversi.core.ActionResult;
import reversi.core.ChessGame;
import reversi.core.Disc;
import reversi.core.GameMode;
import reversi.core.GameSession;
import reversi.core.MinesweeperGame;
import reversi.core.MultiGameManager;
import reversi.core.Position;
import reversi.gamehall.DemoController;
import reversi.gamehall.GamePlugin;
import reversi.gamehall.GameRegistry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class LanternaUi {
    private static final TextColor FG = TextColor.ANSI.WHITE;
    private static final TextColor HIGHLIGHT = TextColor.ANSI.CYAN;
    private static final TextColor WARNING = TextColor.ANSI.YELLOW;
    private static final TextColor MUTED = TextColor.ANSI.WHITE_BRIGHT;
    private static final TextColor COORD = TextColor.ANSI.BLACK_BRIGHT;
    private static final TextColor BLACK_PIECE = TextColor.ANSI.YELLOW_BRIGHT;
    private static final TextColor WHITE_PIECE = TextColor.ANSI.GREEN_BRIGHT;
    private static final TextColor PANEL_BG = TextColor.ANSI.BLACK;
    private static final TextColor MINE_COLOR = TextColor.ANSI.RED;
    private static final TextColor FLAG_COLOR = TextColor.ANSI.YELLOW;
    private static final TextColor ZERO_COLOR = TextColor.ANSI.WHITE;
    private static final String BLACK_DISC = "●";
    private static final String WHITE_DISC = "○";
    private static final String EMPTY_CELL = "·";
    private static final String VALID_MOVE = "+";
    private static final int CELL_WIDTH = 2;
    private static final int OUTER_MARGIN = 1;
    private static final int PANEL_GAP = 1;
    private static final int FOOTER_HEIGHT = 3;
    private static final int BOARD_PANEL_WIDTH = 24;
    private static final int LIST_PANEL_WIDTH = 22;

    public void run(GameSession game) throws Exception {
        MultiGameManager manager = new MultiGameManager(game);
        runMulti(manager);
    }

    public void runMulti(MultiGameManager manager) throws Exception {
        DefaultTerminalFactory factory = new DefaultTerminalFactory();
        try (Screen screen = new TerminalScreen(factory.createTerminal())) {
            screen.startScreen();
            screen.doResizeIfNecessary();
            screen.clear();
            screen.setCursorPosition(null);

            String message = "";
            Position[] cursor = {new Position(0, 0)};
            while (!manager.allGamesOver()) {
                GameSession game = manager.activeGame();
                String prompt = buildPrompt(manager, game);
                String input = readLine(screen, manager, game, message, prompt, cursor);

                if (input == null) {
                    game.quit();
                    break;
                }

                ParsedCommand command = CommandParser.parse(input);
                if (command.type() == CommandType.QUIT) {
                    game.quit();
                    break;
                }
                if (command.type() == CommandType.DEMO) {
                    DemoController demoController = new DemoController(manager);
                    String demoResult = runDemoMode(screen, manager, demoController, cursor);
                    if ("quit".equals(demoResult)) {
                        manager.activeGame().quit();
                        break;
                    }
                    message = demoResult;
                    continue;
                }

                message = handleInput(manager, game, command);
            }

            GameSession game = manager.activeGame();
            draw(screen, manager, game, "All games finished. Press Enter to exit...", cursor[0], false);
            screen.refresh(Screen.RefreshType.DELTA);
            readLine(screen, manager, game, "", "", cursor);
            screen.stopScreen();
        }
    }

    private static String buildPrompt(MultiGameManager manager, GameSession game) {
        int boardNum = manager.activeIndex() + 1;
        if (game.isOver()) {
            return "Board " + boardNum + " over > ";
        }
        if (game.mode() == GameMode.MINESWEEPER) {
            return "B" + boardNum + "/" + manager.totalGames() + " > ";
        }
        return "B" + boardNum + "/" + manager.totalGames() + " > ";
    }

    private static String handleInput(MultiGameManager manager, GameSession game, ParsedCommand command) {
        return switch (command.type()) {
            case NEW_GAME -> {
                manager.addGame(command.gameMode());
                yield "Added " + modeLabel(command.gameMode()) + " game " + manager.totalGames();
            }
            case DEMO -> "Demo mode started";
            case STOP -> "Demo is not running";
            case SWITCH -> {
                if (manager.switchTo(command.boardIndex() - 1)) {
                    yield "Switched to board " + command.boardIndex();
                }
                yield "Invalid board number";
            }
            case PASS -> messageFor(game, game.tryPass());
            case FLAG -> {
                if (game.mode() != GameMode.MINESWEEPER) {
                    yield "Flag is only available in minesweeper";
                }
                yield messageFor(game, game.tryFlag(command.position()));
            }
            case MOVE -> {
                if (game.mode() == GameMode.CHESS) {
                    ChessGame chess = (ChessGame) game;
                    if (command.from() == null || command.to() == null) {
                        yield "坐标越界或格式错误";
                    }
                    chess.tryMove(command.from(), command.to(), command.promotionPiece());
                    yield chess.lastMessage();
                }
                if (game.isOver()) {
                    yield "This board is over";
                }
                yield messageFor(game, game.tryMove(command.position()));
            }
            case INVALID -> "Invalid input. Use a coordinate, move 1a 2a, f <coord>, switch <n>, demo, stop, or quit";
            case QUIT -> "";
        };
    }

    private static String formatDisc(Disc disc) {
        return disc == Disc.BLACK ? "Black" : "White";
    }

    private static String messageFor(GameSession game, ActionResult result) {
        return switch (result) {
            case OK -> "";
            case OUT_OF_BOUNDS -> "Out of bounds";
            case NOT_EMPTY -> "Cell is not empty";
            case INVALID_MOVE -> shouldPromptPass(game) ? "You still have legal moves, so pass is not allowed" : "Invalid move";
            case ALREADY_REVEALED -> game.mode() == GameMode.MINESWEEPER ? "Cell already revealed" : "Cell is already occupied";
            case FLAG_ON -> "Flag placed";
            case FLAG_OFF -> "Flag removed";
            case FLAGGED_CELL -> "Cell is flagged. Remove the flag first";
            case MINE_HIT -> "Hit a mine. Game over";
            case CLEAR_WIN -> "All safe cells revealed. You win";
            case GAME_OVER -> "This board is over";
            case PASS_OK -> "Turn passed";
            case PASS_NOT_ALLOWED -> game.mode() == GameMode.REVERSI ? "Pass is not allowed because legal moves exist" : "Pass is not supported in this mode";
            case CHECKMATE -> "Checkmate! Game over";
            case STALEMATE -> "Stalemate! Game is a draw";
        };
    }

    private static boolean shouldPromptPass(GameSession game) {
        return game.mode() == GameMode.REVERSI && !game.isOver() && game.validMoves().isEmpty();
    }

    private static String readLine(
        Screen screen,
        MultiGameManager manager,
        GameSession game,
        String message,
        String prompt,
        Position[] cursor
    ) throws Exception {
        StringBuilder buffer = new StringBuilder();
        Position selectedChessFrom = null;
        while (true) {
            draw(screen, manager, game, message, cursor[0], false);
            drawInput(screen, game, prompt, inputPreview(buffer, selectedChessFrom, cursor[0]));
            screen.refresh(Screen.RefreshType.DELTA);

            KeyStroke key = screen.readInput();
            if (key == null || key.getKeyType() == KeyType.EOF) {
                return null;
            }
            if (key.getKeyType() == KeyType.Enter) {
                if (game.mode() == GameMode.CHESS && buffer.isEmpty()) {
                    if (selectedChessFrom == null) {
                        selectedChessFrom = cursor[0];
                        continue;
                    }
                    return "m " + cellLabel(selectedChessFrom) + " " + cellLabel(cursor[0]);
                }
                if (buffer.isEmpty()) {
                    return cellLabel(cursor[0]);
                }
                return buffer.toString();
            }
            updateCursor(key.getKeyType(), cursor, game.board().size());
            if (key.getKeyType() == KeyType.Backspace && selectedChessFrom != null && buffer.isEmpty()) {
                selectedChessFrom = null;
                continue;
            }
            if (key.getKeyType() == KeyType.Backspace && !buffer.isEmpty()) {
                buffer.deleteCharAt(buffer.length() - 1);
            }
            if (key.getKeyType() == KeyType.Character) {
                Character ch = key.getCharacter();
                if (ch != null && !Character.isISOControl(ch)) {
                    selectedChessFrom = null;
                    buffer.append(ch);
                }
            }
        }
    }

    private static String runDemoMode(
        Screen screen,
        MultiGameManager manager,
        DemoController demoController,
        Position[] cursor
    ) throws Exception {
        StringBuilder buffer = new StringBuilder();
        String message = demoController.lastMessage();
        while (true) {
            GameSession game = manager.activeGame();
            long now = System.currentTimeMillis();
            if (demoController.shouldTick(now)) {
                message = demoController.tick(now);
                game = manager.activeGame();
            }

            draw(screen, manager, game, message, cursor[0], true);
            drawInput(screen, game, "DEMO> ", buffer.toString());
            screen.refresh(Screen.RefreshType.DELTA);

            KeyStroke key = screen.pollInput();
            if (key != null) {
                if (key.getKeyType() == KeyType.EOF) {
                    return "quit";
                }
                if (key.getKeyType() == KeyType.Enter) {
                    String commandText = buffer.toString();
                    buffer.setLength(0);
                    ParsedCommand command = CommandParser.parse(commandText);
                    if (command.type() == CommandType.QUIT) {
                        return "quit";
                    }
                    if (command.type() == CommandType.STOP) {
                        return "Demo stopped";
                    }
                    message = "[ DEMO MODE ] type stop or quit";
                    continue;
                }
                updateCursor(key.getKeyType(), cursor, game.board().size());
                if (key.getKeyType() == KeyType.Backspace && !buffer.isEmpty()) {
                    buffer.deleteCharAt(buffer.length() - 1);
                }
                if (key.getKeyType() == KeyType.Character) {
                    Character ch = key.getCharacter();
                    if (ch != null && !Character.isISOControl(ch)) {
                        buffer.append(ch);
                    }
                }
            }

            Thread.sleep(50);
        }
    }

    private static void draw(
        Screen screen,
        MultiGameManager manager,
        GameSession game,
        String message,
        Position cursor,
        boolean demoMode
    ) {
        if (screen.doResizeIfNecessary() != null) {
            screen.clear();
        }
        TextGraphics g = screen.newTextGraphics();
        g.setForegroundColor(FG);
        g.setBackgroundColor(PANEL_BG);

        TerminalSize terminalSize = screen.getTerminalSize();
        int totalWidth = terminalSize.getColumns();
        int totalHeight = terminalSize.getRows();
        int contentTop = OUTER_MARGIN;
        int contentLeft = OUTER_MARGIN;
        int contentWidth = Math.max(30, totalWidth - OUTER_MARGIN * 2);
        int contentHeight = Math.max(12, totalHeight - FOOTER_HEIGHT - OUTER_MARGIN * 2);

        int boardPanelWidth = Math.min(BOARD_PANEL_WIDTH, Math.max(20, contentWidth / 3));
        int listPanelWidth = Math.min(LIST_PANEL_WIDTH, Math.max(18, contentWidth / 4));
        int statusPanelWidth = contentWidth - boardPanelWidth - listPanelWidth - PANEL_GAP * 2;
        if (statusPanelWidth < 34) {
            int missing = 34 - statusPanelWidth;
            int shrinkBoard = Math.min(missing / 2 + missing % 2, Math.max(0, boardPanelWidth - 20));
            boardPanelWidth -= shrinkBoard;
            missing -= shrinkBoard;
            int shrinkList = Math.min(missing, Math.max(0, listPanelWidth - 18));
            listPanelWidth -= shrinkList;
            statusPanelWidth = contentWidth - boardPanelWidth - listPanelWidth - PANEL_GAP * 2;
        }

        int boardX = contentLeft;
        int statusX = boardX + boardPanelWidth + PANEL_GAP;
        int listX = statusX + statusPanelWidth + PANEL_GAP;
        int panelsY = contentTop;

        drawBox(g, boardX, panelsY, boardPanelWidth, contentHeight, " BOARD ");
        drawBox(g, statusX, panelsY, statusPanelWidth, contentHeight, " STATUS ");
        drawBox(g, listX, panelsY, listPanelWidth, contentHeight, " GAME LIST ");

        drawBoardPanel(g, boardX, panelsY, boardPanelWidth, contentHeight, game, cursor);
        drawStatusPanel(g, statusX, panelsY, statusPanelWidth, contentHeight, manager, game, message, cursor, demoMode);
        drawGameList(g, listX, panelsY, listPanelWidth, contentHeight, manager);
        drawFooter(screen, game, message, buildFooterHelp(game, demoMode));
    }

    private static void drawBoardPanel(TextGraphics g, int x, int y, int width, int height, GameSession game, Position cursor) {
        int innerX = x + 2;
        int innerY = y + 1;
        int boardSize = game.board().size();

        Set<Position> validMoves = (!game.isOver() && game.mode() == GameMode.REVERSI)
            ? game.validMoves()
            : Set.of();

        g.setForegroundColor(COORD);
        StringBuilder colHeader = new StringBuilder();
        for (int c = 0; c < boardSize; c++) {
            colHeader.append(' ').append(indexToLetter(c));
        }
        putClipped(g, innerX + 3, innerY, width - 7, colHeader.toString());

        for (int r = 0; r < boardSize; r++) {
            int rowY = innerY + 1 + r;
            g.setForegroundColor(COORD);
            putClipped(g, innerX, rowY, 3, padLeft(String.valueOf(r + 1), 2) + " ");

            int cellX = innerX + 3;
            for (int c = 0; c < boardSize; c++) {
                Position p = new Position(r, c);
                boolean isCursor = p.equals(cursor);
                if (isCursor) {
                    g.setBackgroundColor(HIGHLIGHT);
                    g.setForegroundColor(TextColor.ANSI.BLACK);
                } else {
                    g.setBackgroundColor(PANEL_BG);
                }
                if (game.mode() == GameMode.MINESWEEPER) {
                    String cell = ((MinesweeperGame) game).cellDisplay(p);
                    if (!isCursor) {
                        g.setForegroundColor(minesweeperColor(cell));
                    }
                    putClipped(g, cellX + c * CELL_WIDTH, rowY, CELL_WIDTH, " " + cell);
                    g.setBackgroundColor(PANEL_BG);
                    continue;
                }
                if (game.mode() == GameMode.CHESS) {
                    char piece = ((ChessGame) game).pieceAt(p);
                    String cell = String.valueOf(piece);
                    if (!isCursor) {
                        g.setForegroundColor(chessPieceColor(piece));
                    }
                    putClipped(g, cellX + c * CELL_WIDTH, rowY, CELL_WIDTH, " " + cell);
                    g.setBackgroundColor(PANEL_BG);
                    continue;
                }

                Optional<Disc> disc = game.board().get(p);
                String cell;
                if (disc.isPresent()) {
                    cell = disc.get() == Disc.BLACK ? BLACK_DISC : WHITE_DISC;
                } else if (validMoves.contains(p)) {
                    cell = VALID_MOVE;
                } else {
                    cell = EMPTY_CELL;
                }
                if (!isCursor) {
                    g.setForegroundColor(FG);
                }
                putClipped(g, cellX + c * CELL_WIDTH, rowY, CELL_WIDTH, " " + cell);
                g.setBackgroundColor(PANEL_BG);
            }
        }

        int legendY = innerY + boardSize + 2;
        g.setForegroundColor(WARNING);
        String legend = switch (game.mode()) {
            case MINESWEEPER -> "# hidden  F flag  * mine  . zero";
            case CHESS -> "KQRBNP black  kqrbnp white  . empty";
            default -> BLACK_DISC + " black  " + WHITE_DISC + " white  + legal  . empty";
        };
        putClipped(g, innerX, legendY, width - 4, legend);

        int helpY = legendY + 2;
        g.setForegroundColor(MUTED);
        putClipped(g, innerX, helpY++, width - 4, "Commands");
        g.setForegroundColor(FG);
        for (String line : commandHelp(game)) {
            if (helpY >= y + height - 1) {
                break;
            }
            putClipped(g, innerX, helpY++, width - 4, line);
        }
    }

    private static void drawStatusPanel(
        TextGraphics g,
        int x,
        int y,
        int width,
        int height,
        MultiGameManager manager,
        GameSession game,
        String message,
        Position cursor,
        boolean demoMode
    ) {
        int innerX = x + 1;
        int innerY = y + 1;
        int innerWidth = width - 2;

        g.setForegroundColor(FG);
        putClipped(g, innerX, innerY, innerWidth, "Game #" + (manager.activeIndex() + 1));

        List<String> lines = middlePanelLines(game, cursor, message, demoMode);
        int textY = innerY + 2;
        for (String line : lines) {
            g.setForegroundColor(statusLineColor(game, line));
            if (textY >= y + height - 1) {
                break;
            }
            putClipped(g, innerX, textY++, innerWidth, line);
        }
    }

    private static void drawGameList(TextGraphics g, int x, int y, int width, int height, MultiGameManager manager) {
        int innerX = x + 1;
        int innerY = y + 1;
        int active = manager.activeIndex();
        int innerWidth = width - 2;
        for (int i = 0; i < manager.totalGames(); i++) {
            GameSession gSession = manager.getGame(i);
            String mode = gSession == null ? "unknown" : modeLabel(gSession.mode());
            if (i == active) {
                g.setForegroundColor(HIGHLIGHT);
            } else {
                g.setForegroundColor(FG);
            }
            putClipped(g, innerX, innerY + i, innerWidth, gameListLine(i + 1, mode, i == active, innerWidth));
        }

        g.setForegroundColor(MUTED);
        int infoY = innerY + Math.min(manager.totalGames() + 2, height - 6);
        putClipped(g, innerX, infoY++, innerWidth, "New");
        for (GamePlugin plugin : manager.registry().plugins()) {
            if (infoY >= y + height - 1) {
                break;
            }
            putClipped(g, innerX, infoY++, innerWidth, plugin.id());
        }
    }

    private static void drawFooter(Screen screen, GameSession game, String message, String shortcuts) {
        TextGraphics g = screen.newTextGraphics();
        TerminalSize ts = screen.getTerminalSize();
        int y = ts.getRows() - 3;
        int footerWidth = Math.max(0, ts.getColumns() - OUTER_MARGIN * 2 - 1);
        g.setBackgroundColor(PANEL_BG);
        g.setForegroundColor(MUTED);
        String footerMessage = (message == null || message.isBlank()) ? "" : "Message: " + message;
        putClipped(g, OUTER_MARGIN, y, footerWidth, footerMessage);
        g.setForegroundColor(HIGHLIGHT);
        putClipped(g, OUTER_MARGIN, y + 1, footerWidth, shortcuts);
        g.setForegroundColor(WARNING);
        String modeHint = switch (game.mode()) {
            case MINESWEEPER -> "1a open | f 1a flag | s 2 | demo | stop | quit";
            case CHESS -> "m 7a 5a | m 8e 8g castle | s 2 | demo | quit";
            default -> "1a move | pass | s 2 | demo | quit";
        };
        putClipped(g, OUTER_MARGIN, y + 2, footerWidth, modeHint);
    }

    private static String buildFooterHelp(GameSession game, boolean demoMode) {
        if (demoMode) {
            return "Input> demo running | type stop to return | quit exits";
        }
        if (game.mode() == GameMode.MINESWEEPER) {
            return "Input> reveal one cell at a time | first move is always safe";
        }
        if (game.mode() == GameMode.CHESS) {
            return "Input> arrows move cursor | Enter selects source then target";
        }
        return "Input> switch boards freely | each board keeps state";
    }

    private static void drawInput(Screen screen, GameSession game, String prompt, String input) {
        TextGraphics g = screen.newTextGraphics();
        TerminalSize ts = screen.getTerminalSize();
        int inputWidth = Math.max(0, ts.getColumns() - OUTER_MARGIN * 2 - 1);
        String line = fitToWidth((prompt != null ? prompt : "") + (input != null ? input : ""), inputWidth);
        int y = ts.getRows() - 1;
        g.setForegroundColor(HIGHLIGHT);
        putClipped(g, OUTER_MARGIN, y, inputWidth, line);
    }

    private static String inputPreview(StringBuilder buffer, Position selectedChessFrom, Position cursor) {
        if (buffer != null && !buffer.isEmpty()) {
            return buffer.toString();
        }
        if (selectedChessFrom == null) {
            return "";
        }
        return "m " + cellLabel(selectedChessFrom) + " -> " + cellLabel(cursor);
    }

    private static String fitToWidth(String text, int width) {
        if (text == null || width <= 0) {
            return "";
        }
        if (displayWidth(text) <= width) {
            return text;
        }
        if (width <= 3) {
            return clipToWidth(text, width);
        }
        return "..." + clipFromEnd(text, width - 3);
    }

    private static List<String> middlePanelLines(GameSession game, Position cursor, String message, boolean demoMode) {
        if (demoMode) {
            List<String> base = middlePanelLines(game, cursor, message, false);
            java.util.ArrayList<String> lines = new java.util.ArrayList<>();
            lines.add("[ DEMO MODE ]");
            lines.add("Demo: running");
            lines.addAll(base);
            return lines;
        }
        if (game.mode() == GameMode.MINESWEEPER) {
            MinesweeperGame minesweeper = (MinesweeperGame) game;
            String state = minesweeper.isOver()
                ? (minesweeper.lastMessage().contains("win") ? "State: Won" : "State: Lost")
                : "State: In progress";
            return List.of(
                "Mode: minesweeper",
                "Mines: " + minesweeper.mineCount(),
                "Flags: " + minesweeper.flagCount(),
                state,
                "Cursor: " + cellLabel(cursor),
                "Last: " + lastMessage(message, minesweeper.lastMessage()),
                "Hint: first move safe"
            );
        }

        Map<Disc, Integer> counts = game.counts();
        int black = counts.getOrDefault(Disc.BLACK, 0);
        int white = counts.getOrDefault(Disc.WHITE, 0);
        if (game.mode() == GameMode.CHESS) {
            ChessGame chess = (ChessGame) game;
            if (game.isOver()) {
                var detail = chess.pieceDetails();
                var wd = detail.get(Disc.WHITE);
                var bd = detail.get(Disc.BLACK);
                return List.of(
                    "Mode: chess",
                    "State: === GAME OVER ===",
                    chess.resultSummary(),
                    "White: " + formatPieces(wd),
                    "Black: " + formatPieces(bd),
                    "Cursor: " + cellLabel(cursor),
                    "Last: " + lastMessage(message, chess.lastMessage())
                );
            }
            return List.of(
                "Mode: chess",
                "To move: " + chessTurnLabel(game),
                "State: running",
                "Move: " + chess.moveCount(),
                "Cursor: " + cellLabel(cursor),
                "Last: " + lastMessage(message, chess.lastMessage()),
                "Hint: m 1a 2a"
            );
        }
        if (game.mode() == GameMode.PEACE) {
            return List.of(
                "Mode: peace",
                "Turn: " + formatDisc(game.current()),
                "Score B/W: " + black + " / " + white,
                "State: " + (game.isOver() ? "over" : "running"),
                "Cursor: " + cellLabel(cursor),
                "Last: " + lastMessage(message, "")
            );
        }
        String passHint = (!game.isOver() && game.validMoves().isEmpty()) ? "Hint: type pass" : "Hint: enter a coordinate";
        return List.of(
            "Mode: reversi",
            "Turn: " + formatDisc(game.current()),
            "Score B/W: " + black + " / " + white,
            "Legal: " + game.validMoves().size(),
            "Cursor: " + cellLabel(cursor),
            "Last: " + lastMessage(message, ""),
            passHint
        );
    }

    private static List<String> commandHelp(GameSession game) {
        if (game.mode() == GameMode.MINESWEEPER) {
            return List.of(
                "1a       open",
                "f 1a     flag",
                "s 2      switch",
                "minesweeper new",
                "demo     auto",
                "stop     manual",
                "quit"
            );
        }
        if (game.mode() == GameMode.CHESS) {
            return List.of(
                "m 1a 2a  move",
                "m 7a 8a q prom",
                "s 2      switch",
                "chess    new game",
                "demo     auto",
                "stop     manual",
                "quit"
            );
        }
        return List.of(
            "1a       move",
            "s 2      switch",
            "peace    new game",
            "pass",
            "demo     auto",
            "stop     manual",
            "quit"
        );
    }

    private static String modeLabel(GameMode mode) {
        return GameRegistry.defaultRegistry().displayName(mode);
    }

    private static String chessTurnLabel(GameSession game) {
        if (game.isOver()) {
            return "-";
        }
        return game.current() == Disc.WHITE ? "White (lowercase)" : "Black (UPPERCASE)";
    }

    private static TextColor statusLineColor(GameSession game, String line) {
        if (game.mode() == GameMode.CHESS && line != null && line.startsWith("To move:")) {
            return game.current() == Disc.WHITE ? WHITE_PIECE : BLACK_PIECE;
        }
        if ("[ DEMO MODE ]".equals(line)) {
            return WARNING;
        }
        return FG;
    }

    private static String gameListLine(int boardNumber, String mode, boolean active, int width) {
        String base = boardNumber + ". " + mode;
        if (!active) {
            return base;
        }
        String marker = "<-";
        int gap = width - displayWidth(base) - displayWidth(marker);
        if (gap >= 1) {
            return base + " ".repeat(gap) + marker;
        }
        return base;
    }

    private static TextColor chessPieceColor(char piece) {
        if (piece == '.') {
            return COORD;
        }
        if (Character.isUpperCase(piece)) {
            return BLACK_PIECE;
        }
        return WHITE_PIECE;
    }

    private static TextColor minesweeperColor(String cell) {
        return switch (cell) {
            case "*" -> MINE_COLOR;
            case "F" -> FLAG_COLOR;
            case "." -> ZERO_COLOR;
            case "1" -> TextColor.ANSI.CYAN_BRIGHT;
            case "2" -> TextColor.ANSI.GREEN;
            case "3" -> TextColor.ANSI.YELLOW;
            case "4" -> TextColor.ANSI.CYAN;
            case "5" -> TextColor.ANSI.MAGENTA;
            case "6" -> TextColor.ANSI.WHITE_BRIGHT;
            case "7" -> TextColor.ANSI.RED_BRIGHT;
            case "8" -> TextColor.ANSI.WHITE;
            default -> FG;
        };
    }

    private static void drawBox(TextGraphics g, int x, int y, int width, int height, String title) {
        if (width < 4 || height < 3) {
            return;
        }
        g.setForegroundColor(FG);
        g.putString(x, y, "+" + "-".repeat(width - 2) + "+");
        for (int row = 1; row < height - 1; row++) {
            g.putString(x, y + row, "|" + " ".repeat(width - 2) + "|");
        }
        g.putString(x, y + height - 1, "+" + "-".repeat(width - 2) + "+");
        if (title != null && !title.isBlank() && width > title.length() + 4) {
            g.setForegroundColor(HIGHLIGHT);
            g.putString(x + 2, y, title);
        }
    }

    private static void putClipped(TextGraphics g, int x, int y, int width, String text) {
        if (width <= 0) {
            return;
        }
        String safe = clipToWidth(text == null ? "" : text, width);
        g.putString(x, y, padRight(safe, width));
    }

    private static String padRight(String text, int width) {
        int displayWidth = displayWidth(text);
        if (displayWidth >= width) {
            return text;
        }
        return text + " ".repeat(width - displayWidth);
    }

    private static String padLeft(String text, int width) {
        if (text.length() >= width) {
            return text;
        }
        return " ".repeat(width - text.length()) + text;
    }

    private static String indexToLetter(int index) {
        return String.valueOf((char) ('a' + index));
    }

    private static void updateCursor(KeyType keyType, Position[] cursor, int boardSize) {
        int row = cursor[0].row();
        int col = cursor[0].col();
        switch (keyType) {
            case ArrowUp -> row--;
            case ArrowDown -> row++;
            case ArrowLeft -> col--;
            case ArrowRight -> col++;
            default -> {
                return;
            }
        }
        int max = Math.max(0, boardSize - 1);
        row = Math.max(0, Math.min(max, row));
        col = Math.max(0, Math.min(max, col));
        cursor[0] = new Position(row, col);
    }

    private static String cellLabel(Position p) {
        return (p.row() + 1) + String.valueOf((char) ('a' + p.col()));
    }

    private static String lastMessage(String message, String fallback) {
        if (message != null && !message.isBlank()) {
            return message;
        }
        return fallback == null ? "" : fallback;
    }

    private static String formatPieces(Map<Character, Integer> detail) {
        if (detail == null || detail.isEmpty()) return "none";
        StringBuilder sb = new StringBuilder();
        char[] order = {'K', 'Q', 'R', 'B', 'N', 'P'};
        for (char t : order) {
            Integer n = detail.get(t);
            if (n != null) sb.append(n).append(t).append(" ");
        }
        return sb.toString().trim();
    }

    private static String clipToWidth(String text, int width) {
        if (text == null || width <= 0) {
            return "";
        }
        if (displayWidth(text) <= width) {
            return text;
        }
        if (width <= 3) {
            return firstColumns(text, width);
        }
        return firstColumns(text, width - 3) + "...";
    }

    private static String firstColumns(String text, int width) {
        StringBuilder out = new StringBuilder();
        int used = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            int charWidth = charDisplayWidth(ch);
            if (used + charWidth > width) {
                break;
            }
            out.append(ch);
            used += charWidth;
        }
        return out.toString();
    }

    private static String clipFromEnd(String text, int width) {
        StringBuilder out = new StringBuilder();
        int used = 0;
        for (int i = text.length() - 1; i >= 0; i--) {
            char ch = text.charAt(i);
            int charWidth = charDisplayWidth(ch);
            if (used + charWidth > width) {
                break;
            }
            out.insert(0, ch);
            used += charWidth;
        }
        return out.toString();
    }

    private static int displayWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            width += charDisplayWidth(text.charAt(i));
        }
        return width;
    }

    private static int charDisplayWidth(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
            || block == Character.UnicodeBlock.HIRAGANA
            || block == Character.UnicodeBlock.KATAKANA
            || block == Character.UnicodeBlock.HANGUL_SYLLABLES
            || block == Character.UnicodeBlock.HANGUL_JAMO
            || block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO) {
            return 2;
        }
        return 1;
    }
}
