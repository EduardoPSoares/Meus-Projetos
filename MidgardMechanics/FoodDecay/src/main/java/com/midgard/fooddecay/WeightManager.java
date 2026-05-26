package com.midgard.fooddecay;

import com.midgard.core.MidgardCore;
import com.midgard.core.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * TFC-style weight and size system for food items.
 *
 * Stored PDC keys:
 * - {@code food_weight} as kilograms
 * - {@code food_weight_unit} as a unit marker used for legacy migration
 * - {@code food_size} as the configured size category
 */
public class WeightManager {

    static final String WEIGHT_LORE_MARKER = "\u00A78\u00A7l\u00A7r";

    public enum FoodSize {
        TINY("Min\u00FAsculo", "&7"),
        SMALL("Pequeno", "&a"),
        MEDIUM("M\u00E9dio", "&e"),
        LARGE("Grande", "&6"),
        HUGE("Enorme", "&c");

        private final String displayName;
        private final String color;

        FoodSize(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColor() {
            return color;
        }
    }

    private final FoodDecayConfig config;
    private final NamespacedKey weightKey;
    private final NamespacedKey weightUnitKey;
    private final NamespacedKey sizeKey;

    public WeightManager(FoodDecayConfig config) {
        this.config = config;

        MidgardCore core = MidgardCore.getInstance();
        this.weightKey = new NamespacedKey(core, "food_weight");
        this.weightUnitKey = new NamespacedKey(core, "food_weight_unit");
        this.sizeKey = new NamespacedKey(core, "food_size");
    }

    public boolean stampItem(ItemStack item) {
        if (!config.isWeightEnabled()) return false;
        if (item == null || item.getType().isAir()) return false;
        if (isStamped(item)) return false;

        double weightKg = config.getFoodWeightKg(item.getType());
        if (weightKg <= 0) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(weightKey, PersistentDataType.DOUBLE, weightKg);
        pdc.set(weightUnitKey, PersistentDataType.STRING, "kg");
        pdc.set(sizeKey, PersistentDataType.STRING, config.getFoodSize(item.getType()));

        item.setItemMeta(meta);
        updateLore(item);
        return true;
    }

    public boolean isStamped(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(weightKey, PersistentDataType.DOUBLE);
    }

    public double getWeight(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0D;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0D;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Double storedWeight = pdc.get(weightKey, PersistentDataType.DOUBLE);
        if (storedWeight == null) {
            return config.getFoodWeightKg(item.getType());
        }

        String storedUnit = pdc.get(weightUnitKey, PersistentDataType.STRING);
        if (storedUnit == null || !storedUnit.equalsIgnoreCase("kg")) {
            double convertedWeight = FoodDecayConfig.ouncesToKg(storedWeight);
            pdc.set(weightKey, PersistentDataType.DOUBLE, convertedWeight);
            pdc.set(weightUnitKey, PersistentDataType.STRING, "kg");
            item.setItemMeta(meta);
            return convertedWeight;
        }

        return storedWeight;
    }

    public String getSize(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return "MEDIUM";
        String stored = item.getItemMeta().getPersistentDataContainer().get(sizeKey, PersistentDataType.STRING);
        return stored != null ? stored : config.getFoodSize(item.getType());
    }

    public FoodSize parseFoodSize(String name) {
        if (name == null) return FoodSize.MEDIUM;
        try {
            return FoodSize.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return FoodSize.MEDIUM;
        }
    }

    public int getMaxStack(ItemStack item) {
        if (!config.isWeightEnabled()) return item.getMaxStackSize();

        double weightKg = getWeight(item);
        if (weightKg <= 0D) return item.getMaxStackSize();

        double maxKg = config.getWeightMaxKgPerStack();
        int maxStack = (int) Math.floor(maxKg / weightKg);
        return Math.max(1, Math.min(64, maxStack));
    }

    public ItemStack enforceStackLimit(ItemStack item) {
        if (!config.isWeightEnabled()) return null;
        if (item == null || item.getType().isAir()) return null;
        if (!isStamped(item)) return null;

        int maxStack = getMaxStack(item);
        if (item.getAmount() <= maxStack) return null;

        int excess = item.getAmount() - maxStack;
        item.setAmount(maxStack);

        ItemStack overflow = item.clone();
        overflow.setAmount(excess);
        return overflow;
    }

    public boolean canFitInContainer(ItemStack item, Material containerType) {
        if (!config.isWeightEnabled()) return true;
        if (item == null) return true;

        FoodSize size = parseFoodSize(getSize(item));
        Set<String> blocked = config.getContainerSizeRestrictions(containerType);
        if ((blocked == null || blocked.isEmpty()) && containerType.name().endsWith("_SHULKER_BOX")) {
            blocked = config.getContainerSizeRestrictions(Material.SHULKER_BOX);
        }
        if (blocked == null || blocked.isEmpty()) return true;

        return !blocked.contains(size.name());
    }

    public void updateLore(ItemStack item) {
        if (!config.isWeightEnabled()) return;
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<Component> cleanedLore = new ArrayList<>();
        List<Component> currentLore = meta.lore();
        if (currentLore != null) {
            for (Component line : currentLore) {
                String plain = LegacyComponentSerializer.legacySection().serialize(line);
                if (!plain.contains(WEIGHT_LORE_MARKER)) {
                    cleanedLore.add(line);
                }
            }
        }

        if (isStamped(item)) {
            String sizeName = getSize(item);
            FoodSize size = parseFoodSize(sizeName);
            String sizeDisplay = config.getWeightSizeDisplayName(sizeName);
            String sizeColor = config.getWeightSizeColor(sizeName);

            if (sizeDisplay == null) sizeDisplay = size.getDisplayName();
            if (sizeColor == null) sizeColor = size.getColor();

            cleanedLore.add(toComponent(WeightFormatUtil.buildLoreLine(
                    WEIGHT_LORE_MARKER,
                    sizeColor,
                    getWeight(item),
                    sizeDisplay,
                    getMaxStack(item)
            )));
        }

        meta.lore(cleanedLore);
        item.setItemMeta(meta);
    }

    public void enforceInventory(Player player) {
        if (!config.isWeightEnabled()) return;

        Inventory inventory = player.getInventory();
        boolean notified = false;
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType().isAir()) continue;
            if (!isStamped(item)) continue;

            ItemStack overflow = enforceStackLimit(item);
            if (overflow == null) continue;

            boolean placed = false;
            for (int j = i + 1; j < inventory.getSize(); j++) {
                ItemStack slot = inventory.getItem(j);
                if (slot == null || slot.getType().isAir()) {
                    inventory.setItem(j, overflow);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                for (int j = 0; j < i; j++) {
                    ItemStack slot = inventory.getItem(j);
                    if (slot == null || slot.getType().isAir()) {
                        inventory.setItem(j, overflow);
                        placed = true;
                        break;
                    }
                }
            }
            if (!placed) {
                player.getWorld().dropItemNaturally(player.getLocation(), overflow);
            }
            if (!notified) {
                notified = true;
                player.sendMessage(MessageUtils.toComponent(
                        config.msg("weight-stack-overflow")
                                .replace("{max}", String.valueOf(getMaxStack(item)))
                ));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 0.8f);
            }
        }
    }

    public boolean blockContainerInsert(Player player, ItemStack item, Material containerType) {
        if (!config.isWeightEnabled()) return false;
        if (!config.isWeightContainerRestrictionsEnabled()) return false;
        if (item == null || !isStamped(item)) return false;

        if (canFitInContainer(item, containerType)) {
            return false;
        }

        String sizeName = getSize(item);
        FoodSize size = parseFoodSize(sizeName);
        String sizeDisplay = config.getWeightSizeDisplayName(sizeName);
        if (sizeDisplay == null) sizeDisplay = size.getDisplayName();

        player.sendMessage(MessageUtils.toComponent(
                config.msg("weight-size-blocked").replace("{size}", sizeDisplay)
        ));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
        return true;
    }

    private Component toComponent(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
