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
import reversi.core.BoardGameSession;
import reversi.games.chess.ChessSession;
import reversi.core.model.Disc;
import reversi.core.model.GameMode;
import reversi.core.GameSession;
import reversi.games.minesweeper.MinesweeperSession;
import reversi.gamehall.MultiGameManager;
import reversi.core.PassableSession;
import reversi.core.model.Position;
import reversi.gamehall.DemoController;
import reversi.gamehall.GameController;
import reversi.gamehall.GameHall;
import reversi.gamehall.GamePlugin;
import reversi.gamehall.GameStatusPresenter;
import reversi.gamehall.UiPlugin;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class LanternaUi implements UiPlugin {
    private static final TextColor FG = TextColor.ANSI.WHITE;
    private static final TextColor HIGHLIGHT = TextColor.ANSI.CYAN;
    private static final TextColor WARNING = TextColor.ANSI.YELLOW;
    private static final TextColor MUTED = TextColor.ANSI.WHITE_BRIGHT;
    private static final TextColor COORD = TextColor.ANSI.BLACK_BRIGHT;
    private static final TextColor BLACK_PIECE = TextColor.ANSI.BLACK;
    private static final TextColor WHITE_PIECE = TextColor.ANSI.WHITE_BRIGHT;
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

    @Override
    public String name() {
        return "lanterna";
    }

    @Override
    public void launch(GameHall hall) throws Exception {
        runMulti(hall.manager());
    }

    private void runMulti(MultiGameManager manager) throws Exception {
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

                ParsedCommand command = CommandParser.parse(input, manager.registry());
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
        return GameController.dispatch(manager, game, command);
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
            updateCursor(key.getKeyType(), cursor, game.boardSize());
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
                    ParsedCommand command = CommandParser.parse(commandText, manager.registry());
                    if (command.type() == CommandType.QUIT) {
                        return "quit";
                    }
                    if (command.type() == CommandType.STOP) {
                        return "Demo stopped";
                    }
                    message = "[ DEMO MODE ] type stop or quit";
                    continue;
                }
                updateCursor(key.getKeyType(), cursor, game.boardSize());
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
        int boardSize = game.boardSize();

        Set<Position> validMoves = validMovesFor(game);

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
                if (game instanceof MinesweeperSession) {
                    String cell = game.cellDisplay(p);
                    if (!isCursor) {
                        g.setForegroundColor(minesweeperColor(cell));
                    }
                    putClipped(g, cellX + c * CELL_WIDTH, rowY, CELL_WIDTH, " " + cell);
                    g.setBackgroundColor(PANEL_BG);
                    continue;
                }
                if (game instanceof ChessSession chess) {
                    char piece = chess.pieceAt(p);
                    String cell = String.valueOf(piece);
                    if (!isCursor) {
                        setChessCellColors(g, piece);
                    }
                    putClipped(g, cellX + c * CELL_WIDTH, rowY, CELL_WIDTH, " " + cell);
                    g.setBackgroundColor(PANEL_BG);
                    continue;
                }

                Optional<Disc> disc = game instanceof BoardGameSession boardGame ? boardGame.board().get(p) : Optional.empty();
                String cell;
                if (disc.isPresent()) {
                    cell = disc.get() == Disc.BLACK ? BLACK_DISC : WHITE_DISC;
                } else if (validMoves.contains(p)) {
                    cell = VALID_MOVE;
                } else {
                    cell = EMPTY_CELL;
                }
                if (!isCursor) {
                    if (disc.isPresent()) {
                        setDiscCellColors(g, disc.get());
                    } else {
                        g.setForegroundColor(FG);
                    }
                }
                putClipped(g, cellX + c * CELL_WIDTH, rowY, CELL_WIDTH, " " + cell);
                g.setBackgroundColor(PANEL_BG);
            }
        }

        int legendY = innerY + boardSize + 2;
        g.setForegroundColor(WARNING);
        String legend;
        if (game.mode().equals(GameMode.MINESWEEPER)) {
            legend = "# hidden  F flag  * mine  . zero";
        } else if (game.mode().equals(GameMode.CHESS)) {
            legend = "KQRBNP black  kqrbnp white  . empty";
        } else {
            legend = BLACK_DISC + " black  " + WHITE_DISC + " white  + legal  . empty";
        }
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

        List<String> lines = GameStatusPresenter.statusLines(manager.registry(), game, cellLabel(cursor), message, demoMode);
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
            String mode = gSession == null ? "unknown" : manager.registry().displayName(gSession.mode());
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
        String modeHint;
        if (game.mode().equals(GameMode.MINESWEEPER)) {
            modeHint = "1a open | f 1a flag | s 2 | demo | stop | quit";
        } else if (game.mode().equals(GameMode.CHESS)) {
            modeHint = "m 7a 5a | m 8e 8g castle | s 2 | demo | quit";
        } else if (game instanceof PassableSession) {
            modeHint = "1a move | pass | s 2 | demo | quit";
        } else {
            modeHint = "1a move | s 2 | demo | quit";
        }
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

    private static List<String> commandHelp(GameSession game) {
        return GameController.commandHelp(game);
    }

    private static TextColor statusLineColor(GameSession game, String line) {
        if (game.mode() == GameMode.CHESS && line != null && line.startsWith("To move:")) {
            return game instanceof ChessSession chess && chess.current() == Disc.WHITE ? WHITE_PIECE : MUTED;
        }
        if (line != null && line.startsWith("Turn:")) {
            return line.contains("White") ? WHITE_PIECE : MUTED;
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

    private static void setChessCellColors(TextGraphics g, char piece) {
        if (piece == '.') {
            g.setForegroundColor(COORD);
        } else if (Character.isUpperCase(piece)) {
            g.setBackgroundColor(TextColor.ANSI.WHITE_BRIGHT);
            g.setForegroundColor(BLACK_PIECE);
        } else {
            g.setForegroundColor(WHITE_PIECE);
        }
    }

    private static void setDiscCellColors(TextGraphics g, Disc disc) {
        if (disc == Disc.BLACK) {
            g.setBackgroundColor(TextColor.ANSI.WHITE_BRIGHT);
            g.setForegroundColor(BLACK_PIECE);
        } else {
            g.setForegroundColor(WHITE_PIECE);
        }
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

    private static Set<Position> validMovesFor(GameSession game) {
        return GameController.validMovesFor(game);
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
