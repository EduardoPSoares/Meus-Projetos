package me.ray.midgard.core.region;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Set;

public interface RegionProvider {

    /**
     * Gets all region IDs at the specified location.
     */
    Set<String> getRegions(Location location);

    /**
     * Checks if the location is in a specific region.
     */
    default boolean isInRegion(Location location, String regionId) {
        return getRegions(location).contains(regionId);
    }
    
    /**
     * Checks if the player is in a specific region.
     */
    default boolean isInRegion(Player player, String regionId) {
        return isInRegion(player.getLocation(), regionId);
    }
}
