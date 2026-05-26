package me.ray.midgardspigot;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import org.bukkit.event.player.PlayerChangedWorldEvent;

public class MaintenanceFixListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        checkAndRemoveGod(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        checkAndRemoveGod(event.getPlayer());
    }

    private void checkAndRemoveGod(Player player) {
        // Se a manutenção estiver ativa, não removemos (pois todos devem estar imortais)
        if (MidgardSpigot.getInstance().isMaintenanceActive()) return;

        // Delay de 5 ticks para garantir que outros plugins já carregaram o gamemode correto
        org.bukkit.Bukkit.getScheduler().runTaskLater(MidgardSpigot.getInstance(), () -> {
            if (!player.isOnline()) return;
            
            // Ignora se for Ghost do MidgardPermaDeath
            if (MidgardSpigot.getInstance().isMidgardGhost(player)) return;

            // Remove invulnerability if player is not in Creative or Spectator mode
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                if (player.isInvulnerable()) {
                    player.setInvulnerable(false);
                    MidgardSpigot.getInstance().getLogger().info("Imortalidade removida de " + player.getName() + " (Fix Manutencao/WorldChange)");
                }
            }
        }, 5L);
    }
}
