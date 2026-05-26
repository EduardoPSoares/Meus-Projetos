package me.ray.midgard.modules.essentials.listener;

import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class TeleportListener implements Listener {

    private final EssentialsManager manager;

    public TeleportListener(EssentialsManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        try {
            if (manager != null && manager.getTeleportHistoryManager() != null) {
                // Não salvar se for teleporte por movimento desconhecido ou plugin que não queremos rastrear
                // Mas geralmente queremos salvar "de onde ele saiu"
                manager.getTeleportHistoryManager().setLastLocation(event.getPlayer(), event.getFrom());
            }
        } catch (Exception e) {
             MidgardLogger.warn("Erro ao salvar histórico de teleporte", e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        try {
            if (manager != null && manager.getTeleportHistoryManager() != null) {
                manager.getTeleportHistoryManager().setLastLocation(event.getEntity(), event.getEntity().getLocation());
            }
        } catch (Exception e) {
             MidgardLogger.warn("Erro ao salvar local de morte", e);
        }
    }
}
