package me.ray.midgard.modules.professions.blacksmith.forge.smeltery.ghost;

import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryTier;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.schematic.SmelteryTemplate;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Cria e identifica itens de blueprint de smeltery.
 * Um blueprint é um item que, ao ser usado no chão,
 * inicia uma sessão de construção de fundição (mini-game de schematic).
 */
public final class SmelteryBlueprintItem {

    private static final String PDC_KEY_BLUEPRINT = "smeltery_blueprint";
    private static final String PDC_KEY_TIER = "smeltery_blueprint_tier";
    private static final String PDC_KEY_TEMPLATE = "smeltery_blueprint_template";

    private SmelteryBlueprintItem() {}

    private static String msg(String key) {
        var m = ProfessionsModule.getInstance();
        return m != null ? m.getMessage("smeltery.blueprint." + key) : key;
    }

    /**
     * Cria um item de blueprint para o template dado.
     */
    public static ItemStack create(SmelteryTemplate template) {
        SmelteryTier tier = template.getTier();
        Material baseMaterial = getMaterialForTier(tier);

        return new ItemBuilder(baseMaterial)
                .setName(tier.getFormattedName() + " <gray>- <white>" + template.getName())
                .addLore("")
                .addLore("<gray>" + msg("description"))
                .addLore("<gray>" + msg("place_line1"))
                .addLore("<gray>" + msg("place_line2"))
                .addLore("")
                .addLore("<dark_gray>" + msg("required_level") + " <white>" + template.getRequiredLevel())
                .addLore("<dark_gray>" + msg("size") + " <white>" + tier.getTotalWidth() + "x" + tier.getTotalHeight() + "x" + tier.getTotalDepth())
                .addLore("")
                .addLore("<yellow>" + msg("click_to_build"))
                .glow()
                .pdc(getBlueprintKey(), PersistentDataType.BOOLEAN, true)
                .pdc(getTierKey(), PersistentDataType.STRING, tier.name())
                .pdc(getTemplateKey(), PersistentDataType.STRING, template.getTemplateId().toString())
                .build();
    }

    /**
     * Cria um item de blueprint para o tier dado (fallback sem template).
     */
    public static ItemStack create(SmelteryTier tier) {
        Material baseMaterial = getMaterialForTier(tier);

        return new ItemBuilder(baseMaterial)
                .setName(tier.getFormattedName())
                .addLore("")
                .addLore("<gray>" + msg("description"))
                .addLore("<gray>" + msg("place_line1"))
                .addLore("<gray>" + msg("place_line2"))
                .addLore("")
                .addLore("<dark_gray>" + msg("required_level") + " <white>" + tier.getRequiredProfessionLevel())
                .addLore("<dark_gray>" + msg("size") + " <white>" + tier.getTotalWidth() + "x" + tier.getTotalHeight() + "x" + tier.getTotalDepth())
                .addLore("")
                .addLore("<yellow>" + msg("click_to_build"))
                .glow()
                .pdc(getBlueprintKey(), PersistentDataType.BOOLEAN, true)
                .pdc(getTierKey(), PersistentDataType.STRING, tier.name())
                .build();
    }

    /**
     * Verifica se um ItemStack é um blueprint de smeltery.
     */
    public static boolean isBlueprint(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(getBlueprintKey(), PersistentDataType.BOOLEAN);
    }

    /**
     * Obtém o SmelteryTier de um item de blueprint. Retorna null se não for blueprint.
     */
    public static SmelteryTier getTier(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String tierName = pdc.get(getTierKey(), PersistentDataType.STRING);
        if (tierName == null) {
            return null;
        }
        try {
            return SmelteryTier.valueOf(tierName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Obtém o UUID do template de um item de blueprint. Retorna null se não presente.
     */
    public static java.util.UUID getTemplateId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(getTemplateKey(), PersistentDataType.STRING);
        if (id == null) {
            return null;
        }
        try {
            return java.util.UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Material getMaterialForTier(SmelteryTier tier) {
        return switch (tier) {
            case SMALL -> Material.PAPER;
            case MEDIUM -> Material.MAP;
            case LARGE -> Material.FILLED_MAP;
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
