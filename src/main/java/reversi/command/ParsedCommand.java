package reversi.command;

import reversi.core.model.GameMode;
import reversi.core.model.Position;

public record ParsedCommand(
    CommandType type,
    Position position,
    Position from,
    Position to,
    Character promotionPiece,
    Integer boardIndex,
    GameMode gameMode,
    String rawInput
) {
    public static ParsedCommand invalid(String rawInput) {
        return new ParsedCommand(CommandType.INVALID, null, null, null, null, null, null, rawInput);
    }

    public static ParsedCommand move(Position position, String rawInput) {
        return new ParsedCommand(CommandType.MOVE, position, null, null, null, null, null, rawInput);
    }

    public static ParsedCommand move(Position from, Position to, Character promotionPiece, String rawInput) {
        return new ParsedCommand(CommandType.MOVE, null, from, to, promotionPiece, null, null, rawInput);
    }

    public static ParsedCommand flag(Position position, String rawInput) {
        return new ParsedCommand(CommandType.FLAG, position, null, null, null, null, null, rawInput);
    }

    public static ParsedCommand switchBoard(int boardIndex, String rawInput) {
        return new ParsedCommand(CommandType.SWITCH, null, null, null, null, boardIndex, null, rawInput);
    }

    public static ParsedCommand newGame(GameMode gameMode, String rawInput) {
        return new ParsedCommand(CommandType.NEW_GAME, null, null, null, null, null, gameMode, rawInput);
    }

    public static ParsedCommand demo(String rawInput) {
        return new ParsedCommand(CommandType.DEMO, null, null, null, null, null, null, rawInput);
    }

    public static ParsedCommand stop(String rawInput) {
        return new ParsedCommand(CommandType.STOP, null, null, null, null, null, null, rawInput);
    }

    public static ParsedCommand pass(String rawInput) {
        return new ParsedCommand(CommandType.PASS, null, null, null, null, null, null, rawInput);
    }

    public static ParsedCommand quit(String rawInput) {
        return new ParsedCommand(CommandType.QUIT, null, null, null, null, null, null, rawInput);
    }
}
