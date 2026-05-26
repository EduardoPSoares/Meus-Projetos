package de.maxhenkel.voicechat.range.gui;

import de.maxhenkel.voicechat.Voicechat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class RangeMenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (title.equals(RangeListMenu.getTitle())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            handleListClick(player, clicked, event.getSlot());
        } else if (title.equals(RangePlayerSelectMenu.getTitlePrefix())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            handlePlayerSelectClick(player, clicked, event.getSlot());
        } else if (title.startsWith(RangeDistanceMenu.getTitlePrefix())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            UUID targetUuid = RangeDistanceMenu.getSelectedTarget(player.getUniqueId());
            if (targetUuid == null) {
                String targetName = title.substring(RangeDistanceMenu.getTitlePrefix().length());
                targetUuid = findPlayerUuid(targetName);
            }
            handleDistanceClick(player, targetUuid, clicked, event.getSlot());
        } else if (title.equals(RangeGlobalMenu.getTitle())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            handleGlobalClick(player, clicked, event.getSlot());
        } else if (title.equals(RangeGlobalAddMenu.getTitle())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            handleGlobalAddClick(player, clicked, event.getSlot());
        }
    }

    private boolean isDecoration(ItemStack item) {
        Material type = item.getType();
        return type == Material.PURPLE_STAINED_GLASS_PANE
                || type == Material.GRAY_STAINED_GLASS_PANE
                || type == Material.NETHER_STAR
                || type == Material.BOOK;
    }

    private void handleListClick(Player player, ItemStack clicked, int slot) {
        // Botao voltar (slot 45)
        if (slot == 45 && clicked.getType() == Material.ARROW) {
            de.maxhenkel.voicechat.gui.AdminHubMenu.open(player);
            return;
        }

        if (slot == 46 && clicked.getType() == Material.EMERALD) {
            RangePlayerSelectMenu.open(player);
            return;
        }

        if ((slot == 47 || slot == 51) && clicked.getType() == Material.SPECTRAL_ARROW) {
            int currentPage = RangeListMenu.getPage(player.getUniqueId());
            int newPage = slot == 47 ? currentPage - 1 : currentPage + 1;
            RangeListMenu.open(player, newPage);
            return;
        }

        // Botao recarregar (slot 52)
        if (slot == 52 && clicked.getType() == Material.GLOWSTONE_DUST) {
            Voicechat.playerRangeManager.load();
            player.sendMessage(Voicechat.MESSAGES.gui_range_recarregadas);
            int currentPage = RangeListMenu.getPage(player.getUniqueId());
            Bukkit.getScheduler().runTaskLater(Voicechat.INSTANCE, () -> RangeListMenu.open(player, currentPage), 1L);
            return;
        }

        // Botao global (slot 49)
        if (slot == 49 && clicked.getType() == Material.ENDER_EYE) {
            RangeGlobalMenu.open(player);
            return;
        }

        // Clique em jogador com range
        if (clicked.getType() == Material.PLAYER_HEAD) {
            UUID targetUuid = getPlayerUuidFromSkull(clicked);
            if (targetUuid != null) {
                RangeDistanceMenu.open(player, targetUuid);
            }
        }
    }

    private void handlePlayerSelectClick(Player player, ItemStack clicked, int slot) {
        // Botao voltar (slot 45)
        if (slot == 45 && clicked.getType() == Material.ARROW) {
            RangeListMenu.open(player, RangeListMenu.getPage(player.getUniqueId()));
            return;
        }

        if ((slot == 47 || slot == 51) && clicked.getType() == Material.SPECTRAL_ARROW) {
            int currentPage = RangePlayerSelectMenu.getPage(player.getUniqueId());
            int newPage = slot == 47 ? currentPage - 1 : currentPage + 1;
            RangePlayerSelectMenu.open(player, newPage);
            return;
        }

        // Clique em jogador
        if (clicked.getType() == Material.PLAYER_HEAD) {
            UUID targetUuid = getPlayerUuidFromSkull(clicked);
            if (targetUuid != null) {
                RangeDistanceMenu.open(player, targetUuid);
            }
        }
    }

    private void handleDistanceClick(Player player, UUID targetUuid, ItemStack clicked, int slot) {
        if (targetUuid == null) {
            player.sendMessage(Voicechat.MESSAGES.jogador_nao_encontrado);
            RangeListMenu.open(player, RangeListMenu.getPage(player.getUniqueId()));
            return;
        }

        // Botao voltar (slot 36)
        if (slot == 36 && clicked.getType() == Material.ARROW) {
            RangeListMenu.open(player, RangeListMenu.getPage(player.getUniqueId()));
            return;
        }

        // Botao remover range (slot 31)
        if (slot == 31 && clicked.getType() == Material.BARRIER) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
            String name = target.getName() != null ? target.getName() : targetUuid.toString().substring(0, 8);
            if (Voicechat.playerRangeManager.removeRange(targetUuid)) {
                player.sendMessage(String.format(Voicechat.MESSAGES.gui_range_removido_gui, name));
                Voicechat.activityLogger.logRangeRemoved(player.getName(), name);
            } else {
                player.sendMessage(String.format(Voicechat.MESSAGES.range_sem_custom, name));
            }
            RangeListMenu.open(player, RangeListMenu.getPage(player.getUniqueId()));
            return;
        }

        // Opcoes de distancia (slots 19-25)
        if ((clicked.getType() == Material.WHITE_WOOL || clicked.getType() == Material.LIME_WOOL) && slot >= 19 && slot <= 25) {
            int[] distances = {48, 64, 96, 128, 200, 300, 500};
            int index = slot - 19;
            if (index >= 0 && index < distances.length) {
                float distance = distances[index];
                Voicechat.playerRangeManager.setRange(targetUuid, distance);
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
                String name = target.getName() != null ? target.getName() : targetUuid.toString().substring(0, 8);
                player.sendMessage(String.format(Voicechat.MESSAGES.gui_range_distancia_definido, name, String.valueOf(distance)));
                Voicechat.activityLogger.logRangeSet(player.getName(), name, distance);
                Bukkit.getScheduler().runTaskLater(Voicechat.INSTANCE, () -> RangeDistanceMenu.open(player, targetUuid), 1L);
            }
        }
    }

    private void handleGlobalClick(Player player, ItemStack clicked, int slot) {
        if ((slot == 47 || slot == 51) && clicked.getType() == Material.SPECTRAL_ARROW) {
            int currentPage = RangeGlobalMenu.getPage(player.getUniqueId());
            int newPage = slot == 47 ? currentPage - 1 : currentPage + 1;
            RangeGlobalMenu.open(player, newPage);
            return;
        }

        // Botao adicionar (slot 49)
        if (slot == 49 && clicked.getType() == Material.EMERALD) {
            RangeGlobalAddMenu.open(player);
            return;
        }

        // Botao voltar (slot 45)
        if (slot == 45 && clicked.getType() == Material.ARROW) {
            de.maxhenkel.voicechat.gui.AdminHubMenu.open(player);
            return;
        }

        // Clique em jogador global (remover)
        if (clicked.getType() == Material.PLAYER_HEAD) {
            UUID targetUuid = getPlayerUuidFromSkull(clicked);
            if (targetUuid != null) {
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
                String name = target.getName() != null ? target.getName() : targetUuid.toString().substring(0, 8);
                Voicechat.playerRangeManager.removeGlobalPlayer(targetUuid);
                player.sendMessage(String.format(Voicechat.MESSAGES.gui_global_removido, name));
                Voicechat.activityLogger.logGlobalRemoved(player.getName(), name);
                int currentPage = RangeGlobalMenu.getPage(player.getUniqueId());
                Bukkit.getScheduler().runTaskLater(Voicechat.INSTANCE, () -> RangeGlobalMenu.open(player, currentPage), 1L);
            }
        }
    }

    private void handleGlobalAddClick(Player player, ItemStack clicked, int slot) {
        // Botao voltar (slot 45)
        if (slot == 45 && clicked.getType() == Material.ARROW) {
            RangeGlobalMenu.open(player, RangeGlobalMenu.getPage(player.getUniqueId()));
            return;
        }

        if ((slot == 47 || slot == 51) && clicked.getType() == Material.SPECTRAL_ARROW) {
            int currentPage = RangeGlobalAddMenu.getPage(player.getUniqueId());
            int newPage = slot == 47 ? currentPage - 1 : currentPage + 1;
            RangeGlobalAddMenu.open(player, newPage);
            return;
        }

        // Clique em jogador (adicionar como global)
        if (clicked.getType() == Material.PLAYER_HEAD) {
            UUID targetUuid = getPlayerUuidFromSkull(clicked);
            if (targetUuid != null) {
                if (Voicechat.playerRangeManager.isGlobalLimitReached()) {
                    player.sendMessage(String.format(Voicechat.MESSAGES.global_limite_atingido, Voicechat.playerRangeManager.getMaxGlobalPlayers()));
                    return;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
                String name = target.getName() != null ? target.getName() : targetUuid.toString().substring(0, 8);
                Voicechat.playerRangeManager.addGlobalPlayer(targetUuid);
                player.sendMessage(String.format(Voicechat.MESSAGES.gui_global_adicionado, name));
                Voicechat.activityLogger.logGlobalAdded(player.getName(), name);
                int currentPage = RangeGlobalMenu.getPage(player.getUniqueId());
                Bukkit.getScheduler().runTaskLater(Voicechat.INSTANCE, () -> RangeGlobalMenu.open(player, currentPage), 1L);
            }
        }
    }

    private UUID getPlayerUuidFromSkull(ItemStack item) {
        if (item.getType() != Material.PLAYER_HEAD) return null;
        if (!(item.getItemMeta() instanceof SkullMeta)) return null;
        SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
        OfflinePlayer owner = skullMeta.getOwningPlayer();
        if (owner != null) {
            return owner.getUniqueId();
        }
        return null;
    }

    private UUID findPlayerUuid(String name) {
        // Tentar encontrar jogador online primeiro
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online.getUniqueId();
        }
        // Tentar encontrar nos ranges salvos
        for (UUID uuid : Voicechat.playerRangeManager.getAllRanges().keySet()) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if (offlinePlayer.getName() != null && offlinePlayer.getName().equals(name)) {
                return uuid;
            }
        }
        return null;
    }

}
