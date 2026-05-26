package com.midgard.fooddecay;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Calculates environmental decay multipliers based on temperature and season.
 * Integrates with RealisticSeasons when available, falls back to Minecraft biome temperature.
 * Caches per-player results for a configurable TTL to avoid redundant biome/season lookups
 * when processing multiple items in the same inventory.
 */
public class EnvironmentManager {

    private final FoodDecayConfig config;
    private final SeasonHook seasonHook;

    /** Cached multiplier per player UUID, with expiry timestamp. */
    private record CachedMultiplier(double value, long expiresAt) {}
    private final Map<UUID, CachedMultiplier> playerMultiplierCache = new ConcurrentHashMap<>();
    /** Cache TTL — 3 seconds. Environment doesn't change faster than this. */
    private static final long CACHE_TTL_MS = 3000L;

    public EnvironmentManager(FoodDecayConfig config, SeasonHook seasonHook) {
        this.config = config;
        this.seasonHook = seasonHook;
    }

    /**
     * Gets the combined environmental decay multiplier for a player's location.
     * Results are cached per player for CACHE_TTL_MS to avoid redundant lookups.
     */
    public double getEnvironmentMultiplier(Player player) {
        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();
        CachedMultiplier cached = playerMultiplierCache.get(uuid);
        if (cached != null && now < cached.expiresAt) {
            return cached.value;
        }
        double tempMultiplier = getTemperatureMultiplier(player);
        double seasonMultiplier = getSeasonMultiplier(player.getWorld());
        double result = tempMultiplier * seasonMultiplier;
        playerMultiplierCache.put(uuid, new CachedMultiplier(result, now + CACHE_TTL_MS));
        return result;
    }

    /**
     * Evicts expired entries. Call periodically (e.g. every minute) to prevent memory leaks.
     */
    public void cleanupCache() {
        long now = System.currentTimeMillis();
        playerMultiplierCache.entrySet().removeIf(e -> now >= e.getValue().expiresAt);
    }

    /**
     * Gets the combined environmental decay multiplier for a location.
     * Used for items in containers (no player context).
     */
    public double getEnvironmentMultiplier(Location location) {
        double tempMultiplier = getTemperatureMultiplier(location);
        World world = location.getWorld();
        double seasonMultiplier = world != null ? getSeasonMultiplier(world) : 1.0;
        return tempMultiplier * seasonMultiplier;
    }

    /**
     * Gets the temperature-based decay multiplier for a player.
     * Applies depth offset if depth-temperature is enabled.
     */
    public double getTemperatureMultiplier(Player player) {
        if (!config.isTemperatureEnabled()) return 1.0;

        Location loc = player.getLocation();
        Integer temp = seasonHook.getTemperature(player);
        if (temp == null) {
            return getBiomeTemperatureMultiplier(loc);
        }
        int effective = temp + config.getDepthTemperatureOffset(loc.getBlockY());
        return getMultiplierForTemperature(effective);
    }

    /**
     * Gets the temperature-based decay multiplier for a location.
     * Applies depth offset if depth-temperature is enabled.
     */
    public double getTemperatureMultiplier(Location location) {
        if (!config.isTemperatureEnabled()) return 1.0;

        Integer temp = seasonHook.getAirTemperature(location);
        if (temp == null) {
            return getBiomeTemperatureMultiplier(location);
        }
        int effective = temp + config.getDepthTemperatureOffset(location.getBlockY());
        return getMultiplierForTemperature(effective);
    }

    /**
     * Gets the season-based decay multiplier for a world.
     */
    public double getSeasonMultiplier(World world) {
        if (!config.isSeasonEnabled()) return 1.0;

        String season = seasonHook.getSeason(world);
        if (season == null) return 1.0;

        return config.getSeasonMultiplier(season);
    }

    /**
     * Fallback: estimate temperature multiplier from Minecraft biome temperature.
     * Biome temperature ranges roughly from -0.5 (snowy) to 2.0 (desert).
     * Depth offset is applied here as well.
     */
    private double getBiomeTemperatureMultiplier(Location location) {
        double biomeTemp = location.getBlock().getTemperature();
        // Map biome temperature to approximate Celsius: -0.5 → -10°C, 0.5 → 10°C, 1.0 → 25°C, 2.0 → 45°C
        int approxCelsius = (int) (biomeTemp * 25.0 - 2.5);
        int effective = approxCelsius + config.getDepthTemperatureOffset(location.getBlockY());
        return getMultiplierForTemperature(effective);
    }

    /**
     * Maps a temperature (in Celsius) to a decay multiplier using configured temperature zones.
     */
    private double getMultiplierForTemperature(int temperature) {
        return config.getTemperatureMultiplier(temperature);
    }

    /**
     * Gets the trait-based decay multiplier (product of all trait multipliers).
     */
    public double getTraitMultiplier(Set<FoodTrait> traits) {
        if (traits == null || traits.isEmpty()) return 1.0;
        double multiplier = 1.0;
        for (FoodTrait trait : traits) {
            multiplier *= config.getTraitMultiplier(trait);
        }
        return multiplier;
    }

    /**
     * Calculates the total decay multiplier combining environment + traits.
     */
    public double getTotalMultiplier(Player player, Set<FoodTrait> traits) {
        double env = getEnvironmentMultiplier(player);
        double trait = getTraitMultiplier(traits);
        return env * trait;
    }

    /**
     * Calculates the total decay multiplier for a location + traits.
     */
    public double getTotalMultiplier(Location location, Set<FoodTrait> traits) {
        double env = getEnvironmentMultiplier(location);
        double trait = getTraitMultiplier(traits);
        return env * trait;
    }

    public SeasonHook getSeasonHook() {
        return seasonHook;
    }
}
