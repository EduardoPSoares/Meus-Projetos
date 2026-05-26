package me.ray.midgard.modules.economy.listener;

import me.ray.midgard.modules.economy.EconomyModule;
import me.ray.midgard.modules.economy.gui.PouchGui;
import me.ray.midgard.modules.economy.manager.PouchManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class PouchListener implements Listener {

    private final EconomyModule module;

    public PouchListener(EconomyModule module) {
        this.module = module;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack item = event.getItem().getItemStack();
        // String id = ItemModule.getInstance().getItemManager().getItemId(item);
        
        // Check if it's currency
        var currencyManager = module.getCurrencyManager();
        if (currencyManager == null) {
            return;
        }
        
        boolean isCurrency = currencyManager.getValue(item) > 0;

        if (!isCurrency) {
            return;
        }
        
        // Check Offhand first
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (tryAbsorb(player, offhand, item, event)) {
            player.getInventory().setItemInOffHand(offhand);
            return;
        }
        
        // Check Storage
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length; i++) {
             ItemStack invItem = contents[i];
             if (tryAbsorb(player, invItem, item, event)) {
                 // If absorb modified the item, we must update the inventory explicitly
                 // because getStorageContents() returns a copy or might be detached
                 player.getInventory().setItem(i, invItem);
                 return;
             }
        }
    }
    
    private boolean tryAbsorb(Player player, ItemStack pouch, ItemStack pickupItem, EntityPickupItemEvent event) {
        PouchManager pm = module.getPouchManager();
        if (pm.isPouch(pouch)) {
            // CRITICAL: Do not modify pouch if it is currently open in a GUI
            // This prevents race conditions where GUI close overwrites pickup changes
            String sessionId = pm.getSessionId(pouch);
            if (pm.isSessionOpen(sessionId)) {
                return false;
            }

            ItemStack leftover = pm.addItem(pouch, pickupItem);
            
            if (leftover == null) {
                // All absorbed
                event.getItem().remove();
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.5f);
                event.setCancelled(true);
                return true;
            } else {
                // Partially absorbed?
                if (leftover.getAmount() < pickupItem.getAmount()) {
                    // Update the item being picked up so next pouch sees the reduced amount
                    pickupItem.setAmount(leftover.getAmount());
                    // Update the entity in world just in case we stop here
                    event.getItem().setItemStack(pickupItem);
                    
                    // Return false to continue searching for other pouches to fill the rest
                    return false;
                }
            }
        }
        return false;
    }

    private final java.util.Map<java.util.UUID, Long> interactCooldowns = new java.util.concurrent.ConcurrentHashMap<>();

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        interactCooldowns.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        
        PouchManager pm = module.getPouchManager();
        if (pm.isPouch(item)) {
            event.setCancelled(true);
            
            // Cooldown Check (Prevent double open / race conditions)
            long now = System.currentTimeMillis();
            long lastInteract = interactCooldowns.getOrDefault(event.getPlayer().getUniqueId(), 0L);
            if (now - lastInteract < 500) {
                return;
            }
            interactCooldowns.put(event.getPlayer().getUniqueId(), now);
            
            // Check Permission
            String perm = pm.getPermission(item);
            if (perm != null && !event.getPlayer().hasPermission(perm)) {
                me.ray.midgard.core.text.MessageUtils.send(event.getPlayer(), module.getMessage("pouch.error.no_permission"));
                return;
            }
            
            String sessionId = pm.assignSessionId(item); // Generate unique session ID for this interaction
            new PouchGui(event.getPlayer(), item, sessionId).open();
        }
    }
}
