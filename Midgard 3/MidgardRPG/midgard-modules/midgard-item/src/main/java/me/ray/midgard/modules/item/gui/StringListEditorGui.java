package me.ray.midgard.modules.item.gui;

import me.ray.midgard.core.gui.PaginatedGui;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.item.ItemModule;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class StringListEditorGui extends PaginatedGui<String> {

    private final ItemModule module;
    private final List<String> list;
    private final Consumer<List<String>> onSave;
    private final Runnable onBack;

    public StringListEditorGui(Player player, ItemModule module, String title, List<String> list, Consumer<List<String>> onSave, Runnable onBack) {
        super(player, title, new ArrayList<>(list));
        this.module = module;
        this.list = this.items;
        this.onSave = onSave;
        this.onBack = onBack;
    }

    @Override
    public ItemStack createItem(String line) {
        return new ItemBuilder(Material.PAPER)
                .name(MessageUtils.parse(line))
                .lore(
                        MessageUtils.parse(me.ray.midgard.core.MidgardCore.getLanguageManager().getRawMessage("item.gui.string_list_editor.lore.edit")),
                        MessageUtils.parse(me.ray.midgard.core.MidgardCore.getLanguageManager().getRawMessage("item.gui.string_list_editor.lore.delete"))
                )
                .build();
    }

    @Override
    public void addMenuBorder() {
        // Footer Navigation
        
        // Previous Page
        if (page > 0) {
            inventory.setItem(45, new ItemBuilder(Material.ARROW).name(me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.common.previous_page")).build());
        }
        
        // Next Page
        if (index + 1 < items.size()) {
            inventory.setItem(53, new ItemBuilder(Material.ARROW).name(me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.common.next_page")).build());
        }
        
        // Back button (Center)
        inventory.setItem(49, new ItemBuilder(Material.ARROW).name(MessageUtils.parse(me.ray.midgard.core.MidgardCore.getLanguageManager().getRawMessage("item.gui.string_list_editor.buttons.back"))).build());
        
        // Add line button (Right of Back)
        inventory.setItem(51, new ItemBuilder(Material.EMERALD).name(MessageUtils.parse(me.ray.midgard.core.MidgardCore.getLanguageManager().getRawMessage("item.gui.string_list_editor.buttons.add"))).build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        
        if (slot == 51) {
            requestChatInput(player, me.ray.midgard.core.MidgardCore.getLanguageManager().getRawMessage("item.gui.string_list_editor.prompts.new-line"), (text) -> {
                list.add(text);
                onSave.accept(list);
                new StringListEditorGui(player, module, me.ray.midgard.core.MidgardCore.getLanguageManager().getRawMessage("item.gui.string_list_editor.title"), list, onSave, onBack).open();
            });
            return;
        } else if (slot == 49) {
            onBack.run();
            return;
        } else if (slot == 45 && page > 0) {
            page--;
            initializeItems();
            return;
        } else if (slot == 53 && index + 1 < items.size()) {
            page++;
            initializeItems();
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
                    list.remove(realIndex);
                    onSave.accept(list);
                    initializeItems(); // Refresh
                } else if (event.getClick() == ClickType.LEFT) {
                    // Edit
                    requestChatInput(player, me.ray.midgard.core.MidgardCore.getLanguageManager().getRawMessage("item.gui.string_list_editor.prompts.edit-line"), (text) -> {
                        list.set(realIndex, text);
                        onSave.accept(list);
                        new StringListEditorGui(player, module, me.ray.midgard.core.MidgardCore.getLanguageManager().getRawMessage("item.gui.string_list_editor.title"), list, onSave, onBack).open();
                    });
                }
                return; 
            }
        }
    }

    private void requestChatInput(Player player, String prompt, Consumer<String> callback) {
        player.closeInventory();
        String msg = me.ray.midgard.core.MidgardCore.getLanguageManager().getRawMessage("item.gui.editor.enter-prompt-cancel").replace("%s", prompt);
        player.sendMessage(MessageUtils.parse(msg));
        me.ray.midgard.modules.item.listener.ChatInputListener.requestInput(player, callback);
    }
    
    @Override
    public void onItemClick(Player player, int slot) {
        // Handled in onClick
    }
}
