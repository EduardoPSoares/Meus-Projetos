package me.ray.midgard.modules.spells.manager;

import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.PDCUtils;
import me.ray.midgard.modules.spells.SpellsModule;
import me.ray.midgard.modules.spells.obj.Spell;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ScrollManager {

    private final SpellsModule module;

    private static final String PDC_SCROLL_TYPE = "midgard_scroll_type";
    private static final String PDC_SCROLL_TARGET = "midgard_scroll_target";

    public enum ScrollType {
        UNLEARNING,
        LEARNING,
        RESPEC;

        public static ScrollType fromString(String s) {
            try {
                return valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public ScrollManager(SpellsModule module) {
        this.module = module;
    }

    public ItemStack createScroll(ScrollType type, String targetSpellId) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) { return item; }

        PDCUtils.setString(meta, PDC_SCROLL_TYPE, type.name());
        if (targetSpellId != null && !targetSpellId.isEmpty()) {
            PDCUtils.setString(meta, PDC_SCROLL_TARGET, targetSpellId);
        }

        // Set display name
        String nameKey = "scrolls.names." + type.name().toLowerCase();
        String displayName = module.getMessage(nameKey);
        if (targetSpellId != null && !targetSpellId.isEmpty()) {
            Spell spell = module.getSpellManager().getSpell(targetSpellId);
            String spellName = spell != null ? spell.getDisplayName() : targetSpellId;
            displayName = displayName.replace("%spell%", spellName);
        }
        meta.displayName(MessageUtils.parse(displayName));

        // Set lore
        String loreKey = "scrolls.lore." + type.name().toLowerCase();
        List<String> loreLines = module.getMessageList(loreKey);
        if (!loreLines.isEmpty()) {
            List<net.kyori.adventure.text.Component> loreParsed = new ArrayList<>();
            for (String line : loreLines) {
                if (targetSpellId != null) {
                    Spell spell = module.getSpellManager().getSpell(targetSpellId);
                    String spellName = spell != null ? spell.getDisplayName() : targetSpellId;
                    line = line.replace("%spell%", spellName);
                }
                loreParsed.add(MessageUtils.parse(line));
            }
            meta.lore(loreParsed);
        }

        item.setItemMeta(meta);
        return item;
    }

    public boolean isScroll(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) { return false; }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) { return false; }
        return PDCUtils.has(meta, PDC_SCROLL_TYPE);
    }

    public ScrollType getScrollType(ItemStack item) {
        if (item == null) { return null; }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) { return null; }
        String type = PDCUtils.getString(meta, PDC_SCROLL_TYPE);
        return type != null ? ScrollType.fromString(type) : null;
    }

    public String getScrollTarget(ItemStack item) {
        if (item == null) { return null; }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) { return null; }
        return PDCUtils.getString(meta, PDC_SCROLL_TARGET);
    }
}
