package me.ray.midgard.modules.professions.blacksmith.forge.listener;

import me.ray.midgard.modules.professions.blacksmith.forge.ForgeStage;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeBlock;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeRegistry;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeStructure;
import me.ray.midgard.modules.professions.blacksmith.forge.session.ForgeSession;
import me.ray.midgard.modules.professions.blacksmith.forge.session.ForgeSessionManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Handles player interactions with forge structures.
 * Right-clicking on interactive blocks triggers forge actions.
 */
public class ForgeInteractListener implements Listener {

    private final ForgeRegistry registry;
    private final ForgeSessionManager sessionManager;

    // Callbacks to ForgeManager
    private BiConsumer<Player, ForgeStructure> onSmithingTableInteract;
    private BiConsumer<Player, ForgeStructure> onFurnaceInteract;
    private BiConsumer<Player, ForgeStructure> onAnvilInteract;
    private BiConsumer<Player, ForgeStructure> onCauldronInteract;
    private BiConsumer<Player, ForgeStructure> onGrindstoneInteract;
    private BiFunction<Player, ForgeStructure, Boolean> onFuelZoneInteract;

    public ForgeInteractListener(ForgeRegistry registry, ForgeSessionManager sessionManager) {
        this.registry = registry;
        this.sessionManager = sessionManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) { return; }
        if (event.getClickedBlock() == null) { return; }

        Player player = event.getPlayer();
        Location clickedLoc = event.getClickedBlock().getLocation();

        // Check if the clicked block is part of a forge
        ForgeStructure forge = findForgeAtLocation(clickedLoc);
        if (forge == null || !forge.isActive()) {
            // Not an interactive block — check nearby forges for fuel deposit
            ForgeStructure nearbyForge = findAnyNearbyForge(clickedLoc);
            if (nearbyForge != null && nearbyForge.isActive() && onFuelZoneInteract != null) {
                if (onFuelZoneInteract.apply(player, nearbyForge)) {
                    event.setCancelled(true);
                }
            }
            return;
        }

        ForgeBlock.ForgeBlockType blockType = forge.getInteractiveTypeAt(clickedLoc);
        if (blockType == null) {
            // Non-interactive forge block — try fuel deposit
            if (onFuelZoneInteract != null) {
                if (onFuelZoneInteract.apply(player, forge)) {
                    event.setCancelled(true);
                }
            }
            return;
        }

        // Cancel vanilla interaction (prevent opening vanilla furnace, etc.)
        event.setCancelled(true);

        // Dispatch to correct handler
        switch (blockType) {
            case SMITHING_TABLE -> {
                if (onSmithingTableInteract != null) { onSmithingTableInteract.accept(player, forge); }
            }
            case FURNACE, BLAST_FURNACE -> {
                ForgeSession session = sessionManager.getSession(player.getUniqueId());
                if (session != null && session.getCurrentStage() == ForgeStage.HEATING) {
                    if (onFurnaceInteract != null) { onFurnaceInteract.accept(player, forge); }
                } else {
                    // No active heating session — open main menu
                    if (onSmithingTableInteract != null) { onSmithingTableInteract.accept(player, forge); }
                }
            }
            case ANVIL -> {
                ForgeSession session = sessionManager.getSession(player.getUniqueId());
                if (session != null && session.getCurrentStage() == ForgeStage.HAMMERING) {
                    if (onAnvilInteract != null) { onAnvilInteract.accept(player, forge); }
                }
            }
            case CAULDRON -> {
                ForgeSession session = sessionManager.getSession(player.getUniqueId());
                if (session != null && session.getCurrentStage() == ForgeStage.QUENCHING) {
                    if (onCauldronInteract != null) { onCauldronInteract.accept(player, forge); }
                }
            }
            case GRINDSTONE -> {
                ForgeSession session = sessionManager.getSession(player.getUniqueId());
                if (session != null && session.getCurrentStage() == ForgeStage.SHARPENING) {
                    if (onGrindstoneInteract != null) { onGrindstoneInteract.accept(player, forge); }
                }
            }
            default -> {
                // Non-interactive block, ignore
            }
        }
    }

    /**
     * Finds the forge structure that contains the given location.
     */
    private ForgeStructure findForgeAtLocation(Location location) {
        // Use chunk-based lookup for efficiency
        var nearby = registry.getNearby(location, 1);
        for (ForgeStructure forge : nearby) {
            if (forge.getInteractiveTypeAt(location) != null) {
                return forge;
            }
        }
        return null;
    }

    /**
     * Finds any forge structure near the given location.
     */
    private ForgeStructure findAnyNearbyForge(Location location) {
        var nearby = registry.getNearby(location, 1);
        for (ForgeStructure forge : nearby) {
            if (forge.isActive()) { return forge; }
        }
        return null;
    }

    // === Setters for callbacks ===
    public void setOnSmithingTableInteract(BiConsumer<Player, ForgeStructure> cb) { this.onSmithingTableInteract = cb; }
    public void setOnFurnaceInteract(BiConsumer<Player, ForgeStructure> cb) { this.onFurnaceInteract = cb; }
    public void setOnAnvilInteract(BiConsumer<Player, ForgeStructure> cb) { this.onAnvilInteract = cb; }
    public void setOnCauldronInteract(BiConsumer<Player, ForgeStructure> cb) { this.onCauldronInteract = cb; }
    public void setOnGrindstoneInteract(BiConsumer<Player, ForgeStructure> cb) { this.onGrindstoneInteract = cb; }
    public void setOnFuelZoneInteract(BiFunction<Player, ForgeStructure, Boolean> cb) { this.onFuelZoneInteract = cb; }
}
