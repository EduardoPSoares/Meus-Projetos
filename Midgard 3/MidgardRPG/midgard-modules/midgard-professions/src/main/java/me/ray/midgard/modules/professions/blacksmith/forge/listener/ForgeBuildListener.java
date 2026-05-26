package me.ray.midgard.modules.professions.blacksmith.forge.listener;

import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeTier;
import me.ray.midgard.modules.professions.blacksmith.forge.ghost.ForgeBlueprintItem;
import me.ray.midgard.modules.professions.blacksmith.forge.ghost.GhostBlockManager;
import me.ray.midgard.modules.professions.blacksmith.forge.ghost.GhostBlockSession;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeRegistry;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeStructure;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Handles forge construction events:
 * - Blueprint item placement (right-click ground) → starts preview
 * - Shift + right-click during preview → confirms & starts building
 * - Block placement during building → tracked against schematic
 * - Block breaking protection for existing forges
 */
public class ForgeBuildListener implements Listener {

    private String msg(String key) { return ProfessionsModule.getInstance().getMessage(key); }

    private final GhostBlockManager ghostBlockManager;
    private final ForgeRegistry registry;
    private final MiniMessage mm = MiniMessage.miniMessage();

    // Callback to validate if player can build (level check, max forges, etc.)
    // Parameters: Player, ForgeTier, templateId (nullable)
    private BlueprintValidationCallback onBlueprintValidation;

    @FunctionalInterface
    public interface BlueprintValidationCallback {
        void accept(Player player, ForgeTier tier, UUID templateId);
    }

    public ForgeBuildListener(GhostBlockManager ghostBlockManager, ForgeRegistry registry) {
        this.ghostBlockManager = ghostBlockManager;
        this.registry = registry;
    }

    /**
     * Handles right-click interactions:
     * 1. Blueprint item on ground block → start preview session
     * 2. Shift + right-click on ground during preview → confirm build
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) { return; }
        if (event.getHand() != EquipmentSlot.HAND) { return; }
        if (event.getClickedBlock() == null) { return; }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Case 1: Player right-clicks with a blueprint item
        if (ForgeBlueprintItem.isBlueprint(item)) {
            event.setCancelled(true);

            ForgeTier tier = ForgeBlueprintItem.getTier(item);
            if (tier == null) { return; }

            // If already in a preview session, shift-click confirms
            GhostBlockSession session = ghostBlockManager.getSession(player.getUniqueId());
            if (session != null && session.isPreviewing()) {
                if (player.isSneaking()) {
                    ghostBlockManager.confirmBuild(player);
                    // Consume the blueprint item
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.sendMessage(mm.deserialize(
                            msg("forge.ghost.preview_hint2")));
                }
                return;
            }

            // If already building, don't start another
            if (session != null && session.isBuilding()) {
                player.sendMessage(mm.deserialize(msg("forge.build.already_building")));
                return;
            }

            // Fire validation callback (checks level, max forges, etc.)
            UUID templateId = ForgeBlueprintItem.getTemplateId(item);
            if (onBlueprintValidation != null) {
                onBlueprintValidation.accept(player, tier, templateId);
                // If validation cancelled (session wasn't started), return
                // The validation callback is responsible for calling ghostBlockManager.startPreview()
                return;
            }

            // Fallback: start preview directly at clicked block + 1 up
            Location anchor = event.getClickedBlock().getLocation().add(0, 1, 0);
            ghostBlockManager.startPreview(player, tier,
                    me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeSchematic.createBasicForge(), anchor);
            return;
        }

        // Case 2: Player shift-right-clicks ground during preview (no blueprint in hand)
        if (player.isSneaking()) {
            GhostBlockSession session = ghostBlockManager.getSession(player.getUniqueId());
            if (session != null && session.isPreviewing()) {
                event.setCancelled(true);
                ghostBlockManager.confirmBuild(player);
            }
        }
    }

    /**
     * Handles block placement during building session.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (!ghostBlockManager.hasSession(player.getUniqueId())) { return; }

        GhostBlockSession session = ghostBlockManager.getSession(player.getUniqueId());
        if (session == null || !session.isBuilding()) { return; }

        Location placedLoc = event.getBlock().getLocation();
        GhostBlockSession.PlaceResult result = ghostBlockManager.onBlockPlaced(player, placedLoc);

        if (result == null) { return; }

        switch (result) {
            case WRONG_BLOCK -> {
                // Cancel the placement — wrong block type
                event.setCancelled(true);
            }
            case NOT_PART_OF_SCHEMATIC -> {
                // Allow normal placement outside the schematic area
            }
            // CORRECT and CORRECT_AND_COMPLETE are handled by GhostBlockManager
        }
    }

    /**
     * Protects forge structures from being broken by non-owners.
     * Also tracks block breaks during build sessions.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location brokenLoc = event.getBlock().getLocation();

        // Check if the block is part of an active forge
        var nearby = registry.getNearby(brokenLoc, 1);
        for (ForgeStructure forge : nearby) {
            if (forge.getInteractiveTypeAt(brokenLoc) != null) {
                if (!forge.getOwnerUuid().equals(player.getUniqueId())
                        && !player.hasPermission("midgard.forge.admin.break")) {
                    event.setCancelled(true);
                    player.sendActionBar(mm.deserialize(msg("forge.build.cannot_destroy_other")));
                    return;
                }
                break;
            }
        }

        // Update build session if the player is building
        if (ghostBlockManager.hasSession(player.getUniqueId())) {
            ghostBlockManager.onBlockBroken(player, brokenLoc);
        }
    }

    public void setOnBlueprintValidation(BlueprintValidationCallback callback) {
        this.onBlueprintValidation = callback;
    }
}
