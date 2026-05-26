package me.ray.midgard.modules.essentials.listener;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.essentials.data.EssentialsData;
import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

public class VanishListener implements Listener {

    private final EssentialsManager manager;

    public VanishListener(EssentialsManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        try {
            if (manager != null && manager.getVanishManager() != null) {
                Player player = event.getPlayer();
                
                // Verifica persistência ANTES de processar o resto
                // Precisamos acessar os dados diretamente ou confiar no VanishManager
                // O problema é que o profile pode não estar carregado ainda no JoinEvent (depende da prioridade)
                // Mas vamos tentar verificar via manager que checa o profile
                
                // Se o jogador deve estar vanished, removemos a mensagem de entrada
                MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
                boolean shouldVanish = false;
                if (profile != null) {
                    EssentialsData data = profile.getOrCreateData(EssentialsData.class);
                    shouldVanish = data.isVanished();
                }
                
                if (shouldVanish) {
                    event.joinMessage(null);
                    manager.getVanishManager().vanish(player, true); // Silent vanish
                }

                // Atualiza para o jogador ver (ou não) os outros que estão vanished
                manager.getVanishManager().updateFor(event.getPlayer());
            }
        } catch (Exception e) {
             MidgardLogger.error("Erro ao atualizar vanish para jogador " + event.getPlayer().getName(), e);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        try {
            java.util.UUID uuid = event.getPlayer().getUniqueId();
            
            // Cleanup vanish state
            if (manager != null && manager.getVanishManager() != null) {
                if (manager.getVanishManager().isVanished(event.getPlayer())) {
                    manager.getVanishManager().removePlayer(event.getPlayer());
                    event.quitMessage(null);
                }
            }
            
            // Cleanup teleport data to prevent memory leaks
            if (manager != null) {
                if (manager.getTeleportRequestManager() != null) {
                    manager.getTeleportRequestManager().cleanupPlayer(uuid);
                }
                if (manager.getTeleportHistoryManager() != null) {
                    manager.getTeleportHistoryManager().clearHistory(event.getPlayer());
                }
            }
        } catch (Exception e) {
             MidgardLogger.error("Erro ao processar quit para jogador " + event.getPlayer().getName(), e);
        }
    }
    
    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (manager != null && manager.getVanishManager() != null && manager.getVanishManager().isVanished(player)) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onTarget(EntityTargetEvent event) {
        if (event.getTarget() instanceof Player player) {
            if (manager != null && manager.getVanishManager() != null && manager.getVanishManager().isVanished(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (manager != null && manager.getVanishManager() != null && manager.getVanishManager().isVanished(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (manager != null && manager.getVanishManager() != null && manager.getVanishManager().isVanished(player)) {
                event.setCancelled(true);
                me.ray.midgard.core.text.MessageUtils.send(player, manager.getMessage("vanish.no_attack"));
            }
        }
    }
    
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (manager == null || manager.getVanishManager() == null || !manager.getVanishManager().isVanished(player)) {
            return;
        }
        
        // Prevent physical interaction (pressure plates, tripwires, farmland trampling)
        if (event.getAction() == Action.PHYSICAL) {
            event.setCancelled(true);
            return;
        }
        
        // Silent Chests
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            if (event.getClickedBlock().getState() instanceof Container) {
                // Se o jogador estiver em vanish e abrir um baú, queremos que seja silencioso.
                // O Bukkit abre com animação por padrão.
                // Para fazer "Silent Chest", precisamos cancelar o evento e abrir o inventário manualmente sem pacote de animação?
                // Ou apenas abrir o inventário do container.
                
                event.setCancelled(true);
                Container container = (Container) event.getClickedBlock().getState();
                Inventory inv = container.getInventory();
                
                // Create a read-only snapshot to prevent item manipulation
                String silentTitle = manager.getMessage("vanish.silent_view_title");
                Inventory snapshot = org.bukkit.Bukkit.createInventory(null, inv.getSize(), silentTitle);
                for (int si = 0; si < inv.getSize(); si++) {
                    org.bukkit.inventory.ItemStack item = inv.getItem(si);
                    if (item != null) {
                        snapshot.setItem(si, item.clone());
                    }
                }
                player.openInventory(snapshot);
                me.ray.midgard.core.text.MessageUtils.send(player, manager.getMessage("vanish.silent_chest"));
            }
        }
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (manager != null && manager.getVanishManager() != null && manager.getVanishManager().isVanished(player)) {
                if (event.getView().getTitle().equals(manager.getMessage("vanish.silent_view_title"))) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
