package reversi.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import reversi.command.CommandParser;
import reversi.command.CommandType;
import reversi.command.ParsedCommand;
import reversi.core.*;
import reversi.gamehall.DemoController;
import reversi.gamehall.GameHall;
import reversi.gamehall.GamePlugin;
import reversi.gamehall.GameRegistry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class JavaFxUi extends Application {

    private static int startBoardSize = 8;

    // Colors matching the Lanterna terminal theme
    private static final Color BG        = Color.web("#111111");
    private static final Color FG        = Color.WHITE;
    private static final Color HIGHLIGHT = Color.CYAN;
    private static final Color WARNING   = Color.YELLOW;
    private static final Color MUTED     = Color.web("#cccccc");
    private static final Color COORD     = Color.web("#888888");
    private static final Color BLK_PC    = Color.web("#ffd700");  // yellow-bright
    private static final Color WHT_PC    = Color.web("#90ee90");  // green-bright
    private static final Color MINE_CLR  = Color.web("#ff4444");
    private static final Color FLAG_CLR  = Color.YELLOW;

    private static final String FONT   = "Courier New";
    private static final double FS     = 14;
    private static final double CELL   = 36;  // 从 26 改为 36，增大棋子显示

    // Game state
    private MultiGameManager manager;
    private Position cursor   = new Position(0, 0);
    private String   message  = "";
    private boolean  demoMode = false;
    private DemoController demoCtrl;
    private Position chessFrom = null;
    private Timeline demoLine;

    // UI nodes (set during start())
    private GridPane boardGrid;
    private VBox     statusBox;
    private VBox     gameListBox;
    private Label    msgLabel;
    private Label    shortcutLabel;
    private Label    hintLabel;
    private Label    promptLabel;
    private TextField cmdField;

    public static void launchApp(int boardSize) {
        startBoardSize = boardSize;
        launch(JavaFxUi.class);
    }

    @Override
    public void start(Stage stage) {
        GameHall hall = GameHall.newHall(startBoardSize);
        manager = hall.manager();

        BorderPane root = new BorderPane();
        root.setBackground(solidBg(BG));
        root.setPadding(new Insets(8));

        HBox panels = new HBox(8);
        panels.setFillHeight(true);

        VBox boardPanel  = titledPanel(" BOARD ");
        boardGrid = new GridPane();
        boardGrid.setPadding(new Insets(6, 4, 4, 4));
        boardPanel.getChildren().add(boardGrid);

        VBox statusPanel = titledPanel(" STATUS ");
        statusBox = new VBox(3);
        statusBox.setPadding(new Insets(4));
        statusPanel.getChildren().add(statusBox);

        VBox listPanel   = titledPanel(" GAME LIST ");
        gameListBox = new VBox(3);
        gameListBox.setPadding(new Insets(4));
        listPanel.getChildren().add(gameListBox);

        boardPanel.setMinWidth(210);
        listPanel.setMinWidth(160);
        HBox.setHgrow(statusPanel, Priority.ALWAYS);

        panels.getChildren().addAll(boardPanel, statusPanel, listPanel);

        VBox footer = buildFooter();
        root.setCenter(panels);
        root.setBottom(footer);

        Scene scene = new Scene(root, 940, 680);
        scene.setFill(BG);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::onKey);

        stage.setTitle("Game Hall");
        stage.setScene(scene);
        stage.show();

        refresh();
        Platform.runLater(() -> cmdField.requestFocus());
    }

    // ── Key handling ─────────────────────────────────────────────────────────

    private void onKey(KeyEvent e) {
        int size = manager.activeGame().board().size();
        switch (e.getCode()) {
            case UP    -> { shiftCursor(-1,  0, size); e.consume(); }
            case DOWN  -> { shiftCursor( 1,  0, size); e.consume(); }
            case LEFT  -> { shiftCursor( 0, -1, size); e.consume(); }
            case RIGHT -> { shiftCursor( 0,  1, size); e.consume(); }
            case BACK_SPACE -> {
                if (cmdField.getText().isEmpty() && chessFrom != null) {
                    chessFrom = null;
                    message = "Selection cleared";
                    refresh();
                    e.consume();
                }
            }
            default -> {}
        }
    }

    private void shiftCursor(int dr, int dc, int size) {
        int r = Math.max(0, Math.min(size - 1, cursor.row() + dr));
        int c = Math.max(0, Math.min(size - 1, cursor.col() + dc));
        cursor = new Position(r, c);
        refresh();
    }

    private void submit() {
        String txt = cmdField.getText().trim();
        cmdField.clear();

        if (demoMode) {
            ParsedCommand cmd = CommandParser.parse(txt);
            if (cmd.type() == CommandType.QUIT)  { stopDemo(); Platform.exit(); return; }
            if (cmd.type() == CommandType.STOP)  { stopDemo(); message = "Demo stopped"; refresh(); return; }
            message = "[ DEMO MODE ] type stop or quit";
            refresh();
            return;
        }

        GameSession game = manager.activeGame();

        // Chess two-step cursor selection: empty input = select/confirm
        if (game.mode() == GameMode.CHESS && txt.isEmpty()) {
            if (chessFrom == null) {
                chessFrom = cursor;
                message = "Piece at " + pos(cursor) + " selected — move cursor and press Enter";
                refresh();
                return;
            } else {
                txt = "m " + pos(chessFrom) + " " + pos(cursor);
                chessFrom = null;
            }
        } else {
            chessFrom = null;
        }

        if (txt.isEmpty()) txt = pos(cursor);

        ParsedCommand cmd = CommandParser.parse(txt);
        if (cmd.type() == CommandType.QUIT)  { Platform.exit(); return; }
        if (cmd.type() == CommandType.DEMO)  { startDemo(); return; }

        message = dispatch(cmd, game);
        refresh();
    }

    // ── Demo mode ────────────────────────────────────────────────────────────

    private void startDemo() {
        demoMode = true;
        demoCtrl  = new DemoController(manager);
        message   = demoCtrl.lastMessage();
        demoLine  = new Timeline(new KeyFrame(Duration.millis(50), ev -> {
            long now = System.currentTimeMillis();
            if (demoCtrl.shouldTick(now)) message = demoCtrl.tick(now);
            refresh();
        }));
        demoLine.setCycleCount(Timeline.INDEFINITE);
        demoLine.play();
        refresh();
    }

    private void stopDemo() {
        demoMode = false;
        if (demoLine != null) { demoLine.stop(); demoLine = null; }
        demoCtrl = null;
    }

    // ── Command dispatch ─────────────────────────────────────────────────────

    private String dispatch(ParsedCommand cmd, GameSession game) {
        return switch (cmd.type()) {
            case NEW_GAME -> {
                manager.addGame(cmd.gameMode());
                yield "Added " + modeName(cmd.gameMode()) + " game " + manager.totalGames();
            }
            case SWITCH -> {
                if (manager.switchTo(cmd.boardIndex() - 1)) {
                    cursor    = new Position(0, 0);
                    chessFrom = null;
                    yield "Switched to board " + cmd.boardIndex();
                }
                yield "Invalid board number";
            }
            case PASS    -> resultMsg(game, game.tryPass());
            case FLAG    -> {
                if (game.mode() != GameMode.MINESWEEPER) yield "Flag is only available in minesweeper";
                yield resultMsg(game, game.tryFlag(cmd.position()));
            }
            case MOVE    -> {
                if (game.mode() == GameMode.CHESS) {
                    ChessGame chess = (ChessGame) game;
                    if (cmd.from() == null || cmd.to() == null) yield "坐标越界或格式错误";
                    chess.tryMove(cmd.from(), cmd.to(), cmd.promotionPiece());
                    yield chess.lastMessage();
                }
                if (game.isOver()) yield "This board is over";
                yield resultMsg(game, game.tryMove(cmd.position()));
            }
            case DEMO    -> "Demo mode started";
            case STOP    -> "Demo is not running";
            case INVALID -> "Invalid input. Use a coordinate, move 1a 2a, f <coord>, switch <n>, demo, stop, or quit";
            case QUIT    -> "";
        };
    }

    // ── Refresh ──────────────────────────────────────────────────────────────

    private void refresh() {
        GameSession game = manager.activeGame();
        drawBoard(game);
        drawStatus(game);
        drawGameList();
        drawFooter(game);
    }

    // ── Board panel ──────────────────────────────────────────────────────────

    private void drawBoard(GameSession game) {
        boardGrid.getChildren().clear();
        boardGrid.getColumnConstraints().clear();
        boardGrid.getRowConstraints().clear();

        int sz     = game.board().size();
        Set<Position> legal = (!game.isOver() && game.mode() == GameMode.REVERSI)
                ? game.validMoves() : Set.of();
        Font bold  = Font.font(FONT, FontWeight.BOLD, 16);  // 增大字体
        Font plain = Font.font(FONT, 14);

        // Column header
        boardGrid.add(lbl("   ", COORD, plain), 0, 0);
        for (int c = 0; c < sz; c++) {
            Label h = lbl(" " + (char) ('a' + c), COORD, plain);
            h.setMinWidth(CELL);
            h.setAlignment(Pos.CENTER);
            boardGrid.add(h, c + 1, 0);
        }

        // Rows
        for (int r = 0; r < sz; r++) {
            boardGrid.add(lbl(String.format("%2d ", r + 1), COORD, plain), 0, r + 1);
            for (int c = 0; c < sz; c++) {
                Position p     = new Position(r, c);
                boolean isCur  = p.equals(cursor);
                boolean isSel  = chessFrom != null && chessFrom.equals(p);

                String cellTxt;
                Color  cellClr;

                if (game.mode() == GameMode.MINESWEEPER) {
                    cellTxt = ((MinesweeperGame) game).cellDisplay(p);
                    cellClr = isCur ? Color.BLACK : msColor(cellTxt);
                } else if (game.mode() == GameMode.CHESS) {
                    char pc = ((ChessGame) game).pieceAt(p);
                    cellTxt = String.valueOf(pc);
                    cellClr = isCur ? Color.BLACK : chessColor(pc);
                } else {
                    Optional<Disc> d = game.board().get(p);
                    if (d.isPresent()) {
                        cellTxt = d.get() == Disc.BLACK ? "●" : "○";
                        cellClr = isCur ? Color.BLACK : (d.get() == Disc.BLACK ? BLK_PC : WHT_PC);
                    } else if (legal.contains(p)) {
                        cellTxt = "+";
                        cellClr = isCur ? Color.BLACK : WARNING;
                    } else {
                        cellTxt = "·";
                        cellClr = isCur ? Color.BLACK : COORD;
                    }
                }

                StackPane cell = new StackPane();
                cell.setMinSize(CELL, CELL);
                cell.setMaxSize(CELL, CELL);

                // 棋盘交替背景色（国际象棋风格）
                boolean isLight = (r + c) % 2 == 0;
                Color bgColor = isLight ? Color.web("#2a2a2a") : Color.web("#1f1f1f");

                if (isCur) {
                    cell.setStyle("-fx-background-color: #00bcd4; -fx-border-color: #00ffff; -fx-border-width: 2;");
                } else if (isSel) {
                    cell.setStyle("-fx-background-color: #e65100; -fx-border-color: #ff8c00; -fx-border-width: 2;");
                } else {
                    cell.setStyle(String.format("-fx-background-color: %s; -fx-border-color: #444; -fx-border-width: 1;",
                        toHexColor(bgColor)));
                }

                Label cl = lbl(cellTxt, isSel && !isCur ? Color.WHITE : cellClr, bold);
                cell.getChildren().add(cl);
                boardGrid.add(cell, c + 1, r + 1);
            }
        }

        boardGrid.getColumnConstraints().add(new ColumnConstraints(40));
        for (int c = 0; c < sz; c++)
            boardGrid.getColumnConstraints().add(new ColumnConstraints(CELL));
    }

    // ── Status panel ─────────────────────────────────────────────────────────

    private void drawStatus(GameSession game) {
        statusBox.getChildren().clear();
        Font f = Font.font(FONT, FS);

        statusBox.getChildren().add(lbl("Game #" + (manager.activeIndex() + 1), FG, f));
        statusBox.getChildren().add(gap());

        if (demoMode) {
            statusBox.getChildren().add(lbl("[ DEMO MODE ]", WARNING, f));
            statusBox.getChildren().add(lbl("Demo: running", FG, f));
        }

        for (String line : statusLines(game)) {
            Label l = lbl(line, statusColor(game, line), f);
            l.setWrapText(true);
            statusBox.getChildren().add(l);
        }

        statusBox.getChildren().add(gap());
        statusBox.getChildren().add(lbl("Commands", MUTED, f));
        for (String line : commandHelp(game))
            statusBox.getChildren().add(lbl(line, FG, f));

        statusBox.getChildren().add(gap());
        String legend = switch (game.mode()) {
            case MINESWEEPER -> "# hidden  F flag  * mine  . zero";
            case CHESS       -> "KQRBNP=black  kqrbnp=white  .=empty";
            default          -> "● black  ○ white  + legal  · empty";
        };
        Label leg = lbl(legend, WARNING, f);
        leg.setWrapText(true);
        statusBox.getChildren().add(leg);
    }

    // ── Game list panel ──────────────────────────────────────────────────────

    private void drawGameList() {
        gameListBox.getChildren().clear();
        Font f = Font.font(FONT, FS);
        for (int i = 0; i < manager.totalGames(); i++) {
            GameSession gs   = manager.getGame(i);
            String name      = gs == null ? "unknown" : modeName(gs.mode());
            boolean active   = i == manager.activeIndex();
            gameListBox.getChildren().add(
                lbl((i + 1) + ". " + name + (active ? " <-" : ""), active ? HIGHLIGHT : FG, f)
            );
        }
        gameListBox.getChildren().add(gap());
        gameListBox.getChildren().add(lbl("New:", MUTED, f));
        for (GamePlugin plugin : manager.registry().plugins())
            gameListBox.getChildren().add(lbl(plugin.id(), FG, f));
    }

    // ── Footer ───────────────────────────────────────────────────────────────

    private VBox buildFooter() {
        VBox footer = new VBox(3);
        footer.setPadding(new Insets(6, 0, 0, 0));

        msgLabel      = lbl("", MUTED,      Font.font(FONT, FS));
        shortcutLabel = lbl("", HIGHLIGHT,  Font.font(FONT, FS));
        hintLabel     = lbl("", WARNING,    Font.font(FONT, FS));

        HBox inputRow = new HBox(4);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        promptLabel = lbl("B1/1 > ", HIGHLIGHT, Font.font(FONT, FS));

        cmdField = new TextField();
        cmdField.setStyle(
            "-fx-background-color: #1a1a1a;" +
            "-fx-text-fill: #00ffff;" +
            "-fx-font-family: 'Courier New';" +
            "-fx-font-size: 14;" +
            "-fx-border-color: #444;" +
            "-fx-border-width: 1;" +
            "-fx-padding: 2 4 2 4;"
        );
        cmdField.setOnAction(e -> submit());
        HBox.setHgrow(cmdField, Priority.ALWAYS);
        inputRow.getChildren().addAll(promptLabel, cmdField);

        footer.getChildren().addAll(msgLabel, shortcutLabel, hintLabel, inputRow);
        return footer;
    }

    private void drawFooter(GameSession game) {
        msgLabel.setText(message == null || message.isBlank() ? "" : "Message: " + message);
        shortcutLabel.setText(shortcuts(game));
        hintLabel.setText(switch (game.mode()) {
            case MINESWEEPER -> "1a open | f 1a flag | s 2 | demo | stop | quit";
            case CHESS       -> "m 7a 5a | m 8e 8g castle | s 2 | demo | quit";
            default          -> "1a move | pass | s 2 | demo | quit";
        });
        int idx = manager.activeIndex() + 1;
        promptLabel.setText("B" + idx + "/" + manager.totalGames() + (demoMode ? " DEMO" : "") + " > ");
    }

    private String shortcuts(GameSession game) {
        if (demoMode) return "Input> demo running | type stop to return | quit exits";
        if (game.mode() == GameMode.CHESS)
            return chessFrom == null
                ? "Input> arrows move cursor | Enter selects source then target"
                : "Input> arrows move cursor | Enter confirms target | Backspace cancels";
        if (game.mode() == GameMode.MINESWEEPER) return "Input> reveal one cell at a time | first move is always safe";
        return "Input> switch boards freely | each board keeps state";
    }

    // ── Panel / label helpers ────────────────────────────────────────────────

    private VBox titledPanel(String title) {
        VBox panel = new VBox(4);
        panel.setBackground(solidBg(BG));
        panel.setStyle("-fx-border-color: #444444; -fx-border-width: 1;");
        panel.setPadding(new Insets(4));
        panel.getChildren().add(lbl(title, HIGHLIGHT, Font.font(FONT, FontWeight.BOLD, FS)));
        return panel;
    }

    private static Label lbl(String text, Color color, Font font) {
        Label l = new Label(text);
        l.setTextFill(color);
        l.setFont(font);
        return l;
    }

    private static Region gap() {
        Region r = new Region();
        r.setMinHeight(4);
        r.setMaxHeight(4);
        return r;
    }

    private static Background solidBg(Color c) {
        return new Background(new BackgroundFill(c, null, null));
    }

    private static String toHexColor(Color c) {
        return String.format("#%02X%02X%02X",
            (int)(c.getRed() * 255),
            (int)(c.getGreen() * 255),
            (int)(c.getBlue() * 255));
    }

    // ── Game-state helpers ───────────────────────────────────────────────────

    private List<String> statusLines(GameSession game) {
        if (game.mode() == GameMode.MINESWEEPER) {
            MinesweeperGame ms = (MinesweeperGame) game;
            String state = ms.isOver()
                ? (ms.lastMessage().contains("win") ? "State: Won" : "State: Lost")
                : "State: In progress";
            return List.of(
                "Mode: minesweeper",
                "Mines: " + ms.mineCount(),
                "Flags: " + ms.flagCount(),
                state,
                "Cursor: " + pos(cursor),
                "Last: "  + ms.lastMessage(),
                "Hint: first move safe"
            );
        }
        Map<Disc, Integer> cnt = game.counts();
        int b = cnt.getOrDefault(Disc.BLACK, 0);
        int w = cnt.getOrDefault(Disc.WHITE, 0);
        if (game.mode() == GameMode.CHESS) {
            ChessGame chess = (ChessGame) game;
            if (game.isOver()) {
                var detail = chess.pieceDetails();
                var wd = detail.get(Disc.WHITE);
                var bd = detail.get(Disc.BLACK);
                String wPieces = formatPieceDetail(wd);
                String bPieces = formatPieceDetail(bd);
                return List.of(
                    "Mode: chess",
                    "State: === GAME OVER ===",
                    chess.resultSummary(),
                    "White: " + wPieces,
                    "Black: " + bPieces,
                    "Cursor: " + pos(cursor),
                    "Last: " + chess.lastMessage()
                );
            }
            return List.of(
                "Mode: chess",
                "To move: " + chessTurn(game),
                "State: running",
                "Move: " + chess.moveCount(),
                "Cursor: " + pos(cursor),
                "Last: "  + chess.lastMessage(),
                "Hint: m 1a 2a  or cursor+Enter"
            );
        }
        if (game.mode() == GameMode.PEACE) {
            return List.of(
                "Mode: peace",
                "Turn: " + discName(game.current()),
                "Score B/W: " + b + " / " + w,
                "State: " + (game.isOver() ? "over" : "running"),
                "Cursor: " + pos(cursor),
                "Last: "  + message
            );
        }
        String passHint = (!game.isOver() && game.validMoves().isEmpty())
            ? "Hint: type pass" : "Hint: enter a coordinate";
        return List.of(
            "Mode: reversi",
            "Turn: " + discName(game.current()),
            "Score B/W: " + b + " / " + w,
            "Legal: " + game.validMoves().size(),
            "Cursor: " + pos(cursor),
            "Last: "  + message,
            passHint
        );
    }

    private List<String> commandHelp(GameSession game) {
        if (game.mode() == GameMode.MINESWEEPER)
            return List.of("1a       open", "f 1a     flag", "s 2      switch",
                "minesweeper new", "demo     auto", "stop     manual", "quit");
        if (game.mode() == GameMode.CHESS)
            return List.of("m 1a 2a  move", "m 7a 8a q prom", "s 2      switch",
                "chess    new game", "demo     auto", "stop     manual", "quit");
        return List.of("1a       move", "s 2      switch", "peace    new game",
            "pass", "demo     auto", "stop     manual", "quit");
    }

    private Color statusColor(GameSession game, String line) {
        if (game.mode() == GameMode.CHESS && line != null && line.startsWith("To move:"))
            return game.current() == Disc.WHITE ? WHT_PC : BLK_PC;
        if ("[ DEMO MODE ]".equals(line)) return WARNING;
        return FG;
    }

    private Color chessColor(char pc) {
        if (pc == '.') return COORD;
        return Character.isUpperCase(pc) ? BLK_PC : WHT_PC;
    }

    private Color msColor(String c) {
        return switch (c) {
            case "*" -> MINE_CLR;
            case "F" -> FLAG_CLR;
            case "." -> MUTED;
            case "1" -> Color.CYAN;
            case "2" -> Color.LIGHTGREEN;
            case "3" -> Color.YELLOW;
            case "4" -> Color.web("#add8e6");
            case "5" -> Color.MAGENTA;
            case "6" -> Color.AQUAMARINE;
            case "7" -> Color.ORANGERED;
            case "8" -> Color.WHITE;
            default  -> FG;
        };
    }

    private String resultMsg(GameSession game, ActionResult result) {
        return switch (result) {
            case OK               -> "";
            case OUT_OF_BOUNDS    -> "Out of bounds";
            case NOT_EMPTY        -> "Cell is not empty";
            case INVALID_MOVE     -> (game.mode() == GameMode.REVERSI && !game.isOver() && game.validMoves().isEmpty())
                    ? "You still have legal moves, so pass is not allowed" : "Invalid move";
            case ALREADY_REVEALED -> game.mode() == GameMode.MINESWEEPER ? "Cell already revealed" : "Cell is already occupied";
            case FLAG_ON          -> "Flag placed";
            case FLAG_OFF         -> "Flag removed";
            case FLAGGED_CELL     -> "Cell is flagged. Remove the flag first";
            case MINE_HIT         -> "Hit a mine. Game over";
            case CLEAR_WIN        -> "All safe cells revealed. You win";
            case GAME_OVER        -> "This board is over";
            case PASS_OK          -> "Turn passed";
            case PASS_NOT_ALLOWED -> game.mode() == GameMode.REVERSI
                    ? "Pass is not allowed because legal moves exist" : "Pass is not supported in this mode";
            case CHECKMATE         -> "Checkmate! Game over";
            case STALEMATE         -> "Stalemate! Game is a draw";
        };
    }

    private String modeName(GameMode mode) { return GameRegistry.defaultRegistry().displayName(mode); }
    private String formatPieceDetail(Map<Character, Integer> detail) {
        if (detail == null || detail.isEmpty()) return "none";
        StringBuilder sb = new StringBuilder();
        char[] order = {'K', 'Q', 'R', 'B', 'N', 'P'};
        for (char t : order) {
            Integer n = detail.get(t);
            if (n != null) sb.append(n).append(typeSymbol(t)).append(" ");
        }
        return sb.toString().trim();
    }
    private String typeSymbol(char type) {
        return switch (type) {
            case 'K' -> "\u265A";
            case 'Q' -> "\u265B";
            case 'R' -> "\u265C";
            case 'B' -> "\u265D";
            case 'N' -> "\u265E";
            case 'P' -> "\u265F";
            default -> String.valueOf(type);
        };
    }
    private String chessTurn(GameSession g) {
        if (g.isOver()) return "-";
        return g.current() == Disc.WHITE ? "White (lowercase)" : "Black (UPPERCASE)";
    }
    private String discName(Disc d) { return d == Disc.BLACK ? "Black" : "White"; }
    private String pos(Position p)  { return (p.row() + 1) + String.valueOf((char) ('a' + p.col())); }
}
