package me.ray.midgard.modules.professions.blacksmith.forge.structure;

import org.bukkit.Material;
import org.bukkit.util.Vector;

/**
 * Represents a single block within a forge schematic.
 */
public class ForgeBlock {

    private final int relX;
    private final int relY;
    private final int relZ;
    private final Material material;
    private final ForgeBlockType blockType;

    public ForgeBlock(int relX, int relY, int relZ, Material material, ForgeBlockType blockType) {
        this.relX = relX;
        this.relY = relY;
        this.relZ = relZ;
        this.material = material;
        this.blockType = blockType;
    }

    public int getRelX() { return relX; }
    public int getRelY() { return relY; }
    public int getRelZ() { return relZ; }
    public Material getMaterial() { return material; }
    public ForgeBlockType getBlockType() { return blockType; }

    public Vector toVector() {
        return new Vector(relX, relY, relZ);
    }

    public boolean isAir() {
        return material == Material.AIR || material == null;
    }

    public boolean isInteractive() {
        return blockType != ForgeBlockType.STRUCTURE
                && blockType != ForgeBlockType.AIR
                && blockType != ForgeBlockType.FUEL_ZONE;
    }

    public boolean isFuelZone() {
        return blockType == ForgeBlockType.FUEL_ZONE;
    }

    /**
     * Types of blocks in a forge schematic.
     */
    public enum ForgeBlockType {
        AIR,              // Empty space
        STRUCTURE,        // Structural block (stone bricks, etc.)
        FURNACE,          // Heating station
        ANVIL,            // Hammering station
        CAULDRON,         // Quenching station
        GRINDSTONE,       // Sharpening station
        SMITHING_TABLE,   // Recipe selection hub
        CAMPFIRE,         // Auxiliary heating
        BLAST_FURNACE,    // Advanced heating (tier 2+)
        ENCHANTING_TABLE, // Rune engraving (tier 4+)
        FUEL_ZONE         // Fuel zone — invisible area where fuel items are dropped
    }
}
