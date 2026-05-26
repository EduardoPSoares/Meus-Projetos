package me.ray.midgard.modules.item.listener;

import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.core.utils.Task;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class ItemUpdateListener implements Listener {

    private final ItemModule module;

    public ItemUpdateListener(ItemModule module) {
        this.module = module;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Schedule update for a few ticks later to ensure player is fully loaded
        Task.syncLater(player, () -> {
            if (player.isOnline()) {
                module.getItemManager().updateInventory(player);
            }
        }, 20L);
    }

    @EventHandler
    public void onInventoryOpen(org.bukkit.event.inventory.InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) { return; }
        Player player = (Player) event.getPlayer();
        
        // Update items in the opened inventory (chests, etc.)
        // Run 1 tick later to avoid blocking the event and ensure inventory is ready
        Task.syncLater(player, () -> {
            // Check if inventory is still valid/viewed
            if (event.getInventory().getViewers().isEmpty()) { return; }

            ItemStack[] contents = event.getInventory().getContents();
            boolean updated = false;
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (item == null || !item.hasItemMeta()) { continue; }
                
                ItemStack newItem = module.getItemManager().updateItem(item);
                if (newItem != null) {
                    contents[i] = newItem;
                    updated = true;
                }
            }
            
            if (updated) {
                event.getInventory().setContents(contents);
            }
        }, 1L);
    }
}
