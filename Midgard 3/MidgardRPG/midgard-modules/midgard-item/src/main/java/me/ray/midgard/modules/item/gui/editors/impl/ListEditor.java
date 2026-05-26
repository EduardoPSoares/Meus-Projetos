package me.ray.midgard.modules.item.gui.editors.impl;

import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.modules.item.gui.ItemEditionGui;
import me.ray.midgard.modules.item.gui.StringListEditorGui;
import me.ray.midgard.modules.item.gui.editors.StatEditor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ListEditor implements StatEditor {

    private final BiConsumer<MidgardItem, List<String>> setter;
    private final Function<MidgardItem, List<String>> getter;
    private final String title;

    public ListEditor(BiConsumer<MidgardItem, List<String>> setter, Function<MidgardItem, List<String>> getter, String title) {
        this.setter = setter;
        this.getter = getter;
        this.title = title;
    }

    @Override
    public void edit(Player player, ItemModule module, MidgardItem item, ItemEditionGui gui, ClickType clickType) {
        if (clickType.isRightClick()) {
            setter.accept(item, new ArrayList<>());
            String msg = me.ray.midgard.core.MidgardCore.getLanguageManager().getRawMessage("item.gui.editors.list.cleared").replace("%title%", title);
            player.sendMessage(MessageUtils.parse(msg));
            gui.initializeItems();
            return;
        }
        new StringListEditorGui(player, module, title, getter.apply(item), (l) -> {
            setter.accept(item, l);
            String msg = me.ray.midgard.core.MidgardCore.getLanguageManager().getRawMessage("item.gui.editors.list.updated").replace("%title%", title);
            player.sendMessage(MessageUtils.parse(msg));
        }, () -> gui.open()).open();
    }
}
