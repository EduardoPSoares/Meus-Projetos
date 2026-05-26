package me.ray.midgard.modules.item.gui.editors;

import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.modules.item.gui.ItemEditionGui;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface StatEditor {
    void edit(Player player, ItemModule module, MidgardItem item, ItemEditionGui gui, org.bukkit.event.inventory.ClickType clickType);
}
