package reversi.command;

import reversi.core.model.GameMode;
import reversi.core.model.Position;
import reversi.gamehall.GameRegistry;

import java.util.Locale;
import java.util.Optional;

public final class CommandParser {
    private CommandParser() {
    }

    public static ParsedCommand parse(String input) {
        return parse(input, GameRegistry.defaultRegistry());
    }

    public static ParsedCommand parse(String input, GameRegistry registry) {
        if (input == null) {
            return ParsedCommand.invalid("");
        }

        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return ParsedCommand.invalid(input);
        }

        String normalized = trimmed.toLowerCase(Locale.ROOT);
        if (normalized.equals("quit")) {
            return ParsedCommand.quit(input);
        }
        if (normalized.equals("demo")) {
            return ParsedCommand.demo(input);
        }
        if (normalized.equals("stop")) {
            return ParsedCommand.stop(input);
        }
        if (normalized.equals("pass")) {
            return ParsedCommand.pass(input);
        }
        Optional<GameMode> mode = registry.findByCommand(normalized)
            .map(plugin -> plugin.mode());
        if (mode.isPresent()) {
            return ParsedCommand.newGame(mode.get(), input);
        }

        if (normalized.startsWith("switch ") || normalized.startsWith("s ")) {
            String numberPart = trimmed.substring(trimmed.indexOf(' ') + 1).trim();
            return parseBoardSwitch(numberPart, input);
        }
        if (normalized.startsWith("move ") || normalized.startsWith("m ")) {
            String movePart = trimmed.substring(trimmed.indexOf(' ') + 1).trim();
            return parseChessMove(movePart, input);
        }
        if (normalized.startsWith("flag ") || normalized.startsWith("f ")) {
            String positionPart = trimmed.substring(trimmed.indexOf(' ') + 1).trim();
            Optional<Position> position = Position.parse(positionPart);
            return position.map(value -> ParsedCommand.flag(value, input)).orElseGet(() -> ParsedCommand.invalid(input));
        }

        ParsedCommand numberSwitch = parseBoardSwitch(trimmed, input);
        if (numberSwitch.type() == CommandType.SWITCH) {
            return numberSwitch;
        }

        Optional<Position> position = Position.parse(trimmed);
        return position.map(value -> ParsedCommand.move(value, input)).orElseGet(() -> ParsedCommand.invalid(input));
    }

    private static ParsedCommand parseBoardSwitch(String value, String rawInput) {
        try {
            int boardNumber = Integer.parseInt(value);
            if (boardNumber <= 0) {
                return ParsedCommand.invalid(rawInput);
            }
            return ParsedCommand.switchBoard(boardNumber, rawInput);
        } catch (NumberFormatException ignored) {
            return ParsedCommand.invalid(rawInput);
        }
    }

    private static ParsedCommand parseChessMove(String value, String rawInput) {
        String[] parts = value.trim().split("\\s+");
        if (parts.length == 1) {
            Optional<Position> position = Position.parse(parts[0]);
            return position.map(pos -> ParsedCommand.move(pos, rawInput)).orElseGet(() -> ParsedCommand.invalid(rawInput));
        }
        if (parts.length < 2 || parts.length > 3) {
            return ParsedCommand.invalid(rawInput);
        }

        Optional<Position> from = Position.parse(parts[0]);
        Optional<Position> to = Position.parse(parts[1]);
        if (from.isEmpty() || to.isEmpty()) {
            return ParsedCommand.invalid(rawInput);
        }

        Character promotion = null;
        if (parts.length == 3) {
            String piece = parts[2].trim();
            if (piece.length() != 1) {
                return ParsedCommand.invalid(rawInput);
            }
            promotion = Character.toLowerCase(piece.charAt(0));
        }
        return ParsedCommand.move(from.get(), to.get(), promotion, rawInput);
    }
}
