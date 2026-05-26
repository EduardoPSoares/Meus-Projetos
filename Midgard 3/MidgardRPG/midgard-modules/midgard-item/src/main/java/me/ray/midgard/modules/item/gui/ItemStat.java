package me.ray.midgard.modules.item.gui;

import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.modules.item.gui.editors.StatEditor;
import me.ray.midgard.modules.item.model.MidgardItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemStat {
    private final String key;
    private final int page;
    private final int slot;
    private final Material material;
    private final String name;
    private final List<String> lore;
    private boolean inDevelopment = false;
    private StatEditor editor;

    public ItemStat(String key, int page, int slot, Material material, String name, String... lore) {
        this.key = key;
        this.page = page;
        this.slot = slot;
        this.material = material;
        this.name = name;
        this.lore = Arrays.asList(lore);
    }

    public ItemStat setEditor(StatEditor editor) {
        this.editor = editor;
        return this;
    }

    public StatEditor getEditor() {
        return editor;
    }

    public ItemStat setInDevelopment(boolean inDevelopment) {
        this.inDevelopment = inDevelopment;
        return this;
    }

    public boolean isInDevelopment() {
        return inDevelopment;
    }

    public String getKey() {
        return key;
    }

    public int getPage() {
        return page;
    }

    public int getSlot() {
        return slot;
    }

    public ItemStack getIcon(Player player, MidgardItem item) {
        // In a real implementation, we would replace "Current Value" with actual values from the item
        List<Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            loreComponents.add(MessageUtils.parse(line));
        }

        if (inDevelopment) {
            loreComponents.add(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.common.in_development")));
        }

        return new ItemBuilder(material)
                .name(MessageUtils.parse(name))
                .lore(loreComponents)
                .build();
    }
}
