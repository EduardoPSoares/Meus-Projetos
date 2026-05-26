package me.ray.midgard.modules.item.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.PaginatedGui;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SetSelectionGui extends PaginatedGui<String> {

    private final MidgardItem item;
    private final ItemEditionGui parent;

    public SetSelectionGui(Player player, ItemModule module, MidgardItem item, ItemEditionGui parent) {
        super(player, MidgardCore.getLanguageManager().getRawMessage("item.gui.item_set_selection.title"), new ArrayList<>());
        this.item = item;
        this.parent = parent;
        
        // Placeholder sets
        List<String> sets = new ArrayList<>();
        sets.add("NONE");
        sets.add("WARRIOR_SET");
        sets.add("MAGE_SET");
        sets.add("RANGER_SET");
        sets.add("PALADIN_SET");
        
        this.items = sets;
    }

    @Override
    public ItemStack createItem(String set) {
        boolean selected = set.equalsIgnoreCase(item.getItemSet());
        Material mat = selected ? Material.GOLDEN_CHESTPLATE : Material.LEATHER_CHESTPLATE;
        
        return new ItemBuilder(mat)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.item_set_selection.item.name", "%set%", set))
                .lore(MidgardCore.getLanguageManager().getStringList("item.gui.item_set_selection.item.lore").stream()
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
        } else if (slot < 45) {
            int index = page * 45 + slot;
            if (index < items.size()) {
                String set = items.get(index);
                if (set.equals("NONE")) {
                    set = null;
                }
                
                item.setItemSet(set);
                item.save();
                player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.item_set_selection.messages.updated", "%set%", String.valueOf(set)));
                parent.open();
            }
        }
    }

    @Override
    public void addMenuBorder() {
        if (page > 0) {
            inventory.setItem(45, new ItemBuilder(Material.ARROW).name(MidgardCore.getLanguageManager().getMessage("item.common.previous_page")).build());
        }
        if ((page + 1) * 45 < items.size()) {
            inventory.setItem(53, new ItemBuilder(Material.ARROW).name(MidgardCore.getLanguageManager().getMessage("item.common.next_page")).build());
        }
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name(MidgardCore.getLanguageManager().getMessage("item.gui.item_set_selection.buttons.back")).build());
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
