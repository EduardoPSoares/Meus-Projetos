package me.ray.midgard.modules.item.gui.editors.impl;

import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.modules.item.gui.ItemEditionGui;
import me.ray.midgard.modules.item.gui.editors.StatEditor;
import me.ray.midgard.modules.item.listener.ChatInputListener;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.function.BiConsumer;

public class MaterialEditor implements StatEditor {

    private final BiConsumer<MidgardItem, Material> setter;
    private final String prompt;

    public MaterialEditor(BiConsumer<MidgardItem, Material> setter, String prompt) {
        this.setter = setter;
        this.prompt = prompt;
    }

    @Override
    public void edit(Player player, ItemModule module, MidgardItem item, ItemEditionGui gui, ClickType clickType) {
        me.ray.midgard.core.i18n.LanguageManager lang = me.ray.midgard.core.MidgardCore.getLanguageManager();
        if (clickType.isRightClick()) {
            setter.accept(item, Material.STONE);
            String msg = lang.getRawMessage("item.gui.editor.reset-stone").replace("%s", prompt);
            player.sendMessage(MessageUtils.parse(msg));
            new ItemEditionGui(player, module, item).open();
            return;
        }
        player.closeInventory();
        String msg = lang.getRawMessage("item.gui.editor.enter-prompt-cancel").replace("%s", prompt);
        player.sendMessage(MessageUtils.parse(msg));
        ChatInputListener.requestInput(player, (text) -> {
            try {
                Material mat = Material.valueOf(text.toUpperCase());
                setter.accept(item, mat);
                String successMsg = lang.getRawMessage("item.gui.editor.updated").replace("%s", prompt);
                player.sendMessage(MessageUtils.parse(successMsg));
                new ItemEditionGui(player, module, item).open();
            } catch (IllegalArgumentException e) {
                player.sendMessage(lang.getMessage("item.gui.editor.invalid-material"));
                new ItemEditionGui(player, module, item).open();
            }
        });
    }
}
