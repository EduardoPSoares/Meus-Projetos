package me.ray.midgard.core.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class PlaceholderRegistry extends PlaceholderExpansion {

    @SuppressWarnings("unused")
    private final JavaPlugin plugin;
    private final Map<String, BiFunction<OfflinePlayer, String, String>> placeholders = new HashMap<>();

    public PlaceholderRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(String identifier, BiFunction<OfflinePlayer, String, String> handler) {
        placeholders.put(identifier.toLowerCase(), handler);
    }

    @Override
    public @NotNull String getIdentifier() {
        return "midgard";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Ray";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        // Sort by key length descending to match longest prefix first
        return placeholders.entrySet().stream()
                .filter(e -> params.toLowerCase().startsWith(e.getKey()))
                .max(java.util.Comparator.comparingInt(e -> e.getKey().length()))
                .map(e -> {
                    String rest = params.substring(e.getKey().length());
                    if (rest.startsWith("_")) rest = rest.substring(1);
                    return e.getValue().apply(player, rest);
                })
                .orElse(null);
    }
}
