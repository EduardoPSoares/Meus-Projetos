package com.midgard.fooddecay;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * Soft-dependency hook for RealisticSeasons plugin.
 * Uses reflection to avoid compile-time dependency on the premium plugin.
 * Gracefully returns defaults when RealisticSeasons is not installed.
 */
public class SeasonHook {

    private boolean available;
    private Object seasonsAPI;
    private Method getSeasonMethod;
    private Method getTemperatureMethod;
    private Method getAirTemperatureMethod;

    public SeasonHook() {
        try {
            Plugin rsPlugin = Bukkit.getPluginManager().getPlugin("RealisticSeasons");
            if (rsPlugin != null && rsPlugin.isEnabled()) {
                Class<?> apiClass = Class.forName("me.casperge.realisticseasons.api.SeasonsAPI");
                Method getInstance = apiClass.getMethod("getInstance");
                this.seasonsAPI = getInstance.invoke(null);

                this.getSeasonMethod = apiClass.getMethod("getSeason", World.class);
                this.getTemperatureMethod = apiClass.getMethod("getTemperature", Player.class);
                this.getAirTemperatureMethod = apiClass.getMethod("getAirTemperature", Location.class);

                this.available = true;
                Bukkit.getLogger().info("[FoodDecay] RealisticSeasons detected — temperature & season integration enabled.");
            } else {
                this.available = false;
                Bukkit.getLogger().info("[FoodDecay] RealisticSeasons not found — using default decay rates.");
            }
        } catch (Exception e) {
            this.available = false;
            Bukkit.getLogger().warning("[FoodDecay] Failed to hook into RealisticSeasons: " + e.getMessage());
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Gets the current season name for a world.
     * Returns "SPRING", "SUMMER", "FALL", or "WINTER".
     * Returns null if RealisticSeasons is not available.
     */
    public String getSeason(World world) {
        if (!available || seasonsAPI == null || getSeasonMethod == null) return null;
        try {
            Object season = getSeasonMethod.invoke(seasonsAPI, world);
            return season != null ? season.toString() : null;
        } catch (Exception e) {
            Bukkit.getLogger().fine("[FoodDecay] SeasonHook.getSeason failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets the temperature felt by a player (includes biome, altitude, etc.).
     * Returns null if RealisticSeasons is not available.
     */
    public Integer getTemperature(Player player) {
        if (!available || seasonsAPI == null || getTemperatureMethod == null) return null;
        try {
            Object result = getTemperatureMethod.invoke(seasonsAPI, player);
            return result instanceof Number n ? n.intValue() : null;
        } catch (Exception e) {
            Bukkit.getLogger().fine("[FoodDecay] SeasonHook.getTemperature failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets the air temperature at a specific location.
     * Returns null if RealisticSeasons is not available.
     */
    public Integer getAirTemperature(Location location) {
        if (!available || seasonsAPI == null || getAirTemperatureMethod == null) return null;
        try {
            Object result = getAirTemperatureMethod.invoke(seasonsAPI, location);
            return result instanceof Number n ? n.intValue() : null;
        } catch (Exception e) {
            Bukkit.getLogger().fine("[FoodDecay] SeasonHook.getAirTemperature failed: " + e.getMessage());
            return null;
        }
    }
}
