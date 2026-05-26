package me.ray.midgard.modules.item.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.PaginatedGui;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.listener.ChatInputListener;
import me.ray.midgard.modules.item.model.MidgardItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MaterialListSelectionGui extends PaginatedGui<String> {

    private final MidgardItem item;
    private final ItemEditionGui parent;

    public MaterialListSelectionGui(Player player, ItemModule module, MidgardItem item, ItemEditionGui parent) {
        super(player, MidgardCore.getLanguageManager().getRawMessage("item.gui.material_list_selection.title"), new ArrayList<>());
        this.item = item;
        this.parent = parent;
        this.items = item.getCompatibleMaterials();
    }

    @Override
    public ItemStack createItem(String material) {
        Material mat = Material.matchMaterial(material);
        if (mat == null) {
            mat = Material.BARRIER;
        }
        
        return new ItemBuilder(mat)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.material_list_selection.item.name", "%material%", material))
                .lore(MidgardCore.getLanguageManager().getStringList("item.gui.material_list_selection.item.lore").stream()
                        .map(me.ray.midgard.core.text.MessageUtils::parse)
                        .toList())
                .build();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 45 && page > 0) {
            page--;
            initializeItems();
        } else if (slot == 53 && (page + 1) * 45 < items.size()) {
            page++;
            initializeItems();
        } else if (slot == 49) {
            parent.open();
        } else if (slot == 50) { // Add Button
            addMaterial();
        } else if (slot < 45) {
            int index = page * 45 + slot;
            if (index < items.size()) {
                String mat = items.get(index);
                List<String> current = new ArrayList<>(item.getCompatibleMaterials());
                current.remove(mat);
                item.setCompatibleMaterials(current);
                item.save();
                
                this.items = current; // Update internal list
                player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.material_list_selection.messages.removed", "%material%", mat));
                initializeItems();
            }
        }
    }
    
    private void addMaterial() {
        player.closeInventory();
        player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.material_list_selection.messages.prompt"));
        ChatInputListener.requestInput(player, (input) -> {
            Material mat = Material.matchMaterial(input);
            if (mat != null) {
                List<String> current = new ArrayList<>(item.getCompatibleMaterials());
                if (!current.contains(mat.name())) {
                    current.add(mat.name());
                    item.setCompatibleMaterials(current);
                    item.save();
                    player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.material_list_selection.messages.added", "%material%", mat.name()));
                }
                this.items = current;
                this.open();
            } else {
                player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editor.invalid-material"));
                this.open();
            }
        });
    }

    @Override
    public void addMenuBorder() {
        if (page > 0) {
            inventory.setItem(45, new ItemBuilder(Material.ARROW).name(MidgardCore.getLanguageManager().getMessage("item.common.previous_page")).build());
        }
        if ((page + 1) * 45 < items.size()) {
            inventory.setItem(53, new ItemBuilder(Material.ARROW).name(MidgardCore.getLanguageManager().getMessage("item.common.next_page")).build());
        }
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name(MidgardCore.getLanguageManager().getMessage("item.gui.material_list_selection.buttons.back")).build());
        
        inventory.setItem(50, new ItemBuilder(Material.LIME_DYE)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.material_list_selection.buttons.add.name"))
                .lore(MidgardCore.getLanguageManager().getStringList("item.gui.material_list_selection.buttons.add.lore").stream().map(me.ray.midgard.core.text.MessageUtils::parse).toList())
                .build());
    }
    
    @Override
    public void initializeItems() {
        inventory.clear();
        addMenuBorder();
        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, items.size());
        for (int i = startIndex; i < endIndex; i++) {
            inventory.setItem(i - startIndex, createItem(items.get(i)));
        }
    }
}
