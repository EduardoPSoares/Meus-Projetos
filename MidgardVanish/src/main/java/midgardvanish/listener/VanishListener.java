package midgardvanish.listener;

import midgardvanish.MidgardVanish;
import midgardvanish.data.VanishSettingsManager.VanishSetting;
import midgardvanish.manager.VanishManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.Bisected;
import org.bukkit.inventory.EquipmentSlot;

import net.minecraft.core.BlockPos;

import java.util.Set;
import java.util.UUID;

public class VanishListener implements Listener {

    private final MidgardVanish plugin;
    private final VanishManager vanishManager;

    private static final Set<Material> PRESSURE_PLATES = Set.of(
            Material.OAK_PRESSURE_PLATE, Material.SPRUCE_PRESSURE_PLATE,
            Material.BIRCH_PRESSURE_PLATE, Material.JUNGLE_PRESSURE_PLATE,
            Material.ACACIA_PRESSURE_PLATE, Material.DARK_OAK_PRESSURE_PLATE,
            Material.CHERRY_PRESSURE_PLATE, Material.BAMBOO_PRESSURE_PLATE,
            Material.MANGROVE_PRESSURE_PLATE, Material.CRIMSON_PRESSURE_PLATE,
            Material.WARPED_PRESSURE_PLATE, Material.PALE_OAK_PRESSURE_PLATE,
            Material.STONE_PRESSURE_PLATE,
            Material.POLISHED_BLACKSTONE_PRESSURE_PLATE,
            Material.LIGHT_WEIGHTED_PRESSURE_PLATE, Material.HEAVY_WEIGHTED_PRESSURE_PLATE
    );

    public VanishListener(MidgardVanish plugin, VanishManager vanishManager) {
        this.plugin = plugin;
        this.vanishManager = vanishManager;
    }

    // ====== Join / Quit ======

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (vanishManager.isVanished(player)) {
            event.setJoinMessage(null);
        }
        vanishManager.handleJoin(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (vanishManager.isVanished(player)) {
            event.setQuitMessage(null);
        }
        vanishManager.handleQuit(player);
    }

    // ====== Teleport / World Change ======

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (!vanishManager.isVanished(player)) return;

        // Skip cross-world teleports - handled by onPlayerChangedWorld
        if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) return;

        // Re-apply glow after same-world teleport
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            vanishManager.refreshGlowForVanished(player);
        }, 2L);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        if (vanishManager.isVanished(player)) {
            // Re-hide from non-staff in new world
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(player)) continue;
                if (!vanishManager.canSee(online, player)) {
                    online.hidePlayer(plugin, player);
                }
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                vanishManager.refreshGlowForVanished(player);
            }, 5L);
        }

        if (vanishManager.canSeeAny(player)) {
            // Staff changed world - refresh their glow view
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                vanishManager.refreshGlowForViewer(player);
            }, 5L);
        }

        // Refresh nametag visibility after world change
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            vanishManager.refreshNameTagsForPlayer(player);
        }, 10L);
    }

    // ====== Respawn ======

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!vanishManager.isVanished(player)) return;

        // Re-apply vanish after respawn (client gets new entity state)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(player)) continue;
                if (!vanishManager.canSee(online, player)) {
                    online.hidePlayer(plugin, player);
                }
            }
            // Re-apply fly and night vision (lost on respawn)
            if (player.getGameMode() != org.bukkit.GameMode.CREATIVE && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                player.setAllowFlight(true);
                player.setFlying(true);
            }
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false, false));
            vanishManager.refreshGlowForVanished(player);
        }, 3L);
    }

    // ====== Death ======

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (vanishManager.isVanished(player)) {
            event.setDeathMessage(null);
            event.setKeepInventory(true);
            event.getDrops().clear();
            event.setKeepLevel(true);
            event.setDroppedExp(0);
        }
    }

    // ====== Mob targeting ======

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getTarget() instanceof Player player) {
            if (vanishManager.isVanished(player) && vanishManager.hasSetting(player, VanishSetting.NO_MOB_TARGET)) {
                event.setCancelled(true);
            }
        }
    }

    // ====== Combat ======

    // Prevent ALL damage to vanished players (fire, lava, fall, etc.)
    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && vanishManager.isVanished(player)
                && vanishManager.hasSetting(player, VanishSetting.NO_DAMAGE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Player attacker = null;
        if (event.getDamager() instanceof Player player) {
            attacker = player;
        } else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            attacker = player;
        }
        if (attacker != null && vanishManager.isVanished(attacker) && vanishManager.hasSetting(attacker, VanishSetting.NO_DAMAGE)) {
            event.setCancelled(true);
            attacker.sendMessage("§cᴠᴏᴄê ɴãᴏ ᴘᴏᴅᴇ ᴀᴛᴀᴄᴀʀ ᴇɴǫᴜᴀɴᴛᴏ ᴇsᴛá ᴇᴍ ᴠᴀɴɪsʜ.");
        }
    }

    // ====== Food ======

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (vanishManager.isVanished(player) && vanishManager.hasSetting(player, VanishSetting.NO_HUNGER)) {
                event.setCancelled(true);
            }
        }
    }

    // ====== Item pickup / drop ======

    @EventHandler
    public void onPlayerPickupItem(PlayerAttemptPickupItemEvent event) {
        if (vanishManager.isVanished(event.getPlayer()) && vanishManager.hasSetting(event.getPlayer(), VanishSetting.NO_ITEM_PICKUP)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (vanishManager.isVanished(event.getPlayer()) && vanishManager.hasSetting(event.getPlayer(), VanishSetting.NO_ITEM_DROP)) {
            event.setCancelled(true);
        }
    }

    // ====== Block place / break ======

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (vanishManager.isVanished(player) && vanishManager.hasSetting(player, VanishSetting.NO_BLOCK_PLACE)) {
            event.setCancelled(true);
            player.sendMessage("§cᴠᴏᴄê ɴãᴏ ᴘᴏᴅᴇ ᴄᴏʟᴏᴄᴀʀ ʙʟᴏᴄᴏs ᴇɴǫᴜᴀɴᴛᴏ ᴇsᴛá ᴇᴍ ᴠᴀɴɪsʜ.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (vanishManager.isVanished(player) && vanishManager.hasSetting(player, VanishSetting.NO_BLOCK_BREAK)) {
            event.setCancelled(true);
            player.sendMessage("§cᴠᴏᴄê ɴãᴏ ᴘᴏᴅᴇ ǫᴜᴇʙʀᴀʀ ʙʟᴏᴄᴏs ᴇɴǫᴜᴀɴᴛᴏ ᴇsᴛá ᴇᴍ ᴠᴀɴɪsʜ.");
        }
    }

    // ====== Pressure plates / Tripwires ======

    @EventHandler
    public void onPlayerInteractPhysical(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) return;
        if (!vanishManager.isVanished(event.getPlayer())) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Material type = block.getType();
        if (PRESSURE_PLATES.contains(type) || type == Material.TRIPWIRE || type == Material.FARMLAND) {
            event.setCancelled(true);
        }
    }

    // ====== Open player inventory on right-click ======

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!vanishManager.isVanished(player)) return;
        if (!player.hasPermission("midgardvanish.interact")) return;

        if (event.getRightClicked() instanceof Player target) {
            event.setCancelled(true);
            vanishManager.openLiveInventory(player, target);
        }
    }

    // ====== MOTD - Fake player count ======

    @SuppressWarnings("removal")
    @EventHandler
    public void onPaperServerListPing(PaperServerListPingEvent event) {
        // Remove vanished players from the hover player sample
        event.getPlayerSample().removeIf(profile -> vanishManager.isVanished(profile.getId()));
        // Subtract vanished from displayed count
        int vanishedOnline = vanishManager.getVanishedCount();
        event.setNumPlayers(event.getNumPlayers() - vanishedOnline);
    }

    // ====== Anti-Leak: Block /list, /who, /online from showing vanished players ======

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (vanishManager.canSeeAny(player)) return;

        String cmd = event.getMessage().toLowerCase().split(" ")[0];
        if (cmd.equals("/list") || cmd.equals("/who") || cmd.equals("/online")) {
            event.setCancelled(true);

            StringBuilder sb = new StringBuilder("§aᴊᴏɢᴀᴅᴏʀᴇs ᴏɴʟɪɴᴇ §7(");
            int visibleCount = 0;
            StringBuilder names = new StringBuilder();

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!vanishManager.isVanished(online) || vanishManager.canSee(player, online)) {
                    if (names.length() > 0) names.append("§7, ");
                    names.append("§f").append(online.getName());
                    visibleCount++;
                }
            }

            sb.append(visibleCount).append("/").append(Bukkit.getMaxPlayers()).append(")§7: ");
            sb.append(names);
            player.sendMessage(sb.toString());
        }
    }

    // ====== Silent block interactions (doors, gates, levers, buttons) ======

    @EventHandler(priority = EventPriority.LOWEST)
    public void onVanishBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!vanishManager.isVanished(event.getPlayer())) return;
        if (!vanishManager.hasSetting(event.getPlayer(), VanishSetting.SILENT_DOOR)) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        String name = block.getType().name();
        BlockData data = block.getBlockData();

        boolean isOpenable = data instanceof Openable;
        boolean isLever = name.contains("LEVER") && data instanceof Powerable;
        boolean isButton = name.contains("BUTTON");

        if (!isOpenable && !isLever && !isButton) return;

        // Sound suppression tracking
        long packed = BlockPos.asLong(block.getX(), block.getY(), block.getZ());
        vanishManager.addSilentInteraction(packed);

        // For doors/gates/trapdoors + levers: cancel and handle manually
        if (isOpenable || isLever) {
            event.setCancelled(true);

            // Capture old state
            BlockData oldData = data.clone();

            // Handle door other half
            Block otherHalf = null;
            BlockData otherOldData = null;
            if (data instanceof Door door && !name.contains("TRAPDOOR")) {
                otherHalf = door.getHalf() == Bisected.Half.BOTTOM
                        ? block.getRelative(0, 1, 0)
                        : block.getRelative(0, -1, 0);
                otherOldData = otherHalf.getBlockData().clone();
                vanishManager.addSilentInteraction(BlockPos.asLong(otherHalf.getX(), otherHalf.getY(), otherHalf.getZ()));
            }

            // Toggle the block state manually
            if (data instanceof Openable openable) {
                openable.setOpen(!openable.isOpen());
                block.setBlockData(openable, false);
            } else if (data instanceof Powerable powerable) {
                powerable.setPowered(!powerable.isPowered());
                block.setBlockData(powerable, false);
            }

            // Sync door other half (physics=false won't propagate)
            if (otherHalf != null) {
                BlockData otherData = otherHalf.getBlockData();
                if (otherData instanceof Openable otherOpenable) {
                    boolean newOpen = ((Openable) block.getBlockData()).isOpen();
                    otherOpenable.setOpen(newOpen);
                    otherHalf.setBlockData(otherOpenable, false);
                }
            }

            // IMMEDIATELY send old state to non-staff (overrides the broadcast from setBlockData)
            final Block otherF = otherHalf;
            final BlockData otherOldF = otherOldData;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (vanishManager.canSee(online, event.getPlayer())) continue;
                if (vanishManager.isVanished(online)) continue;

                online.sendBlockChange(block.getLocation(), oldData);
                if (otherF != null && otherOldF != null) {
                    online.sendBlockChange(otherF.getLocation(), otherOldF);
                }
            }

            // Safety net: resend old state 1 tick later
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (vanishManager.canSee(online, event.getPlayer())) continue;
                    if (vanishManager.isVanished(online)) continue;

                    online.sendBlockChange(block.getLocation(), oldData);
                    if (otherF != null && otherOldF != null) {
                        online.sendBlockChange(otherF.getLocation(), otherOldF);
                    }
                }
            }, 1L);
        }

        // Cleanup sound tracking
        final Block otherCleanup = (data instanceof Door door && !name.contains("TRAPDOOR"))
                ? (door.getHalf() == Bisected.Half.BOTTOM ? block.getRelative(0, 1, 0) : block.getRelative(0, -1, 0))
                : null;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            vanishManager.removeSilentInteraction(packed);
            if (otherCleanup != null) {
                vanishManager.removeSilentInteraction(BlockPos.asLong(otherCleanup.getX(), otherCleanup.getY(), otherCleanup.getZ()));
            }
        }, 5L);
    }

    // ====== Live inventory viewer ======

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            vanishManager.closeLiveInventory(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (vanishManager.isViewingInventory(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (vanishManager.isViewingInventory(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }
}

