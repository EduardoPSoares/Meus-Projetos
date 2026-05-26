package me.ray.midgard.core.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class RegionUtils {

    /**
     * Checks if a location allows a specific WorldGuard flag.
     * @param location The location to check.
     * @param flag The flag to check (e.g., Flags.PVP).
     * @return True if allowed, false otherwise.
     */
    public static boolean isFlagAllowed(Location location, StateFlag flag) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(location));
        
        return set.testState(null, flag);
    }

    /**
     * Checks if a player is inside any WorldGuard region with a specific ID.
     * @param player The player.
     * @param regionId The region ID to search for.
     * @return True if inside, false otherwise.
     */
    public static boolean isInRegion(Player player, String regionId) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));

        for (ProtectedRegion region : set) {
            if (region.getId().equalsIgnoreCase(regionId)) {
                return true;
            }
        }
        return false;
    }
}
