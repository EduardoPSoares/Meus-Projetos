package com.midgard.fooddecay;

import com.midgard.core.MidgardCore;
import com.midgard.core.utils.MessageUtils;
import static com.midgard.core.utils.MessageUtils.sc;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * TFC-style dynamic food decay manager.
 * Instead of a fixed expiration timestamp, decay accumulates progressively
 * based on environmental conditions (temperature, season) and preservation traits.
 *
 * PDC Keys:
 * - food_timestamp (LONG): when the food was stamped
 * - food_base_minutes (LONG): base decay time in minutes
 * - food_decay_progress (DOUBLE): accumulated decay 0.0–1.0+
 * - food_last_update (LONG): when decay was last recalculated
 * - food_traits (STRING): comma-separated trait names
 */
public class FoodDecayManager {

    private static final String LORE_MARKER = "\u00A78\u00A7m\u00A7r";
    private static final String TRAIT_LORE_MARKER = "\u00A78\u00A7o\u00A7r";

    private final FoodDecayConfig config;
    private final EnvironmentManager environmentManager;

    private final NamespacedKey timestampKey;
    private final NamespacedKey baseMinutesKey;
    private final NamespacedKey decayProgressKey;
    private final NamespacedKey lastUpdateKey;
    private final NamespacedKey traitsKey;
    private final NamespacedKey spoiledModelKey;
    private final NamespacedKey spoiledNameKey;
    private final NamespacedKey portionsTotalKey;
    private final NamespacedKey portionsRemainingKey;
    private final NamespacedKey iceRemovedAtKey;
    private final NamespacedKey iceStoredMultKey;
    private final NamespacedKey iceStartedAtKey;
    private final NamespacedKey iceStartMultKey;
    private final NamespacedKey loreHashKey;

    private int decayTaskId = -1;

    public FoodDecayManager(FoodDecayModule module, EnvironmentManager environmentManager) {
        this.config = module.getDecayConfig();
        this.environmentManager = environmentManager;

        MidgardCore core = MidgardCore.getInstance();
        this.timestampKey = new NamespacedKey(core, "food_timestamp");
        this.baseMinutesKey = new NamespacedKey(core, "food_base_minutes");
        this.decayProgressKey = new NamespacedKey(core, "food_decay_progress");
        this.lastUpdateKey = new NamespacedKey(core, "food_last_update");
        this.traitsKey = new NamespacedKey(core, "food_traits");
        this.spoiledModelKey = new NamespacedKey(core, "food_spoiled_model");
        this.spoiledNameKey = new NamespacedKey(core, "food_spoiled_name");
        this.portionsTotalKey = new NamespacedKey(core, "food_portions_total");
        this.portionsRemainingKey = new NamespacedKey(core, "food_portions_remaining");
        this.iceRemovedAtKey = new NamespacedKey(core, "food_ice_removed_at");
        this.iceStoredMultKey = new NamespacedKey(core, "food_ice_stored_mult");
        this.iceStartedAtKey = new NamespacedKey(core, "food_ice_started_at");
        this.iceStartMultKey = new NamespacedKey(core, "food_ice_start_mult");
        this.loreHashKey = new NamespacedKey(core, "food_lore_hash");
    }

    // =====================================================
    // Stamping
    // =====================================================

    /**
     * Stamps a food item with decay data if tracked and not already stamped.
     */
    public boolean stampItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        if (!config.isTracked(item.getType())) return false;
        if (config.neverExpires(item.getType())) return false;
        if (isStamped(item)) return false;

        long baseMinutes = config.getBaseExpirationMinutes(item.getType());
        long now = System.currentTimeMillis();

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(timestampKey, PersistentDataType.LONG, now);
        pdc.set(baseMinutesKey, PersistentDataType.LONG, baseMinutes);
        pdc.set(decayProgressKey, PersistentDataType.DOUBLE, 0.0);
        pdc.set(lastUpdateKey, PersistentDataType.LONG, now);

        // Portions system
        if (config.isPortionsEnabled()) {
            int portions = config.getPortions(item.getType());
            if (portions > 1) {
                pdc.set(portionsTotalKey, PersistentDataType.INTEGER, portions);
                pdc.set(portionsRemainingKey, PersistentDataType.INTEGER, portions);
            }
        }

        if (config.showLoreTimer()) {
            updateLore(meta, 0.0, baseMinutes);
        }

        item.setItemMeta(meta);
        return true;
    }

    public boolean isStamped(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(decayProgressKey, PersistentDataType.DOUBLE);
    }

    // =====================================================
    // Dynamic Decay Calculation
    // =====================================================

    /**
     * Updates the decay progress on an item for a player (uses player's environment).
     * Returns the updated decay progress (0.0 = fresh, 1.0+ = expired).
     */
    public double updateDecay(ItemStack item, Player player) {
        if (item == null || !item.hasItemMeta()) return 0.0;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        Long lastUpdate = pdc.get(lastUpdateKey, PersistentDataType.LONG);
        Long baseMinutes = pdc.get(baseMinutesKey, PersistentDataType.LONG);
        Double progress = pdc.get(decayProgressKey, PersistentDataType.DOUBLE);

        if (lastUpdate == null || baseMinutes == null || progress == null) return 0.0;

        long now = System.currentTimeMillis();
        long elapsed = now - lastUpdate;
        if (elapsed <= 0) return progress;

        double baseDurationMs = baseMinutes * 60_000.0;
        if (baseDurationMs <= 0) return progress;

        Set<FoodTrait> traits = getTraits(item);
        double envMultiplier = environmentManager.getTotalMultiplier(player, traits);

        WarmingResult warming = computeResidualFreezing(pdc, now);
        envMultiplier *= warming.residualMult();

        double decayIncrement = (elapsed / baseDurationMs) * envMultiplier;
        double newProgress = progress + decayIncrement;

        pdc.set(decayProgressKey, PersistentDataType.DOUBLE, newProgress);
        pdc.set(lastUpdateKey, PersistentDataType.LONG, now);

        if (config.showLoreTimer()) {
            updateLore(meta, newProgress, baseMinutes, warming.warmingInfo(), envMultiplier);
        }

        if (newProgress >= 1.0 && progress < 1.0) {
            applySpoiledModel(meta, pdc);
        }

        applyModelStages(meta, pdc, item.getType(), newProgress, warming.freezeLevel());
        item.setItemMeta(meta);
        applyDecayByWeight(item, progress, newProgress);

        return newProgress;
    }

    private record WarmingResult(double residualMult, double freezeLevel, String warmingInfo) {}

    private WarmingResult computeResidualFreezing(PersistentDataContainer pdc, long now) {
        Long iceRemovedAt = pdc.get(iceRemovedAtKey, PersistentDataType.LONG);
        Double iceStoredMult = pdc.get(iceStoredMultKey, PersistentDataType.DOUBLE);

        if (iceRemovedAt == null || iceStoredMult == null || !config.isIceConservationEnabled()) {
            return new WarmingResult(1.0, 0.0, null);
        }

        pdc.remove(iceStartedAtKey);
        pdc.remove(iceStartMultKey);

        long warmingDurationMs = config.getIceWarmingDuration() * 60_000L;
        long timeSinceRemoval = now - iceRemovedAt;

        if (warmingDurationMs <= 0 || timeSinceRemoval >= warmingDurationMs) {
            pdc.remove(iceRemovedAtKey);
            pdc.remove(iceStoredMultKey);
            return new WarmingResult(1.0, 0.0, null);
        }

        double warmingProgress = (double) timeSinceRemoval / warmingDurationMs;
        double residualMult = iceStoredMult + (1.0 - iceStoredMult) * warmingProgress;
        double freezeLevel = computeFreezeLevel(residualMult);
        long warmingRemainingMs = warmingDurationMs - timeSinceRemoval;
        int residualPercent = (int) ((1.0 - residualMult) * 100);
        String warmingInfo = "  &b\u2744 &e" + sc(config.msg("lore-warming-label"))
                + " &7(-" + residualPercent + "%) &8\u00B7 &7" + formatTime(warmingRemainingMs);

        return new WarmingResult(residualMult, freezeLevel, warmingInfo);
    }

    /**
     * Updates the decay progress on an item using a specific location and container type.
     * Uses the container's location for environment calculations (depth, biome, etc.)
     * and applies the container type multiplier (BARREL=0.7, CHEST=0.9, etc.).
     */
    public double updateDecay(ItemStack item, Location location, Material containerType) {
        if (item == null || !item.hasItemMeta()) return 0.0;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        Long lastUpdate = pdc.get(lastUpdateKey, PersistentDataType.LONG);
        Long baseMinutes = pdc.get(baseMinutesKey, PersistentDataType.LONG);
        Double progress = pdc.get(decayProgressKey, PersistentDataType.DOUBLE);

        if (lastUpdate == null || baseMinutes == null || progress == null) return 0.0;

        long now = System.currentTimeMillis();
        long elapsed = now - lastUpdate;
        if (elapsed <= 0) return progress;

        double baseDurationMs = baseMinutes * 60_000.0;
        if (baseDurationMs <= 0) return progress;

        Set<FoodTrait> traits = getTraits(item);
        double envMultiplier = environmentManager.getTotalMultiplier(location, traits);

        double containerMult = 1.0;
        if (config.isContainerModifiersEnabled() && containerType != null) {
            containerMult = config.getContainerMultiplier(containerType);
            envMultiplier *= containerMult;
        }

        int adjacentIce = countAdjacentIce(location);
        IceResult ice = computeIceConservation(pdc, adjacentIce, now);
        envMultiplier *= ice.multiplier();

        double decayIncrement = (elapsed / baseDurationMs) * envMultiplier;
        double newProgress = progress + decayIncrement;

        pdc.set(decayProgressKey, PersistentDataType.DOUBLE, newProgress);
        pdc.set(lastUpdateKey, PersistentDataType.LONG, now);

        if (config.showLoreTimer()) {
            String storageInfo = buildStorageInfo(location, containerType, containerMult,
                    adjacentIce, ice.multiplier(), ice.freezingInProgress(), ice.freezingRemainingMs());
            updateLore(meta, newProgress, baseMinutes, storageInfo, envMultiplier);
        }

        if (newProgress >= 1.0 && progress < 1.0) {
            applySpoiledModel(meta, pdc);
        }

        double freezeLevel = computeFreezeLevel(ice.multiplier());
        applyModelStages(meta, pdc, item.getType(), newProgress, freezeLevel);
        item.setItemMeta(meta);
        applyDecayByWeight(item, progress, newProgress);

        return newProgress;
    }

    private record IceResult(double multiplier, boolean freezingInProgress, long freezingRemainingMs) {}

    private IceResult computeIceConservation(PersistentDataContainer pdc, int adjacentIce, long now) {
        if (adjacentIce <= 0 || !config.isIceConservationEnabled()) {
            if (pdc.has(iceStartedAtKey)) {
                pdc.remove(iceStartedAtKey);
                pdc.remove(iceStartMultKey);
            }
            return new IceResult(1.0, false, 0);
        }

        double iceStrength = Math.min(1.0, adjacentIce / 6.0);
        double targetIceMult = 1.0 - (1.0 - config.getIceMultiplier()) * iceStrength;

        Long iceStartedAt = pdc.get(iceStartedAtKey, PersistentDataType.LONG);
        Double iceStartMult = pdc.get(iceStartMultKey, PersistentDataType.DOUBLE);

        if (iceStartedAt == null) {
            double startMult = computeIceStartMult(pdc, now);
            iceStartedAt = now;
            iceStartMult = startMult;
            pdc.set(iceStartedAtKey, PersistentDataType.LONG, iceStartedAt);
            pdc.set(iceStartMultKey, PersistentDataType.DOUBLE, iceStartMult);
        }

        double iceMult;
        boolean freezingInProgress = false;
        long freezingRemainingMs = 0;
        long freezeDurMs = config.getIceFreezingDuration() * 60_000L;

        if (freezeDurMs > 0) {
            double freezeProgress = Math.min(1.0, (double) (now - iceStartedAt) / freezeDurMs);
            iceMult = iceStartMult - (iceStartMult - targetIceMult) * freezeProgress;
            freezingInProgress = freezeProgress < 1.0;
            if (freezingInProgress) {
                freezingRemainingMs = freezeDurMs - (now - iceStartedAt);
            }
        } else {
            iceMult = targetIceMult;
        }

        pdc.set(iceRemovedAtKey, PersistentDataType.LONG, now);
        pdc.set(iceStoredMultKey, PersistentDataType.DOUBLE, iceMult);

        return new IceResult(iceMult, freezingInProgress, freezingRemainingMs);
    }

    private double computeIceStartMult(PersistentDataContainer pdc, long now) {
        Double residualMult = pdc.get(iceStoredMultKey, PersistentDataType.DOUBLE);
        Long iceRemovedAt = pdc.get(iceRemovedAtKey, PersistentDataType.LONG);
        if (residualMult != null && iceRemovedAt != null) {
            long warmDurMs = config.getIceWarmingDuration() * 60_000L;
            long sinceRemoval = now - iceRemovedAt;
            if (warmDurMs > 0 && sinceRemoval < warmDurMs) {
                double warmProg = (double) sinceRemoval / warmDurMs;
                double startMult = residualMult + (1.0 - residualMult) * warmProg;
                pdc.remove(iceRemovedAtKey);
                pdc.remove(iceStoredMultKey);
                return startMult;
            }
            pdc.remove(iceRemovedAtKey);
            pdc.remove(iceStoredMultKey);
        }
        return 1.0;
    }

    private void applyDecayByWeight(ItemStack item, double oldProgress, double newProgress) {
        if (!config.isDecayByWeightEnabled() || item.getAmount() <= 1) return;
        double interval = config.getDecayByWeightLossInterval();
        if (interval <= 0) return;
        int previousLosses = (int) (oldProgress / interval);
        int currentLosses = (int) (newProgress / interval);
        int lossCount = currentLosses - previousLosses;
        if (lossCount > 0) {
            item.setAmount(Math.max(1, item.getAmount() - lossCount));
        }
    }

    private static final BlockFace[] ADJACENT_FACES = {
        BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
        BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    };

    /**
     * Counts how many adjacent blocks around the given location are ice blocks.
     */
    private int countAdjacentIce(Location location) {
        if (location == null || location.getWorld() == null) return 0;
        if (!config.isIceConservationEnabled()) return 0;
        Block center = location.getBlock();
        Set<Material> iceTypes = config.getIceBlocks();
        int count = 0;
        for (BlockFace face : ADJACENT_FACES) {
            if (iceTypes.contains(center.getRelative(face).getType())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Builds the storage info line for the lore.
     * Shows container name, depth zone, and conservation percentage.
     */
    private String buildStorageInfo(Location location, Material containerType, double containerMult,
                                     int adjacentIce, double iceMult,
                                     boolean freezingInProgress, long freezingRemainingMs) {
        StringBuilder sb = new StringBuilder();

        // Container name
        if (containerType != null) {
            String name = formatContainerName(containerType);
            sb.append(name);
        }

        // Depth zone
        if (config.isDepthTemperatureEnabled() && location != null) {
            String zoneName = config.getDepthZoneName(location.getBlockY());
            if (zoneName != null) {
                if (sb.length() > 0) sb.append(" &8\u00B7 ");
                int offset = config.getDepthTemperatureOffset(location.getBlockY());
                sb.append("&b").append(zoneName);
                if (offset != 0) {
                    sb.append(" &7(").append(offset > 0 ? "+" : "").append(offset).append("\u00B0C)");
                }
            }
        }

        // Container conservation percentage
        if (containerMult < 1.0) {
            int conservePercent = (int) ((1.0 - containerMult) * 100);
            sb.append(" &8(&a-").append(conservePercent).append("% decay&8)");
        }

        // Ice conservation
        if (adjacentIce > 0 && config.isIceConservationEnabled()) {
            if (sb.length() > 0) sb.append("\n");
            int icePercent = (int) ((1.0 - iceMult) * 100);
            if (freezingInProgress) {
                sb.append("  &b\u2744 &e").append(sc(config.msg("lore-freezing-label")));
                sb.append(" &7(").append(adjacentIce).append("/6)");
                sb.append(" &8(&a-").append(icePercent).append("% decay&8)");
                sb.append(" &8\u00B7 &7").append(formatTime(freezingRemainingMs));
            } else {
                sb.append("  &b\u2744 ").append(sc(config.msg("lore-ice-label")));
                sb.append(" &7(").append(adjacentIce).append("/6)");
                sb.append(" &8(&a-").append(icePercent).append("% decay&8)");
            }
        }

        return sb.toString();
    }

    private String formatContainerName(Material mat) {
        String raw = mat.name().replace("_", " ");
        StringBuilder result = new StringBuilder();
        boolean capitalize = true;
        for (char c : raw.toCharArray()) {
            if (c == ' ') {
                result.append(' ');
                capitalize = true;
            } else {
                result.append(capitalize ? Character.toUpperCase(c) : Character.toLowerCase(c));
                capitalize = false;
            }
        }
        return result.toString();
    }

    // =====================================================
    // Expiration & Freshness
    // =====================================================

    /**
     * Returns true if the food item is expired (decay >= 1.0).
     * Calculates on-the-fly using elapsed time since last PDC persist,
     * so expiration is detected even if the periodic task hasn't persisted yet.
     */
    public boolean isExpired(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        Double progress = pdc.get(decayProgressKey, PersistentDataType.DOUBLE);
        if (progress == null) return false;
        if (progress >= 1.0) return true;

        // Estimate current progress on-the-fly
        Long lastUpdate = pdc.get(lastUpdateKey, PersistentDataType.LONG);
        Long baseMinutes = pdc.get(baseMinutesKey, PersistentDataType.LONG);
        if (lastUpdate == null || baseMinutes == null) return false;
        long elapsed = System.currentTimeMillis() - lastUpdate;
        if (elapsed <= 0) return false;
        double baseDurationMs = baseMinutes * 60_000.0;
        if (baseDurationMs <= 0) return false;
        // Use multiplier of 1.0 (conservative — real multiplier may be lower)
        double estimatedProgress = progress + (elapsed / baseDurationMs);
        return estimatedProgress >= 1.0;
    }

    /**
     * Gets the freshness ratio (1.0 = fresh, 0.0 = expired).
     */
    public double getFreshness(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 1.0;
        Double progress = item.getItemMeta().getPersistentDataContainer()
                .get(decayProgressKey, PersistentDataType.DOUBLE);
        if (progress == null) return 1.0;
        return Math.max(0.0, Math.min(1.0, 1.0 - progress));
    }

    /**
     * Gets estimated remaining time in milliseconds based on current decay rate.
     * Uses a default multiplier of 1.0 if no player context is available.
     */
    public long getEstimatedRemainingMillis(ItemStack item) {
        return getEstimatedRemainingMillis(item, 1.0);
    }

    /**
     * Gets estimated remaining time in milliseconds based on the given decay multiplier.
     */
    public long getEstimatedRemainingMillis(ItemStack item, double currentMultiplier) {
        if (item == null || !item.hasItemMeta()) return -1;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        Long baseMinutes = pdc.get(baseMinutesKey, PersistentDataType.LONG);
        Double progress = pdc.get(decayProgressKey, PersistentDataType.DOUBLE);

        if (baseMinutes == null || progress == null) return -1;
        if (progress >= 1.0) return 0;

        double remaining = 1.0 - progress;
        double baseDurationMs = baseMinutes * 60_000.0;

        if (currentMultiplier <= 0) currentMultiplier = 1.0;

        // Time remaining = remaining fraction * base duration / multiplier
        return (long) (remaining * baseDurationMs / currentMultiplier);
    }

    // =====================================================
    // Trait Management
    // =====================================================

    /**
     * Gets the set of preservation traits on an item.
     */
    public Set<FoodTrait> getTraits(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return EnumSet.noneOf(FoodTrait.class);
        String traitsStr = item.getItemMeta().getPersistentDataContainer()
                .get(traitsKey, PersistentDataType.STRING);
        return parseTraits(traitsStr);
    }

    /**
     * Adds a preservation trait to a food item.
     * Returns true if the trait was successfully added.
     */
    public boolean addTrait(ItemStack item, FoodTrait trait) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        Set<FoodTrait> traits = parseTraits(pdc.get(traitsKey, PersistentDataType.STRING));

        if (traits.contains(trait)) return false;
        if (traits.size() >= config.getMaxTraitsPerItem()) return false;

        traits.add(trait);
        pdc.set(traitsKey, PersistentDataType.STRING, serializeTraits(traits));

        // Update lore to show the trait
        if (config.showLoreTimer()) {
            Double progress = pdc.getOrDefault(decayProgressKey, PersistentDataType.DOUBLE, 0.0);
            Long baseMinutes = pdc.getOrDefault(baseMinutesKey, PersistentDataType.LONG, 60L);
            updateLore(meta, progress, baseMinutes);
        }

        item.setItemMeta(meta);
        return true;
    }

    /**
     * Checks if an item has a specific trait.
     */
    public boolean hasTrait(ItemStack item, FoodTrait trait) {
        return getTraits(item).contains(trait);
    }

    private Set<FoodTrait> parseTraits(String traitsStr) {
        Set<FoodTrait> traits = EnumSet.noneOf(FoodTrait.class);
        if (traitsStr == null || traitsStr.isEmpty()) return traits;
        for (String name : traitsStr.split(",")) {
            FoodTrait trait = FoodTrait.fromString(name.trim());
            if (trait != null) traits.add(trait);
        }
        return traits;
    }

    private String serializeTraits(Set<FoodTrait> traits) {
        if (traits.isEmpty()) return "";
        StringJoiner joiner = new StringJoiner(",");
        for (FoodTrait trait : traits) {
            joiner.add(trait.name());
        }
        return joiner.toString();
    }

    // =====================================================
    // Spoiled Model Management
    // =====================================================

    /**
     * Stores the spoiled custom model data and name on an item.
     * When the item expires, this model data and name will be applied.
     */
    public void setSpoiledModelData(ItemStack item, int customModelData, String spoiledName) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (customModelData > 0) {
            pdc.set(spoiledModelKey, PersistentDataType.INTEGER, customModelData);
        }
        if (spoiledName != null && !spoiledName.isEmpty()) {
            pdc.set(spoiledNameKey, PersistentDataType.STRING, spoiledName);
        }
        item.setItemMeta(meta);
    }

    /**
     * Applies the spoiled model/name to an item when it expires.
     */
    private void applySpoiledModel(ItemMeta meta, PersistentDataContainer pdc) {
        Integer spoiledModel = pdc.get(spoiledModelKey, PersistentDataType.INTEGER);
        if (spoiledModel != null && spoiledModel > 0) {
            meta.setCustomModelData(spoiledModel);
        }
        String spoiledName = pdc.get(spoiledNameKey, PersistentDataType.STRING);
        if (spoiledName != null && !spoiledName.isEmpty()) {
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(spoiledName));
        }
    }

    /**
     * Applies the correct visual model stage based on decay progress and freeze level.
     * Priority: spoiledModel (recipe) > freeze stages > decay stages.
     * freezeLevel: 0.0 = not frozen, 1.0 = fully frozen (relative to max ice effect).
     */
    private void applyModelStages(ItemMeta meta, PersistentDataContainer pdc,
                                   Material material, double decayProgress, double freezeLevel) {
        // Recipe-specific spoiled model takes absolute priority when expired
        if (decayProgress >= 1.0 && pdc.has(spoiledModelKey)) {
            return;
        }

        // Freeze model stages take visual priority (frozen food looks frozen)
        if (freezeLevel > 0) {
            int[] freezeStages = config.getFreezeModelStages(material);
            if (freezeStages != null && freezeStages.length > 0) {
                double clamped = Math.max(0.0, Math.min(freezeLevel, 1.0));
                int index = (clamped >= 1.0)
                        ? freezeStages.length - 1
                        : Math.min((int) (clamped * freezeStages.length), freezeStages.length - 1);
                if (freezeStages[index] > 0) {
                    meta.setCustomModelData(freezeStages[index]);
                    return;
                }
            }
        }

        // Decay model stages
        int[] decayStages = config.getDecayModelStages(material);
        if (decayStages != null && decayStages.length > 0) {
            double clamped = Math.max(0.0, Math.min(decayProgress, 1.0));
            int index = (clamped >= 1.0)
                    ? decayStages.length - 1
                    : Math.min((int) (clamped * decayStages.length), decayStages.length - 1);
            if (decayStages[index] > 0) {
                meta.setCustomModelData(decayStages[index]);
            }
        }
    }

    /**
     * Computes the freeze level (0.0 = unfrozen, 1.0 = fully frozen)
     * normalized relative to the maximum ice effect from config.
     */
    private double computeFreezeLevel(double currentIceMult) {
        double baseIce = config.getIceMultiplier();
        if (baseIce >= 1.0) return 0.0;
        return Math.max(0.0, Math.min((1.0 - currentIceMult) / (1.0 - baseIce), 1.0));
    }

    // =====================================================
    // Lore Management
    // =====================================================

    private void updateLore(ItemMeta meta, double decayProgress, long baseMinutes) {
        updateLore(meta, decayProgress, baseMinutes, null, 1.0);
    }

    private void updateLore(ItemMeta meta, double decayProgress, long baseMinutes, String storageInfo) {
        updateLore(meta, decayProgress, baseMinutes, storageInfo, 1.0);
    }

    private void updateLore(ItemMeta meta, double decayProgress, long baseMinutes, String storageInfo, double decayMultiplier) {
        double freshness = Math.max(0.0, 1.0 - decayProgress);
        int percent = (int) (freshness * 100);

        // Compute a cheap hash of what the lore would look like.
        // If it hasn't changed, skip the entire rebuild to avoid garbage + setItemMeta cost.
        int storageHash = storageInfo != null ? storageInfo.hashCode() : 0;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer portionsRemaining = pdc.get(portionsRemainingKey, PersistentDataType.INTEGER);
        int portionHash = portionsRemaining != null ? portionsRemaining : 0;
        int newHash = (percent * 31 + storageHash) * 31 + portionHash;
        Integer oldHash = pdc.get(loreHashKey, PersistentDataType.INTEGER);
        if (oldHash != null && oldHash == newHash) {
            return; // Lore is identical — skip rebuild
        }
        pdc.set(loreHashKey, PersistentDataType.INTEGER, newHash);

        List<Component> lore = meta.lore();
        if (lore == null) {
            lore = new ArrayList<>();
        } else {
            lore = new ArrayList<>(lore);
        }

        lore.removeIf(c -> {
            String plain = LegacyComponentSerializer.legacySection().serialize(c);
            return plain.contains(LORE_MARKER) || plain.contains(TRAIT_LORE_MARKER);
        });

        String color;
        if (decayProgress >= 1.0) {
            color = config.getExpiredColor();
        } else if (freshness <= config.getWarningThreshold()) {
            color = config.getWarningColor();
        } else {
            color = config.getFreshColor();
        }

        addLore(lore, LORE_MARKER + "&8&m                              ");

        String bar = buildFreshnessBar(freshness, color);
        addLore(lore, LORE_MARKER + "  " + color + sc(config.msg("lore-freshness-label")) + bar + " " + color + percent + "%");

        if (decayProgress >= 1.0) {
            addLore(lore, LORE_MARKER + "  &c\u2620 &c&l" + config.getLoreExpiredStatus());
        } else {
            double effectiveMultiplier = decayMultiplier > 0 ? decayMultiplier : 1.0;
            long estimatedRemainingMs = (long) (freshness * baseMinutes * 60_000.0 / effectiveMultiplier);
            String timeStr = formatTime(estimatedRemainingMs);
            addLore(lore, LORE_MARKER + "  " + config.getLoreFormat()
                    .replace("{color}", color)
                    .replace("{status}", timeStr));
        }

        appendPortionLore(lore, meta.getPersistentDataContainer());
        appendTraitLore(lore, meta.getPersistentDataContainer());
        appendStorageLore(lore, storageInfo);

        addLore(lore, LORE_MARKER + "&8&m                              ");

        meta.lore(lore);
    }

    private void appendPortionLore(List<Component> lore, PersistentDataContainer pdc) {
        if (!config.isPortionsEnabled()) return;
        Integer total = pdc.get(portionsTotalKey, PersistentDataType.INTEGER);
        Integer remaining = pdc.get(portionsRemainingKey, PersistentDataType.INTEGER);
        if (total == null || total <= 1 || remaining == null) return;
        String portionBar = buildPortionBar(remaining, total);
        addLore(lore, LORE_MARKER + "  &f" + sc(config.msg("lore-portions-label"))
                + " " + portionBar + " &f" + remaining + "&7/&f" + total);
    }

    private void appendTraitLore(List<Component> lore, PersistentDataContainer pdc) {
        Set<FoodTrait> traits = parseTraits(pdc.get(traitsKey, PersistentDataType.STRING));
        if (traits.isEmpty()) return;
        addLore(lore, TRAIT_LORE_MARKER + "  &8|");
        addLore(lore, TRAIT_LORE_MARKER + "  " + sc(config.msg("lore-conservation-header")));
        for (FoodTrait trait : traits) {
            String traitDisplay = config.getTraitDisplayName(trait);
            String traitDesc = config.getTraitLoreLine(trait);
            double traitMult = config.getTraitMultiplier(trait);
            int traitPercent = (int) ((1.0 - traitMult) * 100);
            addLore(lore, TRAIT_LORE_MARKER + "  &8|  " + traitDisplay + " " + sc(config.msg("lore-trait-decay").replace("{percent}", String.valueOf(traitPercent))));
            addLore(lore, TRAIT_LORE_MARKER + "  &8|   &8>> " + traitDesc);
        }
    }

    private void appendStorageLore(List<Component> lore, String storageInfo) {
        if (storageInfo == null || storageInfo.isEmpty()) return;
        String[] storageLines = storageInfo.split("\n");
        addLore(lore, LORE_MARKER + "  &7" + sc(config.msg("lore-storage-label")) + " " + storageLines[0]);
        for (int i = 1; i < storageLines.length; i++) {
            addLore(lore, LORE_MARKER + storageLines[i]);
        }
    }

    private void addLore(List<Component> lore, String line) {
        lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
    }

    /**
     * Builds a visual freshness bar using filled/empty blocks.
     * Example: &a██████████&8░░░░░░░░░░
     */
    private String buildFreshnessBar(double freshness, String color) {
        int totalBars = 10;
        int filled = (int) Math.round(freshness * totalBars);
        int empty = totalBars - filled;

        StringBuilder bar = new StringBuilder();
        bar.append(color);
        bar.append("\u2588".repeat(filled));
        bar.append("&8");
        bar.append("\u2591".repeat(empty));
        return bar.toString();
    }

    /**
     * Builds a visual portion bar.
     * Example: &f●●●&8○○ (3 remaining of 5 total)
     */
    private String buildPortionBar(int remaining, int total) {
        StringBuilder bar = new StringBuilder();
        bar.append("&f");
        bar.append("\u25CF".repeat(Math.max(0, remaining)));
        bar.append("&8");
        bar.append("\u25CB".repeat(Math.max(0, total - remaining)));
        return bar.toString();
    }

    // =====================================================
    // Portions
    // =====================================================

    /**
     * Gets remaining portions on a food item.
     * Returns -1 if item has no portions data.
     */
    public int getPortionsRemaining(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return -1;
        Integer remaining = item.getItemMeta().getPersistentDataContainer()
                .get(portionsRemainingKey, PersistentDataType.INTEGER);
        return remaining != null ? remaining : -1;
    }

    /**
     * Gets the total configured portions stored on a food item.
     * Returns -1 if the item has no portions data.
     */
    public int getPortionsTotal(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return -1;
        Integer total = item.getItemMeta().getPersistentDataContainer()
                .get(portionsTotalKey, PersistentDataType.INTEGER);
        return total != null ? total : -1;
    }

    /**
     * Consumes one portion from a food item.
     * Returns true if the item still has portions left (should NOT be fully consumed).
     * Returns false if this was the last portion (item should be consumed normally).
     */
    public boolean consumePortion(ItemStack item) {
        if (!config.isPortionsEnabled()) return false;
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer remaining = pdc.get(portionsRemainingKey, PersistentDataType.INTEGER);
        Integer total = pdc.get(portionsTotalKey, PersistentDataType.INTEGER);

        if (remaining == null || total == null || total <= 1) return false;

        int newRemaining = remaining - 1;
        if (newRemaining <= 0) {
            return false;
        }

        // Still has portions — update and prevent full consumption
        pdc.set(portionsRemainingKey, PersistentDataType.INTEGER, newRemaining);

        // Refresh lore to show updated portions
        Long baseMinutes = pdc.get(baseMinutesKey, PersistentDataType.LONG);
        Double progress = pdc.get(decayProgressKey, PersistentDataType.DOUBLE);
        if (baseMinutes != null && progress != null && config.showLoreTimer()) {
            updateLore(meta, progress, baseMinutes);
        }

        item.setItemMeta(meta);
        return true;
    }

    // =====================================================
    // Decay Task
    // =====================================================

    /** Index for staggered player processing — spreads load across ticks. */
    private int staggerIndex = 0;
    /** How many players to process per sub-tick in staggered mode. */
    private static final int PLAYERS_PER_BATCH = 20;

    /**
     * Starts the periodic decay task that processes all online players.
     * Uses a short sub-tick interval: every 5 ticks, processes a batch of
     * PLAYERS_PER_BATCH players silently (PDC only, no lore rebuild).
     * This spreads the CPU load across multiple ticks instead of spiking.
     */
    public void startDecayTask() {
        // Sub-tick interval: run every 5 ticks (0.25s) and process a batch
        decayTaskId = Bukkit.getScheduler().runTaskTimer(
                MidgardCore.getInstance(),
                this::processPlayerBatch,
                config.getDecayCheckInterval(),
                5L // run every 5 ticks, batch by batch
        ).getTaskId();
    }

    public void stopDecayTask() {
        if (decayTaskId != -1) {
            Bukkit.getScheduler().cancelTask(decayTaskId);
            decayTaskId = -1;
        }
    }

    /**
     * Processes a batch of players silently (PDC-only, no lore).
     * Cycles through all online players over multiple ticks.
     */
    private void processPlayerBatch() {
        List<? extends Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (online.isEmpty()) return;

        int total = online.size();
        if (staggerIndex >= total) {
            staggerIndex = 0;
            // Full cycle completed — clean up cached environment multipliers
            environmentManager.cleanupCache();
        }

        int end = Math.min(staggerIndex + PLAYERS_PER_BATCH, total);
        for (int i = staggerIndex; i < end; i++) {
            processInventorySilent(online.get(i));
        }
        staggerIndex = end;
    }

    /**
     * Silent inventory processing — updates PDC data only, skips lore rebuild.
     * Only calls setItemMeta when the item actually expired (needs spoiled model)
     * or when the decay change is significant enough to persist (> 0.1%).
     * This avoids sending inventory update packets every tick.
     */
    private void processInventorySilent(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            if (!config.isTracked(item.getType())) continue;
            if (config.neverExpires(item.getType())) continue;

            if (!isStamped(item)) {
                stampItem(item);
            } else {
                updateDecaySilent(item, player);
            }
        }
    }

    /**
     * Silent decay update — calculates and persists decay progress without
     * rebuilding lore or sending unnecessary packets.
     * Only calls setItemMeta if:
     *   - The item just expired (needs spoiled model applied)
     *   - Decay-by-weight triggered a stack reduction
     *   - Ice warming data expired and needs cleanup
     * Otherwise, PDC changes are deferred until the next visual event.
     */
    private double updateDecaySilent(ItemStack item, Player player) {
        if (item == null || !item.hasItemMeta()) return 0.0;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        Long lastUpdate = pdc.get(lastUpdateKey, PersistentDataType.LONG);
        Long baseMinutes = pdc.get(baseMinutesKey, PersistentDataType.LONG);
        Double progress = pdc.get(decayProgressKey, PersistentDataType.DOUBLE);

        if (lastUpdate == null || baseMinutes == null || progress == null) return 0.0;

        long now = System.currentTimeMillis();
        long elapsed = now - lastUpdate;
        if (elapsed <= 0) return progress;

        double baseDurationMs = baseMinutes * 60_000.0;
        if (baseDurationMs <= 0) return progress;

        Set<FoodTrait> traits = getTraits(item);
        double envMultiplier = environmentManager.getTotalMultiplier(player, traits);

        // Residual refrigeration
        boolean needsMeta = false;
        Long iceRemovedAt = pdc.get(iceRemovedAtKey, PersistentDataType.LONG);
        Double iceStoredMult = pdc.get(iceStoredMultKey, PersistentDataType.DOUBLE);
        if (iceRemovedAt != null && iceStoredMult != null && config.isIceConservationEnabled()) {
            long warmingDurationMs = config.getIceWarmingDuration() * 60_000L;
            long timeSinceRemoval = now - iceRemovedAt;
            if (warmingDurationMs > 0 && timeSinceRemoval < warmingDurationMs) {
                double warmingProgress = (double) timeSinceRemoval / warmingDurationMs;
                double residualMult = iceStoredMult + (1.0 - iceStoredMult) * warmingProgress;
                envMultiplier *= residualMult;
            } else {
                pdc.remove(iceRemovedAtKey);
                pdc.remove(iceStoredMultKey);
                needsMeta = true;
            }
        }

        double decayIncrement = (elapsed / baseDurationMs) * envMultiplier;
        double newProgress = progress + decayIncrement;

        pdc.set(decayProgressKey, PersistentDataType.DOUBLE, newProgress);
        pdc.set(lastUpdateKey, PersistentDataType.LONG, now);

        // Only apply meta if something gameplay-critical changed
        // (expiration, model stage change, or ice warming cleanup).
        if (newProgress >= 1.0 && progress < 1.0) {
            applySpoiledModel(meta, pdc);
            updateLore(meta, newProgress, baseMinutes);
            needsMeta = true;
        }

        // Detect decay model stage changes (cheap check)
        if (!needsMeta) {
            int[] decayStages = config.getDecayModelStages(item.getType());
            if (decayStages != null && decayStages.length > 0) {
                int oldIdx = (progress >= 1.0) ? decayStages.length - 1
                        : Math.min((int) (Math.max(progress, 0) * decayStages.length), decayStages.length - 1);
                int newIdx = (newProgress >= 1.0) ? decayStages.length - 1
                        : Math.min((int) (Math.max(newProgress, 0) * decayStages.length), decayStages.length - 1);
                if (newIdx != oldIdx && decayStages[newIdx] > 0) {
                    meta.setCustomModelData(decayStages[newIdx]);
                    needsMeta = true;
                }
            }
        }

        if (needsMeta) {
            item.setItemMeta(meta);
        }

        // Decay by weight — always needs setItemMeta since stack size changes
        if (config.isDecayByWeightEnabled() && item.getAmount() > 1) {
            double interval = config.getDecayByWeightLossInterval();
            if (interval > 0) {
                int previousLosses = (int) (progress / interval);
                int currentLosses = (int) (newProgress / interval);
                int lossCount = currentLosses - previousLosses;
                if (lossCount > 0) {
                    if (!needsMeta) {
                        // Must persist PDC before changing stack size
                        item.setItemMeta(meta);
                    }
                    item.setAmount(Math.max(1, item.getAmount() - lossCount));
                }
            }
        }

        return newProgress;
    }

    private void processAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            processInventory(player);
        }
    }

    /**
     * Processes a player's entire inventory — stamps new food and updates decay on existing.
     * Full update with lore — used by event handlers (inventory open, click, close).
     */
    public void processInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            if (!config.isTracked(item.getType())) continue;
            if (config.neverExpires(item.getType())) continue;

            if (!isStamped(item)) {
                stampItem(item);
            } else {
                updateDecay(item, player);
            }
        }
        // Force immediate visual sync — sends only changed slots via NMS
        NmsHelper.syncInventory(player);
    }

    /**
     * Processes all food items inside a container inventory using the container's location.
     * Stamps unstamped food and updates decay based on the container's environment.
     */
    public void pauseInventoryDecay(Player player) {
        if (player == null) return;

        long now = System.currentTimeMillis();
        for (ItemStack item : player.getInventory().getContents()) {
            if (!shouldManageInventoryItem(item) || !isStamped(item)) continue;

            updateDecay(item, player);
            freezeInventoryItemForOffline(item, now);
        }
    }

    public void resumeInventoryDecay(Player player) {
        if (player == null) return;

        long now = System.currentTimeMillis();
        for (ItemStack item : player.getInventory().getContents()) {
            if (!shouldManageInventoryItem(item) || !isStamped(item) || !item.hasItemMeta()) continue;

            ItemMeta meta = item.getItemMeta();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (!pdc.has(lastUpdateKey, PersistentDataType.LONG)) continue;

            pdc.set(lastUpdateKey, PersistentDataType.LONG, now);
            if (pdc.has(iceRemovedAtKey, PersistentDataType.LONG)
                    && pdc.has(iceStoredMultKey, PersistentDataType.DOUBLE)) {
                pdc.set(iceRemovedAtKey, PersistentDataType.LONG, now);
                pdc.remove(iceStartedAtKey);
                pdc.remove(iceStartMultKey);
            }

            item.setItemMeta(meta);
        }
    }

    public void processContainer(Inventory inventory, Location containerLocation, Material containerType) {
        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType().isAir()) continue;
            if (!config.isTracked(item.getType())) continue;
            if (config.neverExpires(item.getType())) continue;

            if (!isStamped(item)) {
                stampItem(item);
            } else {
                updateDecay(item, containerLocation, containerType);
            }
        }
    }

    /**
     * Refreshes the lore on an already-stamped item without changing decay progress.
     */
    public void refreshLore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        Double progress = pdc.get(decayProgressKey, PersistentDataType.DOUBLE);
        Long baseMinutes = pdc.get(baseMinutesKey, PersistentDataType.LONG);
        if (progress == null || baseMinutes == null) return;

        updateLore(meta, progress, baseMinutes);
        item.setItemMeta(meta);
    }

    // =====================================================
    // Time Formatting
    // =====================================================

    public String formatTime(long millis) {
        if (millis <= 0) return config.getTimeFormatExpired();

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return config.getTimeFormatDays()
                    .replace("{d}", String.valueOf(days))
                    .replace("{h}", String.valueOf(hours % 24));
        } else if (hours > 0) {
            return config.getTimeFormatHours()
                    .replace("{h}", String.valueOf(hours))
                    .replace("{m}", String.valueOf(minutes % 60));
        } else if (minutes > 0) {
            return config.getTimeFormatMinutes()
                    .replace("{m}", String.valueOf(minutes))
                    .replace("{s}", String.valueOf(seconds % 60));
        } else {
            return config.getTimeFormatSeconds()
                    .replace("{s}", String.valueOf(seconds));
        }
    }

    private boolean shouldManageInventoryItem(ItemStack item) {
        return item != null
                && !item.getType().isAir()
                && config.isTracked(item.getType())
                && !config.neverExpires(item.getType());
    }

    private void freezeInventoryItemForOffline(ItemStack item, long now) {
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(lastUpdateKey, PersistentDataType.LONG)) return;

        pdc.set(lastUpdateKey, PersistentDataType.LONG, now);
        freezeResidualCoolingForOffline(pdc, now);
        item.setItemMeta(meta);
    }

    private void freezeResidualCoolingForOffline(PersistentDataContainer pdc, long now) {
        Long iceRemovedAt = pdc.get(iceRemovedAtKey, PersistentDataType.LONG);
        Double iceStoredMult = pdc.get(iceStoredMultKey, PersistentDataType.DOUBLE);
        if (iceRemovedAt == null || iceStoredMult == null || !config.isIceConservationEnabled()) return;

        OfflineInventoryDecaySupport.ResidualCoolingSnapshot snapshot =
                OfflineInventoryDecaySupport.snapshotResidualCooling(
                        iceRemovedAt,
                        iceStoredMult,
                        now,
                        config.getIceWarmingDuration() * 60_000L
                );

        if (!snapshot.active()) {
            pdc.remove(iceRemovedAtKey);
            pdc.remove(iceStoredMultKey);
            pdc.remove(iceStartedAtKey);
            pdc.remove(iceStartMultKey);
            return;
        }

        pdc.set(iceStoredMultKey, PersistentDataType.DOUBLE, snapshot.storedMultiplier());
        pdc.set(iceRemovedAtKey, PersistentDataType.LONG, now);
        pdc.remove(iceStartedAtKey);
        pdc.remove(iceStartMultKey);
    }

    // =====================================================
    // Key Getters
    // =====================================================

    public NamespacedKey getTimestampKey() { return timestampKey; }
    public NamespacedKey getBaseMinutesKey() { return baseMinutesKey; }
    public NamespacedKey getDecayProgressKey() { return decayProgressKey; }
    public NamespacedKey getLastUpdateKey() { return lastUpdateKey; }
    public NamespacedKey getTraitsKey() { return traitsKey; }

    public EnvironmentManager getEnvironmentManager() { return environmentManager; }
}
