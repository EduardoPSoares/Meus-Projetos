package me.ray.midgard.modules.professions.blacksmith.forge.ghost;

import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeRotation;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeTier;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeBlock;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeSchematic;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Manages forge construction mini-game sessions using Block Display entities.
 * Ghost blocks are visualized as BlockDisplay entities with glow outlines,
 * replacing the old sendBlockChange approach for a more immersive experience.
 *
 * Flow:
 * 1. Player places blueprint → PREVIEWING (display entities visible, can rotate)
 * 2. Player shift-right-clicks → BUILDING (blocks tracked, HUD shown)
 * 3. Player places all blocks → COMPLETED → callback fires
 */
public class GhostBlockManager {

    private String msg(String key) { return ProfessionsModule.getInstance().getMessage(key); }

    private final Map<UUID, GhostBlockSession> activeSessions = new ConcurrentHashMap<>();
    private final JavaPlugin plugin;

    private BukkitTask renderTask;
    private BukkitTask timeoutTask;
    private final MiniMessage mm = MiniMessage.miniMessage();

    // Callback when a forge build is completed
    private BiConsumer<Player, GhostBlockSession> onBuildComplete;

    // Display entity appearance settings
    private static final float DISPLAY_SCALE = 0.9f;
    private static final float DISPLAY_OFFSET = (1f - DISPLAY_SCALE) / 2f;
    private static final float VIEW_RANGE = 0.5f; // ~40 blocks

    public GhostBlockManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts hint update and timeout tasks.
     */
    public void start() {
        // Update hint display glow (every 10 ticks = 0.5s)
        renderTask = Task.syncTimer(() -> {
            try {
                for (GhostBlockSession session : activeSessions.values()) {
                    Player player = Bukkit.getPlayer(session.getPlayerId());
                    if (player == null || !player.isOnline()) {
                        cleanupSession(session.getPlayerId());
                        continue;
                    }
                    if (!session.isActive()) {
                        cleanupSession(session.getPlayerId());
                        continue;
                    }
                    // Schedule display update on region thread (Folia safety)
                    Location anchor = session.getAnchor();
                    if (anchor != null && anchor.getWorld() != null) {
                        Task.sync(anchor, () -> {
                            try {
                                updateHintDisplay(player, session);
                            } catch (Exception e) {
                                MidgardLogger.error("Erro ao atualizar hint display de forja", e);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                MidgardLogger.error("Erro no render task de construção de forja", e);
            }
        }, 20L, 10L);

        // Timeout check (every 5s)
        timeoutTask = Task.syncTimer(() -> {
            try {
                for (GhostBlockSession session : activeSessions.values()) {
                    if (session.isExpired()) {
                        Player player = Bukkit.getPlayer(session.getPlayerId());
                        if (player != null) {
                            player.sendMessage(mm.deserialize(msg("forge.ghost.session_expired")));
                        }
                        cleanupSession(session.getPlayerId());
                    }
                }
            } catch (Exception e) {
                MidgardLogger.error("Erro no timeout task de construção de forja", e);
            }
        }, 100L, 100L);
    }

    /**
     * Stops all tasks and cleans up sessions.
     */
    public void shutdown() {
        if (renderTask != null) { renderTask.cancel(); }
        if (timeoutTask != null) { timeoutTask.cancel(); }
        for (GhostBlockSession session : activeSessions.values()) {
            Player player = Bukkit.getPlayer(session.getPlayerId());
            removeAllDisplays(session);
            if (player != null) { session.hideHUD(player); }
        }
        activeSessions.clear();
    }

    // ==================== Session Lifecycle ====================

    /**
     * Starts a new preview session when a blueprint is placed.
     * Spawns BlockDisplay entities as ghost blocks that the player can see.
     *
     * @return the created session, or null if validation failed
     */
    public GhostBlockSession startPreview(Player player, ForgeTier tier,
                                           ForgeSchematic schematic, Location anchor) {
        UUID playerId = player.getUniqueId();

        // Cancel existing session immediately (we're on region thread from event handler)
        GhostBlockSession existing = activeSessions.remove(playerId);
        if (existing != null) {
            removeAllDisplays(existing);
            existing.hideHUD(player);
            existing.cancel();
        }

        ForgeRotation rotation = ForgeRotation.fromYaw(player.getLocation().getYaw());

        // Check space
        if (!schematic.hasEnoughSpace(anchor, rotation)) {
            player.sendMessage(mm.deserialize(msg("forge.ghost.no_space")));
            return null;
        }

        GhostBlockSession session = new GhostBlockSession(playerId, tier, schematic, anchor, rotation);
        activeSessions.put(playerId, session);

        // Spawn Block Display entities for ghost blocks
        spawnDisplays(player, session);

        // Show preview HUD
        session.showHUD(player);

        player.sendMessage(mm.deserialize(""));
        player.sendMessage(mm.deserialize(msg("forge.ghost.preview_title").replace("%tier%", tier.getName())));
        player.sendMessage(mm.deserialize(msg("forge.ghost.preview_hint1")));
        player.sendMessage(mm.deserialize(msg("forge.ghost.preview_hint2")));
        player.sendMessage(mm.deserialize(msg("forge.ghost.preview_hint3")));
        player.sendMessage(mm.deserialize(msg("forge.ghost.preview_hint4")));
        player.sendMessage(mm.deserialize(""));

        player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1f, 1.2f);

        return session;
    }

    /**
     * Confirms the preview and starts the building phase.
     * Called when the player shift-right-clicks during PREVIEWING.
     */
    public boolean confirmBuild(Player player) {
        GhostBlockSession session = activeSessions.get(player.getUniqueId());
        if (session == null || !session.isPreviewing()) { return false; }

        session.confirmAndStartBuilding();
        session.updateHUD();

        // Remove all preview displays (all layers) and spawn only the current build layer
        removeAllDisplays(session);
        spawnLayerDisplays(player, session);

        // Show first hint
        String nextBlock = session.getNextBlockName();
        player.sendMessage(mm.deserialize(""));
        player.sendMessage(mm.deserialize(msg("forge.ghost.build_started")));
        player.sendMessage(mm.deserialize(msg("forge.ghost.build_hint")));
        if (nextBlock != null) {
            player.sendMessage(mm.deserialize(msg("forge.ghost.next_block").replace("%name%", nextBlock)));
        }
        player.sendMessage(mm.deserialize(""));

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);

        return true;
    }

    /**
     * Called when a block is placed in the world during a build session.
     */
    public GhostBlockSession.PlaceResult onBlockPlaced(Player player, Location location) {
        GhostBlockSession session = activeSessions.get(player.getUniqueId());
        if (session == null || !session.isBuilding()) { return null; }

        int layerYBefore = session.getCurrentLayerY();
        GhostBlockSession.PlaceResult result = session.onBlockPlaced(location);

        switch (result) {
            case CORRECT -> {
                session.updateHUD();
                // Remove the display entity at this position
                GhostBlockSession.BlockPosition pos = new GhostBlockSession.BlockPosition(location);
                removeDisplay(session, pos);

                // Particle + sound for correct placement
                Location center = location.clone().add(0.5, 0.5, 0.5);
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, center, 8, 0.3, 0.3, 0.3, 0);
                player.playSound(location, Sound.BLOCK_STONE_PLACE, 1f, 1.3f);

                // Check for layer transition — spawn next layer when current is complete
                int layerYAfter = session.getCurrentLayerY();
                if (layerYAfter != layerYBefore && layerYAfter >= 0) {
                    spawnLayerDisplays(player, session);
                    int completedLayer = layerYBefore + 1;
                    int totalLayers = session.getTotalLayers();
                    player.sendMessage(mm.deserialize(
                            msg("forge.ghost.layer_complete")
                                    .replace("%done%", String.valueOf(completedLayer))
                                    .replace("%total%", String.valueOf(totalLayers))));
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
                }

                // Show next hint via ActionBar
                sendNextHintActionBar(player, session);
                updateHintDisplay(player, session);
            }
            case CORRECT_AND_COMPLETE -> {
                session.updateHUD();
                // Remove the last display entity
                GhostBlockSession.BlockPosition pos = new GhostBlockSession.BlockPosition(location);
                removeDisplay(session, pos);
                onBuildCompleted(player, session);
            }
            case WRONG_BLOCK -> {
                // Don't update displays, block will be cancelled by listener
                ForgeBlock expected = session.getExpectedBlock(location);
                if (expected != null) {
                    String name = GhostBlockSession.getPlaceableName(expected.getMaterial());
                    player.sendActionBar(mm.deserialize(
                            msg("forge.ghost.wrong_block").replace("%name%", name)));
                }
                player.playSound(location, Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
            }
            default -> {
                // NOT_PART_OF_SCHEMATIC — ignore
            }
        }

        return result;
    }

    /**
     * Called when a block is broken that was part of an active session.
     */
    public void onBlockBroken(Player player, Location location) {
        GhostBlockSession session = activeSessions.get(player.getUniqueId());
        if (session != null && session.isBuilding()) {
            int layerYBefore = session.getCurrentLayerY();
            session.onBlockBroken(location);
            session.updateHUD();

            int layerYAfter = session.getCurrentLayerY();
            if (layerYAfter != layerYBefore) {
                // Layer regressed — full refresh to show the correct layer
                removeAllDisplays(session);
                spawnLayerDisplays(player, session);
            } else {
                // Same layer — just re-spawn the broken block's display
                GhostBlockSession.BlockPosition pos = new GhostBlockSession.BlockPosition(location);
                ForgeBlock block = session.getExpectedBlock(location);
                if (block != null) {
                    spawnSingleDisplay(player, session, pos, block);
                }
            }
            updateHintDisplay(player, session);
        }
    }

    /**
     * Rotates the session (only during PREVIEWING).
     */
    public boolean rotateSession(Player player) {
        GhostBlockSession session = activeSessions.get(player.getUniqueId());
        if (session == null) { return false; }

        // Remove old display entities
        removeAllDisplays(session);

        if (!session.rotate()) {
            player.sendMessage(mm.deserialize(msg("forge.ghost.rotate_preview_only")));
            // Re-spawn displays for current rotation
            spawnDisplays(player, session);
            return false;
        }

        // Check space in new rotation
        if (!session.getSchematic().hasEnoughSpace(session.getAnchor(), session.getRotation())) {
            // Rotate back
            session.rotate();
            session.rotate();
            session.rotate(); // 3 more rotations = back to original
            spawnDisplays(player, session);
            player.sendMessage(mm.deserialize(msg("forge.ghost.rotate_no_space")));
            return false;
        }

        // Spawn new display entities
        spawnDisplays(player, session);
        session.updateHUD();
        player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 0.8f, 1.2f);
        player.sendMessage(mm.deserialize(msg("forge.ghost.rotated")));
        return true;
    }

    /**
     * Cancels a player's build session and cleans up.
     */
    public void cancelSession(UUID playerId) {
        cleanupSession(playerId);
    }

    // ==================== Display Entity Management ====================

    /**
     * Spawns Block Display entities for all remaining blocks in the session.
     * Also spawns fuel zone indicators during preview.
     */
    private void spawnDisplays(Player player, GhostBlockSession session) {
        Map<Location, ForgeBlock> remaining = session.getRemainingBlocksWithLocations();
        for (var entry : remaining.entrySet()) {
            Location loc = entry.getKey();
            ForgeBlock block = entry.getValue();
            GhostBlockSession.BlockPosition pos = new GhostBlockSession.BlockPosition(loc);
            spawnSingleDisplay(player, session, pos, block);
        }
        // Spawn fuel zone indicators (shown during preview as orange-tinted ghost blocks)
        spawnFuelZoneDisplays(player, session);
        updateHintDisplay(player, session);
    }

    /**
     * Spawns Block Display entities for only the current build layer.
     * Used during BUILDING state to show one layer at a time.
     */
    private void spawnLayerDisplays(Player player, GhostBlockSession session) {
        int layerY = session.getCurrentLayerY();
        if (layerY < 0) { return; }
        Map<Location, ForgeBlock> layerBlocks = session.getRemainingBlocksForLayerWithLocations(layerY);
        for (var entry : layerBlocks.entrySet()) {
            GhostBlockSession.BlockPosition pos = new GhostBlockSession.BlockPosition(entry.getKey());
            spawnSingleDisplay(player, session, pos, entry.getValue());
        }
        updateHintDisplay(player, session);
    }

    /**
     * Spawns a single Block Display entity for a ghost block position.
     */
    private void spawnSingleDisplay(Player player, GhostBlockSession session,
                                     GhostBlockSession.BlockPosition pos, ForgeBlock block) {
        Material mat = block.getMaterial();
        if (mat == null || mat == Material.AIR) { return; }

        World world = session.getAnchor().getWorld();
        if (world == null) { return; }

        Location loc = pos.toLocation(world);
        BlockDisplay display = world.spawn(loc, BlockDisplay.class, entity -> {
            entity.setBlock(mat.createBlockData());
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setTransformation(new Transformation(
                    new Vector3f(DISPLAY_OFFSET, DISPLAY_OFFSET, DISPLAY_OFFSET),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(DISPLAY_SCALE, DISPLAY_SCALE, DISPLAY_SCALE),
                    new AxisAngle4f(0, 0, 1, 0)
            ));
            entity.setViewRange(VIEW_RANGE);
            entity.setVisibleByDefault(false);
            entity.setPersistent(false);
            entity.setGlowing(true);
            entity.setGlowColorOverride(Color.LIME);
        });

        player.showEntity(plugin, display);
        session.addDisplayEntity(pos, display);
    }

    /**
     * Spawns fuel zone indicator displays (orange-tinted, shown in preview).
     * These are not part of the build queue — purely visual zone markers.
     */
    private void spawnFuelZoneDisplays(Player player, GhostBlockSession session) {
        ForgeSchematic schematic = session.getSchematic();
        List<ForgeBlock> fuelZoneBlocks = schematic.getFuelZoneBlocks();
        if (fuelZoneBlocks.isEmpty()) { return; }

        World world = session.getAnchor().getWorld();
        if (world == null) { return; }

        for (ForgeBlock block : fuelZoneBlocks) {
            Location worldLoc = schematic.toWorldLocation(session.getAnchor(), session.getRotation(), block);
            GhostBlockSession.BlockPosition pos = new GhostBlockSession.BlockPosition(worldLoc);

            BlockDisplay display = world.spawn(worldLoc, BlockDisplay.class, entity -> {
                entity.setBlock(Material.ORANGE_STAINED_GLASS.createBlockData());
                entity.setBrightness(new Display.Brightness(15, 15));
                entity.setTransformation(new Transformation(
                        new Vector3f(DISPLAY_OFFSET, DISPLAY_OFFSET, DISPLAY_OFFSET),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(DISPLAY_SCALE, DISPLAY_SCALE, DISPLAY_SCALE),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                entity.setViewRange(VIEW_RANGE);
                entity.setVisibleByDefault(false);
                entity.setPersistent(false);
                entity.setGlowing(true);
                entity.setGlowColorOverride(Color.fromRGB(255, 140, 0)); // Orange
            });

            player.showEntity(plugin, display);
            session.addDisplayEntity(pos, display);
        }
    }

    /**
     * Removes a single display entity from the session.
     */
    private void removeDisplay(GhostBlockSession session, GhostBlockSession.BlockPosition pos) {
        BlockDisplay display = session.removeDisplayEntity(pos);
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    /**
     * Removes all display entities from a session.
     */
    private void removeAllDisplays(GhostBlockSession session) {
        for (BlockDisplay display : session.getDisplayEntities().values()) {
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
        session.getDisplayEntities().clear();
    }

    /**
     * Updates the glow color on display entities to highlight the hint block.
     * Hint block gets gold glow, other blocks get green glow.
     */
    private void updateHintDisplay(Player player, GhostBlockSession session) {
        if (!session.isActive()) { return; }

        GhostBlockSession.BlockPosition hint = session.getCurrentHint();
        for (var entry : session.getDisplayEntities().entrySet()) {
            BlockDisplay display = entry.getValue();
            if (display == null || !display.isValid()) { continue; }

            boolean isHint = session.isBuilding() && entry.getKey().equals(hint);
            if (isHint) {
                display.setGlowColorOverride(Color.fromRGB(255, 215, 0)); // Gold
                // Spawn hint particles
                Location center = entry.getKey().toLocation(session.getAnchor().getWorld())
                        .add(0.5, 0.5, 0.5);
                Particle.DustOptions gold = new Particle.DustOptions(
                        Color.fromRGB(255, 215, 0), 0.6f);
                player.spawnParticle(Particle.DUST, center, 3, 0.2, 0.2, 0.2, 0, gold);
            } else {
                display.setGlowColorOverride(Color.LIME);
            }
        }
    }

    // ==================== Internal ====================

    private void onBuildCompleted(Player player, GhostBlockSession session) {
        // Big completion effect
        Location center = session.getAnchor().clone().add(0, 2, 0);
        World world = center.getWorld();
        if (world != null) {
            world.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 30, 1, 1, 1, 0.3);
            world.spawnParticle(Particle.ENCHANT, center, 20, 1, 1, 1, 1.0);
            world.playSound(center, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
        }

        long buildTime = System.currentTimeMillis() - session.getBuildStartedAt();
        int seconds = (int) (buildTime / 1000);
        int minutes = seconds / 60;
        seconds %= 60;

        player.sendMessage(mm.deserialize(""));
        player.sendMessage(mm.deserialize(msg("forge.ghost.build_complete_title")));
        player.sendMessage(mm.deserialize(msg("forge.ghost.build_complete_time").replace("%min%", String.valueOf(minutes)).replace("%sec%", String.valueOf(seconds))));
        player.sendMessage(mm.deserialize(msg("forge.ghost.build_complete_blocks").replace("%count%", String.valueOf(session.getTotalCount()))));
        player.sendMessage(mm.deserialize(msg("forge.ghost.build_complete_errors").replace("%count%", String.valueOf(session.getWrongPlacements()))));
        player.sendMessage(mm.deserialize(msg("forge.ghost.build_complete_order").replace("%perfect%", String.valueOf(session.getPerfectPlacements())).replace("%total%", String.valueOf(session.getTotalCount()))));
        player.sendMessage(mm.deserialize(""));

        session.hideHUD(player);
        removeAllDisplays(session);
        activeSessions.remove(player.getUniqueId());

        if (onBuildComplete != null) {
            onBuildComplete.accept(player, session);
        }
    }

    private void cleanupSession(UUID playerId) {
        GhostBlockSession session = activeSessions.remove(playerId);
        if (session == null) { return; }

        session.cancel();
        Player player = Bukkit.getPlayer(playerId);

        // Schedule display removal on region thread (Folia safety)
        Location anchor = session.getAnchor();
        if (anchor != null && anchor.getWorld() != null) {
            Task.sync(anchor, () -> {
                removeAllDisplays(session);
                if (player != null && player.isOnline()) {
                    session.hideHUD(player);
                }
            });
        }
    }

    private void sendNextHintActionBar(Player player, GhostBlockSession session) {
        String next = session.getNextBlockName();
        if (next != null) {
            int remaining = session.getRemainingCount();
            player.sendActionBar(mm.deserialize(
                    msg("forge.ghost.hint_actionbar")
                            .replace("%remaining%", String.valueOf(remaining))
                            .replace("%next%", next)));
        }
    }

    // ==================== Queries ====================

    public GhostBlockSession getSession(UUID playerId) {
        return activeSessions.get(playerId);
    }

    public boolean hasSession(UUID playerId) {
        return activeSessions.containsKey(playerId);
    }

    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    public void setOnBuildComplete(BiConsumer<Player, GhostBlockSession> callback) {
        this.onBuildComplete = callback;
    }
}
