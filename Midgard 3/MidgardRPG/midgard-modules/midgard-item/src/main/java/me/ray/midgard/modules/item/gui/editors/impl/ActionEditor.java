package me.ray.midgard.modules.item.gui.editors.impl;

import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.gui.editors.StatEditor;
import me.ray.midgard.modules.item.model.MidgardItem;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.function.BiConsumer;

public class ActionEditor implements StatEditor {

    private final BiConsumer<Player, MidgardItem> action;

    public ActionEditor(BiConsumer<Player, MidgardItem> action) {
        this.action = action;
    }

    @Override
    public void edit(Player player, ItemModule module, MidgardItem item, me.ray.midgard.modules.item.gui.ItemEditionGui gui, ClickType clickType) {
        action.accept(player, item);
    }
}
