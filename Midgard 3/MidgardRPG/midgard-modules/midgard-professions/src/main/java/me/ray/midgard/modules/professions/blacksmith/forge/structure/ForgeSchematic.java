package me.ray.midgard.modules.professions.blacksmith.forge.structure;

import me.ray.midgard.modules.professions.blacksmith.forge.ForgeRotation;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeTier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.*;

/**
 * Represents a forge schematic — the blueprint of blocks that form a forge.
 * Loaded from configuration and used to guide construction via ghost blocks
 * and to validate existing structures.
 */
public class ForgeSchematic {

    private final ForgeTier tier;
    private final int width;
    private final int height;
    private final int depth;
    private final int anchorX;
    private final int anchorY;
    private final int anchorZ;
    private final List<ForgeBlock> blocks;
    private final Map<ForgeBlock.ForgeBlockType, ForgeBlock> interactiveBlocks;

    public ForgeSchematic(ForgeTier tier, int width, int height, int depth,
                          int anchorX, int anchorY, int anchorZ,
                          List<ForgeBlock> blocks) {
        this.tier = tier;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
        this.blocks = Collections.unmodifiableList(blocks);
        this.interactiveBlocks = new HashMap<>();
        for (ForgeBlock block : blocks) {
            if (block.isInteractive()) {
                interactiveBlocks.put(block.getBlockType(), block);
            }
        }
    }

    public ForgeTier getTier() { return tier; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getDepth() { return depth; }
    public int getAnchorX() { return anchorX; }
    public int getAnchorY() { return anchorY; }
    public int getAnchorZ() { return anchorZ; }
    public List<ForgeBlock> getBlocks() { return blocks; }

    /**
     * Gets all non-air blocks that need to be physically placed.
     * Excludes FUEL_ZONE blocks (they are invisible zone markers).
     */
    public List<ForgeBlock> getSolidBlocks() {
        List<ForgeBlock> solid = new ArrayList<>();
        for (ForgeBlock block : blocks) {
            if (!block.isAir() && !block.isFuelZone()) { solid.add(block); }
        }
        return solid;
    }

    /**
     * Gets all FUEL_ZONE blocks (invisible zone markers for fuel placement).
     */
    public List<ForgeBlock> getFuelZoneBlocks() {
        List<ForgeBlock> zones = new ArrayList<>();
        for (ForgeBlock block : blocks) {
            if (block.isFuelZone()) { zones.add(block); }
        }
        return zones;
    }

    /**
     * Gets the interactive block for a specific function.
     */
    public ForgeBlock getInteractiveBlock(ForgeBlock.ForgeBlockType type) {
        return interactiveBlocks.get(type);
    }

    /**
     * Returns all interactive blocks.
     */
    public Map<ForgeBlock.ForgeBlockType, ForgeBlock> getInteractiveBlocks() {
        return Collections.unmodifiableMap(interactiveBlocks);
    }

    /**
     * Converts a relative block position to a world location, applying rotation.
     */
    public Location toWorldLocation(Location anchor, ForgeRotation rotation, ForgeBlock block) {
        int relX = block.getRelX() - anchorX;
        int relY = block.getRelY() - anchorY;
        int relZ = block.getRelZ() - anchorZ;

        int[] rotated = rotation.rotate(relX, relZ, width, depth);

        return anchor.clone().add(rotated[0], relY, rotated[1]);
    }

    /**
     * Validates whether the world blocks at the anchor match this schematic.
     *
     * @return A validation result with details about matching/missing blocks.
     */
    public ValidationResult validate(Location anchor, ForgeRotation rotation) {
        List<ForgeBlock> correct = new ArrayList<>();
        List<ForgeBlock> missing = new ArrayList<>();
        List<ForgeBlock> wrong = new ArrayList<>();

        for (ForgeBlock schematicBlock : getSolidBlocks()) {
            Location worldLoc = toWorldLocation(anchor, rotation, schematicBlock);
            Block worldBlock = worldLoc.getBlock();

            if (worldBlock.getType() == schematicBlock.getMaterial()) {
                correct.add(schematicBlock);
            } else if (worldBlock.getType() == Material.AIR) {
                missing.add(schematicBlock);
            } else {
                wrong.add(schematicBlock);
            }
        }

        return new ValidationResult(correct, missing, wrong);
    }

    /**
     * Checks if the given location has enough space for this schematic.
     */
    public boolean hasEnoughSpace(Location anchor, ForgeRotation rotation) {
        for (ForgeBlock block : getSolidBlocks()) {
            Location worldLoc = toWorldLocation(anchor, rotation, block);
            Block worldBlock = worldLoc.getBlock();
            if (worldBlock.getType() != Material.AIR && worldBlock.getType() != block.getMaterial()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Result of a schematic validation against world blocks.
     */
    public record ValidationResult(
            List<ForgeBlock> correct,
            List<ForgeBlock> missing,
            List<ForgeBlock> wrong
    ) {
        public boolean isComplete() {
            return missing.isEmpty() && wrong.isEmpty();
        }

        public int totalRequired() {
            return correct.size() + missing.size() + wrong.size();
        }

        public double completionPercent() {
            int total = totalRequired();
            if (total == 0) { return 1.0; }
            return (double) correct.size() / total;
        }
    }

    /** Cached instance of the basic forge schematic (immutable). */
    private static volatile ForgeSchematic BASIC_CACHE;

    /**
     * Creates (or returns cached) the default basic forge schematic (Tier 1).
     * 5x4x5 multiblock structure. Thread-safe lazy initialization.
     */
    public static ForgeSchematic createBasicForge() {
        if (BASIC_CACHE != null) { return BASIC_CACHE; }
        synchronized (ForgeSchematic.class) {
            if (BASIC_CACHE != null) { return BASIC_CACHE; }
            BASIC_CACHE = buildBasicForge();
            return BASIC_CACHE;
        }
    }

    private static ForgeSchematic buildBasicForge() {
        List<ForgeBlock> blocks = new ArrayList<>();

        // Layer 0 (floor) - 5x5 stone bricks with iron block pillars
        Material S = Material.STONE_BRICKS;
        Material I = Material.IRON_BLOCK;

        // Floor layer (y=0)
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                Material mat = S;
                if ((x == 1 || x == 3) && (z == 1 || z == 3)) {
                    mat = I;
                }
                blocks.add(new ForgeBlock(x, 0, z, mat, ForgeBlock.ForgeBlockType.STRUCTURE));
            }
        }

        // Layer 1 (main) - Interactive blocks + air
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                blocks.add(new ForgeBlock(x, 1, z, Material.AIR, ForgeBlock.ForgeBlockType.AIR));
            }
        }
        // Override interactive positions
        blocks.removeIf(b -> b.getRelY() == 1 && b.getRelX() == 1 && b.getRelZ() == 1);
        blocks.add(new ForgeBlock(1, 1, 1, Material.FURNACE, ForgeBlock.ForgeBlockType.FURNACE));

        blocks.removeIf(b -> b.getRelY() == 1 && b.getRelX() == 2 && b.getRelZ() == 2);
        blocks.add(new ForgeBlock(2, 1, 2, Material.ANVIL, ForgeBlock.ForgeBlockType.ANVIL));

        blocks.removeIf(b -> b.getRelY() == 1 && b.getRelX() == 3 && b.getRelZ() == 1);
        blocks.add(new ForgeBlock(3, 1, 1, Material.WATER_CAULDRON, ForgeBlock.ForgeBlockType.CAULDRON));

        blocks.removeIf(b -> b.getRelY() == 1 && b.getRelX() == 3 && b.getRelZ() == 3);
        blocks.add(new ForgeBlock(3, 1, 3, Material.GRINDSTONE, ForgeBlock.ForgeBlockType.GRINDSTONE));

        blocks.removeIf(b -> b.getRelY() == 1 && b.getRelX() == 1 && b.getRelZ() == 3);
        blocks.add(new ForgeBlock(1, 1, 3, Material.SMITHING_TABLE, ForgeBlock.ForgeBlockType.SMITHING_TABLE));

        blocks.removeIf(b -> b.getRelY() == 1 && b.getRelX() == 2 && b.getRelZ() == 4);
        blocks.add(new ForgeBlock(2, 1, 4, Material.CAMPFIRE, ForgeBlock.ForgeBlockType.CAMPFIRE));

        // Fuel zone — ground area where players drop fuel items (invisible in world)
        // Multiple blocks define the zone area along one side of the forge
        blocks.removeIf(b -> b.getRelY() == 1 && b.getRelX() == 0 && b.getRelZ() == 1);
        blocks.add(new ForgeBlock(0, 1, 1, Material.MAGMA_BLOCK, ForgeBlock.ForgeBlockType.FUEL_ZONE));
        blocks.removeIf(b -> b.getRelY() == 1 && b.getRelX() == 0 && b.getRelZ() == 2);
        blocks.add(new ForgeBlock(0, 1, 2, Material.MAGMA_BLOCK, ForgeBlock.ForgeBlockType.FUEL_ZONE));
        blocks.removeIf(b -> b.getRelY() == 1 && b.getRelX() == 0 && b.getRelZ() == 3);
        blocks.add(new ForgeBlock(0, 1, 3, Material.MAGMA_BLOCK, ForgeBlock.ForgeBlockType.FUEL_ZONE));

        // Layer 2 - Furnace chimney start
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                blocks.add(new ForgeBlock(x, 2, z, Material.AIR, ForgeBlock.ForgeBlockType.AIR));
            }
        }
        blocks.removeIf(b -> b.getRelY() == 2 && b.getRelX() == 1 && b.getRelZ() == 1);
        blocks.add(new ForgeBlock(1, 2, 1, Material.FURNACE, ForgeBlock.ForgeBlockType.STRUCTURE));

        // Layer 3 - Chimney top
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                blocks.add(new ForgeBlock(x, 3, z, Material.AIR, ForgeBlock.ForgeBlockType.AIR));
            }
        }
        blocks.removeIf(b -> b.getRelY() == 3 && b.getRelX() == 1 && b.getRelZ() == 1);
        blocks.add(new ForgeBlock(1, 3, 1, Material.STONE_BRICKS, ForgeBlock.ForgeBlockType.STRUCTURE));

        return new ForgeSchematic(ForgeTier.BASIC, 5, 4, 5, 2, 0, 2, blocks);
    }
}
