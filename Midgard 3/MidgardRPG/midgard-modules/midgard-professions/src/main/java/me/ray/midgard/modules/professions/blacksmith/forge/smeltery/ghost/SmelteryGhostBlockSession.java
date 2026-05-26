package me.ray.midgard.modules.professions.blacksmith.forge.smeltery.ghost;

import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeRotation;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryBlockType;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryTier;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.schematic.SmelteryBlock;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.schematic.SmelterySchematic;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Rastreia a sessão de construção de smeltery de um jogador.
 * Guia o jogador bloco a bloco com ghost blocks e BossBar de progresso.
 */
public class SmelteryGhostBlockSession {

    public enum BuildState {
        PREVIEWING,
        BUILDING,
        COMPLETED,
        CANCELLED
    }

    private final UUID playerId;
    private final SmelteryTier tier;
    private final SmelterySchematic schematic;
    private final Location anchor;
    private ForgeRotation rotation;
    private BuildState state;
    private final long createdAt;
    private long buildStartedAt;

    private final Set<BlockPosition> remainingBlocks;
    private final Set<BlockPosition> completedBlocks;
    private final Map<BlockPosition, SmelteryBlock> blockMap;

    private final List<BlockPosition> buildOrder;
    private BlockPosition currentHint;

    private final Map<BlockPosition, BlockDisplay> displayEntities = new HashMap<>();

    private BossBar bossBar;

    private int wrongPlacements;
    private int perfectPlacements;

    private static final long BUILD_TIMEOUT_MS = 10 * 60 * 1000;
    private static final MiniMessage mm = MiniMessage.miniMessage();

    private static String msg(String key) {
        var m = ProfessionsModule.getInstance();
        return m != null ? m.getMessage("smeltery.ghost_session." + key) : key;
    }

    public SmelteryGhostBlockSession(UUID playerId, SmelteryTier tier, SmelterySchematic schematic,
                                      Location anchor, ForgeRotation rotation) {
        this.playerId = playerId;
        this.tier = tier;
        this.schematic = schematic;
        this.anchor = anchor.clone();
        this.rotation = rotation;
        this.state = BuildState.PREVIEWING;
        this.createdAt = System.currentTimeMillis();
        this.remainingBlocks = new LinkedHashSet<>();
        this.completedBlocks = new LinkedHashSet<>();
        this.blockMap = new HashMap<>();
        this.buildOrder = new ArrayList<>();
        this.wrongPlacements = 0;
        this.perfectPlacements = 0;

        calculateBlocks();
    }

    // ==================== Block Tracking ====================

    private void calculateBlocks() {
        remainingBlocks.clear();
        completedBlocks.clear();
        blockMap.clear();
        buildOrder.clear();

        List<SmelteryBlock> solidBlocks = schematic.getSolidBlocks();
        solidBlocks.sort(Comparator.comparingInt(SmelteryBlock::getRelY)
                .thenComparingInt(SmelteryBlock::getRelX)
                .thenComparingInt(SmelteryBlock::getRelZ));

        for (SmelteryBlock block : solidBlocks) {
            Location worldLoc = schematic.toWorldLocation(anchor, rotation, block);
            BlockPosition pos = new BlockPosition(worldLoc);
            blockMap.put(pos, block);

            if (worldLoc.getBlock().getType() == block.getMaterial()) {
                completedBlocks.add(pos);
            } else {
                remainingBlocks.add(pos);
                buildOrder.add(pos);
            }
        }

        updateHint();
    }

    private void updateHint() {
        currentHint = buildOrder.isEmpty() ? null : buildOrder.getFirst();
    }

    // ==================== State Management ====================

    public void confirmAndStartBuilding() {
        if (state != BuildState.PREVIEWING) {
            return;
        }
        this.state = BuildState.BUILDING;
        this.buildStartedAt = System.currentTimeMillis();
    }

    public void cancel() {
        this.state = BuildState.CANCELLED;
    }

    // ==================== Block Placement ====================

    public PlaceResult onBlockPlaced(Location location) {
        if (state != BuildState.BUILDING) {
            return PlaceResult.NOT_PART_OF_SCHEMATIC;
        }

        BlockPosition pos = new BlockPosition(location);
        SmelteryBlock expected = blockMap.get(pos);
        if (expected == null) {
            return PlaceResult.NOT_PART_OF_SCHEMATIC;
        }

        Material placed = location.getBlock().getType();
        Material expectedMat = expected.getMaterial();

        if (placed == expectedMat || isAcceptedSubstitute(placed, expectedMat)) {
            if (placed != expectedMat) {
                location.getBlock().setType(expectedMat);
            }

            remainingBlocks.remove(pos);
            completedBlocks.add(pos);
            buildOrder.remove(pos);

            if (pos.equals(currentHint)) {
                perfectPlacements++;
            }

            updateHint();

            if (remainingBlocks.isEmpty()) {
                state = BuildState.COMPLETED;
                return PlaceResult.CORRECT_AND_COMPLETE;
            }
            return PlaceResult.CORRECT;
        } else {
            wrongPlacements++;
            return PlaceResult.WRONG_BLOCK;
        }
    }

    private static boolean isAcceptedSubstitute(Material placed, Material expected) {
        if (expected == Material.WATER_CAULDRON && placed == Material.CAULDRON) {
            return true;
        }
        if (expected == Material.LAVA_CAULDRON && placed == Material.CAULDRON) {
            return true;
        }
        // Variações de nether bricks
        if (expected == Material.NETHER_BRICKS && (placed == Material.RED_NETHER_BRICKS
                || placed == Material.CHISELED_NETHER_BRICKS
                || placed == Material.CRACKED_NETHER_BRICKS)) {
            return true;
        }
        return false;
    }

    public void onBlockBroken(Location location) {
        BlockPosition pos = new BlockPosition(location);
        if (completedBlocks.remove(pos)) {
            remainingBlocks.add(pos);
            SmelteryBlock block = blockMap.get(pos);
            if (block != null) {
                int insertIdx = 0;
                for (int i = 0; i < buildOrder.size(); i++) {
                    SmelteryBlock other = blockMap.get(buildOrder.get(i));
                    if (other != null && other.getRelY() <= block.getRelY()) {
                        insertIdx = i + 1;
                    }
                }
                buildOrder.add(Math.min(insertIdx, buildOrder.size()), pos);
            }
            updateHint();

            if (state == BuildState.COMPLETED) {
                state = BuildState.BUILDING;
            }
        }
    }

    public boolean rotate() {
        if (state != BuildState.PREVIEWING) {
            return false;
        }
        this.rotation = rotation.next();
        calculateBlocks();
        return true;
    }

    // ==================== BossBar HUD ====================

    public void showHUD(Player player) {
        bossBar = BossBar.bossBar(
                mm.deserialize(getHudText()),
                getProgress(),
                BossBar.Color.YELLOW,
                BossBar.Overlay.NOTCHED_20
        );
        player.showBossBar(bossBar);
    }

    public void updateHUD() {
        if (bossBar == null) {
            return;
        }

        float progress = getProgress();
        bossBar.progress(progress);
        bossBar.name(mm.deserialize(getHudText()));

        if (progress >= 0.75f) {
            bossBar.color(BossBar.Color.GREEN);
        } else if (progress >= 0.40f) {
            bossBar.color(BossBar.Color.YELLOW);
        } else {
            bossBar.color(BossBar.Color.WHITE);
        }
    }

    public void hideHUD(Player player) {
        if (bossBar != null) {
            player.hideBossBar(bossBar);
            bossBar = null;
        }
    }

    private String getHudText() {
        if (state == BuildState.PREVIEWING) {
            return "<gold>⚗ " + tier.getName() + " <gray>— <yellow>" + msg("confirm_position");
        }

        int completed = getCompletedCount();
        int total = getTotalCount();
        int currentLayer = getCurrentLayer();
        int totalLayers = getTotalLayers();

        return "<gold>⚗ " + msg("building") + " " + tier.getName() +
                " <gray>— <white>" + completed + "/" + total +
                " <gray>" + msg("blocks") + " | " + msg("layer") + " <white>" + currentLayer + "/" + totalLayers;
    }

    // ==================== Progress & Info ====================

    public float getProgress() {
        int total = blockMap.size();
        if (total == 0) {
            return 1.0f;
        }
        return (float) completedBlocks.size() / total;
    }

    public int getCurrentLayer() {
        if (currentHint == null) {
            return getTotalLayers();
        }
        SmelteryBlock hintBlock = blockMap.get(currentHint);
        return hintBlock != null ? hintBlock.getRelY() + 1 : 1;
    }

    public int getTotalLayers() {
        return blockMap.values().stream()
                .mapToInt(SmelteryBlock::getRelY)
                .max().orElse(0) + 1;
    }

    public String getNextBlockName() {
        if (currentHint == null) {
            return null;
        }
        SmelteryBlock block = blockMap.get(currentHint);
        return block != null ? getPlaceableName(block.getMaterial()) : null;
    }

    public static String getPlaceableName(Material expected) {
        return switch (expected) {
            case WATER_CAULDRON, LAVA_CAULDRON, POWDER_SNOW_CAULDRON -> formatMaterialName(Material.CAULDRON.name());
            default -> formatMaterialName(expected.name());
        };
    }

    public Location getHintLocation() {
        if (currentHint == null) {
            return null;
        }
        return currentHint.toLocation(anchor.getWorld());
    }

    public boolean isExpired() {
        long timeout = (state == BuildState.PREVIEWING) ? 120_000 : BUILD_TIMEOUT_MS;
        return System.currentTimeMillis() - createdAt > timeout;
    }

    private static String formatMaterialName(String name) {
        return name.toLowerCase().replace('_', ' ');
    }

    // ==================== Getters ====================

    public UUID getPlayerId() { return playerId; }
    public SmelteryTier getTier() { return tier; }
    public SmelterySchematic getSchematic() { return schematic; }
    public Location getAnchor() { return anchor.clone(); }
    public ForgeRotation getRotation() { return rotation; }
    public BuildState getState() { return state; }
    public long getCreatedAt() { return createdAt; }
    public long getBuildStartedAt() { return buildStartedAt; }
    public BlockPosition getCurrentHint() { return currentHint; }
    public int getWrongPlacements() { return wrongPlacements; }
    public int getPerfectPlacements() { return perfectPlacements; }
    public int getRemainingCount() { return remainingBlocks.size(); }
    public int getCompletedCount() { return completedBlocks.size(); }
    public int getTotalCount() { return blockMap.size(); }

    public boolean isComplete() { return state == BuildState.COMPLETED; }
    public boolean isActive() { return state == BuildState.PREVIEWING || state == BuildState.BUILDING; }
    public boolean isBuilding() { return state == BuildState.BUILDING; }
    public boolean isPreviewing() { return state == BuildState.PREVIEWING; }

    public Map<Location, SmelteryBlock> getRemainingBlocksWithLocations() {
        Map<Location, SmelteryBlock> result = new HashMap<>();
        for (BlockPosition pos : remainingBlocks) {
            SmelteryBlock block = blockMap.get(pos);
            if (block != null) {
                result.put(pos.toLocation(anchor.getWorld()), block);
            }
        }
        return result;
    }

    /**
     * Gets the relY of the current building layer (from the hint block).
     * Returns -1 if there's no current hint.
     */
    public int getCurrentLayerY() {
        if (currentHint == null) {
            return -1;
        }
        SmelteryBlock hintBlock = blockMap.get(currentHint);
        return hintBlock != null ? hintBlock.getRelY() : -1;
    }

    /**
     * Gets remaining blocks for a specific relY layer with their world locations.
     */
    public Map<Location, SmelteryBlock> getRemainingBlocksForLayerWithLocations(int relY) {
        Map<Location, SmelteryBlock> result = new HashMap<>();
        for (BlockPosition pos : remainingBlocks) {
            SmelteryBlock block = blockMap.get(pos);
            if (block != null && block.getRelY() == relY) {
                result.put(pos.toLocation(anchor.getWorld()), block);
            }
        }
        return result;
    }

    /**
     * Checks if all blocks at a specific relY layer are completed.
     */
    public boolean isLayerComplete(int relY) {
        for (BlockPosition pos : remainingBlocks) {
            SmelteryBlock block = blockMap.get(pos);
            if (block != null && block.getRelY() == relY) {
                return false;
            }
        }
        return true;
    }

    public SmelteryBlock getExpectedBlock(Location location) {
        return blockMap.get(new BlockPosition(location));
    }

    public BossBar getBossBar() { return bossBar; }

    // ==================== Display Entity Tracking ====================

    public void addDisplayEntity(BlockPosition pos, BlockDisplay entity) {
        displayEntities.put(pos, entity);
    }

    public BlockDisplay removeDisplayEntity(BlockPosition pos) {
        return displayEntities.remove(pos);
    }

    public Map<BlockPosition, BlockDisplay> getDisplayEntities() {
        return displayEntities;
    }

    public enum PlaceResult {
        CORRECT,
        CORRECT_AND_COMPLETE,
        WRONG_BLOCK,
        NOT_PART_OF_SCHEMATIC
    }

    public record BlockPosition(int x, int y, int z) {
        public BlockPosition(Location loc) {
            this(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }

        public Location toLocation(org.bukkit.World world) {
            return new Location(world, x, y, z);
        }
    }
}
