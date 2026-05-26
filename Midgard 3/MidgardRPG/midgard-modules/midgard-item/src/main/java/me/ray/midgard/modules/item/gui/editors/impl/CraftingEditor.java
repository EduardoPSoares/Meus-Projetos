package me.ray.midgard.modules.item.gui.editors.impl;

import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.modules.item.gui.ItemEditionGui;
import me.ray.midgard.modules.item.gui.RecipeConfigurationGui;
import me.ray.midgard.modules.item.gui.editors.StatEditor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public class CraftingEditor implements StatEditor {
    @Override
    public void edit(Player player, ItemModule module, MidgardItem item, ItemEditionGui gui, ClickType clickType) {
        new RecipeConfigurationGui(player, module, item, gui).open();
    }
}
