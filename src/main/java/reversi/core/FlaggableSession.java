package reversi.core;

import reversi.core.model.*;

public interface FlaggableSession extends GameSession {
    ActionResult tryFlag(Position p);
}
