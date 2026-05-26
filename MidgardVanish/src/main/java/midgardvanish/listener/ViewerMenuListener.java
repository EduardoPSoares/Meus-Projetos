package midgardvanish.listener;

import midgardvanish.MidgardVanish;
import midgardvanish.data.ViewerDataManager;
import midgardvanish.gui.ViewerMenuGUI;
import midgardvanish.manager.VanishManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class ViewerMenuListener implements Listener {

    private final MidgardVanish plugin;
    private final ViewerDataManager viewerDataManager;
    private final ViewerMenuGUI viewerMenuGUI;
    private final VanishManager vanishManager;

    public ViewerMenuListener(MidgardVanish plugin, ViewerDataManager viewerDataManager,
                              ViewerMenuGUI viewerMenuGUI, VanishManager vanishManager) {
        this.plugin = plugin;
        this.viewerDataManager = viewerDataManager;
        this.viewerMenuGUI = viewerMenuGUI;
        this.vanishManager = vanishManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();

        if (title.startsWith(ViewerMenuGUI.MENU_TITLE)) {
            event.setCancelled(true);
            handleMainMenuClick(player, event);
        } else if (title.startsWith(ViewerMenuGUI.ADD_MENU_TITLE)) {
            event.setCancelled(true);
            handleAddMenuClick(player, event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (title.startsWith(ViewerMenuGUI.MENU_TITLE) || title.startsWith(ViewerMenuGUI.ADD_MENU_TITLE)) {
            // Cleanup on next tick to avoid clearing state during page navigation
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                String openTitle = player.getOpenInventory().getTitle();
                if (!openTitle.startsWith(ViewerMenuGUI.MENU_TITLE) && !openTitle.startsWith(ViewerMenuGUI.ADD_MENU_TITLE)) {
                    viewerMenuGUI.cleanup(player.getUniqueId());
                }
            }, 1L);
        }
    }

    private void handleMainMenuClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = event.getRawSlot();
        int page = viewerMenuGUI.getPlayerPage(player.getUniqueId());
        UUID vanishedUUID = viewerMenuGUI.getManagingTarget(player.getUniqueId());
        if (vanishedUUID == null) return;

        // Previous page
        if (slot == 48 && clicked.getType() == Material.ARROW) {
            viewerMenuGUI.openMainMenu(player, vanishedUUID, page - 1);
            return;
        }

        // Add viewer button
        if (slot == 49 && clicked.getType() == Material.EMERALD) {
            viewerMenuGUI.openAddMenu(player, vanishedUUID, 0);
            return;
        }

        // Next page
        if (slot == 50 && clicked.getType() == Material.ARROW) {
            viewerMenuGUI.openMainMenu(player, vanishedUUID, page + 1);
            return;
        }

        // Click on a player head = remove viewer
        if (clicked.getType() == Material.PLAYER_HEAD && clicked.getItemMeta() instanceof SkullMeta skullMeta) {
            OfflinePlayer target = skullMeta.getOwningPlayer();
            if (target == null) return;

            viewerDataManager.removeViewer(vanishedUUID, target.getUniqueId());
            player.sendMessage("§e" + (target.getName() != null ? target.getName() : "???") + " §cʀᴇᴍᴏᴠɪᴅᴏ ᴅᴏs ᴠɪᴇᴡᴇʀs.");

            // Refresh visibility for this viewer if online
            Player onlineTarget = Bukkit.getPlayer(target.getUniqueId());
            if (onlineTarget != null) {
                vanishManager.refreshVisibilityFor(onlineTarget);
            }

            viewerMenuGUI.openMainMenu(player, vanishedUUID, page);
        }
    }

    private void handleAddMenuClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = event.getRawSlot();
        int page = viewerMenuGUI.getPlayerPage(player.getUniqueId());
        UUID vanishedUUID = viewerMenuGUI.getManagingTarget(player.getUniqueId());
        if (vanishedUUID == null) return;

        // Previous page
        if (slot == 48 && clicked.getType() == Material.ARROW) {
            viewerMenuGUI.openAddMenu(player, vanishedUUID, page - 1);
            return;
        }

        // Back button
        if (slot == 49 && clicked.getType() == Material.BARRIER) {
            viewerMenuGUI.openMainMenu(player, vanishedUUID, 0);
            return;
        }

        // Next page
        if (slot == 50 && clicked.getType() == Material.ARROW) {
            viewerMenuGUI.openAddMenu(player, vanishedUUID, page + 1);
            return;
        }

        // Click on a player head = add viewer
        if (clicked.getType() == Material.PLAYER_HEAD && clicked.getItemMeta() instanceof SkullMeta skullMeta) {
            OfflinePlayer target = skullMeta.getOwningPlayer();
            if (target == null) return;

            viewerDataManager.addViewer(vanishedUUID, target.getUniqueId());
            player.sendMessage("§e" + (target.getName() != null ? target.getName() : "???") + " §aᴀᴅɪᴄɪᴏɴᴀᴅᴏ ᴄᴏᴍᴏ ᴠɪᴇᴡᴇʀ.");

            // Refresh visibility for this viewer if online
            Player onlineTarget = Bukkit.getPlayer(target.getUniqueId());
            if (onlineTarget != null) {
                vanishManager.refreshVisibilityFor(onlineTarget);
            }

            viewerMenuGUI.openAddMenu(player, vanishedUUID, page);
        }
    }
}
