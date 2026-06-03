package reversi.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import reversi.core.BoardGameSession;
import reversi.core.FlaggableSession;
import reversi.core.GameSession;
import reversi.core.PassableSession;
import reversi.core.PositionMoveSession;
import reversi.core.model.ActionResult;
import reversi.core.model.Disc;
import reversi.core.model.GameMode;
import reversi.core.model.Position;
import reversi.games.chess.ChessSession;
import reversi.games.minesweeper.MinesweeperSession;
import reversi.gamehall.DemoController;
import reversi.gamehall.GameController;
import reversi.gamehall.GameHall;
import reversi.gamehall.GamePlugin;
import reversi.gamehall.GameStatusPresenter;
import reversi.gamehall.MultiGameManager;
import reversi.gamehall.UiPlugin;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JavaFxUi extends Application implements UiPlugin {
    private static final Color BG = Color.web("#171511");
    private static final Color PANEL_BG = Color.web("#211f1a");
    private static final Color SURFACE = Color.web("#27231c");
    private static final Color SURFACE_2 = Color.web("#332d23");
    private static final Color BORDER = Color.web("#3b3327");
    private static final Color FG = Color.web("#eee7d6");
    private static final Color HIGHLIGHT = Color.web("#9b7347");
    private static final Color WARNING = Color.web("#b9823b");
    private static final Color DANGER_COLOR = Color.web("#9f4a3f");
    private static final Color SUCCESS = Color.web("#6f8557");
    private static final Color MUTED = Color.web("#a99f8c");
    private static final Color COORD = Color.web("#7f725e");
    private static final Color BLACK_TURN = Color.web("#ded6c8");
    private static final Color WHITE_TURN = Color.web("#fff8e8");

    private static final String FONT = "System";
    private static final double FS = 14;
    private static final double CELL = 58;

    private static GameHall sharedHall;

    private MultiGameManager manager;
    private Position lastClicked = new Position(0, 0);
    private Position chessFrom;
    private String message = "Click a board square to play";
    private boolean demoMode;
    private DemoController demoCtrl;
    private Timeline demoLine;

    private GridPane boardGrid;
    private StackPane boardShell;
    private VBox controlsBox;
    private VBox statusBox;
    private VBox gameListBox;
    private Label boardTitle;
    private HBox boardMeta;
    private Label messageLabel;

    @Override
    public String name() {
        return "javafx";
    }

    @Override
    public void launch(GameHall hall) {
        sharedHall = hall;
        Application.launch(JavaFxUi.class);
    }

    @Override
    public void start(Stage stage) {
        GameHall hall = sharedHall;
        manager = hall.manager();

        BorderPane root = new BorderPane();
        root.setBackground(solidBg(BG));
        root.setPadding(new Insets(14));

        VBox leftPanel = titledPanel("Control");
        controlsBox = new VBox(8);
        controlsBox.setPadding(new Insets(4, 0, 8, 0));
        statusBox = new VBox(4);
        statusBox.setPadding(new Insets(8, 0, 0, 0));
        leftPanel.getChildren().addAll(controlsBox, separator(), statusBox);
        leftPanel.setPrefWidth(292);
        leftPanel.setMinWidth(270);

        VBox boardPanel = titledPanel("Board");
        HBox boardHeader = new HBox(12);
        boardHeader.setAlignment(Pos.CENTER_LEFT);
        boardTitle = lbl("", FG, Font.font(FONT, FontWeight.BOLD, 24));
        boardTitle.setWrapText(true);
        boardMeta = new HBox(8);
        boardMeta.setAlignment(Pos.CENTER_RIGHT);
        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);
        boardHeader.getChildren().addAll(boardTitle, titleSpacer, boardMeta);

        boardGrid = new GridPane();
        boardGrid.setAlignment(Pos.CENTER);
        boardGrid.setPadding(new Insets(12));
        boardShell = new StackPane(boardGrid);
        boardShell.setPadding(new Insets(12));
        boardShell.setStyle(boardShellStyle(null));
        boardPanel.getChildren().addAll(boardHeader, boardShell);
        HBox.setHgrow(boardPanel, Priority.ALWAYS);

        VBox rightPanel = titledPanel("Games");
        gameListBox = new VBox(6);
        gameListBox.setPadding(new Insets(4, 0, 0, 0));
        ScrollPane listScroll = scroll(gameListBox);
        rightPanel.getChildren().add(listScroll);
        rightPanel.setPrefWidth(260);
        rightPanel.setMinWidth(238);

        HBox main = new HBox(12, leftPanel, boardPanel, rightPanel);
        main.setFillHeight(true);
        root.setCenter(main);

        messageLabel = lbl("", MUTED, Font.font(FONT, FontWeight.BOLD, FS));
        messageLabel.setWrapText(true);
        messageLabel.setPadding(new Insets(10, 14, 10, 14));
        root.setBottom(messageLabel);

        Scene scene = new Scene(root, 1280, 760);
        scene.setFill(BG);
        stage.setTitle("Game Hall - JavaFX");
        stage.setScene(scene);
        stage.show();

        refresh();
    }

    private void refresh() {
        GameSession game = manager.activeGame();
        drawControls(game);
        drawStatus(game);
        drawBoard(game);
        drawGameList();
        drawMessage();
    }

    private void drawControls(GameSession game) {
        controlsBox.getChildren().clear();

        controlsBox.getChildren().addAll(modeBanner(game), sectionLabel("Match actions"));

        Button newGame = button("New Game", "Reset the selected board", ButtonTone.PRIMARY, () -> {
            stopDemo();
            if (manager.resetActiveGame()) {
                clearSelection();
                message = "New " + modeName(manager.activeGame().mode()) + " game started";
            }
            refresh();
        });

        Button pass = button("Pass", "Pass when the current Reversi player has no legal move", ButtonTone.SECONDARY, () -> {
            stopDemo();
            if (manager.activeGame() instanceof PassableSession passable) {
                ActionResult result = passable.tryPass();
                message = resultMsg(manager.activeGame(), result);
                if (message.isBlank()) {
                    message = "Turn passed";
                }
            }
            refresh();
        });
        pass.setDisable(!(game instanceof PassableSession) || game.isOver() || demoMode);

        Button demo = button("Demo", "Start automatic demo mode", ButtonTone.SECONDARY, this::startDemo);
        demo.setDisable(demoMode);

        Button stop = button("Stop Demo", "Return to manual mouse play", ButtonTone.SECONDARY, () -> {
            stopDemo();
            message = "Demo stopped";
            refresh();
        });
        stop.setDisable(!demoMode);

        HBox demoRow = new HBox(8, demo, stop);
        HBox.setHgrow(demo, Priority.ALWAYS);
        HBox.setHgrow(stop, Priority.ALWAYS);

        Button quit = button("Quit", "Exit the program safely", ButtonTone.DANGER, Platform::exit);

        controlsBox.getChildren().addAll(newGame, pass, demoRow, quit, separator(), sectionLabel("Add board"));

        for (GamePlugin plugin : manager.registry().plugins()) {
            Button add = button("+ " + plugin.displayName(), "Create another " + plugin.displayName() + " board",
                ButtonTone.SECONDARY, () -> {
                stopDemo();
                manager.addGame(plugin.mode());
                manager.switchTo(manager.totalGames() - 1);
                clearSelection();
                message = "Added " + plugin.displayName() + " game #" + manager.totalGames();
                refresh();
            });
            controlsBox.getChildren().add(add);
        }
    }

    private void drawStatus(GameSession game) {
        statusBox.getChildren().clear();
        Font f = Font.font(FONT, FS);
        statusBox.getChildren().add(sectionLabel("Status"));
        for (String line : statusLines(game)) {
            Label label = lbl(line, statusColor(game, line), f);
            label.setWrapText(true);
            label.setMaxWidth(Double.MAX_VALUE);
            label.setPadding(new Insets(7, 9, 7, 9));
            label.setStyle(panelStyle(Color.web("#0d141b"), BORDER, 6));
            statusBox.getChildren().add(label);
        }
        statusBox.getChildren().add(separator());
        Label hint = lbl(guiHint(game), WARNING, Font.font(FONT, FontWeight.BOLD, FS));
        hint.setWrapText(true);
        hint.setMaxWidth(Double.MAX_VALUE);
        hint.setPadding(new Insets(8, 10, 8, 10));
        hint.setStyle(panelStyle(Color.web("#1f1a10"), Color.web("#5c4213"), 6));
        statusBox.getChildren().add(hint);
    }

    private void drawBoard(GameSession game) {
        boardTitle.setText("Game #" + (manager.activeIndex() + 1) + " - " + displayMode(game.mode()));
        boardMeta.getChildren().setAll(summaryChips(game));
        boardShell.setStyle(boardShellStyle(game));
        boardGrid.getChildren().clear();
        boardGrid.getColumnConstraints().clear();
        boardGrid.setHgap(game instanceof MinesweeperSession ? 2 : 0);
        boardGrid.setVgap(game instanceof MinesweeperSession ? 2 : 0);

        int size = game.boardSize();
        Set<Position> legal = validMovesFor(game);
        Font coordFont = Font.font(FONT, FS);
        Font cellFont = Font.font(FONT, FontWeight.BOLD, 28);

        boardGrid.add(header(""), 0, 0);
        for (int col = 0; col < size; col++) {
            boardGrid.add(header(String.valueOf((char) ('a' + col))), col + 1, 0);
        }

        for (int row = 0; row < size; row++) {
            Label rowLabel = lbl(String.valueOf(row + 1), COORD, coordFont);
            rowLabel.setMinWidth(34);
            rowLabel.setAlignment(Pos.CENTER);
            boardGrid.add(rowLabel, 0, row + 1);
            for (int col = 0; col < size; col++) {
                Position p = new Position(row, col);
                StackPane cell = cell(game, p, legal, cellFont);
                boardGrid.add(cell, col + 1, row + 1);
            }
        }

        boardGrid.getColumnConstraints().add(new ColumnConstraints(34));
        for (int col = 0; col < size; col++) {
            boardGrid.getColumnConstraints().add(new ColumnConstraints(CELL));
        }
    }

    private StackPane cell(GameSession game, Position p, Set<Position> legal, Font cellFont) {
        JavaFxCellView.CellView view = JavaFxCellView.of(game, p, legal, chessFrom, lastClicked);
        Label label = lbl(view.text(), view.textColor(), cellFont);
        label.setAlignment(Pos.CENTER);
        label.setStyle(view.textStyle());

        StackPane cell = new StackPane(label);
        cell.setMinSize(CELL, CELL);
        cell.setMaxSize(CELL, CELL);
        cell.setCursor(Cursor.HAND);
        cell.setStyle(view.style());
        cell.setOnMouseClicked(event -> {
            handleCellClick(p, event.getButton());
            event.consume();
        });
        Tooltip.install(cell, new Tooltip(view.tip()));
        return cell;
    }

    private void drawGameList() {
        gameListBox.getChildren().clear();
        for (int i = 0; i < manager.totalGames(); i++) {
            GameSession game = manager.getGame(i);
            boolean active = i == manager.activeIndex();
            Label title = lbl("#" + (i + 1), active ? HIGHLIGHT : MUTED, Font.font(FONT, FontWeight.BOLD, 12));
            Label mode = lbl(displayMode(game.mode()), active ? FG : Color.web("#d8cfbd"),
                Font.font(FONT, FontWeight.BOLD, FS));
            Label state = lbl(game.isOver() ? "Finished" : "Playing", game.isOver() ? WARNING : SUCCESS,
                Font.font(FONT, FontWeight.BOLD, 12));
            state.setPadding(new Insets(2, 7, 2, 7));
            state.setStyle(panelStyle(game.isOver() ? Color.web("#2a2118") : Color.web("#202719"),
                game.isOver() ? Color.web("#5b4124") : Color.web("#475735"), 4));
            Region rowSpacer = new Region();
            HBox.setHgrow(rowSpacer, Priority.ALWAYS);
            HBox top = new HBox(8, title, rowSpacer, state);
            top.setAlignment(Pos.CENTER_LEFT);
            VBox itemText = new VBox(5, top, mode);
            StackPane item = new StackPane(itemText);
            item.setAlignment(Pos.CENTER_LEFT);
            item.setPadding(new Insets(10));
            item.setCursor(Cursor.HAND);
            item.setStyle(active
                ? panelStyle(Color.web("#2f281d"), HIGHLIGHT, 5)
                : panelStyle(SURFACE, BORDER, 7));
            int index = i;
            item.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    stopDemo();
                    if (manager.switchTo(index)) {
                        clearSelection();
                        message = "Switched to game #" + (index + 1);
                        refresh();
                    }
                }
                event.consume();
            });
            gameListBox.getChildren().add(item);
        }
    }

    private void drawMessage() {
        String demo = demoMode ? " [DEMO]" : "";
        messageLabel.setText((demoMode ? "Demo" : "Message") + demo + "  |  " + (message == null ? "" : message));
        messageLabel.setTextFill(demoMode ? WARNING : MUTED);
        messageLabel.setStyle(panelStyle(PANEL_BG, demoMode ? WARNING : BORDER, 8));
    }

    private void handleCellClick(Position p, MouseButton button) {
        lastClicked = p;
        if (demoMode) {
            message = "Stop Demo before manual play";
            refresh();
            return;
        }

        GameSession game = manager.activeGame();
        if (button == MouseButton.SECONDARY) {
            if (game instanceof FlaggableSession flaggable) {
                ActionResult result = flaggable.tryFlag(p);
                message = resultMsg(game, result);
            } else {
                message = "Right click is only used for minesweeper flags";
            }
            refresh();
            return;
        }
        if (button != MouseButton.PRIMARY) {
            return;
        }

        if (game instanceof ChessSession chess) {
            handleChessClick(chess, p);
        } else if (game instanceof PositionMoveSession mover) {
            ActionResult result = mover.tryMove(p);
            message = resultMsg(game, result);
            if (message.isBlank()) {
                message = "Played " + pos(p);
            }
        } else {
            message = "This game does not use board clicks";
        }
        refresh();
    }

    private void handleChessClick(ChessSession chess, Position p) {
        char piece = chess.pieceAt(p);
        if (chessFrom == null) {
            if (piece == '.') {
                message = "Select a chess piece first";
                return;
            }
            if (!isCurrentChessPiece(piece, chess.current())) {
                message = "Select a " + GameController.chessTurnLabel(chess) + " piece";
                return;
            }
            chessFrom = p;
            message = "Selected " + pos(p) + ". Click a target square";
            return;
        }

        if (chessFrom.equals(p)) {
            chessFrom = null;
            message = "Selection cleared";
            return;
        }
        if (piece != '.' && isCurrentChessPiece(piece, chess.current())) {
            chessFrom = p;
            message = "Selected " + pos(p) + ". Click a target square";
            return;
        }

        chess.tryMove(chessFrom, p, null);
        message = chess.lastMessage();
        chessFrom = null;
    }

    private void startDemo() {
        if (demoMode) {
            return;
        }
        clearSelection();
        demoMode = true;
        demoCtrl = new DemoController(manager);
        message = demoCtrl.lastMessage();
        demoLine = new Timeline(new KeyFrame(Duration.millis(50), event -> {
            long now = System.currentTimeMillis();
            if (!demoMode || demoCtrl == null || !demoCtrl.shouldTick(now)) {
                return;
            }
            int before = manager.activeIndex();
            message = demoCtrl.tick(now);
            if (manager.activeIndex() != before) {
                clearSelection();
            }
            refresh();
        }));
        demoLine.setCycleCount(Timeline.INDEFINITE);
        demoLine.play();
        refresh();
    }

    private void stopDemo() {
        demoMode = false;
        if (demoLine != null) {
            demoLine.stop();
            demoLine = null;
        }
        demoCtrl = null;
    }

    private List<String> statusLines(GameSession game) {
        String cursorLabel = chessFrom == null ? pos(lastClicked) : pos(lastClicked) + " selected " + pos(chessFrom);
        return GameStatusPresenter.statusLines(manager.registry(), game, cursorLabel, message, demoMode);
    }

    private String guiHint(GameSession game) {
        if (demoMode) {
            return "Demo is running. Use Stop Demo to return to manual play.";
        }
        if (game instanceof MinesweeperSession) {
            return "Left click opens a cell. Right click toggles a flag.";
        }
        if (game instanceof ChessSession) {
            return "Left click one of your pieces, then left click the target square.";
        }
        if (game instanceof PassableSession boardGame && boardGame instanceof BoardGameSession bg
                && !game.isOver() && bg.validMoves().isEmpty()) {
            return "No legal move. Click Pass.";
        }
        return "Left click an available square to play.";
    }

    private VBox modeBanner(GameSession game) {
        Label mode = lbl(displayMode(game.mode()), FG, Font.font(FONT, FontWeight.BOLD, 20));
        Label state = lbl("Game #" + (manager.activeIndex() + 1) + " of " + manager.totalGames()
            + "  |  " + stateText(game), stateColor(game), Font.font(FONT, FontWeight.BOLD, 12));
        VBox banner = new VBox(4, mode, state);
        banner.setPadding(new Insets(12));
        banner.setStyle(panelStyle(Color.web("#1b1813"), BORDER, 5));
        return banner;
    }

    private List<Label> summaryChips(GameSession game) {
        if (game instanceof MinesweeperSession ms) {
            return List.of(
                chip("Mines " + ms.mineCount(), WARNING),
                chip("Flags " + ms.flagCount(), HIGHLIGHT),
                chip(stateText(ms), stateColor(ms))
            );
        }
        if (game instanceof ChessSession chess) {
            return List.of(
                chip(turnText(chess.current()), chess.current() == Disc.WHITE ? WHITE_TURN : BLACK_TURN),
                chip("Move " + chess.moveCount(), HIGHLIGHT),
                chip(stateText(chess), stateColor(chess))
            );
        }
        if (game instanceof BoardGameSession boardGame) {
            Map<Disc, Integer> counts = boardGame.counts();
            return List.of(
                chip(turnText(boardGame.current()), boardGame.current() == Disc.WHITE ? WHITE_TURN : BLACK_TURN),
                chip("Black " + counts.getOrDefault(Disc.BLACK, 0), BLACK_TURN),
                chip("White " + counts.getOrDefault(Disc.WHITE, 0), WHITE_TURN),
                chip(game.mode().equals(GameMode.REVERSI) ? "Legal " + boardGame.validMoves().size() : stateText(game),
                    game.isOver() ? WARNING : HIGHLIGHT)
            );
        }
        return List.of(chip(stateText(game), stateColor(game)));
    }

    private Label chip(String text, Color color) {
        Label label = lbl(text, color, Font.font(FONT, FontWeight.BOLD, 12));
        label.setPadding(new Insets(4, 9, 4, 9));
        label.setStyle(panelStyle(Color.web("#1b1813"), Color.web("#4a3b28"), 4));
        return label;
    }

    private Label sectionLabel(String text) {
        Label label = lbl(text, MUTED, Font.font(FONT, FontWeight.BOLD, 12));
        label.setMaxWidth(Double.MAX_VALUE);
        label.setPadding(new Insets(0, 0, 3, 0));
        return label;
    }

    private Label header(String text) {
        Label label = lbl(text, COORD, Font.font(FONT, FontWeight.BOLD, FS));
        label.setMinWidth(CELL);
        label.setMinHeight(24);
        label.setAlignment(Pos.CENTER);
        return label;
    }

    private Button button(String text, String tip, Runnable action) {
        return button(text, tip, ButtonTone.SECONDARY, action);
    }

    private Button button(String text, String tip, ButtonTone tone, Runnable action) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinHeight(38);
        button.setCursor(Cursor.HAND);
        button.setTooltip(new Tooltip(tip));
        button.setFont(Font.font(FONT, FontWeight.BOLD, FS));
        button.setStyle(buttonStyle(tone));
        button.setOnAction(event -> action.run());
        return button;
    }

    private VBox titledPanel(String title) {
        VBox panel = new VBox(8);
        panel.setBackground(solidBg(PANEL_BG));
        panel.setStyle(panelStyle(PANEL_BG, BORDER, 4));
        panel.setPadding(new Insets(12));
        panel.getChildren().add(sectionLabel(title));
        return panel;
    }

    private ScrollPane scroll(VBox content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;"
            + "-fx-control-inner-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    private static Label lbl(String text, Color color, Font font) {
        Label label = new Label(text);
        label.setTextFill(color);
        label.setFont(font);
        return label;
    }

    private static Region separator() {
        Region region = new Region();
        region.setMinHeight(1);
        region.setMaxHeight(1);
        region.setStyle("-fx-background-color: " + hex(BORDER) + ";");
        return region;
    }

    private static Background solidBg(Color color) {
        return new Background(new BackgroundFill(color, null, null));
    }

    private void clearSelection() {
        chessFrom = null;
        lastClicked = new Position(0, 0);
    }

    private boolean isCurrentChessPiece(char piece, Disc side) {
        if (piece == '.') {
            return false;
        }
        return side == Disc.WHITE ? Character.isLowerCase(piece) : Character.isUpperCase(piece);
    }

    private String resultMsg(GameSession game, ActionResult result) {
        return GameController.resultMessage(game, result);
    }

    private Set<Position> validMovesFor(GameSession game) {
        return GameController.validMovesFor(game);
    }

    private Color statusColor(GameSession game, String line) {
        if (game instanceof ChessSession chess && line.startsWith("To move:")) {
            return chess.current() == Disc.WHITE ? WHITE_TURN : BLACK_TURN;
        }
        if (line.contains("Won")) {
            return SUCCESS;
        }
        if (line.contains("game over") || line.contains("Lost") || line.contains("Finished")) {
            return WARNING;
        }
        if (line.startsWith("Turn:")) {
            return line.contains("White") ? WHITE_TURN : BLACK_TURN;
        }
        return FG;
    }

    private Color stateColor(GameSession game) {
        if (!game.isOver()) {
            return SUCCESS;
        }
        if (game instanceof MinesweeperSession ms && ms.lastMessage().contains("win")) {
            return SUCCESS;
        }
        return WARNING;
    }

    private String stateText(GameSession game) {
        if (game instanceof MinesweeperSession ms && ms.isOver()) {
            return ms.lastMessage().contains("win") ? "Won" : "Lost";
        }
        return game.isOver() ? "Over" : "Live";
    }

    private String turnText(Disc disc) {
        return disc == Disc.BLACK ? "Black to move" : "White to move";
    }

    private String modeName(GameMode mode) {
        return manager.registry().displayName(mode);
    }

    private String displayMode(GameMode mode) {
        String name = modeName(mode);
        if (name == null || name.isBlank()) {
            return "";
        }
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private String pos(Position p) {
        return (p.row() + 1) + String.valueOf((char) ('a' + p.col()));
    }

    private static String buttonStyle(ButtonTone tone) {
        Color background = switch (tone) {
            case PRIMARY -> Color.web("#3a2b1d");
            case DANGER -> Color.web("#33201c");
            case SECONDARY -> SURFACE_2;
        };
        Color border = switch (tone) {
            case PRIMARY -> Color.web("#6b4c2d");
            case DANGER -> DANGER_COLOR;
            case SECONDARY -> BORDER;
        };
        Color text = switch (tone) {
            case DANGER -> Color.web("#ffd5d7");
            default -> FG;
        };
        return panelStyle(background, border, 4)
            + "-fx-text-fill: " + hex(text) + ";"
            + "-fx-padding: 8 12 8 12;";
    }

    private static String panelStyle(Color background, Color border, int radius) {
        return "-fx-background-color: " + hex(background) + ";"
            + "-fx-background-radius: " + radius + ";"
            + "-fx-border-color: " + hex(border) + ";"
            + "-fx-border-radius: " + radius + ";"
            + "-fx-border-width: 1;";
    }

    private static String boardShellStyle(GameSession game) {
        if (game instanceof ChessSession) {
            return panelStyle(Color.web("#24201b"), Color.web("#6f4e2e"), 4)
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 14, 0.18, 0, 5);";
        }
        if (game instanceof MinesweeperSession) {
            return panelStyle(Color.web("#2a2a2a"), Color.web("#8a8a8a"), 3)
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 10, 0.16, 0, 4);";
        }
        return panelStyle(Color.web("#182018"), Color.web("#2f7537"), 4)
            + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.32), 12, 0.18, 0, 4);";
    }

    private static String hex(Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }

    private enum ButtonTone {
        PRIMARY,
        SECONDARY,
        DANGER
    }

}
