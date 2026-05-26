package me.ray.midgard.modules.professions.blacksmith.forge.smeltery.ghost;

import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeRotation;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryTier;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.schematic.SmelteryBlock;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.schematic.SmelterySchematic;
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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Gerencia sessões de construção de smeltery usando Block Display entities.
 * Análogo ao GhostBlockManager da forja, adaptado para smeltery.
 */
public class SmelteryGhostBlockManager {

    private String msg(String key) { return ProfessionsModule.getInstance().getMessage(key); }

    private final Map<UUID, SmelteryGhostBlockSession> activeSessions = new ConcurrentHashMap<>();
    private final JavaPlugin plugin;

    private BukkitTask renderTask;
    private BukkitTask timeoutTask;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private BiConsumer<Player, SmelteryGhostBlockSession> onBuildComplete;

    private static final float DISPLAY_SCALE = 0.9f;
    private static final float DISPLAY_OFFSET = (1f - DISPLAY_SCALE) / 2f;
    private static final float VIEW_RANGE = 0.5f;

    public SmelteryGhostBlockManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        renderTask = Task.syncTimer(() -> {
            try {
                for (SmelteryGhostBlockSession session : activeSessions.values()) {
                    Player player = Bukkit.getPlayer(session.getPlayerId());
                    if (player == null || !player.isOnline()) {
                        cleanupSession(session.getPlayerId());
                        continue;
                    }
                    if (!session.isActive()) {
                        cleanupSession(session.getPlayerId());
                        continue;
                    }
                    Location anchor = session.getAnchor();
                    if (anchor != null && anchor.getWorld() != null) {
                        Task.sync(anchor, () -> {
                            try {
                                updateHintDisplay(player, session);
                            } catch (Exception e) {
                                MidgardLogger.error("Erro ao atualizar hint display de smeltery", e);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                MidgardLogger.error("Erro no render task de construção de smeltery", e);
            }
        }, 20L, 10L);

        timeoutTask = Task.syncTimer(() -> {
            try {
                for (SmelteryGhostBlockSession session : activeSessions.values()) {
                    if (session.isExpired()) {
                        Player player = Bukkit.getPlayer(session.getPlayerId());
                        if (player != null) {
                            player.sendMessage(mm.deserialize(msg("smeltery.ghost.session_expired")));
                        }
                        cleanupSession(session.getPlayerId());
                    }
                }
            } catch (Exception e) {
                MidgardLogger.error("Erro no timeout task de construção de smeltery", e);
            }
        }, 100L, 100L);
    }

    public void shutdown() {
        if (renderTask != null) {
            renderTask.cancel();
        }
        if (timeoutTask != null) {
            timeoutTask.cancel();
        }
        for (SmelteryGhostBlockSession session : activeSessions.values()) {
            Player player = Bukkit.getPlayer(session.getPlayerId());
            removeAllDisplays(session);
            if (player != null) {
                session.hideHUD(player);
            }
        }
        activeSessions.clear();
    }

    // ==================== Session Lifecycle ====================

    public SmelteryGhostBlockSession startPreview(Player player, SmelteryTier tier,
                                                   SmelterySchematic schematic, Location anchor) {
        UUID playerId = player.getUniqueId();

        SmelteryGhostBlockSession existing = activeSessions.remove(playerId);
        if (existing != null) {
            removeAllDisplays(existing);
            existing.hideHUD(player);
            existing.cancel();
        }

        ForgeRotation rotation = ForgeRotation.fromYaw(player.getLocation().getYaw());

        if (!schematic.hasEnoughSpace(anchor, rotation)) {
            player.sendMessage(mm.deserialize(msg("smeltery.ghost.no_space")));
            return null;
        }

        SmelteryGhostBlockSession session = new SmelteryGhostBlockSession(playerId, tier, schematic, anchor, rotation);
        activeSessions.put(playerId, session);

        spawnDisplays(player, session);
        session.showHUD(player);

        player.sendMessage(mm.deserialize(""));
        player.sendMessage(mm.deserialize(msg("smeltery.ghost.preview_title").replace("%tier%", tier.getName())));
        player.sendMessage(mm.deserialize(msg("smeltery.ghost.preview_hint1")));
        player.sendMessage(mm.deserialize(msg("smeltery.ghost.preview_hint2")));
        player.sendMessage(mm.deserialize(msg("smeltery.ghost.preview_hint3")));
        player.sendMessage(mm.deserialize(msg("smeltery.ghost.preview_hint4")));
        player.sendMessage(mm.deserialize(""));

        player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1f, 1.2f);

        return session;
    }

    public boolean confirmBuild(Player player) {
        SmelteryGhostBlockSession session = activeSessions.get(player.getUniqueId());
        if (session == null || !session.isPreviewing()) {
            return false;
        }

        session.confirmAndStartBuilding();
        session.updateHUD();

        // Remove all preview displays (all layers) and spawn only the current build layer
        removeAllDisplays(session);
        spawnLayerDisplays(player, session);

        String nextBlock = session.getNextBlockName();
        player.sendMessage(mm.deserialize(""));
        player.sendMessage(mm.deserialize(msg("smeltery.ghost.build_started")));
        player.sendMessage(mm.deserialize(msg("smeltery.ghost.build_hint")));
        if (nextBlock != null) {
            player.sendMessage(mm.deserialize(msg("smeltery.ghost.next_block").replace("%name%", nextBlock)));
        }
        player.sendMessage(mm.deserialize(""));

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);

        return true;
    }

    public SmelteryGhostBlockSession.PlaceResult onBlockPlaced(Player player, Location location) {
        SmelteryGhostBlockSession session = activeSessions.get(player.getUniqueId());
        if (session == null || !session.isBuilding()) {
            return null;
        }

        int layerYBefore = session.getCurrentLayerY();
        SmelteryGhostBlockSession.PlaceResult result = session.onBlockPlaced(location);

        switch (result) {
            case CORRECT -> {
                session.updateHUD();
                SmelteryGhostBlockSession.BlockPosition pos = new SmelteryGhostBlockSession.BlockPosition(location);
                removeDisplay(session, pos);

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
                            msg("smeltery.ghost.layer_complete").replace("%done%", String.valueOf(completedLayer)).replace("%total%", String.valueOf(totalLayers))));
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
                }

                sendNextHintActionBar(player, session);
                updateHintDisplay(player, session);
            }
            case CORRECT_AND_COMPLETE -> {
                session.updateHUD();
                SmelteryGhostBlockSession.BlockPosition pos = new SmelteryGhostBlockSession.BlockPosition(location);
                removeDisplay(session, pos);
                onBuildCompleted(player, session);
            }
            case WRONG_BLOCK -> {
                SmelteryBlock expected = session.getExpectedBlock(location);
                if (expected != null) {
                    String name = SmelteryGhostBlockSession.getPlaceableName(expected.getMaterial());
                    player.sendActionBar(mm.deserialize(
                            msg("smeltery.ghost.wrong_block").replace("%name%", name)));
                }
                player.playSound(location, Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
            }
            default -> {
                // NOT_PART_OF_SCHEMATIC — ignorar
            }
        }

        return result;
    }

    public void onBlockBroken(Player player, Location location) {
        SmelteryGhostBlockSession session = activeSessions.get(player.getUniqueId());
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
                SmelteryGhostBlockSession.BlockPosition pos = new SmelteryGhostBlockSession.BlockPosition(location);
                SmelteryBlock block = session.getExpectedBlock(location);
                if (block != null) {
                    spawnSingleDisplay(player, session, pos, block);
                }
            }
            updateHintDisplay(player, session);
        }
    }

    public boolean rotateSession(Player player) {
        SmelteryGhostBlockSession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            return false;
        }

        removeAllDisplays(session);

        if (!session.rotate()) {
            player.sendMessage(mm.deserialize(msg("smeltery.ghost.rotate_preview_only")));
            spawnDisplays(player, session);
            return false;
        }

        if (!session.getSchematic().hasEnoughSpace(session.getAnchor(), session.getRotation())) {
            session.rotate();
            session.rotate();
            session.rotate();
            spawnDisplays(player, session);
            player.sendMessage(mm.deserialize(msg("smeltery.ghost.rotate_no_space")));
            return false;
        }

        spawnDisplays(player, session);
        session.updateHUD();
        player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 0.8f, 1.2f);
        player.sendMessage(mm.deserialize(msg("smeltery.ghost.rotated")));
        return true;
    }

    public void cancelSession(UUID playerId) {
        cleanupSession(playerId);
    }

    // ==================== Display Entity Management ====================

    private void spawnDisplays(Player player, SmelteryGhostBlockSession session) {
        Map<Location, SmelteryBlock> remaining = session.getRemainingBlocksWithLocations();
        for (var entry : remaining.entrySet()) {
            Location loc = entry.getKey();
            SmelteryBlock block = entry.getValue();
            SmelteryGhostBlockSession.BlockPosition pos = new SmelteryGhostBlockSession.BlockPosition(loc);
            spawnSingleDisplay(player, session, pos, block);
        }
        updateHintDisplay(player, session);
    }

    /**
     * Spawns Block Display entities for only the current build layer.
     * Used during BUILDING state to show one layer at a time.
     */
    private void spawnLayerDisplays(Player player, SmelteryGhostBlockSession session) {
        int layerY = session.getCurrentLayerY();
        if (layerY < 0) {
            return;
        }
        Map<Location, SmelteryBlock> layerBlocks = session.getRemainingBlocksForLayerWithLocations(layerY);
        for (var entry : layerBlocks.entrySet()) {
            SmelteryGhostBlockSession.BlockPosition pos = new SmelteryGhostBlockSession.BlockPosition(entry.getKey());
            spawnSingleDisplay(player, session, pos, entry.getValue());
        }
        updateHintDisplay(player, session);
    }

    private void spawnSingleDisplay(Player player, SmelteryGhostBlockSession session,
                                     SmelteryGhostBlockSession.BlockPosition pos, SmelteryBlock block) {
        Material mat = block.getMaterial();
        if (mat == null || mat == Material.AIR) {
            return;
        }

        World world = session.getAnchor().getWorld();
        if (world == null) {
            return;
        }

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

    private void removeDisplay(SmelteryGhostBlockSession session, SmelteryGhostBlockSession.BlockPosition pos) {
        BlockDisplay display = session.removeDisplayEntity(pos);
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    private void removeAllDisplays(SmelteryGhostBlockSession session) {
        for (BlockDisplay display : session.getDisplayEntities().values()) {
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
        session.getDisplayEntities().clear();
    }

    private void updateHintDisplay(Player player, SmelteryGhostBlockSession session) {
        if (!session.isActive()) {
            return;
        }

        SmelteryGhostBlockSession.BlockPosition hint = session.getCurrentHint();
        for (var entry : session.getDisplayEntities().entrySet()) {
            BlockDisplay display = entry.getValue();
            if (display == null || !display.isValid()) {
                continue;
            }

            boolean isHint = session.isBuilding() && entry.getKey().equals(hint);
            if (isHint) {
                display.setGlowColorOverride(Color.fromRGB(255, 170, 0)); // Laranja (tema smeltery)
                Location center = entry.getKey().toLocation(session.getAnchor().getWorld())
                        .add(0.5, 0.5, 0.5);
                Particle.DustOptions orange = new Particle.DustOptions(
                        Color.fromRGB(255, 170, 0), 0.6f);
                player.spawnParticle(Particle.DUST, center, 3, 0.2, 0.2, 0.2, 0, orange);
            } else {
                display.setGlowColorOverride(Color.LIME);
            }
        }
    }

    // ==================== Internal ====================

    private void onBuildCompleted(Player player, SmelteryGhostBlockSession session) {
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
        player.sendMessage(mm.deserialize(msg("smeltery.ghost.build_complete_title")));
        player.sendMessage(mm.deserialize(msg("smeltery.ghost.build_complete_time").replace("%min%", String.valueOf(minutes)).replace("%sec%", String.valueOf(seconds))));
        player.sendMessage(mm.deserialize(msg("smeltery.ghost.build_complete_blocks").replace("%count%", String.valueOf(session.getTotalCount()))));
        player.sendMessage(mm.deserialize(msg("smeltery.ghost.build_complete_errors").replace("%count%", String.valueOf(session.getWrongPlacements()))));
        player.sendMessage(mm.deserialize(msg("smeltery.ghost.build_complete_order").replace("%perfect%", String.valueOf(session.getPerfectPlacements())).replace("%total%", String.valueOf(session.getTotalCount()))));
        player.sendMessage(mm.deserialize(""));

        session.hideHUD(player);
        removeAllDisplays(session);
        activeSessions.remove(player.getUniqueId());

        if (onBuildComplete != null) {
            onBuildComplete.accept(player, session);
        }
    }

    private void cleanupSession(UUID playerId) {
        SmelteryGhostBlockSession session = activeSessions.remove(playerId);
        if (session == null) {
            return;
        }

        session.cancel();
        Player player = Bukkit.getPlayer(playerId);

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

    private void sendNextHintActionBar(Player player, SmelteryGhostBlockSession session) {
        String next = session.getNextBlockName();
        if (next != null) {
            int remaining = session.getRemainingCount();
            player.sendActionBar(mm.deserialize(
                    msg("smeltery.ghost.hint_actionbar").replace("%remaining%", String.valueOf(remaining)).replace("%next%", next)));
        }
    }

    // ==================== Queries ====================

    public SmelteryGhostBlockSession getSession(UUID playerId) {
        return activeSessions.get(playerId);
    }

    public boolean hasSession(UUID playerId) {
        return activeSessions.containsKey(playerId);
    }

    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    public void setOnBuildComplete(BiConsumer<Player, SmelteryGhostBlockSession> callback) {
        this.onBuildComplete = callback;
    }
}
