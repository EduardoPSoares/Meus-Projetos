package com.midgard.fooddecay.multiblock;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Handles world events for the multiblock session-based building system.
 * Routes: control items, blueprint use, session blocks, active multiblock interact.
 * Prevents inventory changes during POSITIONING phase.
 */
public class MultiblockListener implements Listener {

    private final MultiblockManager multiblockManager;

    public MultiblockListener(MultiblockManager multiblockManager) {
        this.multiblockManager = multiblockManager;
    }

    // =========================================================================
    //  Interaction (controls, blueprint, multiblock)
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        // Positioning phase: all interactions blocked, control items handled
        if (multiblockManager.isInPositioningPhase(player.getUniqueId())) {
            event.setCancelled(true);
            if (event.getAction() != Action.PHYSICAL) {
                String action = multiblockManager.getControlAction(hand);
                if (action != null) {
                    boolean rightClick = event.getAction() == Action.RIGHT_CLICK_AIR
                            || event.getAction() == Action.RIGHT_CLICK_BLOCK;
                    multiblockManager.handleControl(player, action, rightClick);
                }
            }
            return;
        }

        // Blueprint right-click on block → start session
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block == null) return;

            MultiblockType blueprintType = multiblockManager.getBlueprintType(hand);
            if (blueprintType != null) {
                event.setCancelled(true);
                int tier = multiblockManager.getBlueprintTier(hand);
                multiblockManager.startSession(player, blueprintType, tier, block.getLocation());
                return;
            }

            // Active multiblock interaction (food processing)
            if (multiblockManager.onInteract(player, block)) {
                event.setCancelled(true);
            }
        }
    }

    // =========================================================================
    //  Block Place / Break
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (multiblockManager.isInPositioningPhase(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        if (multiblockManager.onSessionBlockPlace(player, event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (multiblockManager.isInPositioningPhase(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        multiblockManager.onSessionBlockBreak(player, event.getBlock());
        multiblockManager.onMultiblockBreak(player, event.getBlock());
    }

    // =========================================================================
    //  Inventory Protection (positioning phase)
    // =========================================================================

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (multiblockManager.isInPositioningPhase(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (multiblockManager.isInPositioningPhase(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (multiblockManager.isInPositioningPhase(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // =========================================================================
    //  Disconnect & Reconnect
    // =========================================================================

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (multiblockManager.hasActiveSession(event.getPlayer().getUniqueId())) {
            multiblockManager.cancelSession(event.getPlayer());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (multiblockManager.hasActiveSession(event.getEntity().getUniqueId())) {
            multiblockManager.cancelSession(event.getEntity());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        multiblockManager.restoreInventoryBackup(event.getPlayer());
    }

    // =========================================================================
    //  Piston & Explosion Protection
    // =========================================================================

    /**
     * Prevent pistons from pushing blocks that are part of active multiblock structures.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (multiblockManager.isActiveMultiblock(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Prevent pistons from pulling blocks that are part of active multiblock structures.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (multiblockManager.isActiveMultiblock(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Clean up multiblock structures destroyed by entity explosions (TNT, creepers).
     * BlockBreakEvent does not fire for explosions.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosionBlocks(event.blockList());
    }

    /**
     * Clean up multiblock structures destroyed by block explosions (beds, respawn anchors).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosionBlocks(event.blockList());
    }

    private void handleExplosionBlocks(java.util.List<Block> blockList) {
        for (Block block : blockList) {
            if (multiblockManager.isActiveMultiblock(block.getLocation())) {
                multiblockManager.onMultiblockBreak(null, block);
            }
        }
    }

    // =========================================================================
    //  Chunk Load — clean orphaned entities
    // =========================================================================

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        multiblockManager.cleanOrphanedInChunk(event.getChunk());
    }
}
