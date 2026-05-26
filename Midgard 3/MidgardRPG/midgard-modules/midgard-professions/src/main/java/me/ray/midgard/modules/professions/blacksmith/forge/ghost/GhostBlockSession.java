package me.ray.midgard.modules.professions.blacksmith.forge.ghost;

import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeRotation;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeTier;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeBlock;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeSchematic;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Tracks a player's active forge construction mini-game session.
 * Created when a player places a forge blueprint item on the ground.
 *
 * The session guides the player through building a schematic block-by-block
 * using standard Minecraft building mechanics, with ghost blocks as visual guides,
 * a BossBar HUD for progress, and layer-based building order.
 */
public class GhostBlockSession {

    /** Session states */
    public enum BuildState {
        /** Player placed the blueprint; can rotate before confirming */
        PREVIEWING,
        /** Actively placing blocks */
        BUILDING,
        /** All blocks placed — awaiting activation */
        COMPLETED,
        /** Cancelled by player or timeout */
        CANCELLED
    }

    private final UUID playerId;
    private final ForgeTier tier;
    private final ForgeSchematic schematic;
    private final Location anchor;
    private ForgeRotation rotation;
    private BuildState state;
    private final long createdAt;
    private long buildStartedAt;

    // Blocks that still need to be placed
    private final Set<BlockPosition> remainingBlocks;
    // Blocks that have been correctly placed
    private final Set<BlockPosition> completedBlocks;
    // Mapping of world positions to expected schematic blocks
    private final Map<BlockPosition, ForgeBlock> blockMap;

    // Ordered build queue (layer by layer, bottom to top)
    private final List<BlockPosition> buildOrder;
    // The current "hint" block — the next one the player should place
    private BlockPosition currentHint;

    // Block Display entity tracking
    private final Map<BlockPosition, BlockDisplay> displayEntities = new HashMap<>();

    // HUD
    private BossBar bossBar;

    // Stats
    private int wrongPlacements;
    private int perfectPlacements; // placed the hint block in order

    // Timeout (10 minutes)
    private static final long BUILD_TIMEOUT_MS = 10 * 60 * 1000;

    private static final MiniMessage mm = MiniMessage.miniMessage();

    private static String msg(String key) {
        var m = ProfessionsModule.getInstance();
        return m != null ? m.getMessage("forge.ghost." + key) : key;
    }

    public GhostBlockSession(UUID playerId, ForgeTier tier, ForgeSchematic schematic,
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

        // Collect all solid blocks with world positions
        List<ForgeBlock> solidBlocks = schematic.getSolidBlocks();
        // Sort by Y (layer), then X, then Z for deterministic build order
        solidBlocks.sort(Comparator.comparingInt(ForgeBlock::getRelY)
                .thenComparingInt(ForgeBlock::getRelX)
                .thenComparingInt(ForgeBlock::getRelZ));

        for (ForgeBlock block : solidBlocks) {
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

    /**
     * Transitions from PREVIEWING to BUILDING.
     * Called when the player confirms the placement position.
     */
    public void confirmAndStartBuilding() {
        if (state != BuildState.PREVIEWING) { return; }
        this.state = BuildState.BUILDING;
        this.buildStartedAt = System.currentTimeMillis();
    }

    public void cancel() {
        this.state = BuildState.CANCELLED;
    }

    // ==================== Block Placement ====================

    /**
     * Called when a block is placed. Returns the result.
     * Handles material substitutes (e.g., CAULDRON accepted for WATER_CAULDRON).
     */
    public PlaceResult onBlockPlaced(Location location) {
        if (state != BuildState.BUILDING) { return PlaceResult.NOT_PART_OF_SCHEMATIC; }

        BlockPosition pos = new BlockPosition(location);
        ForgeBlock expected = blockMap.get(pos);
        if (expected == null) { return PlaceResult.NOT_PART_OF_SCHEMATIC; }

        Material placed = location.getBlock().getType();
        Material expectedMat = expected.getMaterial();

        if (placed == expectedMat || isAcceptedSubstitute(placed, expectedMat)) {
            // Auto-convert substitutes (e.g., empty cauldron → water cauldron)
            if (placed != expectedMat) {
                location.getBlock().setType(expectedMat);
            }

            remainingBlocks.remove(pos);
            completedBlocks.add(pos);
            buildOrder.remove(pos);

            // Check if this was the hint block (placed in order)
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

    /**
     * Checks if a placed material is an accepted substitute for the expected material.
     * Handles blocks that can't be placed directly (e.g., water cauldron).
     */
    private static boolean isAcceptedSubstitute(Material placed, Material expected) {
        if (expected == Material.WATER_CAULDRON && placed == Material.CAULDRON) { return true; }
        if (expected == Material.LAVA_CAULDRON && placed == Material.CAULDRON) { return true; }
        if (expected == Material.POWDER_SNOW_CAULDRON && placed == Material.CAULDRON) { return true; }
        return false;
    }

    /**
     * Called when a block is broken. Tracks regression.
     */
    public void onBlockBroken(Location location) {
        BlockPosition pos = new BlockPosition(location);
        if (completedBlocks.remove(pos)) {
            remainingBlocks.add(pos);
            // Re-insert at correct position in build order (maintain layer order)
            ForgeBlock block = blockMap.get(pos);
            if (block != null) {
                int insertIdx = 0;
                for (int i = 0; i < buildOrder.size(); i++) {
                    ForgeBlock other = blockMap.get(buildOrder.get(i));
                    if (other != null && other.getRelY() <= block.getRelY()) {
                        insertIdx = i + 1;
                    }
                }
                buildOrder.add(Math.min(insertIdx, buildOrder.size()), pos);
            }
            updateHint();

            // If we were completed, revert to building
            if (state == BuildState.COMPLETED) {
                state = BuildState.BUILDING;
            }
        }
    }

    /**
     * Rotates the schematic preview. Only allowed during PREVIEWING state.
     */
    public boolean rotate() {
        if (state != BuildState.PREVIEWING) { return false; }
        this.rotation = rotation.next();
        calculateBlocks();
        return true;
    }

    // ==================== BossBar HUD ====================

    /**
     * Creates and shows the BossBar HUD to the player.
     */
    public void showHUD(Player player) {
        bossBar = BossBar.bossBar(
                mm.deserialize(getHudText()),
                getProgress(),
                BossBar.Color.YELLOW,
                BossBar.Overlay.NOTCHED_20
        );
        player.showBossBar(bossBar);
    }

    /**
     * Updates the BossBar HUD with current progress.
     */
    public void updateHUD() {
        if (bossBar == null) { return; }

        float progress = getProgress();
        bossBar.progress(progress);
        bossBar.name(mm.deserialize(getHudText()));

        // Color changes with progress
        if (progress >= 0.75f) {
            bossBar.color(BossBar.Color.GREEN);
        } else if (progress >= 0.40f) {
            bossBar.color(BossBar.Color.YELLOW);
        } else {
            bossBar.color(BossBar.Color.WHITE);
        }
    }

    /**
     * Hides and removes the BossBar HUD.
     */
    public void hideHUD(Player player) {
        if (bossBar != null) {
            player.hideBossBar(bossBar);
            bossBar = null;
        }
    }

    private String getHudText() {
        if (state == BuildState.PREVIEWING) {
            return "<gold>⚒ " + tier.getDisplayName() + " <gray>— <yellow>" + msg("confirm_position");
        }

        int completed = getCompletedCount();
        int total = getTotalCount();
        int currentLayer = getCurrentLayer();
        int totalLayers = getTotalLayers();

        return "<gold>⚒ " + msg("building") + " " + tier.getName() +
                " <gray>— <white>" + completed + "/" + total +
                " <gray>" + msg("blocks") + " | " + msg("layer") + " <white>" + currentLayer + "/" + totalLayers;
    }

    // ==================== Progress & Info ====================

    public float getProgress() {
        int total = blockMap.size();
        if (total == 0) { return 1.0f; }
        return (float) completedBlocks.size() / total;
    }

    /**
     * Gets the current layer being built (based on the hint block).
     */
    public int getCurrentLayer() {
        if (currentHint == null) { return getTotalLayers(); }
        ForgeBlock hintBlock = blockMap.get(currentHint);
        return hintBlock != null ? hintBlock.getRelY() + 1 : 1;
    }

    /**
     * Gets total number of distinct layers in the schematic.
     */
    public int getTotalLayers() {
        return blockMap.values().stream()
                .mapToInt(ForgeBlock::getRelY)
                .max().orElse(0) + 1;
    }

    /**
     * Gets the expected material name for the next hint block.
     * Shows the material the player should physically place (e.g., "cauldron" for WATER_CAULDRON).
     */
    public String getNextBlockName() {
        if (currentHint == null) { return null; }
        ForgeBlock block = blockMap.get(currentHint);
        return block != null ? getPlaceableName(block.getMaterial()) : null;
    }

    /**
     * Gets the name of the material the player should physically place.
     * Maps unobtainable materials to their placeable equivalents.
     */
    public static String getPlaceableName(Material expected) {
        return switch (expected) {
            case WATER_CAULDRON, LAVA_CAULDRON, POWDER_SNOW_CAULDRON -> formatMaterialName(Material.CAULDRON.name());
            default -> formatMaterialName(expected.name());
        };
    }

    /**
     * Gets the world location of the current hint block.
     */
    public Location getHintLocation() {
        if (currentHint == null) { return null; }
        return currentHint.toLocation(anchor.getWorld());
    }

    /**
     * Checks if the build session has expired.
     */
    public boolean isExpired() {
        long timeout = (state == BuildState.PREVIEWING) ? 120_000 : BUILD_TIMEOUT_MS;
        return System.currentTimeMillis() - createdAt > timeout;
    }

    private static String formatMaterialName(String name) {
        return name.toLowerCase().replace('_', ' ');
    }

    // ==================== Getters ====================

    public UUID getPlayerId() { return playerId; }
    public ForgeTier getTier() { return tier; }
    public ForgeSchematic getSchematic() { return schematic; }
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

    /**
     * Gets all remaining block positions and their expected materials (for ghost rendering).
     */
    public Map<Location, ForgeBlock> getRemainingBlocksWithLocations() {
        Map<Location, ForgeBlock> result = new HashMap<>();
        for (BlockPosition pos : remainingBlocks) {
            ForgeBlock block = blockMap.get(pos);
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
        if (currentHint == null) { return -1; }
        ForgeBlock hintBlock = blockMap.get(currentHint);
        return hintBlock != null ? hintBlock.getRelY() : -1;
    }

    /**
     * Gets remaining blocks for a specific relY layer with their world locations.
     */
    public Map<Location, ForgeBlock> getRemainingBlocksForLayerWithLocations(int relY) {
        Map<Location, ForgeBlock> result = new HashMap<>();
        for (BlockPosition pos : remainingBlocks) {
            ForgeBlock block = blockMap.get(pos);
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
            ForgeBlock block = blockMap.get(pos);
            if (block != null && block.getRelY() == relY) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the expected block at a specific position.
     */
    public ForgeBlock getExpectedBlock(Location location) {
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

    /**
     * Immutable position key for block tracking.
     */
    public record BlockPosition(int x, int y, int z) {
        public BlockPosition(Location loc) {
            this(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }

        public Location toLocation(org.bukkit.World world) {
            return new Location(world, x, y, z);
        }
    }
}
