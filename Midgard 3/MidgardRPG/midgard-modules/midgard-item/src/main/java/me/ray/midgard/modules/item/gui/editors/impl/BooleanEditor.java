package me.ray.midgard.modules.item.gui.editors.impl;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.modules.item.gui.ItemEditionGui;
import me.ray.midgard.modules.item.gui.editors.StatEditor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class BooleanEditor implements StatEditor {

    private final BiConsumer<MidgardItem, Boolean> setter;
    private final Function<MidgardItem, Boolean> getter;

    public BooleanEditor(BiConsumer<MidgardItem, Boolean> setter, Function<MidgardItem, Boolean> getter) {
        this.setter = setter;
        this.getter = getter;
    }

    @Override
    public void edit(Player player, ItemModule module, MidgardItem item, ItemEditionGui gui, ClickType clickType) {
        if (clickType.isRightClick()) {
            setter.accept(item, false);
            player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editors.boolean.reset"));
            gui.initializeItems();
            return;
        }
        boolean newValue = !getter.apply(item);
        setter.accept(item, newValue);
        player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editors.boolean.toggled", "%value%", String.valueOf(newValue)));
        gui.initializeItems();
    }
}
