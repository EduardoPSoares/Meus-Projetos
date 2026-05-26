package midgardvanish.manager;

import midgardvanish.MidgardVanish;
import midgardvanish.data.VanishDataManager;
import midgardvanish.data.VanishSettingsManager;
import midgardvanish.data.VanishSettingsManager.VanishSetting;
import midgardvanish.data.ViewerDataManager;
import midgardvanish.hook.TabHook;
import midgardvanish.listener.PacketListener;
import midgardvanish.nms.NMSHandler;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

public class VanishManager {

    private final MidgardVanish plugin;
    private final VanishDataManager dataManager;
    private final ViewerDataManager viewerDataManager;
    private final VanishSettingsManager settingsManager;
    private final NMSHandler nmsHandler;
    private final Set<UUID> glowTeamViewers = new HashSet<>();
    private final Set<UUID> hadFlyBefore = new HashSet<>();
    private final Set<Long> silentInteractions = ConcurrentHashMap.newKeySet();
    private final Map<UUID, UUID> inventoryViewers = new HashMap<>();
    private final Map<UUID, BukkitTask> inventoryTasks = new HashMap<>();
    private PacketListener packetListener;
    private boolean tabEnabled = false;

    public VanishManager(MidgardVanish plugin, VanishDataManager dataManager, ViewerDataManager viewerDataManager, VanishSettingsManager settingsManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.viewerDataManager = viewerDataManager;
        this.settingsManager = settingsManager;
        this.nmsHandler = new NMSHandler();
        this.tabEnabled = plugin.getServer().getPluginManager().getPlugin("TAB") != null;
        if (tabEnabled) {
            plugin.getLogger().info("TAB detectado! Integração de glow ativada.");
        }
    }

    public void setPacketListener(PacketListener packetListener) {
        this.packetListener = packetListener;
    }

    public boolean canSee(Player viewer, Player vanished) {
        if (isVanished(viewer)) return true;
        if (viewer.hasPermission("midgardvanish.see")) return true;
        return viewerDataManager.isViewer(viewer.getUniqueId(), vanished.getUniqueId());
    }

    public boolean canSeeAny(Player viewer) {
        if (isVanished(viewer)) return true;
        if (viewer.hasPermission("midgardvanish.see")) return true;
        return viewerDataManager.canSeeAny(viewer.getUniqueId());
    }

    public void enableVanish(Player player) {
        dataManager.setVanished(player.getUniqueId(), true);

        // Auto fly (only if player doesn't already have flight from another source)
        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            if (!player.getAllowFlight()) {
                hadFlyBefore.add(player.getUniqueId());
            }
            player.setAllowFlight(true);
            player.setFlying(true);
        }

        // Night vision
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false, false));

        // Log
        plugin.getLogger().info("[VANISH] " + player.getName() + " entrou em vanish.");

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;

            if (canSee(online, player)) {
                online.showPlayer(plugin, player);
                applyGlowForViewer(online, player);
            } else {
                online.hidePlayer(plugin, player);
            }
        }

        // Trigger TAB refresh for all staff so RED glow color is applied
        if (tabEnabled) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!online.isOnline()) continue;
                    TabHook.pauseTeamHandling(online);
                    TabHook.resumeTeamHandling(online);
                }
            }, 10L);
        }
    }

    public void disableVanish(Player player) {
        dataManager.setVanished(player.getUniqueId(), false);

        // Remove fly if we gave it
        if (hadFlyBefore.remove(player.getUniqueId())) {
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }
        }

        // Remove night vision
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);

        // Log
        plugin.getLogger().info("[VANISH] " + player.getName() + " saiu do vanish.");

        removeGlow(player);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            online.showPlayer(plugin, player);
        }

        // Trigger TAB refresh for all staff so RED glow color is removed
        if (tabEnabled) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!online.isOnline()) continue;
                    TabHook.pauseTeamHandling(online);
                    TabHook.resumeTeamHandling(online);
                }
            }, 10L);
        }
    }

    public boolean isVanished(Player player) {
        return dataManager.isVanished(player.getUniqueId());
    }

    public boolean isVanished(UUID uuid) {
        return dataManager.isVanished(uuid);
    }

    public void toggleVanish(Player player) {
        if (isVanished(player)) {
            disableVanish(player);
        } else {
            enableVanish(player);
        }
    }

    public Set<UUID> getVanishedPlayers() {
        return dataManager.getVanishedPlayers();
    }

    public void handleJoin(Player player) {
        if (isVanished(player)) {
            // Immediately hide from non-staff, ensure visibility for those with specific see permission
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(player)) continue;
                if (canSee(online, player)) {
                    online.showPlayer(plugin, player);
                } else {
                    online.hidePlayer(plugin, player);
                }
            }

            // Delayed: apply effects and glow
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;

                if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                    if (!player.getAllowFlight()) {
                        hadFlyBefore.add(player.getUniqueId());
                    }
                    player.setAllowFlight(true);
                    player.setFlying(true);
                }
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false, false));

                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.equals(player)) continue;
                    if (canSee(online, player) && online.getWorld().equals(player.getWorld())) {
                        applyGlowForViewer(online, player);
                    }
                }
            }, 5L);
        }

        // Immediately hide vanished players from non-staff, show to those with specific see permission
        for (UUID vanishedUUID : getVanishedPlayers()) {
            Player vanished = Bukkit.getPlayer(vanishedUUID);
            if (vanished == null || vanished.equals(player)) continue;
            if (canSee(player, vanished)) {
                player.showPlayer(plugin, vanished);
            } else {
                player.hidePlayer(plugin, vanished);
            }
        }

        // Delayed: glow for staff
        if (canSeeAny(player)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                for (UUID vanishedUUID : getVanishedPlayers()) {
                    Player vanished = Bukkit.getPlayer(vanishedUUID);
                    if (vanished == null || vanished.equals(player)) continue;
                    if (!canSee(player, vanished)) continue;
                    if (vanished.getWorld().equals(player.getWorld())) {
                        applyGlowForViewer(player, vanished);
                    }
                }
            }, 5L);
        }

        // Trigger TAB refresh so interceptor can apply RED color
        if (tabEnabled) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                TabHook.pauseTeamHandling(player);
                TabHook.resumeTeamHandling(player);
            }, 40L);
        }
    }

    public void handleQuit(Player player) {
        closeLiveInventory(player);
        if (isVanished(player)) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(player)) continue;
                nmsHandler.removeFromGlowTeam(online, player.getName());
            }
        }
        glowTeamViewers.remove(player.getUniqueId());
        hadFlyBefore.remove(player.getUniqueId());
    }

    public void applyGlow(Player vanished) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(vanished)) continue;
            if (!canSee(online, vanished)) continue;
            applyGlowForViewer(online, vanished);
        }
    }

    public void removeGlow(Player vanished) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(vanished)) continue;
            nmsHandler.removeFromGlowTeam(online, vanished.getName());
            nmsHandler.removeGlowing(online, vanished);
        }
    }

    private void applyGlowForViewer(Player viewer, Player vanished) {
        ensureGlowTeam(viewer);
        nmsHandler.addToGlowTeam(viewer, vanished.getName());
        nmsHandler.sendGlowing(viewer, vanished);
    }

    public void reapplyGlowForViewer(Player viewer, Player vanished) {
        applyGlowForViewer(viewer, vanished);
    }

    private void ensureGlowTeam(Player viewer) {
        if (glowTeamViewers.add(viewer.getUniqueId())) {
            nmsHandler.createGlowTeam(viewer);
        }
    }

    public void refreshGlow() {
        for (UUID vanishedUUID : getVanishedPlayers()) {
            Player vanished = Bukkit.getPlayer(vanishedUUID);
            if (vanished == null || !vanished.isOnline()) continue;

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(vanished)) continue;
                if (canSee(online, vanished)) {
                    online.showPlayer(plugin, vanished);
                    applyGlowForViewer(online, vanished);
                } else {
                    online.hidePlayer(plugin, vanished);
                }
            }
        }
    }

    public void removeAllGlow() {
        for (UUID vanishedUUID : getVanishedPlayers()) {
            Player vanished = Bukkit.getPlayer(vanishedUUID);
            if (vanished == null) continue;
            removeGlow(vanished);
        }
        glowTeamViewers.clear();
    }

    public void restoreAllVisibility() {
        for (UUID vanishedUUID : getVanishedPlayers()) {
            Player vanished = Bukkit.getPlayer(vanishedUUID);
            if (vanished == null) continue;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(vanished)) continue;
                online.showPlayer(plugin, vanished);
            }
        }
    }

    public int getVanishedCount() {
        return (int) getVanishedPlayers().stream()
                .filter(uuid -> Bukkit.getPlayer(uuid) != null)
                .count();
    }

    public void refreshGlowForVanished(Player vanished) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(vanished)) continue;
            if (canSee(online, vanished) && online.getWorld().equals(vanished.getWorld())) {
                applyGlowForViewer(online, vanished);
            }
        }
    }

    public void refreshGlowForViewer(Player viewer) {
        for (UUID vanishedUUID : getVanishedPlayers()) {
            Player vanished = Bukkit.getPlayer(vanishedUUID);
            if (vanished == null || vanished.equals(viewer)) continue;
            if (!canSee(viewer, vanished)) continue;
            if (vanished.getWorld().equals(viewer.getWorld())) {
                applyGlowForViewer(viewer, vanished);
            }
        }
    }

    // ====== Silent Block Interactions ======

    public void addSilentInteraction(long packedPos) {
        silentInteractions.add(packedPos);
    }

    public void removeSilentInteraction(long packedPos) {
        silentInteractions.remove(packedPos);
    }

    public boolean isSilentInteraction(long packedPos) {
        return silentInteractions.contains(packedPos);
    }

    // ====== Live Inventory Viewer ======

    public void openLiveInventory(Player viewer, Player target) {
        closeLiveInventory(viewer);

        Inventory inv = Bukkit.createInventory(null, 54, "§8ɪɴᴠᴇɴᴛáʀɪᴏ ᴅᴇ §e" + target.getName());
        updateInventoryContents(inv, target);

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);

        int[] glassSlots = {36, 41, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53};
        for (int slot : glassSlots) {
            inv.setItem(slot, glass);
        }

        viewer.openInventory(inv);
        inventoryViewers.put(viewer.getUniqueId(), target.getUniqueId());

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            var targetId = inventoryViewers.get(viewer.getUniqueId());
            if (targetId == null) return;

            Player t = Bukkit.getPlayer(targetId);
            if (t == null || !viewer.isOnline()) {
                if (viewer.isOnline()) viewer.closeInventory();
                return;
            }

            Inventory topInv = viewer.getOpenInventory().getTopInventory();
            if (topInv.getSize() != 54) {
                closeLiveInventory(viewer);
                return;
            }

            updateInventoryContents(topInv, t);
        }, 5L, 5L);

        inventoryTasks.put(viewer.getUniqueId(), task);
    }

    private void updateInventoryContents(Inventory inv, Player target) {
        PlayerInventory pi = target.getInventory();

        for (int i = 9; i <= 35; i++) {
            inv.setItem(i - 9, pi.getItem(i));
        }

        for (int i = 0; i <= 8; i++) {
            inv.setItem(27 + i, pi.getItem(i));
        }

        inv.setItem(37, pi.getHelmet());
        inv.setItem(38, pi.getChestplate());
        inv.setItem(39, pi.getLeggings());
        inv.setItem(40, pi.getBoots());
        inv.setItem(42, pi.getItemInOffHand());
    }

    public void closeLiveInventory(Player viewer) {
        inventoryViewers.remove(viewer.getUniqueId());
        BukkitTask task = inventoryTasks.remove(viewer.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    public boolean isViewingInventory(UUID viewerUUID) {
        return inventoryViewers.containsKey(viewerUUID);
    }

    public void refreshVisibilityFor(Player viewer) {
        for (UUID vanishedUUID : getVanishedPlayers()) {
            Player vanished = Bukkit.getPlayer(vanishedUUID);
            if (vanished == null || vanished.equals(viewer)) continue;

            if (canSee(viewer, vanished)) {
                viewer.showPlayer(plugin, vanished);
                if (vanished.getWorld().equals(viewer.getWorld())) {
                    applyGlowForViewer(viewer, vanished);
                }
            } else {
                viewer.hidePlayer(plugin, vanished);
                nmsHandler.removeFromGlowTeam(viewer, vanished.getName());
                nmsHandler.removeGlowing(viewer, vanished);
            }
        }
    }

    public void refreshNameTagsForPlayer(Player player) {
        if (tabEnabled) {
            TabHook.pauseTeamHandling(player);
            TabHook.resumeTeamHandling(player);
        }
    }

    public boolean isTabEnabled() {
        return tabEnabled;
    }

    public boolean hasSetting(Player player, VanishSetting setting) {
        return settingsManager.isEnabled(player.getUniqueId(), setting);
    }

    public VanishSettingsManager getSettingsManager() {
        return settingsManager;
    }

    public ViewerDataManager getViewerDataManager() {
        return viewerDataManager;
    }

    public NMSHandler getNmsHandler() {
        return nmsHandler;
    }
}
