package me.ray.midgard.modules.item.gui.editors.impl;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.listener.ChatInputListener;
import me.ray.midgard.modules.item.model.ItemStat;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.modules.item.gui.ItemEditionGui;
import me.ray.midgard.modules.item.gui.editors.StatEditor;
import me.ray.midgard.modules.item.utils.StatRange;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public class RpgStatEditor implements StatEditor {

    private final ItemStat stat;

    public RpgStatEditor(ItemStat stat) {
        this.stat = stat;
    }

    @Override
    public void edit(Player player, ItemModule module, MidgardItem item, ItemEditionGui gui, ClickType clickType) {
        if (clickType.isRightClick()) {
            item.setStat(stat, 0.0);
            player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editors.stat.reset", "%stat%", stat.getName()));
            gui.initializeItems();
            return;
        }

        player.closeInventory();
        player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editor.enter-prompt", "%s", stat.getName()));
        player.sendMessage(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.editors.stat_range.examples")));

        ChatInputListener.requestInput(player, (input) -> {
            try {
                StatRange.parse(input);
                item.setStat(stat, input);
                player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editors.stat.updated", "%stat%", stat.getName()));
            } catch (Exception e) {
                player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editor.invalid-number"));
            }
            gui.open();
        });
    }
}
