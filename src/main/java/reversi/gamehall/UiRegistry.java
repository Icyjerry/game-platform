package reversi.gamehall;

import java.util.List;
import java.util.ServiceLoader;

public final class UiRegistry {
    private UiRegistry() {
    }

    public static UiPlugin discover(String name) {
        List<UiPlugin> plugins = ServiceLoader.load(UiPlugin.class)
            .stream()
            .map(ServiceLoader.Provider::get)
            .toList();
        if (plugins.isEmpty()) {
            throw new IllegalStateException(
                "No UiPlugin found. Ensure a UI implementation is on the classpath.");
        }
        if (name == null || name.isBlank()) {
            return plugins.get(0);
        }
        String requested = name.trim();
        return plugins.stream()
            .filter(p -> p.name().equalsIgnoreCase(requested))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Unknown UI '" + requested + "'. Available: "
                    + String.join(", ", plugins.stream().map(UiPlugin::name).toList())));
    }

}
