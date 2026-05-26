package com.midgard.fooddecay.multiblock;

import com.midgard.core.MidgardCore;
import com.midgard.fooddecay.FoodDecayConfig;
import com.midgard.fooddecay.FoodDecayPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.bukkit.util.Transformation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;

/**
 * Handles save/load of active multiblocks to YAML, plus orphaned entity cleanup.
 */
public final class MultiblockPersistence {

    private MultiblockPersistence() {}

    private static File getDataFile() {
        return new File(FoodDecayPlugin.getInstance().getDataFolder(), "multiblocks.yml");
    }

    /**
     * Saves all active multiblocks to multiblocks.yml.
     */
    public static void saveData(Map<Location, ProcessingMultiblock> activeMultiblocks,
                                 NamespacedKey entityTagKey) {
        File file = getDataFile();
        if (activeMultiblocks.isEmpty()) {
            file.delete();
            return;
        }
        YamlConfiguration data = new YamlConfiguration();
        int idx = 0;
        for (Map.Entry<Location, ProcessingMultiblock> entry : activeMultiblocks.entrySet()) {
            ProcessingMultiblock mb = entry.getValue();
            Location anchor = entry.getKey();
            if (anchor.getWorld() == null) continue;
            String p = "mb." + idx;
            data.set(p + ".type", mb.type.name());
            data.set(p + ".tier", mb.tier);
            data.set(p + ".world", anchor.getWorld().getName());
            data.set(p + ".x", anchor.getBlockX());
            data.set(p + ".y", anchor.getBlockY());
            data.set(p + ".z", anchor.getBlockZ());
            int rotIdx = mb.type.getRotations(mb.tier).indexOf(mb.patternRotation);
            data.set(p + ".rot", Math.max(0, rotIdx));
            if (mb.processingFood != null) {
                data.set(p + ".food", mb.processingFood);
            }
            if (mb.activeRecipe != null) {
                data.set(p + ".recipe", mb.activeRecipe.getId());
            }
            data.set(p + ".start", mb.startTime);
            data.set(p + ".paused", mb.pausedMs);
            data.set(p + ".completed", mb.completedTime);
            data.set(p + ".quality", mb.qualityBonus);
            data.set(p + ".eventsOk", mb.eventsHandled);
            data.set(p + ".eventsMiss", mb.eventsMissed);
            data.set(p + ".fuel", mb.fuel);
            data.set(p + ".salt", mb.salt);
            data.set(p + ".water", mb.hasWater);
            data.set(p + ".vinegar", mb.hasVinegar);
            data.set(p + ".wax", mb.wax);
            data.set(p + ".eventActive", mb.eventActive);
            data.set(p + ".eventStart", mb.eventStartTime);
            data.set(p + ".eventType", mb.eventType);
            data.set(p + ".proximity", mb.proximityTicks);
            if (mb.ownerId != null) {
                data.set(p + ".owner", mb.ownerId.toString());
            }

            // Save extra slots
            if (!mb.extraSlots.isEmpty()) {
                int si = 0;
                for (ProcessingSlot slot : mb.extraSlots) {
                    if (!slot.hasFood()) continue;
                    String sp = p + ".slots." + si;
                    data.set(sp + ".food", slot.food);
                    if (slot.recipe != null) data.set(sp + ".recipe", slot.recipe.getId());
                    data.set(sp + ".start", slot.startTime);
                    data.set(sp + ".paused", slot.pausedMs);
                    data.set(sp + ".completed", slot.completedTime);
                    data.set(sp + ".quality", slot.qualityBonus);
                    if (slot.ownerId != null) data.set(sp + ".owner", slot.ownerId.toString());
                    si++;
                }
            }

            idx++;
        }
        File tempFile = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            file.getParentFile().mkdirs();
            data.save(tempFile);
            Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            MidgardCore.getInstance().getLogger().log(Level.WARNING,
                    "[FoodDecay] Failed to save multiblock data", e);
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Loads active multiblocks from multiblocks.yml.
     * Verifies structure integrity and discards invalid ones.
     */
    public static Map<Location, ProcessingMultiblock> loadData(FoodDecayConfig config) {
        File file = getDataFile();
        Map<Location, ProcessingMultiblock> result = new LinkedHashMap<>();
        if (!file.exists()) return result;

        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        var section = data.getConfigurationSection("mb");
        if (section == null) {
            MidgardCore.getInstance().getLogger().warning(
                    "[FoodDecay] multiblocks.yml is missing the 'mb' section; keeping the file for recovery.");
            return result;
        }

        int loaded = 0;
        for (String key : section.getKeys(false)) {
            String p = "mb." + key;
            String typeName = data.getString(p + ".type");
            MultiblockType type = typeName != null ? MultiblockType.fromKey(typeName) : null;
            if (type == null) continue;

            String worldName = data.getString(p + ".world");
            World world = worldName != null ? Bukkit.getWorld(worldName) : null;
            if (world == null) continue;

            int x = data.getInt(p + ".x");
            int y = data.getInt(p + ".y");
            int z = data.getInt(p + ".z");
            Location anchor = new Location(world, x, y, z);

            int tier = data.getInt(p + ".tier", 1);
            int rotIdx = data.getInt(p + ".rot", 0);
            List<List<MultiblockType.RB>> rotations = type.getRotations(tier);
            List<MultiblockType.RB> rotation = rotations.get(
                    Math.min(rotIdx, rotations.size() - 1));

            List<Location> blocks = new ArrayList<>();
            blocks.add(anchor.clone());
            for (MultiblockType.RB rb : rotation) {
                blocks.add(new Location(world, x + rb.x(), y + rb.y(), z + rb.z()));
            }

            ProcessingMultiblock mb = new ProcessingMultiblock(type, blocks, rotation);
            mb.tier = tier;
            mb.startTime = data.getLong(p + ".start");
            mb.pausedMs = data.getLong(p + ".paused");
            mb.completedTime = data.getLong(p + ".completed");
            mb.qualityBonus = (float) data.getDouble(p + ".quality");
            mb.eventsHandled = data.getInt(p + ".eventsOk");
            mb.eventsMissed = data.getInt(p + ".eventsMiss");
            mb.fuel = data.getInt(p + ".fuel");
            mb.salt = data.getInt(p + ".salt");
            mb.hasWater = data.getBoolean(p + ".water");
            mb.hasVinegar = data.getBoolean(p + ".vinegar");
            mb.wax = data.getInt(p + ".wax");
            mb.eventActive = data.getBoolean(p + ".eventActive");
            mb.eventStartTime = data.getLong(p + ".eventStart");
            mb.eventType = data.getInt(p + ".eventType");
            mb.proximityTicks = data.getInt(p + ".proximity");

            String ownerStr = data.getString(p + ".owner");
            if (ownerStr != null) {
                try { mb.ownerId = UUID.fromString(ownerStr); }
                catch (IllegalArgumentException ignored) {
                    // Corrupted owner UUID in save data — leave as null
                }
            }

            ItemStack food = data.getItemStack(p + ".food");
            if (food != null) {
                mb.processingFood = food;
            }

            // Restore recipe
            String recipeId = data.getString(p + ".recipe");
            if (recipeId != null) {
                for (MultiblockRecipe r : config.getRecipes(type)) {
                    if (r.getId().equals(recipeId)) {
                        mb.activeRecipe = r;
                        break;
                    }
                }
            }

            // Restore extra slots
            var slotsSection = data.getConfigurationSection(p + ".slots");
            if (slotsSection != null) {
                for (String slotKey : slotsSection.getKeys(false)) {
                    String sp = p + ".slots." + slotKey;
                    ItemStack slotFood = data.getItemStack(sp + ".food");
                    if (slotFood == null) continue;

                    ProcessingSlot slot = new ProcessingSlot();
                    slot.food = slotFood;
                    slot.startTime = data.getLong(sp + ".start");
                    slot.pausedMs = data.getLong(sp + ".paused");
                    slot.completedTime = data.getLong(sp + ".completed");
                    slot.qualityBonus = (float) data.getDouble(sp + ".quality");

                    String slotOwner = data.getString(sp + ".owner");
                    if (slotOwner != null) {
                        try { slot.ownerId = UUID.fromString(slotOwner); }
                        catch (IllegalArgumentException ignored) {}
                    }

                    String slotRecipeId = data.getString(sp + ".recipe");
                    if (slotRecipeId != null) {
                        for (MultiblockRecipe r : config.getRecipes(type)) {
                            if (r.getId().equals(slotRecipeId)) {
                                slot.recipe = r;
                                break;
                            }
                        }
                    }

                    mb.extraSlots.add(slot);
                }
            }

            // Verify structure only if chunk is loaded
            boolean chunkLoaded = world.isChunkLoaded(x >> 4, z >> 4);
            if (chunkLoaded && !isStructureIntact(mb)) {
                Location dropLoc = anchor.clone().add(0.5, 1.5, 0.5);
                if (mb.hasFood()) {
                    world.dropItemNaturally(dropLoc, mb.processingFood);
                }
                for (ProcessingSlot slot : mb.extraSlots) {
                    if (slot.hasFood()) {
                        world.dropItemNaturally(dropLoc, slot.food);
                    }
                }
                continue;
            }

            result.put(anchor, mb);
            loaded++;
        }
        if (loaded > 0) {
            MidgardCore.getInstance().getLogger().info(
                    "[FoodDecay] Restored " + loaded + " active multiblock(s).");
        }
        return result;
    }

    /**
     * Checks that all blocks of the multiblock still match expected materials.
     */
    public static boolean isStructureIntact(ProcessingMultiblock mb) {
        for (int i = 0; i < mb.blocks.size(); i++) {
            Location loc = mb.blocks.get(i);
            Material expected = i == 0
                    ? mb.type.getAnchorMaterial(mb.tier)
                    : mb.patternRotation.get(i - 1).material();
            if (loc.getBlock().getType() != expected) return false;
        }
        return true;
    }

    /**
     * Spawns or restores the item display entity for a processing multiblock.
     */
    public static void ensureFoodDisplay(ProcessingMultiblock mb, Location anchor,
                                          NamespacedKey entityTagKey) {
        if (mb.foodDisplay != null && mb.foodDisplay.isValid()) return;
        MultiblockAnimations.DisplayPlacement dp =
                MultiblockAnimations.getDisplayPlacement(mb.type);
        Location displayLoc = anchor.clone().add(dp.offX(), dp.offY(), dp.offZ());
        ItemDisplay display = (ItemDisplay) anchor.getWorld().spawnEntity(
                displayLoc, EntityType.ITEM_DISPLAY);
        display.setItemStack(mb.processingFood);
        float hs = dp.scale() / 2f;
        display.setTransformation(new Transformation(
                new Vector3f(-hs, 0, -hs),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(dp.scale(), dp.scale(), dp.scale()),
                new AxisAngle4f(0, 0, 0, 1)
        ));
        display.setBillboard(dp.billboard());
        display.setBrightness(new Display.Brightness(15, 15));
        display.setInterpolationDuration(20);
        display.getPersistentDataContainer().set(entityTagKey, PersistentDataType.BYTE, (byte) 1);
        mb.foodDisplay = display;
    }

    /**
     * Removes all entities in all worlds tagged with the entity tag key.
     */
    public static void removeOrphanedEntities(NamespacedKey entityTagKey) {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getPersistentDataContainer().has(entityTagKey)) {
                    entity.remove();
                    removed++;
                }
            }
        }
        if (removed > 0) {
            MidgardCore.getInstance().getLogger().info(
                    "[FoodDecay] Removed " + removed + " orphaned display entity(ies).");
        }
    }

    /**
     * Removes orphaned entities in a specific chunk that are not tracked by active multiblocks.
     */
    public static void cleanOrphanedInChunk(Chunk chunk, NamespacedKey entityTagKey,
                                             Map<Location, ProcessingMultiblock> activeMultiblocks) {
        for (Entity entity : chunk.getEntities()) {
            if (!entity.getPersistentDataContainer().has(entityTagKey)) continue;
            boolean tracked = false;
            for (ProcessingMultiblock mb : activeMultiblocks.values()) {
                if (entity.equals(mb.foodDisplay)) { tracked = true; break; }
            }
            if (!tracked) entity.remove();
        }
    }
}
