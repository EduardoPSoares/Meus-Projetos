package com.midgard.fooddecay.multiblock;

import com.midgard.core.utils.MessageUtils;
import com.midgard.fooddecay.FoodDecayConfig;
import static com.midgard.core.utils.MessageUtils.sc;
import com.midgard.fooddecay.multiblock.MultiblockType.RB;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

/**
 * Represents an active building session (minigame) for a player.
 * Two phases: POSITIONING (adjust position/rotation with control items)
 * and BUILDING (place blocks matching holographic ghosts).
 */
public class BuildSession {

    public enum Phase { POSITIONING, BUILDING }

    private final UUID playerId;
    private final MultiblockType type;
    private final int tier;
    private final NamespacedKey entityTagKey;
    private final FoodDecayConfig config;
    private Location anchor;
    private int rotationIndex;
    private List<RB> currentRotation;

    private final Map<Location, Entity> ghostEntities = new HashMap<>();
    private final Map<Location, Material> pendingBlocks = new HashMap<>();
    private final Set<Location> completedBlocks = new HashSet<>();

    private Phase phase;
    private ItemStack[] savedInventory;
    private BossBar bossBar;
    private final long startTime;
    private int totalBlocks;

    public BuildSession(UUID playerId, MultiblockType type, int tier, Location anchor,
                         NamespacedKey entityTagKey, FoodDecayConfig config) {
        this.playerId = playerId;
        this.type = type;
        this.tier = tier;
        this.entityTagKey = entityTagKey;
        this.config = config;
        this.anchor = anchor;
        this.rotationIndex = 0;
        this.currentRotation = type.getRotations(tier).getFirst();
        this.startTime = System.currentTimeMillis();
    }

    // =========================================================================
    //  Positioning Phase
    // =========================================================================

    /**
     * Enters the POSITIONING phase: saves inventory, creates BossBar,
     * spawns ghost blocks for preview.
     */
    public void startPositioning(Player player) {
        this.phase = Phase.POSITIONING;

        // Deep clone the player's full inventory
        ItemStack[] contents = player.getInventory().getContents();
        savedInventory = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            savedInventory[i] = contents[i] != null ? contents[i].clone() : null;
        }

        // Create BossBar
        bossBar = Bukkit.createBossBar(
                MessageUtils.colorize(sc(config.msg("session-bossbar-positioning")
                        .replace("{name}", config.getMultiblockDisplayName(type)))),
                BarColor.YELLOW, BarStyle.SOLID);
        bossBar.setProgress(1.0);
        bossBar.addPlayer(player);

        spawnGhosts();
    }

    /**
     * Cycles through available rotations and respawns ghosts.
     */
    public void rotate(Player player) {
        List<List<RB>> rotations = type.getRotations(tier);
        if (rotations.size() <= 1) {
            player.playSound(player.getLocation(),
                    Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
            return;
        }
        rotationIndex = (rotationIndex + 1) % rotations.size();
        currentRotation = rotations.get(rotationIndex);
        spawnGhosts();
        player.playSound(player.getLocation(),
                Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 0.6f, 1.4f);
    }

    /**
     * Moves the anchor by the given offset and respawns ghosts.
     */
    public void moveAnchor(Player player, int dx, int dy, int dz) {
        anchor = anchor.clone().add(dx, dy, dz);
        spawnGhosts();
        player.playSound(player.getLocation(),
                Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.2f);
    }

    /**
     * Checks if all ghost positions are air blocks.
     */
    public boolean isAreaClear() {
        Location anchorLoc = toBlockLoc(anchor);
        Block anchorBlock = anchorLoc.getBlock();
        if (!anchorBlock.getType().isAir()) return false;
        for (RB rb : currentRotation) {
            if (!anchorBlock.getRelative(rb.x(), rb.y(), rb.z())
                    .getType().isAir()) return false;
        }
        return true;
    }

    /**
     * Transitions from POSITIONING to BUILDING phase.
     * Returns false if the area is not clear.
     */
    public boolean confirmPosition() {
        if (!isAreaClear()) return false;

        this.phase = Phase.BUILDING;
        pendingBlocks.clear();
        completedBlocks.clear();

        Location anchorLoc = toBlockLoc(anchor);
        pendingBlocks.put(anchorLoc, type.getAnchorMaterial(tier));
        for (RB rb : currentRotation) {
            Location loc = toBlockLoc(anchor.clone().add(rb.x(), rb.y(), rb.z()));
            pendingBlocks.put(loc, rb.material());
        }
        totalBlocks = pendingBlocks.size();

        updateBossBarBuilding();
        return true;
    }

    // =========================================================================
    //  Building Phase
    // =========================================================================

    /**
     * Called when the player places a block.
     * Returns: 1=correct, 0=not session block, -1=wrong material.
     */
    public int onBlockPlace(Block block) {
        if (phase != Phase.BUILDING) return 0;

        Location blockLoc = toBlockLoc(block.getLocation());
        Material expected = pendingBlocks.get(blockLoc);
        if (expected == null) return 0;

        if (block.getType() == expected) {
            pendingBlocks.remove(blockLoc);
            completedBlocks.add(blockLoc);

            // Force pistons to face downward
            if (block.getBlockData() instanceof Directional dir
                    && (expected == Material.PISTON || expected == Material.STICKY_PISTON)) {
                dir.setFacing(BlockFace.DOWN);
                block.setBlockData(dir, false);
            }

            Entity ghost = ghostEntities.remove(blockLoc);
            if (ghost != null && ghost.isValid()) ghost.remove();

            updateBossBarBuilding();
            return 1;
        }

        return -1;
    }

    public boolean isSessionBlock(Block block) {
        return completedBlocks.contains(toBlockLoc(block.getLocation()));
    }

    public boolean isComplete() {
        return phase == Phase.BUILDING && pendingBlocks.isEmpty();
    }

    public int getPlacedCount() { return completedBlocks.size(); }
    public int getTotalBlocks() { return totalBlocks; }
    public int getTier() { return tier; }

    public Material getExpectedMaterial(Location loc) {
        return pendingBlocks.get(toBlockLoc(loc));
    }

    // =========================================================================
    //  BossBar
    // =========================================================================

    private void updateBossBarBuilding() {
        if (bossBar == null || totalBlocks == 0) return;
        int placed = completedBlocks.size();
        bossBar.setTitle(MessageUtils.colorize(sc(config.msg("session-bossbar-building")
                .replace("{name}", config.getMultiblockDisplayName(type))
                .replace("{placed}", String.valueOf(placed))
                .replace("{total}", String.valueOf(totalBlocks)))));
        bossBar.setColor(BarColor.GREEN);
        bossBar.setProgress((double) placed / totalBlocks);
    }

    // =========================================================================
    //  Cleanup
    // =========================================================================

    public void cancel() {
        clearGhosts();
        removeBossBar();
    }

    /**
     * Restores the player's saved inventory.
     */
    public void restoreInventory(Player player) {
        if (savedInventory != null) {
            player.getInventory().setContents(savedInventory);
            savedInventory = null;
        }
    }

    public void removeBossBar() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }

    // =========================================================================
    //  Getters
    // =========================================================================

    public UUID getPlayerId() { return playerId; }
    public MultiblockType getType() { return type; }
    public Location getAnchor() { return anchor; }
    public Phase getPhase() { return phase; }
    public List<RB> getCurrentRotation() { return currentRotation; }
    public long getStartTime() { return startTime; }
    public ItemStack[] getSavedInventory() { return savedInventory; }

    /** Collects all block locations of the structure in order. */
    public List<Location> getAllBlockLocations() {
        List<Location> locs = new ArrayList<>();
        locs.add(toBlockLoc(anchor));
        for (RB rb : currentRotation) {
            locs.add(toBlockLoc(anchor.clone().add(rb.x(), rb.y(), rb.z())));
        }
        return locs;
    }

    // =========================================================================
    //  Ghost Block Management
    // =========================================================================

    private void spawnGhosts() {
        clearGhosts();

        Location anchorLoc = toBlockLoc(anchor);
        spawnGhost(anchorLoc, type.getAnchorMaterial(tier));

        for (RB rb : currentRotation) {
            Location loc = toBlockLoc(anchor.clone().add(rb.x(), rb.y(), rb.z()));
            spawnGhost(loc, rb.material());
        }
    }

    private void clearGhosts() {
        ghostEntities.values().forEach(e -> {
            if (e.isValid()) e.remove();
        });
        ghostEntities.clear();
    }

    private void spawnGhost(Location loc, Material material) {
        BlockDisplay bd = (BlockDisplay) loc.getWorld().spawnEntity(
                loc, EntityType.BLOCK_DISPLAY);
        bd.setBlock(material.createBlockData());
        bd.setTransformation(new Transformation(
                new Vector3f(0.15f, 0.15f, 0.15f),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(0.7f, 0.7f, 0.7f),
                new AxisAngle4f(0, 0, 0, 1)
        ));
        bd.setBrightness(new Display.Brightness(15, 15));
        bd.setGlowColorOverride(Color.fromRGB(80, 255, 120));
        bd.setGlowing(true);
        bd.setShadowRadius(0);
        bd.setShadowStrength(0);
        bd.getPersistentDataContainer().set(entityTagKey, PersistentDataType.BYTE, (byte) 1);
        ghostEntities.put(loc, bd);
    }

    static Location toBlockLoc(Location loc) {
        return new Location(loc.getWorld(),
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}
