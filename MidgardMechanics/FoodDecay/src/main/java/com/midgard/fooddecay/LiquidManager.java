package com.midgard.fooddecay;

import com.midgard.core.MidgardCore;
import com.midgard.core.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * TFC-style liquid container system using millibuckets (mB).
 * Items can hold liquids (WATER, MILK, VINEGAR, HONEY, BROTH, OIL).
 * Right-click water sources or cauldrons to fill, Shift+right-click to pour.
 * Lore shows a visual bar with liquid type and amount.
 */
public class LiquidManager {

    private static final String LIQUID_LORE_MARKER = "&0&1&0&1";
    private static final String LEGACY_LIQUID_LORE_MARKER = "\u00A70\u00A71\u00A70\u00A71";
    private static final String OLD_LIQUID_LORE_MARKER = "\u200B\u200C\u200B";
    private static final String OLD_LEGACY_LIQUID_LORE_MARKER = "\u00A78\u00A7n\u00A7r";
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private final FoodDecayConfig config;
    private final NamespacedKey liquidTypeKey;
    private final NamespacedKey liquidAmountKey;
    private final NamespacedKey liquidCapacityKey;

    public LiquidManager(FoodDecayConfig config) {
        this.config = config;
        MidgardCore core = MidgardCore.getInstance();
        this.liquidTypeKey = new NamespacedKey(core, "liquid_type");
        this.liquidAmountKey = new NamespacedKey(core, "liquid_amount");
        this.liquidCapacityKey = new NamespacedKey(core, "liquid_capacity");
    }

    // =====================================================
    // PDC Helpers
    // =====================================================

    /** Checks if an item is a valid liquid container (has capacity PDC). */
    public boolean isContainer(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(liquidCapacityKey, PersistentDataType.INTEGER);
    }

    /** Gets the liquid type stored in this container, or null if empty. */
    public String getLiquidType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(liquidTypeKey, PersistentDataType.STRING);
    }

    /** Gets the current mB stored. */
    public int getLiquidAmount(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(liquidAmountKey, PersistentDataType.INTEGER, 0);
    }

    /** Gets the max mB capacity. */
    public int getCapacity(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(liquidCapacityKey, PersistentDataType.INTEGER, 0);
    }

    /** Returns true if the container is full. */
    public boolean isFull(ItemStack item) {
        return getLiquidAmount(item) >= getCapacity(item);
    }

    /** Returns true if the container is empty (0 mB). */
    public boolean isEmpty(ItemStack item) {
        return getLiquidAmount(item) <= 0;
    }

    // =====================================================
    // Stamping — make an item into a liquid container
    // =====================================================

    /**
     * Stamps a container item with liquid PDC data if it's a recognized container material.
     * Called when a player obtains or crafts a container item.
     * Returns true if the item was stamped.
     */
    public boolean stampContainer(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        if (isContainer(item)) return false; // already stamped

        int capacity = config.getLiquidContainerCapacity(item.getType());
        if (capacity <= 0) return false; // not a registered container type

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(liquidCapacityKey, PersistentDataType.INTEGER, capacity);
        pdc.set(liquidAmountKey, PersistentDataType.INTEGER, 0);
        // No liquid type set until filled
        item.setItemMeta(meta);

        updateLore(item);
        return true;
    }

    // =====================================================
    // Fill / Pour
    // =====================================================

    /**
     * Fills the container with as much liquid as possible.
     * Returns the amount actually filled (may be less if near capacity, or 0 if incompatible).
     */
    public int fill(ItemStack item, String liquidType, int amount) {
        if (!isContainer(item) || amount <= 0) return 0;

        String current = getLiquidType(item);
        int currentAmount = getLiquidAmount(item);
        int capacity = getCapacity(item);

        // If container has a different liquid, can't mix
        if (current != null && !current.isEmpty() && currentAmount > 0
                && !current.equalsIgnoreCase(liquidType)) {
            return 0;
        }

        int space = capacity - currentAmount;
        if (space <= 0) return 0;

        int toFill = Math.min(amount, space);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(liquidTypeKey, PersistentDataType.STRING, liquidType.toUpperCase());
        pdc.set(liquidAmountKey, PersistentDataType.INTEGER, currentAmount + toFill);
        item.setItemMeta(meta);
        updateLore(item);

        return toFill;
    }

    /**
     * Drains liquid from the container.
     * Returns the amount actually drained.
     */
    public int drain(ItemStack item, int amount) {
        if (!isContainer(item) || amount <= 0) return 0;

        int currentAmount = getLiquidAmount(item);
        if (currentAmount <= 0) return 0;

        int toDrain = Math.min(amount, currentAmount);
        int remaining = currentAmount - toDrain;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(liquidAmountKey, PersistentDataType.INTEGER, remaining);
        if (remaining <= 0) {
            pdc.remove(liquidTypeKey); // clear type when empty
        }
        item.setItemMeta(meta);
        updateLore(item);

        return toDrain;
    }

    // =====================================================
    // Interactions
    // =====================================================

    /**
     * Handles player right-clicking a water source, cauldron, or water cauldron
     * with a liquid container. Returns true if the event should be cancelled.
     */
    public boolean onContainerInteract(Player player, Block block, ItemStack handItem, boolean isSneaking) {
        if (!config.isLiquidContainersEnabled()) return false;
        if (!isContainer(handItem)) return false;
        if (block == null) return false;

        Material blockType = block.getType();

        // ─── Fill from water source ───
        if (blockType == Material.WATER) {
            return tryFillFromWater(player, handItem, "WATER", 1000);
        }

        // ─── Fill from water cauldron ───
        if (blockType == Material.WATER_CAULDRON) {
            if (isSneaking) {
                return tryPourIntoCauldron(player, block, handItem);
            }
            return tryFillFromCauldron(player, block, handItem, "WATER");
        }

        // ─── Fill from lava cauldron → not allowed, safety ───
        // ─── Fill from empty cauldron (pour into) ───
        if (blockType == Material.CAULDRON) {
            if (isSneaking) {
                return tryPourIntoCauldron(player, block, handItem);
            }
        }

        // ─── Empty hand on container → just show info ───
        return false;
    }

    /**
     * Handles right-clicking in air (or non-interactive block) with a container
     * while sneaking → pour out liquid on the ground.
     */
    public boolean onContainerPour(Player player, ItemStack handItem) {
        if (!config.isLiquidContainersEnabled()) return false;
        if (!isContainer(handItem)) return false;
        if (isEmpty(handItem)) return false;

        // Capture liquid type BEFORE draining (drain may empty the container, losing the type)
        String liquidType = getLiquidType(handItem);
        int drained = drain(handItem, config.getLiquidPourAmount());
        if (drained > 0) {
            player.playSound(player.getLocation(), Sound.ITEM_BUCKET_EMPTY, 0.6f, 1.2f);
            String msg = config.msg("liquid-poured");
            if (msg != null && !msg.isEmpty()) {
                player.sendActionBar(MessageUtils.toComponent(
                        msg.replace("{amount}", String.valueOf(drained))
                           .replace("{liquid}", getLiquidDisplayName(liquidType))
                ));
            }
            return true;
        }
        return false;
    }

    private boolean tryFillFromWater(Player player, ItemStack item, String liquidType, int amount) {
        if (isFull(item)) {
            sendMsg(player, "liquid-container-full");
            return true;
        }
        String current = getLiquidType(item);
        if (current != null && !current.isEmpty() && getLiquidAmount(item) > 0
                && !current.equalsIgnoreCase(liquidType)) {
            sendMsg(player, "liquid-incompatible");
            return true;
        }

        int filled = fill(item, liquidType, amount);
        if (filled > 0) {
            player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 0.6f, 1.2f);
            String msg = config.msg("liquid-filled");
            if (msg != null && !msg.isEmpty()) {
                player.sendActionBar(MessageUtils.toComponent(
                        msg.replace("{amount}", String.valueOf(filled))
                           .replace("{liquid}", getLiquidDisplayName(liquidType))
                           .replace("{current}", String.valueOf(getLiquidAmount(item)))
                           .replace("{max}", String.valueOf(getCapacity(item)))
                ));
            }
        }
        return true;
    }

    private boolean tryFillFromCauldron(Player player, Block block, ItemStack item, String liquidType) {
        if (isFull(item)) {
            sendMsg(player, "liquid-container-full");
            return true;
        }
        if (!(block.getBlockData() instanceof Levelled levelled)) return false;

        int cauldronLevel = levelled.getLevel();
        if (cauldronLevel <= 0) return false;

        // Each cauldron level = 333 mB (3 levels = ~1000 mB = 1 bucket)
        int mbPerLevel = config.getLiquidMbPerCauldronLevel();
        int available = cauldronLevel * mbPerLevel;
        int space = getCapacity(item) - getLiquidAmount(item);
        int toTransfer = Math.min(available, space);

        if (toTransfer <= 0) return true;

        // Calculate how many cauldron levels to consume
        int levelsConsumed = (int) Math.ceil((double) toTransfer / mbPerLevel);
        levelsConsumed = Math.min(levelsConsumed, cauldronLevel);
        int actualMb = levelsConsumed * mbPerLevel;
        actualMb = Math.min(actualMb, space);

        int filled = fill(item, liquidType, actualMb);
        if (filled > 0) {
            int newLevel = cauldronLevel - levelsConsumed;
            if (newLevel <= 0) {
                block.setType(Material.CAULDRON);
            } else {
                levelled.setLevel(newLevel);
                block.setBlockData(levelled);
            }
            player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 0.6f, 1.2f);
            String msg = config.msg("liquid-filled");
            if (msg != null && !msg.isEmpty()) {
                player.sendActionBar(MessageUtils.toComponent(
                        msg.replace("{amount}", String.valueOf(filled))
                           .replace("{liquid}", getLiquidDisplayName(liquidType))
                           .replace("{current}", String.valueOf(getLiquidAmount(item)))
                           .replace("{max}", String.valueOf(getCapacity(item)))
                ));
            }
        }
        return true;
    }

    private boolean tryPourIntoCauldron(Player player, Block block, ItemStack item) {
        if (isEmpty(item)) {
            sendMsg(player, "liquid-container-empty");
            return true;
        }

        String liquidType = getLiquidType(item);
        // Only water can be poured into cauldrons
        if (liquidType == null || !liquidType.equalsIgnoreCase("WATER")) {
            sendMsg(player, "liquid-cauldron-water-only");
            return true;
        }

        int mbPerLevel = config.getLiquidMbPerCauldronLevel();
        int currentLevel;
        int maxLevel;

        if (block.getType() == Material.CAULDRON) {
            currentLevel = 0;
            maxLevel = 3;
        } else if (block.getBlockData() instanceof Levelled levelled) {
            currentLevel = levelled.getLevel();
            maxLevel = levelled.getMaximumLevel();
        } else {
            return false;
        }

        if (currentLevel >= maxLevel) {
            sendMsg(player, "liquid-cauldron-full");
            return true;
        }

        int levelsToFill = maxLevel - currentLevel;
        int mbNeeded = levelsToFill * mbPerLevel;
        int available = getLiquidAmount(item);
        int toTransfer = Math.min(mbNeeded, available);
        int levelsAdded = toTransfer / mbPerLevel;
        if (levelsAdded <= 0) {
            sendMsg(player, "liquid-not-enough");
            return true;
        }

        int mbDrained = levelsAdded * mbPerLevel;
        drain(item, mbDrained);

        int newLevel = currentLevel + levelsAdded;
        if (block.getType() == Material.CAULDRON && newLevel > 0) {
            block.setType(Material.WATER_CAULDRON);
        }
        if (block.getBlockData() instanceof Levelled levelled) {
            levelled.setLevel(Math.min(newLevel, maxLevel));
            block.setBlockData(levelled);
        }

        player.playSound(player.getLocation(), Sound.ITEM_BUCKET_EMPTY, 0.6f, 1.2f);
        String msg = config.msg("liquid-poured-cauldron");
        if (msg != null && !msg.isEmpty()) {
            player.sendActionBar(MessageUtils.toComponent(
                    msg.replace("{amount}", String.valueOf(mbDrained))
            ));
        }
        return true;
    }

    // =====================================================
    // Lore Display
    // =====================================================

    /**
     * Updates the lore on a liquid container to show current volume and type.
     */
    public void updateLore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<Component> lore = meta.lore();
        List<Component> cleaned = new ArrayList<>();
        if (lore != null) {
            for (Component line : lore) {
                String legacy = LegacyComponentSerializer.legacySection().serialize(line);
                String plain = PLAIN_TEXT.serialize(line);
                if (!isLiquidLoreLine(legacy, plain)) {
                    cleaned.add(line);
                }
            }
        }

        int amount = getLiquidAmount(item);
        int capacity = getCapacity(item);
        String type = getLiquidType(item);

        if (capacity > 0) {
            int fillPercent = (int) Math.round((double) amount * 100D / capacity);
            String liquidName = (type != null && !type.isEmpty()) ? getLiquidDisplayName(type) : "Vazio";
            String liquidColor = (type != null && !type.isEmpty()) ? getLiquidColor(type) : "&8";
            String bar = buildLiquidBar(amount, capacity, liquidColor);
            String percentColor = amount <= 0 ? "&8" : amount >= capacity ? "&a" : "&b";

            cleaned.add(MessageUtils.toComponent(
                    LIQUID_LORE_MARKER + "&8Conteudo"));
            cleaned.add(MessageUtils.toComponent(
                    LIQUID_LORE_MARKER + " &7Tipo: " + liquidColor + liquidName));
            cleaned.add(MessageUtils.toComponent(
                    LIQUID_LORE_MARKER + " &7Volume: &f" + amount + "&8/&f" + capacity + " &7mB"));
            cleaned.add(MessageUtils.toComponent(
                    LIQUID_LORE_MARKER + " &7Carga: " + percentColor + fillPercent + "%"));
            cleaned.add(MessageUtils.toComponent(
                    LIQUID_LORE_MARKER + "&r"));
            cleaned.add(MessageUtils.toComponent(
                    LIQUID_LORE_MARKER + " &8[" + bar + "&8]"));
        }

        meta.lore(cleaned);
        item.setItemMeta(meta);
    }

    private boolean isLiquidLoreLine(String legacy, String plain) {
        if (legacy.contains(LEGACY_LIQUID_LORE_MARKER)
                || legacy.contains(OLD_LEGACY_LIQUID_LORE_MARKER)
                || plain.contains(OLD_LIQUID_LORE_MARKER)) {
            return true;
        }

        String normalized = plain
                .replace(OLD_LIQUID_LORE_MARKER, "")
                .replaceFirst("^[^\\p{L}\\[]+", "")
                .trim();
        if (normalized.isEmpty()) {
            return false;
        }

        String lower = normalized.toLowerCase();
        if (lower.startsWith("liquido:")
                || lower.startsWith("líquido:")
                || lower.startsWith("tipo:")
                || lower.startsWith("volume:")
                || lower.startsWith("carga:")
                || lower.equals("conteudo")
                || lower.equals("conteúdo")) {
            return true;
        }

        return normalized.matches("^\\[?[█░▒▓\\-—–.\\s]+\\]?$");
    }

    private String buildLiquidBar(int amount, int capacity, String liquidColor) {
        int bars = 14;
        int filled = (capacity > 0) ? (int) Math.round((double) amount / capacity * bars) : 0;
        filled = Math.max(0, Math.min(bars, filled));

        String color = (amount <= 0) ? "&8" : liquidColor;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filled; i++) sb.append('\u2588');
        if (filled > 0) {
            sb.insert(0, color);
        }
        sb.append("&8");
        for (int i = filled; i < bars; i++) sb.append('\u2591');
        return sb.toString();
    }

    // =====================================================
    // Liquid Registry
    // =====================================================

    /** Gets the display name for a liquid type. */
    public String getLiquidDisplayName(String type) {
        if (type == null) return "Vazio";
        return config.getLiquidDisplayName(type.toUpperCase());
    }

    /** Gets the color code for a liquid type (for lore). */
    public String getLiquidColor(String type) {
        if (type == null) return "&8";
        return config.getLiquidColor(type.toUpperCase());
    }

    // =====================================================
    // Utility
    // =====================================================

    private void sendMsg(Player player, String key) {
        String msg = config.msg(key);
        if (msg != null && !msg.isEmpty()) {
            player.sendActionBar(MessageUtils.toComponent(msg));
        }
    }

    public NamespacedKey getLiquidTypeKey() { return liquidTypeKey; }
    public NamespacedKey getLiquidAmountKey() { return liquidAmountKey; }
    public NamespacedKey getLiquidCapacityKey() { return liquidCapacityKey; }
}
