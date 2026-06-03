package reversi.core.model;

import java.util.Locale;
import java.util.Optional;

public record Position(int row, int col) {
    public static Optional<Position> parse(String input) {
        if (input == null) {
            return Optional.empty();
        }

        String s = input.trim();
        if (s.isEmpty()) {
            return Optional.empty();
        }

        s = s.toUpperCase(Locale.ROOT);
        Optional<Position> letterFirst = parseLetterFirst(s);
        if (letterFirst.isPresent()) {
            return letterFirst;
        }
        return parseDigitFirst(s);
    }

    private static Optional<Position> parseLetterFirst(String s) {
        int split = 0;
        while (split < s.length() && s.charAt(split) >= 'A' && s.charAt(split) <= 'Z') {
            split++;
        }
        if (split == 0 || split == s.length()) {
            return Optional.empty();
        }

        String colPart = s.substring(0, split);
        String rowPart = s.substring(split);
        return toPosition(rowPart, colPart);
    }

    private static Optional<Position> parseDigitFirst(String s) {
        int split = 0;
        while (split < s.length() && Character.isDigit(s.charAt(split))) {
            split++;
        }
        if (split == 0 || split == s.length()) {
            return Optional.empty();
        }
        String rowPart = s.substring(0, split);
        String colPart = s.substring(split);
        return toPosition(rowPart, colPart);
    }

    private static Optional<Position> toPosition(String rowPart, String colPart) {
        int col = excelLettersToIndex(colPart);
        if (col < 0) {
            return Optional.empty();
        }

        int row1Based;
        try {
            row1Based = Integer.parseInt(rowPart);
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
        if (row1Based <= 0) {
            return Optional.empty();
        }

        return Optional.of(new Position(row1Based - 1, col));
    }

    private static int excelLettersToIndex(String letters) {
        int value = 0;
        for (int i = 0; i < letters.length(); i++) {
            char ch = letters.charAt(i);
            if (ch < 'A' || ch > 'Z') {
                return -1;
            }
            int digit = (ch - 'A') + 1;
            value = value * 26 + digit;
        }
        return value - 1;
    }
}
