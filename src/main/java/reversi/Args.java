package reversi;

import java.util.Optional;

public final class Args {
    private Args() {
    }

    public static Optional<Integer> parseBoardSize(String[] args) {
        if (args == null || args.length == 0) {
            return Optional.empty();
        }

        for (String arg : args) {
            if (arg == null) {
                continue;
            }

            String trimmed = arg.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (trimmed.startsWith("--size=")) {
                return parsePositiveInt(trimmed.substring("--size=".length()));
            }

            if (trimmed.equals("--size")) {
                continue;
            }

            if (!trimmed.startsWith("--")) {
                Optional<Integer> parsed = parsePositiveInt(trimmed);
                if (parsed.isPresent()) {
                    return parsed;
                }
            }
        }

        for (int i = 0; i < args.length - 1; i++) {
            if ("--size".equals(args[i])) {
                return parsePositiveInt(args[i + 1]);
            }
        }

        return Optional.empty();
    }

    public static String parseUiName(String[] args) {
        if (args == null || args.length == 0) {
            return "javafx";
        }

        for (String arg : args) {
            if (arg == null) {
                continue;
            }

            String trimmed = arg.trim().toLowerCase();
            if (trimmed.startsWith("--ui=")) {
                return trimmed.substring("--ui=".length());
            }
        }

        for (int i = 0; i < args.length - 1; i++) {
            if ("--ui".equals(args[i].trim().toLowerCase())) {
                return args[i + 1].trim().toLowerCase();
            }
        }

        return "javafx";
    }

    private static Optional<Integer> parsePositiveInt(String s) {
        if (s == null) {
            return Optional.empty();
        }
        try {
            int value = Integer.parseInt(s.trim());
            if (value <= 0) {
                return Optional.empty();
            }
            return Optional.of(value);
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }
}
