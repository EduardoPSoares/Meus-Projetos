package me.ray.midgard.loader.listener;

import me.ray.midgard.core.debug.DebugCategory;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.loader.gui.MobDebugGui;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public class MobDebugListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        if (!player.hasPermission("midgard.debug")) return;
        
        if (event.getRightClicked() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) event.getRightClicked();
            
            // Open Debug GUI
            MidgardLogger.debug(DebugCategory.CORE, "Abrindo GUI de Debug de Mob para %s alvo: %s", player.getName(), target.getType());
            new MobDebugGui(player, target).open();
            event.setCancelled(true); // Prevent other interactions (like trading or riding)
        }
    }
}
