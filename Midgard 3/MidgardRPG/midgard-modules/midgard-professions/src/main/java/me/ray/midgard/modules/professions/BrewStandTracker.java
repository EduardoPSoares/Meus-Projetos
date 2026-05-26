package me.ray.midgard.modules.professions;

import org.bukkit.Location;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rastreia o último jogador que abriu cada brewing stand.
 * Compartilhado entre ProfessionXpListener e PenaltyBrewListener
 * para evitar maps duplicados e listeners redundantes de InventoryOpen.
 */
public final class BrewStandTracker {

    private final Map<Location, UUID> lastUser = new ConcurrentHashMap<>();

    public void track(Location location, UUID playerId) {
        lastUser.put(location, playerId);
    }

    public UUID getLastUser(Location location) {
        return lastUser.get(location);
    }

    public void clear() {
        lastUser.clear();
    }
}
