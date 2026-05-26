package me.ray.midgard.core.gui;

import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public abstract class PaginatedGui<T> extends BaseGui {

    protected int page = 0;
    protected int maxItemsPerPage = 21; // Standard chest middle area
    protected int index = 0;
    protected List<T> items;

    public PaginatedGui(Player player, String title, List<T> items) {
        super(player, 6, title); // Default to 6 rows for paginated GUIs
        this.items = items != null ? items : new java.util.ArrayList<>();
    }

    public abstract ItemStack createItem(T data);

    public void addMenuBorder() {
        // Navigation buttons
        // Slot 18: Previous Page
        if (page > 0) {
            inventory.setItem(18, new ItemBuilder(Material.ARROW).name(MessageUtils.parse("<green>Previous Page")).build());
        } else {
            // Keep the slot empty or maybe a placeholder if requested "que nao suma"
            // For now, empty as per standard, unless "que nao suma" means always visible.
            // If user wants it to not disappear, maybe show a gray arrow?
            // Let's stick to standard behavior first, but user said "que nao suma para passar de pagina ao voltar da pagina"
            // This might mean "don't let the button disappear when I toggle pages".
            // I'll leave it empty if not applicable for now.
            inventory.setItem(18, null);
        }

        // Slot 49: Close
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name(MessageUtils.parse("<red>Close")).build());

        // Slot 26: Next Page
        if (index + 1 < items.size()) {
            inventory.setItem(26, new ItemBuilder(Material.ARROW).name(MessageUtils.parse("<green>Next Page")).build());
        } else {
            inventory.setItem(26, null);
        }
    }

    @Override
    public void initializeItems() {
        inventory.clear();
        
        maxItemsPerPage = 21;
        
        // Calculate start index
        int startIndex = page * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, items.size());

        // Fill items in the middle slots (indices 10-16, 19-25, 28-34)
        
        int itemIndex = startIndex;
        for (int row = 1; row < 4; row++) {
            for (int col = 1; col < 8; col++) {
                if (itemIndex >= endIndex) { break; }
                
                int slot = row * 9 + col;
                inventory.setItem(slot, createItem(items.get(itemIndex)));
                itemIndex++;
            }
        }
        this.index = itemIndex - 1;
        
        // Add border/buttons AFTER items to ensure they overwrite if necessary (though slots shouldn't overlap)
        addMenuBorder();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 18 && page > 0) {
            page--;
            initializeItems();
        } else if (slot == 49) {
            event.getWhoClicked().closeInventory();
        } else if (slot == 26 && index + 1 < items.size()) {
            page++;
            initializeItems();
        } else {
            // Handle item click
            if (slot >= 0 && slot < inventory.getSize()) {
                 ItemStack clicked = inventory.getItem(slot);
                 if (clicked != null && !clicked.getType().equals(Material.ARROW) && !clicked.getType().equals(Material.BARRIER)) {
                     if (event.getWhoClicked() instanceof Player clicker) {
                         onItemClick(clicker, slot);
                     }
                 }
            }
        }
    }
    
    public void onItemClick(Player player, int slot) {
        // To be overridden
    }
}
