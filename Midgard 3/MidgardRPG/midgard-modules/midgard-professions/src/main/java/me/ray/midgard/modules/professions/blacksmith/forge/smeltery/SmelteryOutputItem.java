package me.ray.midgard.modules.professions.blacksmith.forge.smeltery;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import me.ray.midgard.modules.professions.ProfessionsModule;

import java.util.ArrayList;
import java.util.List;

/**
 * Cria itens de saída da Smeltery (lingotes e blocos).
 * Metais base produzem itens vanilla; ligas produzem itens customizados com PDC.
 */
public class SmelteryOutputItem {

    private static final MiniMessage mm = MiniMessage.miniMessage();
    private static NamespacedKey ALLOY_KEY;
    private static NamespacedKey IS_SMELTERY_OUTPUT_KEY;

    public static void init(JavaPlugin plugin) {
        ALLOY_KEY = new NamespacedKey(plugin, "smeltery_alloy");
        IS_SMELTERY_OUTPUT_KEY = new NamespacedKey(plugin, "is_smeltery_output");
    }

    private static String msg(String key) {
        var m = ProfessionsModule.getInstance();
        return m != null ? m.getMessage("smeltery.output." + key) : key;
    }

    /**
     * Cria um lingote do metal/liga especificado.
     * Metais base retornam o item vanilla (IRON_INGOT, GOLD_INGOT, etc.).
     * Ligas retornam um item customizado com PDC.
     */
    public static ItemStack createIngot(MoltenMetal metal) {
        if (!metal.isAlloy() && metal.getSourceItem() != null) {
            return new ItemStack(metal.getSourceItem());
        }

        ItemStack item = new ItemStack(getIngotBaseMaterial(metal));
        ItemMeta meta = item.getItemMeta();

        String colorTag = getColorTag(metal);
        String displayName = metal.getDisplayName()
                .replace(" Fundido", "").replace(" Fundida", "");
        meta.displayName(mm.deserialize(colorTag + msg("ingot_prefix") + displayName));

        List<Component> lore = new ArrayList<>();
        lore.add(mm.deserialize(" "));
        lore.add(mm.deserialize(metal.getFormattedName()));
        lore.add(mm.deserialize("<gray>" + msg("hardness") + " <white>" + String.format("%.1f", metal.getHardness())));
        lore.add(mm.deserialize(" "));
        lore.add(mm.deserialize("<dark_gray>" + msg("alloy_label")));
        lore.add(mm.deserialize("<dark_gray>" + msg("use_in_forge")));
        meta.lore(lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(IS_SMELTERY_OUTPUT_KEY, PersistentDataType.BYTE, (byte) 1);
        pdc.set(ALLOY_KEY, PersistentDataType.STRING, metal.name());

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Cria um bloco do metal/liga especificado.
     * Metais base retornam blocos vanilla.
     * Ligas retornam blocos customizados com PDC.
     */
    public static ItemStack createBlock(MoltenMetal metal) {
        Material blockMat = metal.getVisualBlock();

        if (!metal.isAlloy()) {
            return new ItemStack(blockMat);
        }

        ItemStack item = new ItemStack(blockMat);
        ItemMeta meta = item.getItemMeta();

        String colorTag = getColorTag(metal);
        String displayName = metal.getDisplayName()
                .replace(" Fundido", "").replace(" Fundida", "");
        meta.displayName(mm.deserialize(colorTag + msg("block_prefix") + displayName));

        List<Component> lore = new ArrayList<>();
        lore.add(mm.deserialize(" "));
        lore.add(mm.deserialize(metal.getFormattedName()));
        lore.add(mm.deserialize("<gray>" + msg("hardness") + " <white>" + String.format("%.1f", metal.getHardness())));
        lore.add(mm.deserialize(" "));
        lore.add(mm.deserialize("<dark_gray>" + msg("alloy_block_label")));
        lore.add(mm.deserialize("<dark_gray>" + msg("use_in_forge")));
        meta.lore(lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(IS_SMELTERY_OUTPUT_KEY, PersistentDataType.BYTE, (byte) 1);
        pdc.set(ALLOY_KEY, PersistentDataType.STRING, metal.name());

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isSmelteryOutput(ItemStack item) {
        if (item == null || !item.hasItemMeta()) { return false; }
        if (IS_SMELTERY_OUTPUT_KEY == null) { return false; }
        return item.getItemMeta().getPersistentDataContainer()
                .has(IS_SMELTERY_OUTPUT_KEY, PersistentDataType.BYTE);
    }

    public static MoltenMetal getAlloyType(ItemStack item) {
        if (!isSmelteryOutput(item)) { return null; }
        String name = item.getItemMeta().getPersistentDataContainer()
                .get(ALLOY_KEY, PersistentDataType.STRING);
        if (name == null) { return null; }
        try {
            return MoltenMetal.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Material getIngotBaseMaterial(MoltenMetal metal) {
        return switch (metal) {
            case BRONZE, STEEL, ELECTRUM, OBSIDIAN_ALLOY -> Material.IRON_INGOT;
            case MANYULLYN, KNIGHTSLIME -> Material.NETHERITE_SCRAP;
            case ROSE_GOLD -> Material.GOLD_INGOT;
            default -> Material.IRON_INGOT;
        };
    }

    private static String getColorTag(MoltenMetal metal) {
        return switch (metal) {
            case IRON -> "<white>";
            case GOLD -> "<gold>";
            case COPPER -> "<#E87E04>";
            case BRONZE -> "<#CD7F32>";
            case STEEL -> "<gray>";
            case DIAMOND -> "<aqua>";
            case NETHERITE_SCRAP -> "<dark_red>";
            case MANYULLYN -> "<dark_purple>";
            case OBSIDIAN_ALLOY -> "<dark_gray>";
            case ROSE_GOLD -> "<#F5A0C0>";
            case ELECTRUM -> "<yellow>";
            case KNIGHTSLIME -> "<dark_aqua>";
            case EMERALD -> "<green>";
            case AMETHYST -> "<light_purple>";
            default -> "<white>";
        };
    }
}
