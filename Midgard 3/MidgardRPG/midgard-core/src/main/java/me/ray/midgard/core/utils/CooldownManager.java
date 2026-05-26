package me.ray.midgard.core.utils;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    /**
     * Sets a cooldown for a player.
     * @param player The player UUID.
     * @param key The unique key for the cooldown (e.g., "spell_fireball").
     * @param duration The duration of the cooldown.
     */
    public void setCooldown(UUID player, String key, Duration duration) {
        cooldowns.computeIfAbsent(player, k -> new ConcurrentHashMap<>())
                 .put(key, System.currentTimeMillis() + duration.toMillis());
    }

    /**
     * Checks if a player is on cooldown.
     * @param player The player UUID.
     * @param key The cooldown key.
     * @return True if on cooldown, false otherwise.
     */
    public boolean isOnCooldown(UUID player, String key) {
        Map<String, Long> playerCooldowns = cooldowns.get(player);
        if (playerCooldowns == null) { return false; }
        
        Long expiry = playerCooldowns.get(key);
        if (expiry == null) { return false; }

        if (System.currentTimeMillis() >= expiry) {
            playerCooldowns.remove(key);
            return false;
        }
        return true;
    }

    /**
     * Gets the remaining time in milliseconds.
     */
    public long getRemainingMillis(UUID player, String key) {
        Map<String, Long> playerCooldowns = cooldowns.get(player);
        if (playerCooldowns == null) { return 0; }

        Long expiry = playerCooldowns.get(key);
        if (expiry == null) { return 0; }

        long remaining = expiry - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /**
     * Gets the remaining time formatted as seconds (e.g., "3.5s").
     */
    public String getRemainingFormatted(UUID player, String key) {
        double seconds = getRemainingMillis(player, key) / 1000.0;
        return String.format("%.1fs", seconds);
    }

    /**
     * Removes all cooldowns for a player (call on quit to prevent memory leak).
     * @param player The player UUID to clean up.
     */
    public void cleanupPlayer(UUID player) {
        cooldowns.remove(player);
    }
}
