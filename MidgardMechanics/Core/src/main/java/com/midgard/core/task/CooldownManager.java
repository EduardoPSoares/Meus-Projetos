package com.midgard.core.task;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cooldown manager with per-player, per-action cooldowns.
 */
public class CooldownManager {

    private final Map<String, Map<UUID, Long>> cooldowns = new ConcurrentHashMap<>();

    /**
     * Set a cooldown for a player on a specific action.
     *
     * @param action   the action identifier
     * @param uuid     the player UUID
     * @param millis   cooldown duration in milliseconds
     */
    public void setCooldown(String action, UUID uuid, long millis) {
        cooldowns.computeIfAbsent(action, k -> new ConcurrentHashMap<>())
                .put(uuid, System.currentTimeMillis() + millis);
    }

    /**
     * Check if a player is on cooldown for an action.
     */
    public boolean isOnCooldown(String action, UUID uuid) {
        Map<UUID, Long> map = cooldowns.get(action);
        if (map == null) return false;
        Long expiry = map.get(uuid);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            map.remove(uuid);
            return false;
        }
        return true;
    }

    /**
     * Get remaining cooldown time in milliseconds.
     */
    public long getRemaining(String action, UUID uuid) {
        Map<UUID, Long> map = cooldowns.get(action);
        if (map == null) return 0;
        Long expiry = map.get(uuid);
        if (expiry == null) return 0;
        long remaining = expiry - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /**
     * Get remaining cooldown in seconds (rounded up).
     */
    public int getRemainingSeconds(String action, UUID uuid) {
        return (int) Math.ceil(getRemaining(action, uuid) / 1000.0);
    }

    /**
     * Remove a specific cooldown.
     */
    public void removeCooldown(String action, UUID uuid) {
        Map<UUID, Long> map = cooldowns.get(action);
        if (map != null) {
            map.remove(uuid);
        }
    }

    /**
     * Clear all cooldowns for a specific action.
     */
    public void clearAction(String action) {
        cooldowns.remove(action);
    }

    /**
     * Clear all cooldowns.
     */
    public void clearAll() {
        cooldowns.clear();
    }

    /**
     * Purge all expired entries to free memory.
     */
    public void purgeExpired() {
        long now = System.currentTimeMillis();
        cooldowns.values().forEach(map ->
                map.entrySet().removeIf(entry -> now >= entry.getValue())
        );
        cooldowns.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}
