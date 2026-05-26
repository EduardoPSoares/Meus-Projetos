package me.ray.midgard.modules.item.gui.editors.impl;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.item.gui.ItemEditionGui;
import me.ray.midgard.modules.item.listener.ChatInputListener;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.function.Consumer;

public class TextEditorGui extends BaseGui {

    private final String currentValue;
    private final Consumer<String> onSave;
    private final ItemEditionGui parentGui;
    private final String statName;

    public TextEditorGui(Player player, String currentValue, Consumer<String> onSave, ItemEditionGui parentGui, String statName) {
        super(player, 3, MidgardCore.getLanguageManager().getRawMessage("item.gui.editor.text.title").replace("%name%", statName));
        this.currentValue = currentValue;
        this.onSave = onSave;
        this.parentGui = parentGui;
        this.statName = statName;
    }

    @Override
    public void initializeItems() {
        // Background filler removed per user request


        // Current Value
        inventory.setItem(13, new ItemBuilder(Material.PAPER)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.editor.text.current"))
                .lore(MessageUtils.parse("<white>" + (currentValue == null || currentValue.isEmpty() ? "<empty>" : currentValue)))
                .build());

        // Edit via Chat
        inventory.setItem(20, new ItemBuilder(Material.WRITABLE_BOOK)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.editor.text.edit"))
                .lore(MidgardCore.getLanguageManager().getMessage("item.gui.editor.text.click-edit"))
                .build());

        // Clear Text
        inventory.setItem(22, new ItemBuilder(Material.LAVA_BUCKET)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.editor.text.clear"))
                .lore(MidgardCore.getLanguageManager().getMessage("item.gui.editor.text.click-clear"))
                .build());

        // Cancel / Back
        inventory.setItem(24, new ItemBuilder(Material.ARROW)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.editor.text.cancel"))
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 24) { // Back
            parentGui.open();
            return;
        }

        if (slot == 22) { // Clear
            onSave.accept("");
            parentGui.initializeItems();
            parentGui.open();
            player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editor.reset", "%s", statName));
            return;
        }

        if (slot == 20 || slot == 13) { // Edit
            player.closeInventory();
            player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editor.enter-prompt", "%s", statName));

            ChatInputListener.requestInput(player, (input) -> {
                onSave.accept(input);
                parentGui.initializeItems();
                parentGui.open();
                player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editor.updated", "%s", statName));
            });
        }
    }
}
