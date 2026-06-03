package reversi.core;

import reversi.core.model.*;

public interface PassableSession extends GameSession {
    ActionResult tryPass();
}
