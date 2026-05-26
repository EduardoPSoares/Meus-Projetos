package me.ray.midgard.modules.professions.blacksmith.forge.admin;

import me.ray.midgard.modules.professions.blacksmith.forge.ForgeTier;
import me.ray.midgard.modules.professions.blacksmith.forge.shared.AbstractCreationSession;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeBlock;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeSchematic;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.*;

/**
 * Sessão de criação de template de forja.
 * Estende AbstractCreationSession com tipos específicos de forja.
 */
public class ForgeCreationSession extends AbstractCreationSession<ForgeBlock.ForgeBlockType> {

    private ForgeTier tier;

    private static final ForgeBlock.ForgeBlockType[] REQUIRED_TYPES = {
            ForgeBlock.ForgeBlockType.FURNACE,
            ForgeBlock.ForgeBlockType.ANVIL,
            ForgeBlock.ForgeBlockType.CAULDRON,
            ForgeBlock.ForgeBlockType.GRINDSTONE,
            ForgeBlock.ForgeBlockType.SMITHING_TABLE,
            ForgeBlock.ForgeBlockType.FUEL_ZONE
    };

    public ForgeCreationSession(UUID adminId) {
        super(adminId, "Nova Forja");
        this.tier = ForgeTier.BASIC;
    }

    // === Forge-specific ===

    public ForgeTier getTier() { return tier; }
    public void setTier(ForgeTier tier) { this.tier = tier; }

    @Override
    protected Color getWireframeColor() {
        return Color.fromRGB(0, 255, 255);
    }

    @Override
    protected ForgeBlock.ForgeBlockType getDefaultBlockType() {
        return ForgeBlock.ForgeBlockType.STRUCTURE;
    }

    @Override
    protected ForgeBlock.ForgeBlockType autoDetectType(Material mat) {
        return switch (mat) {
            case FURNACE -> ForgeBlock.ForgeBlockType.FURNACE;
            case ANVIL, CHIPPED_ANVIL, DAMAGED_ANVIL -> ForgeBlock.ForgeBlockType.ANVIL;
            case CAULDRON, WATER_CAULDRON, LAVA_CAULDRON, POWDER_SNOW_CAULDRON -> ForgeBlock.ForgeBlockType.CAULDRON;
            case GRINDSTONE -> ForgeBlock.ForgeBlockType.GRINDSTONE;
            case SMITHING_TABLE -> ForgeBlock.ForgeBlockType.SMITHING_TABLE;
            case CAMPFIRE, SOUL_CAMPFIRE -> ForgeBlock.ForgeBlockType.CAMPFIRE;
            case BLAST_FURNACE -> ForgeBlock.ForgeBlockType.BLAST_FURNACE;
            case ENCHANTING_TABLE -> ForgeBlock.ForgeBlockType.ENCHANTING_TABLE;
            case MAGMA_BLOCK -> ForgeBlock.ForgeBlockType.FUEL_ZONE;
            default -> ForgeBlock.ForgeBlockType.STRUCTURE;
        };
    }

    @Override
    protected ForgeBlock.ForgeBlockType[] getRequiredBlockTypes() {
        return REQUIRED_TYPES;
    }

    // === Schematic Building ===

    public ForgeSchematic buildSchematic() {
        Location min = getMinCorner();
        List<ForgeBlock> blocks = new ArrayList<>();

        for (ScannedBlock<ForgeBlock.ForgeBlockType> sb : getScannedBlocks()) {
            int relX = sb.worldX() - min.getBlockX();
            int relY = sb.worldY() - min.getBlockY();
            int relZ = sb.worldZ() - min.getBlockZ();

            ForgeBlock.ForgeBlockType type = getAssignedRole(sb.locationKey());
            blocks.add(new ForgeBlock(relX, relY, relZ, sb.material(), type));
        }

        return new ForgeSchematic(tier, getWidth(), getHeight(), getDepth(),
                0, 0, 0, blocks);
    }
}
