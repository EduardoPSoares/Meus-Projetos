package me.ray.midgard.modules.professions.blacksmith.forge.smeltery.admin;

import me.ray.midgard.modules.professions.blacksmith.forge.shared.AbstractCreationSession;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryBlockType;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryTier;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.schematic.SmelteryBlock;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.schematic.SmelterySchematic;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.*;

/**
 * Sessão de criação de template de smeltery.
 * Estende AbstractCreationSession com tipos específicos de fundição.
 */
public class SmelteryCreationSession extends AbstractCreationSession<SmelteryBlockType> {

    private SmelteryTier tier;

    private static final SmelteryBlockType[] REQUIRED_TYPES = {
            SmelteryBlockType.CONTROLLER,
            SmelteryBlockType.DRAIN
    };

    public SmelteryCreationSession(UUID adminId) {
        super(adminId, "Nova Fundição");
        this.tier = SmelteryTier.SMALL;
    }

    // === Smeltery-specific ===

    public SmelteryTier getTier() { return tier; }
    public void setTier(SmelteryTier tier) { this.tier = tier; }

    @Override
    protected Color getWireframeColor() {
        return Color.fromRGB(255, 100, 0);
    }

    @Override
    protected SmelteryBlockType getDefaultBlockType() {
        return SmelteryBlockType.WALL;
    }

    @Override
    protected SmelteryBlockType autoDetectType(Material mat) {
        return switch (mat) {
            case BLAST_FURNACE -> SmelteryBlockType.CONTROLLER;
            case HOPPER -> SmelteryBlockType.DRAIN;
            case TINTED_GLASS -> SmelteryBlockType.TANK_WINDOW;
            case DROPPER -> SmelteryBlockType.ITEM_INPUT;
            case BARREL -> SmelteryBlockType.FUEL_INPUT;
            case SMOOTH_STONE_SLAB -> SmelteryBlockType.CASTING_TABLE;
            case CAULDRON, WATER_CAULDRON, LAVA_CAULDRON -> SmelteryBlockType.CASTING_BASIN;
            default -> SmelteryBlockType.WALL;
        };
    }

    @Override
    protected SmelteryBlockType[] getRequiredBlockTypes() {
        return REQUIRED_TYPES;
    }

    // === Schematic Building ===

    public SmelterySchematic buildSchematic() {
        Location min = getMinCorner();
        List<SmelteryBlock> blocks = new ArrayList<>();

        for (ScannedBlock<SmelteryBlockType> sb : getScannedBlocks()) {
            int relX = sb.worldX() - min.getBlockX();
            int relY = sb.worldY() - min.getBlockY();
            int relZ = sb.worldZ() - min.getBlockZ();

            SmelteryBlockType type = getAssignedRole(sb.locationKey());
            blocks.add(new SmelteryBlock(relX, relY, relZ, sb.material(), type));
        }

        return new SmelterySchematic(tier, getWidth(), getHeight(), getDepth(),
                0, 0, 0, blocks);
    }
}
