package reversi.gamehall;

public interface UiPlugin {
    String name();

    void launch(GameHall hall) throws Exception;
}
