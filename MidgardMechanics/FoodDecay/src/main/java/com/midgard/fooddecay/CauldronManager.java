package com.midgard.fooddecay;

import com.midgard.core.MidgardCore;
import com.midgard.core.utils.MessageUtils;
import static com.midgard.core.utils.MessageUtils.sc;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages cauldron-based cooking recipes.
 * Players add food ingredients to a water cauldron to create custom stews.
 */
public class CauldronManager {

    private final FoodDecayConfig config;
    private final NutritionManager nutritionManager;
    private final FoodDecayManager decayManager;
    private final Map<Location, List<Material>> cauldronContents = new ConcurrentHashMap<>();
    private int saveTaskId = -1;

    public CauldronManager(FoodDecayConfig config, NutritionManager nutritionManager,
                            FoodDecayManager decayManager) {
        this.config = config;
        this.nutritionManager = nutritionManager;
        this.decayManager = decayManager;
        loadData();
    }

    /**
     * Schedules a save for 1 second later, coalescing multiple rapid changes.
     */
    private void markDirty() {
        if (saveTaskId != -1) return;
        saveTaskId = Bukkit.getScheduler().runTaskLater(
                MidgardCore.getInstance(),
                () -> {
                    saveTaskId = -1;
                    saveData();
                },
                20L
        ).getTaskId();
    }

    /**
     * Handles a player right-clicking a water cauldron with food.
     * Returns true if the interaction was handled.
     */
    public boolean onCauldronInteract(Player player, Block block, ItemStack food) {
        if (!config.isCauldronRecipesEnabled()) return false;
        if (block.getType() != Material.WATER_CAULDRON) return false;
        if (food == null || food.getType().isAir() || !food.getType().isEdible()) return false;

        // Block expired food
        if (decayManager != null && decayManager.isExpired(food)) {
            player.sendMessage(MessageUtils.toComponent(sc(config.msg("cauldron-expired-ingredient"))));
            return true;
        }

        Location loc = block.getLocation();
        List<Material> contents = cauldronContents.computeIfAbsent(loc, k -> new ArrayList<>());

        int maxIngredients = config.getCauldronMaxIngredients();
        if (contents.size() >= maxIngredients) {
            player.sendMessage(MessageUtils.toComponent(sc(config.msg("cauldron-full"))));
            return true;
        }

        // Add ingredient
        contents.add(food.getType());
        food.setAmount(food.getAmount() - 1);
        markDirty();

        // Lower water level
        if (!(block.getBlockData() instanceof Levelled data)) return true;
        if (data.getLevel() > 1) {
            data.setLevel(data.getLevel() - 1);
            block.setBlockData(data);
        }

        // Feedback
        Location effectLoc = loc.clone().add(0.5, 1.0, 0.5);
        block.getWorld().spawnParticle(Particle.SPLASH, effectLoc, 15, 0.2, 0.1, 0.2, 0.05);
        block.getWorld().playSound(loc, Sound.ENTITY_GENERIC_SPLASH, 0.6f, 1.3f);

        player.sendMessage(MessageUtils.toComponent(
                sc(config.msg("cauldron-ingredient-added")
                        .replace("{count}", String.valueOf(contents.size()))
                        .replace("{max}", String.valueOf(maxIngredients)))));

        // Check if cauldron is full
        if (contents.size() >= maxIngredients) {
            ItemStack result = createStew(contents);
            block.setType(Material.CAULDRON);
            cauldronContents.remove(loc);
            markDirty();

            Map<Integer, ItemStack> overflow = player.getInventory().addItem(result);
            for (ItemStack leftover : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }

            block.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,
                    effectLoc, 10, 0.2, 0.3, 0.2, 0.02);
            block.getWorld().playSound(loc, Sound.BLOCK_BREWING_STAND_BREW, 0.8f, 1.0f);
            player.sendMessage(MessageUtils.toComponent(sc(config.msg("cauldron-stew-ready"))));
        }

        return true;
    }

    private ItemStack createStew(List<Material> ingredients) {
        ItemStack stew = new ItemStack(config.getCauldronResultMaterial());
        var meta = stew.getItemMeta();

        StringJoiner joiner = new StringJoiner(", ");
        for (Material m : ingredients) {
            joiner.add(formatMaterial(m.name()));
        }

        meta.displayName(MessageUtils.toComponent(
                sc(config.getCauldronResultName())));

        List<Component> lore = new ArrayList<>();
        for (String line : config.getCauldronResultLore()) {
            lore.add(MessageUtils.toComponent(
                    sc(line.replace("{ingredients}", joiner.toString()))));
        }

        // Show nutrition groups
        Set<NutritionManager.FoodGroup> groups = EnumSet.noneOf(NutritionManager.FoodGroup.class);
        for (Material m : ingredients) {
            groups.addAll(nutritionManager.getFoodGroups(m));
        }

        if (!groups.isEmpty()) {
            StringJoiner groupJoiner = new StringJoiner(", ");
            for (NutritionManager.FoodGroup g : groups) {
                groupJoiner.add(nutritionManager.getGroupDisplayName(g));
            }
            lore.add(MessageUtils.toComponent(
                    sc(config.msg("cauldron-nutrition-label")
                            .replace("{groups}", groupJoiner.toString()))));
        }

        meta.lore(lore);
        stew.setItemMeta(meta);

        // Stamp with decay data so the stew ages properly
        if (decayManager != null) {
            decayManager.stampItem(stew);
        }
        if (!groups.isEmpty()) {
            nutritionManager.setFoodGroups(stew, groups);
        }

        return stew;
    }

    /**
     * Clears a cauldron's tracked contents (e.g., when the block breaks).
     * Drops stored ingredients on the ground.
     */
    public void clearCauldron(Location location) {
        List<Material> contents = cauldronContents.remove(location);
        if (contents != null && !contents.isEmpty() && location.getWorld() != null) {
            for (Material mat : contents) {
                location.getWorld().dropItemNaturally(location, new ItemStack(mat));
            }
            markDirty();
        }
    }

    public void clearAll() {
        if (saveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(saveTaskId);
            saveTaskId = -1;
        }
        saveData();
        cauldronContents.clear();
    }

    // =====================================================
    //  Persistence
    // =====================================================

    private File getDataFile() {
        return new File(FoodDecayPlugin.getInstance().getDataFolder(), "cauldrons.yml");
    }

    public void saveData() {
        File file = getDataFile();
        if (cauldronContents.isEmpty()) {
            file.delete();
            return;
        }
        YamlConfiguration data = new YamlConfiguration();
        int idx = 0;
        for (Map.Entry<Location, List<Material>> entry : cauldronContents.entrySet()) {
            Location loc = entry.getKey();
            if (loc.getWorld() == null) continue;
            String p = "c." + idx;
            data.set(p + ".world", loc.getWorld().getName());
            data.set(p + ".x", loc.getBlockX());
            data.set(p + ".y", loc.getBlockY());
            data.set(p + ".z", loc.getBlockZ());
            List<String> mats = new ArrayList<>();
            for (Material m : entry.getValue()) mats.add(m.name());
            data.set(p + ".items", mats);
            idx++;
        }
        try {
            file.getParentFile().mkdirs();
            data.save(file);
        } catch (IOException e) {
            MidgardCore.getInstance().getLogger().log(Level.WARNING,
                    "[FoodDecay] Failed to save cauldron data", e);
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
            // Only restore if block is still a water cauldron
            if (loc.getBlock().getType() != Material.WATER_CAULDRON) continue;
            List<String> matNames = data.getStringList(p + ".items");
            List<Material> mats = new ArrayList<>();
            for (String name : matNames) {
                try { mats.add(Material.valueOf(name)); }
                catch (IllegalArgumentException ignored) { /* skip invalid */ }
            }
            if (!mats.isEmpty()) {
                cauldronContents.put(loc, mats);
                loaded++;
            }
        }
        if (loaded > 0) {
            MidgardCore.getInstance().getLogger().info(
                    "[FoodDecay] Restored " + loaded + " cauldron recipe(s) in progress.");
        }
    }

    private static String formatMaterial(String name) {
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(part.charAt(0)).append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
