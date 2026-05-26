package me.ray.midgard.modules.professions.blacksmith.forge.smeltery.schematic;

import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryBlockType;
import org.bukkit.Material;
import org.bukkit.util.Vector;

/**
 * Representa um bloco individual dentro de um esquemático de smeltery.
 */
public class SmelteryBlock {

    private final int relX;
    private final int relY;
    private final int relZ;
    private final Material material;
    private final SmelteryBlockType blockType;

    public SmelteryBlock(int relX, int relY, int relZ, Material material, SmelteryBlockType blockType) {
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
    public SmelteryBlockType getBlockType() { return blockType; }

    public Vector toVector() {
        return new Vector(relX, relY, relZ);
    }

    public boolean isAir() {
        return material == Material.AIR || material == null;
    }

    public boolean isInteractive() {
        return blockType != SmelteryBlockType.WALL && blockType != SmelteryBlockType.AIR;
    }
}
