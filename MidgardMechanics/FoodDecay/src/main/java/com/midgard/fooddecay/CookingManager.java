package com.midgard.fooddecay;

import com.midgard.core.MidgardCore;
import com.midgard.core.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * TFC-style campfire cooking system.
 * Players right-click a lit campfire/soul campfire with raw food to start cooking.
 * Food cooks over time based on heat level. Leaving it too long burns it.
 */
public class CookingManager {

    private final FoodDecayConfig config;
    private final FoodDecayManager decayManager;
    private final Map<Location, CookingEntry> activeCooking = new ConcurrentHashMap<>();
    private int taskId = -1;
    private boolean dirty = false;

    public CookingManager(FoodDecayConfig config, FoodDecayManager decayManager) {
        this.config = config;
        this.decayManager = decayManager;
        loadData();
    }

    private void markDirty() { dirty = true; }

    // =====================================================
    // Data
    // =====================================================

    public record CookingEntry(
            ItemStack rawFood,
            Material cookedResult,
            long startTime,
            long cookDurationMs,
            long burnDurationMs,
            double heatMultiplier,
            UUID playerUuid,
            ItemDisplay display
    ) {
        public long elapsed() { return System.currentTimeMillis() - startTime; }
        public boolean isCooked() { return elapsed() >= cookDurationMs; }
        public boolean isBurnt() { return burnDurationMs > 0 && elapsed() >= cookDurationMs + burnDurationMs; }
        public float progress() {
            return Math.min(1f, (float) elapsed() / Math.max(1, cookDurationMs));
        }
    }

    // =====================================================
    // Interaction
    // =====================================================

    /**
     * Handles a player right-clicking a campfire or soul campfire with food.
     * Returns true if the interaction was consumed.
     */
    public boolean onCampfireInteract(Player player, Block block, ItemStack handItem) {
        if (!config.isCookingEnabled()) return false;

        Material blockType = block.getType();
        boolean isCampfire = blockType == Material.CAMPFIRE;
        boolean isSoulCampfire = blockType == Material.SOUL_CAMPFIRE;
        if (!isCampfire && !isSoulCampfire) return false;

        // Check if campfire is lit
        if (block.getBlockData() instanceof org.bukkit.block.data.type.Campfire campfireData) {
            if (!campfireData.isLit()) return false;
        }

        Location loc = block.getLocation();

        // If food is already cooking here — check status
        if (activeCooking.containsKey(loc)) {
            CookingEntry entry = activeCooking.get(loc);
            if (entry.isCooked()) {
                collectCookedFood(player, loc);
                return true;
            }
            String msg = config.msg("cooking-already-active");
            if (msg != null && !msg.isEmpty()) {
                float pct = entry.progress() * 100;
                player.sendActionBar(MessageUtils.toComponent(
                        msg.replace("{progress}", String.format("%.0f", pct))));
            }
            return true;
        }

        // Try to place food for cooking
        if (handItem == null || handItem.getType().isAir()) return false;

        // Look up cooking recipe
        Material cookedResult = config.getCookingResult(handItem.getType());
        if (cookedResult == null) return false;

        long cookMinutes = config.getCookingTime(handItem.getType());
        if (cookMinutes <= 0) return false;

        double heatMult = isSoulCampfire
                ? config.getCookingHeatMultiplier("soul-campfire")
                : config.getCookingHeatMultiplier("campfire");
        long cookDurationMs = (long) (cookMinutes * 60_000.0 / Math.max(0.01, heatMult));

        long burnDurationMs = 0;
        if (config.isCookingBurnEnabled()) {
            burnDurationMs = config.getCookingBurnMinutes() * 60_000L;
        }

        // Consume 1 item from hand
        ItemStack raw = handItem.clone();
        raw.setAmount(1);
        handItem.setAmount(handItem.getAmount() - 1);

        // Spawn food display entity on the campfire
        ItemDisplay display = spawnFoodDisplay(loc, raw);

        CookingEntry entry = new CookingEntry(
                raw, cookedResult, System.currentTimeMillis(),
                cookDurationMs, burnDurationMs, heatMult,
                player.getUniqueId(), display
        );
        activeCooking.put(loc, entry);
        markDirty();
        block.getWorld().playSound(loc, Sound.BLOCK_CAMPFIRE_CRACKLE, 1f, 1f);
        String msg = config.msg("cooking-started");
        if (msg != null && !msg.isEmpty()) {
            player.sendActionBar(MessageUtils.toComponent(msg));
        }

        return true;
    }

    /**
     * Collects cooked food from a campfire location.
     */
    private void collectCookedFood(Player player, Location loc) {
        CookingEntry entry = activeCooking.remove(loc);
        if (entry == null) return;
        markDirty();

        // Remove display entity
        if (entry.display != null && entry.display.isValid()) {
            entry.display.remove();
        }

        if (entry.isBurnt()) {
            // Burnt! Return charcoal
            ItemStack burnt = new ItemStack(Material.CHARCOAL);
            player.getInventory().addItem(burnt).values()
                    .forEach(overflow -> player.getWorld().dropItemNaturally(player.getLocation(), overflow));

            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, 0.5f);
            String msg = config.msg("cooking-burnt");
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(MessageUtils.toComponent(msg));
            }
        } else {
            // Cooked successfully!
            ItemStack cooked = new ItemStack(entry.cookedResult);
            if (decayManager != null) {
                decayManager.stampItem(cooked);
            }
            player.getInventory().addItem(cooked).values()
                    .forEach(overflow -> player.getWorld().dropItemNaturally(player.getLocation(), overflow));

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
            String msg = config.msg("cooking-complete");
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(MessageUtils.toComponent(msg));
            }
        }
    }

    // =====================================================
    // Display Entity
    // =====================================================

    private ItemDisplay spawnFoodDisplay(Location loc, ItemStack item) {
        World world = loc.getWorld();
        if (world == null) return null;

        Location spawnLoc = loc.clone().add(0.5, 0.4, 0.5);
        return world.spawn(spawnLoc, ItemDisplay.class, display -> {
            display.setItemStack(item);
            display.setBillboard(Display.Billboard.FIXED);
            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f((float) Math.toRadians(90), 1, 0, 0),
                    new Vector3f(0.35f, 0.35f, 0.35f),
                    new AxisAngle4f(0, 0, 0, 1)
            ));
            display.setPersistent(false);
        });
    }

    // =====================================================
    // Task
    // =====================================================

    public void startTask() {
        if (!config.isCookingEnabled()) return;
        taskId = Bukkit.getScheduler().runTaskTimer(
                MidgardCore.getInstance(), this::tick, 20L, 20L
        ).getTaskId();
    }

    public void stopTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        // Drop food and clean up display entities on shutdown
        for (Map.Entry<Location, CookingEntry> entry : activeCooking.entrySet()) {
            Location loc = entry.getKey();
            CookingEntry cooking = entry.getValue();
            if (cooking.display != null && cooking.display.isValid()) {
                cooking.display.remove();
            }
            if (loc.isWorldLoaded()) {
                if (cooking.isBurnt()) {
                    loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.CHARCOAL));
                } else if (cooking.isCooked()) {
                    ItemStack cookedDrop = new ItemStack(cooking.cookedResult());
                    if (decayManager != null) decayManager.stampItem(cookedDrop);
                    loc.getWorld().dropItemNaturally(loc, cookedDrop);
                } else {
                    loc.getWorld().dropItemNaturally(loc, cooking.rawFood());
                }
            }
        }
        activeCooking.clear();
        // Delete persisted data since we already dropped everything
        getDataFile().delete();
    }

    private int saveCounter = 0;

    private void tick() {
        // Lazy save every ~10 seconds (10 ticks * 20L interval = 10s)
        if (++saveCounter >= 10 && dirty) {
            saveCounter = 0;
            saveData();
        }

        Iterator<Map.Entry<Location, CookingEntry>> it = activeCooking.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Location, CookingEntry> mapEntry = it.next();
            Location loc = mapEntry.getKey();
            CookingEntry entry = mapEntry.getValue();

            // Verify campfire still exists and is lit
            if (!loc.isWorldLoaded()) {
                removeEntry(it, entry, loc);
                continue;
            }
            Block block = loc.getBlock();
            if (block.getType() != Material.CAMPFIRE && block.getType() != Material.SOUL_CAMPFIRE) {
                removeEntry(it, entry, loc);
                continue;
            }
            if (block.getBlockData() instanceof org.bukkit.block.data.type.Campfire campfireData) {
                if (!campfireData.isLit()) {
                    removeEntry(it, entry, loc);
                    continue;
                }
            }

            // Spawn cooking particles
            World world = loc.getWorld();
            double x = loc.getBlockX() + 0.5;
            double y = loc.getBlockY() + 0.5;
            double z = loc.getBlockZ() + 0.5;

            if (entry.isBurnt()) {
                // Burning — dark smoke
                world.spawnParticle(Particle.LARGE_SMOKE, x, y + 0.3, z, 1, 0.1, 0.05, 0.1, 0.01);
                if (entry.elapsed() % 4000 < 1000) {
                    world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.3f, 0.5f);
                }
            } else if (entry.isCooked()) {
                // Ready — golden sparkle
                world.spawnParticle(Particle.WAX_ON, x, y + 0.4, z, 1, 0.1, 0.05, 0.1, 0.01);
            } else {
                // Cooking — sizzle
                world.spawnParticle(Particle.SMOKE, x, y + 0.3, z, 1, 0.1, 0.02, 0.1, 0.005);
                if (entry.progress() > 0.5f) {
                    world.spawnParticle(Particle.FLAME, x, y + 0.2, z, 0, 0.05, 0.01, 0.05, 0.005);
                }
            }

            // Rotate display entity for visual interest
            if (entry.display != null && entry.display.isValid()) {
                float angle = (float) Math.toRadians(90) + (float) Math.sin(entry.elapsed() / 2000.0) * 0.1f;
                entry.display.setTransformation(new Transformation(
                        new Vector3f(0, 0, 0),
                        new AxisAngle4f(angle, 1, 0, 0),
                        new Vector3f(0.35f, 0.35f, 0.35f),
                        new AxisAngle4f(0, 0, 0, 1)
                ));
            }
        }
    }

    private void removeEntry(Iterator<Map.Entry<Location, CookingEntry>> it, CookingEntry entry, Location campfireLoc) {
        if (entry.display != null && entry.display.isValid()) {
            entry.display.remove();
        }
        // Drop the appropriate item based on cooking state
        if (campfireLoc != null && campfireLoc.isWorldLoaded()) {
            if (entry.isBurnt()) {
                campfireLoc.getWorld().dropItemNaturally(campfireLoc, new ItemStack(Material.CHARCOAL));
            } else if (entry.isCooked()) {
                ItemStack cookedDrop = new ItemStack(entry.cookedResult());
                if (decayManager != null) decayManager.stampItem(cookedDrop);
                campfireLoc.getWorld().dropItemNaturally(campfireLoc, cookedDrop);
            } else {
                campfireLoc.getWorld().dropItemNaturally(campfireLoc, entry.rawFood());
            }
        }
        it.remove();
        markDirty();
    }

    public Map<Location, CookingEntry> getActiveCooking() {
        return Collections.unmodifiableMap(activeCooking);
    }

    /**
     * Checks if there is an active cooking entry at a specific location.
     */
    public boolean hasActiveCooking(Location loc) {
        return activeCooking.containsKey(loc);
    }

    /**
     * Immediately handles campfire removal (explosion, break, piston).
     * Drops the appropriate item and cleans up display entity.
     */
    public void onCampfireRemoved(Location loc) {
        CookingEntry entry = activeCooking.remove(loc);
        if (entry == null) return;
        markDirty();

        if (entry.display() != null && entry.display().isValid()) {
            entry.display().remove();
        }
        if (loc.isWorldLoaded()) {
            if (entry.isBurnt()) {
                loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.CHARCOAL));
            } else if (entry.isCooked()) {
                ItemStack cookedDrop = new ItemStack(entry.cookedResult());
                if (decayManager != null) decayManager.stampItem(cookedDrop);
                loc.getWorld().dropItemNaturally(loc, cookedDrop);
            } else {
                loc.getWorld().dropItemNaturally(loc, entry.rawFood());
            }
        }
    }

    // =====================================================
    // Persistence — crash-safe cooking data
    // =====================================================

    private File getDataFile() {
        return new File(FoodDecayPlugin.getInstance().getDataFolder(), "cooking.yml");
    }

    public void saveData() {
        if (!dirty) return;
        dirty = false;

        File file = getDataFile();
        if (activeCooking.isEmpty()) {
            file.delete();
            return;
        }

        YamlConfiguration data = new YamlConfiguration();
        int idx = 0;
        for (Map.Entry<Location, CookingEntry> entry : activeCooking.entrySet()) {
            Location loc = entry.getKey();
            CookingEntry cooking = entry.getValue();
            if (loc.getWorld() == null) continue;

            String p = "c." + idx;
            data.set(p + ".world", loc.getWorld().getName());
            data.set(p + ".x", loc.getBlockX());
            data.set(p + ".y", loc.getBlockY());
            data.set(p + ".z", loc.getBlockZ());
            data.set(p + ".raw", cooking.rawFood());
            data.set(p + ".result", cooking.cookedResult().name());
            data.set(p + ".start", cooking.startTime());
            data.set(p + ".cookMs", cooking.cookDurationMs());
            data.set(p + ".burnMs", cooking.burnDurationMs());
            data.set(p + ".heat", cooking.heatMultiplier());
            data.set(p + ".player", cooking.playerUuid().toString());
            idx++;
        }

        try {
            file.getParentFile().mkdirs();
            data.save(file);
        } catch (IOException e) {
            MidgardCore.getInstance().getLogger().log(Level.WARNING,
                    "[FoodDecay] Failed to save cooking data", e);
        }
    }

    private void loadData() {
        File file = getDataFile();
        if (!file.exists()) return;

        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        var section = data.getConfigurationSection("c");
        if (section == null) { file.delete(); return; }

        int loaded = 0;
        for (String key : section.getKeys(false)) {
            String p = "c." + key;
            String worldName = data.getString(p + ".world");
            World world = worldName != null ? Bukkit.getWorld(worldName) : null;
            if (world == null) continue;

            int x = data.getInt(p + ".x");
            int y = data.getInt(p + ".y");
            int z = data.getInt(p + ".z");
            Location loc = new Location(world, x, y, z);

            // Only restore if block is still a campfire
            Material blockType = loc.getBlock().getType();
            if (blockType != Material.CAMPFIRE && blockType != Material.SOUL_CAMPFIRE) continue;

            ItemStack raw = data.getItemStack(p + ".raw");
            if (raw == null) continue;

            String resultName = data.getString(p + ".result");
            Material cookedResult;
            try { cookedResult = Material.valueOf(resultName); }
            catch (IllegalArgumentException ignored) { continue; }

            long startTime = data.getLong(p + ".start");
            long cookMs = data.getLong(p + ".cookMs");
            long burnMs = data.getLong(p + ".burnMs");
            double heat = data.getDouble(p + ".heat", 1.0);
            String playerStr = data.getString(p + ".player");
            UUID playerUuid;
            try { playerUuid = UUID.fromString(playerStr); }
            catch (Exception ignored) { continue; }

            // Re-spawn display entity
            ItemDisplay display = spawnFoodDisplay(loc, raw);

            CookingEntry entry = new CookingEntry(
                    raw, cookedResult, startTime, cookMs, burnMs, heat, playerUuid, display
            );
            activeCooking.put(loc, entry);
            loaded++;
        }

        if (loaded > 0) {
            MidgardCore.getInstance().getLogger().info(
                    "[FoodDecay] Restored " + loaded + " campfire cooking(s) in progress.");
        }
    }
}
