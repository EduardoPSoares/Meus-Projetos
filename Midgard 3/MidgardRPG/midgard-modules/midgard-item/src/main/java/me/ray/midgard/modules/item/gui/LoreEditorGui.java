package me.ray.midgard.modules.item.gui;

import me.ray.midgard.core.gui.PaginatedGui;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.core.MidgardCore;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class LoreEditorGui extends PaginatedGui<String> {

    private final ItemModule module;
    private final MidgardItem item;
    private final List<String> loreLines;
    private final ItemEditionGui parent;

    public LoreEditorGui(Player player, ItemModule module, MidgardItem item, ItemEditionGui parent) {
        super(player, MidgardCore.getLanguageManager().getRawMessage("item.gui.lore_editor.title").replace("%id%", item.getId()), new ArrayList<>(item.getLore()));
        this.module = module;
        this.item = item;
        this.loreLines = this.items;
        this.parent = parent;
    }

    @Override
    public ItemStack createItem(String line) {
        return new ItemBuilder(Material.PAPER)
                .name(MessageUtils.parse(line))
                .lore(
                        MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.lore_editor.lore.edit")),
                        MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.lore_editor.lore.delete"))
                )
                .build();
    }

    @Override
    public void addMenuBorder() {
        super.addMenuBorder();
        
        // Back to Item Editor
        inventory.setItem(48, new ItemBuilder(Material.ARROW).name(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.lore_editor.buttons.back"))).build());
        
        // Add line button
        inventory.setItem(50, new ItemBuilder(Material.EMERALD).name(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.lore_editor.buttons.add"))).build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        
        if (slot == 50) {
            requestChatInput(player, MidgardCore.getLanguageManager().getRawMessage("item.gui.lore_editor.prompts.new-line"), (text) -> {
                loreLines.add(text);
                item.setLore(loreLines);
                new LoreEditorGui(player, module, item, parent).open();
            });
            return;
        } else if (slot == 48) {
            parent.open();
            return;
        }

        // Check for item click manually to get ClickType
        int row = slot / 9;
        int col = slot % 9;
        
        if (row >= 1 && row < 4 && col >= 1 && col < 8) {
            int relativeIndex = (row - 1) * 7 + (col - 1);
            int realIndex = (page * maxItemsPerPage) + relativeIndex;
            
            if (realIndex >= 0 && realIndex < items.size()) {
                if (event.getClick() == ClickType.RIGHT) {
                    // Delete
                    loreLines.remove(realIndex);
                    item.setLore(loreLines);
                    initializeItems(); // Refresh
                } else if (event.getClick() == ClickType.LEFT) {
                    // Edit
                    requestChatInput(player, MidgardCore.getLanguageManager().getRawMessage("item.gui.lore_editor.prompts.edit-line"), (text) -> {
                        loreLines.set(realIndex, text);
                        item.setLore(loreLines);
                        new LoreEditorGui(player, module, item, parent).open();
                    });
                }
                return; 
            }
        }

        super.onClick(event);
    }

    private void requestChatInput(Player player, String prompt, java.util.function.Consumer<String> callback) {
        player.closeInventory();
        String msg = MidgardCore.getLanguageManager().getRawMessage("item.gui.editor.enter-prompt-cancel").replace("%s", prompt);
        player.sendMessage(MessageUtils.parse(msg));
        me.ray.midgard.modules.item.listener.ChatInputListener.requestInput(player, callback);
    }
    
    @Override
    public void onItemClick(Player player, int slot) {
        // Handled in onClick
    }
}
