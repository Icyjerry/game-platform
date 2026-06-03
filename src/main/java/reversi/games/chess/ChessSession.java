package reversi.games.chess;

import reversi.core.GameSession;
import reversi.core.model.*;

import java.util.Map;

public interface ChessSession extends GameSession {
    Disc current();

    ActionResult tryMove(Position from, Position to, Character promotionPiece);

    char pieceAt(Position p);

    String lastMessage();

    Disc winner();

    int moveCount();

    String resultSummary();

    Map<Disc, Map<Character, Integer>> pieceDetails();
}
