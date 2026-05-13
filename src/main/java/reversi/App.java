package reversi;

import reversi.gamehall.GameHall;
import reversi.ui.JavaFxUi;
import reversi.ui.LanternaUi;

public final class App {
    public static void main(String[] args) throws Exception {
        int boardSize = normalizeBoardSize(Args.parseBoardSize(args).orElse(8));
        Args.UiType uiType = Args.parseUiType(args);

        GameHall hall = GameHall.newHall(boardSize);

        if (uiType == Args.UiType.LANTERNA) {
            LanternaUi ui = new LanternaUi();
            ui.runMulti(hall.manager());
        } else {
            JavaFxUi.launchApp(boardSize);
        }
    }

    private static int normalizeBoardSize(int requested) {
        int size = requested;
        if (size < 4) {
            size = 8;
        }
        if (size % 2 != 0) {
            size += 1;
        }
        return size;
    }
}
