package com.midgard.fooddecay.multiblock;

import com.midgard.fooddecay.FoodDecayConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles quality tier calculation and application to processed food items.
 * Quality tiers are fully configurable via config.yml.
 */
public final class MultiblockQuality {

    private MultiblockQuality() {}

    public static int getQualityTier(float qualityBonus, FoodDecayConfig config) {
        if (qualityBonus >= config.getQualityTier2Threshold()) return 2;
        if (qualityBonus >= config.getQualityTier1Threshold()) return 1;
        return 0;
    }

    public static String getQualityPrefix(int tier, FoodDecayConfig config) {
        return switch (tier) {
            case 2 -> config.getQualityTier2Prefix();
            case 1 -> config.getQualityTier1Prefix();
            default -> config.getQualityTier0Prefix();
        };
    }

    public static String getQualityLore(int tier, FoodDecayConfig config) {
        return switch (tier) {
            case 2 -> config.getQualityTier2Lore();
            case 1 -> config.getQualityTier1Lore();
            default -> config.getQualityTier0Lore();
        };
    }

    /**
     * Applies quality prefix and lore to the given item.
     * Returns the modified item.
     */
    public static ItemStack applyQuality(ItemStack item, float qualityBonus, FoodDecayConfig config) {
        int tier = getQualityTier(qualityBonus, config);
        ItemStack result = item.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) return result;

        // Add quality prefix to display name
        String prefix = getQualityPrefix(tier, config);
        Component displayName = meta.hasDisplayName()
                ? meta.displayName()
                : Component.translatable(result.translationKey());

        String serialized = LegacyComponentSerializer.legacyAmpersand()
                .serialize(displayName);
        meta.displayName(LegacyComponentSerializer.legacyAmpersand()
                .deserialize(prefix + serialized));

        // Add quality lore
        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(LegacyComponentSerializer.legacyAmpersand()
                .deserialize(getQualityLore(tier, config)));
        meta.lore(lore);

        result.setItemMeta(meta);
        return result;
    }
}
