package reversi;

import reversi.gamehall.GameHall;
import reversi.gamehall.UiPlugin;
import reversi.gamehall.UiRegistry;

public final class App {
    public static void main(String[] args) throws Exception {
        int boardSize = normalizeBoardSize(Args.parseBoardSize(args).orElse(8));
        String uiName = Args.parseUiName(args);

        GameHall hall = GameHall.newHall(boardSize);

        UiPlugin ui = UiRegistry.discover(uiName);
        ui.launch(hall);
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
