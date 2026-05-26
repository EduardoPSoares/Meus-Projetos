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

public class StringEditor implements StatEditor {

    private final BiConsumer<MidgardItem, String> setter;
    private final Function<MidgardItem, String> getter;
    private final String prompt;

    public StringEditor(BiConsumer<MidgardItem, String> setter, Function<MidgardItem, String> getter, String prompt) {
        this.setter = setter;
        this.getter = getter;
        this.prompt = prompt;
    }

    @Override
    public void edit(Player player, ItemModule module, MidgardItem item, ItemEditionGui gui, ClickType clickType) {
        if (clickType.isRightClick()) {
            setter.accept(item, "");
            player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editors.string.cleared", "%prompt%", prompt));
            gui.initializeItems();
            gui.open();
            return;
        }
        
        String current = getter.apply(item);
        new TextEditorGui(player, current, (val) -> setter.accept(item, val), gui, prompt).open();
    }
}
