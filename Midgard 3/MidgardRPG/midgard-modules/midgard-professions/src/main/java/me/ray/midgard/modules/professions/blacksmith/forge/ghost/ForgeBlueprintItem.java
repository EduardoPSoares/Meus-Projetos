package me.ray.midgard.modules.professions.blacksmith.forge.ghost;

import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeTier;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeTemplate;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Creates and identifies forge blueprint items.
 * A blueprint is a placeable item that, when used on the ground,
 * starts a forge construction session (schematic building mini-game).
 */
public final class ForgeBlueprintItem {

    private static final String PDC_KEY_BLUEPRINT = "forge_blueprint";
    private static final String PDC_KEY_TIER = "forge_blueprint_tier";
    private static final String PDC_KEY_TEMPLATE = "forge_blueprint_template";

    private ForgeBlueprintItem() {}

    private static String msg(String key) {
        var m = ProfessionsModule.getInstance();
        return m != null ? m.getMessage("forge.blueprint." + key) : key;
    }

    /**
     * Creates a forge blueprint item for the given template.
     */
    public static ItemStack create(ForgeTemplate template) {
        ForgeTier tier = template.getTier();
        Material baseMaterial = getMaterialForTier(tier);

        return new ItemBuilder(baseMaterial)
                .setName(tier.getDisplayName() + " <gray>- <white>" + template.getName())
                .addLore("")
                .addLore("<gray>" + msg("description"))
                .addLore("<gray>" + msg("place_line1"))
                .addLore("<gray>" + msg("place_line2"))
                .addLore("")
                .addLore("<dark_gray>" + msg("required_level") + " <white>" + template.getRequiredLevel())
                .addLore("<dark_gray>" + msg("size") + " <white>" + tier.getWidth() + "x" + tier.getHeight() + "x" + tier.getDepth())
                .addLore("")
                .addLore("<yellow>" + msg("click_to_build"))
                .glow()
                .pdc(getBlueprintKey(), PersistentDataType.BOOLEAN, true)
                .pdc(getTierKey(), PersistentDataType.STRING, tier.name())
                .pdc(getTemplateKey(), PersistentDataType.STRING, template.getTemplateId().toString())
                .build();
    }

    /**
     * Checks if an ItemStack is a forge blueprint.
     */
    public static boolean isBlueprint(ItemStack item) {
        if (item == null || !item.hasItemMeta()) { return false; }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(getBlueprintKey(), PersistentDataType.BOOLEAN);
    }

    /**
     * Gets the ForgeTier from a blueprint item. Returns null if not a blueprint.
     */
    public static ForgeTier getTier(ItemStack item) {
        if (item == null || !item.hasItemMeta()) { return null; }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String tierName = pdc.get(getTierKey(), PersistentDataType.STRING);
        if (tierName == null) { return null; }
        try {
            return ForgeTier.valueOf(tierName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Gets the template UUID from a blueprint item. Returns null if not present.
     */
    public static java.util.UUID getTemplateId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) { return null; }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(getTemplateKey(), PersistentDataType.STRING);
        if (id == null) { return null; }
        try {
            return java.util.UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Material getMaterialForTier(ForgeTier tier) {
        return switch (tier) {
            case BASIC -> Material.PAPER;
            case INTERMEDIATE -> Material.MAP;
            case ADVANCED -> Material.FILLED_MAP;
            case MASTER -> Material.KNOWLEDGE_BOOK;
            case LEGENDARY -> Material.ENCHANTED_BOOK;
        };
    }

    private static NamespacedKey getBlueprintKey() {
        return new NamespacedKey(ItemModule.getInstance().getPlugin(), PDC_KEY_BLUEPRINT);
    }

    private static NamespacedKey getTierKey() {
        return new NamespacedKey(ItemModule.getInstance().getPlugin(), PDC_KEY_TIER);
    }

    private static NamespacedKey getTemplateKey() {
        return new NamespacedKey(ItemModule.getInstance().getPlugin(), PDC_KEY_TEMPLATE);
    }
}
