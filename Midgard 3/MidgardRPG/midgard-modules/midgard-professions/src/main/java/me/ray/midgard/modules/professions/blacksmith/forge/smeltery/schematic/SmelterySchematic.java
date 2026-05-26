package me.ray.midgard.modules.professions.blacksmith.forge.smeltery.schematic;

import me.ray.midgard.modules.professions.blacksmith.forge.ForgeRotation;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryBlockType;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryTier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.*;

/**
 * Representa o esquemático de uma smeltery — o blueprint de blocos que formam a fundição.
 * Análogo ao ForgeSchematic, adaptado para SmelteryBlockType.
 */
public class SmelterySchematic {

    private final SmelteryTier tier;
    private final int width;
    private final int height;
    private final int depth;
    private final int anchorX;
    private final int anchorY;
    private final int anchorZ;
    private final List<SmelteryBlock> blocks;
    private final Map<SmelteryBlockType, SmelteryBlock> interactiveBlocks;

    public SmelterySchematic(SmelteryTier tier, int width, int height, int depth,
                             int anchorX, int anchorY, int anchorZ,
                             List<SmelteryBlock> blocks) {
        this.tier = tier;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
        this.blocks = Collections.unmodifiableList(blocks);
        this.interactiveBlocks = new HashMap<>();
        for (SmelteryBlock block : blocks) {
            if (block.isInteractive()) {
                interactiveBlocks.put(block.getBlockType(), block);
            }
        }
    }

    public SmelteryTier getTier() { return tier; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getDepth() { return depth; }
    public int getAnchorX() { return anchorX; }
    public int getAnchorY() { return anchorY; }
    public int getAnchorZ() { return anchorZ; }
    public List<SmelteryBlock> getBlocks() { return blocks; }

    /**
     * Retorna todos os blocos sólidos (não-ar) que precisam ser colocados.
     */
    public List<SmelteryBlock> getSolidBlocks() {
        List<SmelteryBlock> solid = new ArrayList<>();
        for (SmelteryBlock block : blocks) {
            if (!block.isAir()) { solid.add(block); }
        }
        return solid;
    }

    public SmelteryBlock getInteractiveBlock(SmelteryBlockType type) {
        return interactiveBlocks.get(type);
    }

    public Map<SmelteryBlockType, SmelteryBlock> getInteractiveBlocks() {
        return Collections.unmodifiableMap(interactiveBlocks);
    }

    /**
     * Converte posição relativa de um bloco para localização no mundo, aplicando rotação.
     */
    public Location toWorldLocation(Location anchor, ForgeRotation rotation, SmelteryBlock block) {
        int relX = block.getRelX() - anchorX;
        int relY = block.getRelY() - anchorY;
        int relZ = block.getRelZ() - anchorZ;

        int[] rotated = rotation.rotate(relX, relZ, width, depth);

        return anchor.clone().add(rotated[0], relY, rotated[1]);
    }

    /**
     * Valida se os blocos no mundo correspondem a este esquemático.
     */
    public ValidationResult validate(Location anchor, ForgeRotation rotation) {
        List<SmelteryBlock> correct = new ArrayList<>();
        List<SmelteryBlock> missing = new ArrayList<>();
        List<SmelteryBlock> wrong = new ArrayList<>();

        for (SmelteryBlock schematicBlock : getSolidBlocks()) {
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
     * Verifica se há espaço suficiente para construir a smeltery nesta posição.
     */
    public boolean hasEnoughSpace(Location anchor, ForgeRotation rotation) {
        for (SmelteryBlock block : getSolidBlocks()) {
            Location worldLoc = toWorldLocation(anchor, rotation, block);
            Block worldBlock = worldLoc.getBlock();
            if (worldBlock.getType() != Material.AIR && worldBlock.getType() != block.getMaterial()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Resultado da validação do esquemático contra os blocos do mundo.
     */
    public record ValidationResult(
            List<SmelteryBlock> correct,
            List<SmelteryBlock> missing,
            List<SmelteryBlock> wrong
    ) {
        public boolean isComplete() {
            return missing.isEmpty() && wrong.isEmpty();
        }

        public int totalRequired() {
            return correct.size() + missing.size() + wrong.size();
        }

        public double completionPercent() {
            int total = totalRequired();
            if (total == 0) {
                return 1.0;
            }
            return (double) correct.size() / total;
        }
    }

    /** Cached instance of the basic smeltery schematic (immutable). */
    private static volatile SmelterySchematic BASIC_CACHE;

    /**
     * Cria (ou retorna em cache) o esquemático básico de uma smeltery SMALL (5x5x5).
     * Paredes de nether bricks, controller, drain, item input, fuel input.
     * Thread-safe lazy initialization.
     */
    public static SmelterySchematic createBasicSmeltery() {
        if (BASIC_CACHE != null) { return BASIC_CACHE; }
        synchronized (SmelterySchematic.class) {
            if (BASIC_CACHE != null) { return BASIC_CACHE; }
            BASIC_CACHE = buildBasicSmeltery();
            return BASIC_CACHE;
        }
    }

    private static SmelterySchematic buildBasicSmeltery() {
        List<SmelteryBlock> blocks = new ArrayList<>();

        int tw = 5, th = 5, td = 5;

        for (int x = 0; x < tw; x++) {
            for (int y = 0; y < th; y++) {
                for (int z = 0; z < td; z++) {
                    boolean isEdgeX = x == 0 || x == tw - 1;
                    boolean isEdgeZ = z == 0 || z == td - 1;
                    boolean isBottom = y == 0;
                    boolean isTop = y == th - 1;
                    boolean isInterior = !isEdgeX && !isEdgeZ && !isBottom && !isTop;

                    if (isInterior) {
                        blocks.add(new SmelteryBlock(x, y, z, Material.AIR, SmelteryBlockType.AIR));
                        continue;
                    }

                    // Default: parede de nether bricks
                    blocks.add(new SmelteryBlock(x, y, z, Material.NETHER_BRICKS, SmelteryBlockType.WALL));
                }
            }
        }

        // Override blocos interativos
        replaceBlock(blocks, 2, 2, 0, Material.BLAST_FURNACE, SmelteryBlockType.CONTROLLER);
        replaceBlock(blocks, 2, 1, 0, Material.HOPPER, SmelteryBlockType.DRAIN);
        replaceBlock(blocks, 0, 2, 2, Material.DROPPER, SmelteryBlockType.ITEM_INPUT);
        replaceBlock(blocks, 2, 0, 2, Material.BARREL, SmelteryBlockType.FUEL_INPUT);
        replaceBlock(blocks, 4, 2, 2, Material.TINTED_GLASS, SmelteryBlockType.TANK_WINDOW);

        return new SmelterySchematic(SmelteryTier.SMALL, tw, th, td, 2, 0, 2, blocks);
    }

    private static void replaceBlock(List<SmelteryBlock> blocks, int x, int y, int z,
                                      Material material, SmelteryBlockType type) {
        blocks.removeIf(b -> b.getRelX() == x && b.getRelY() == y && b.getRelZ() == z);
        blocks.add(new SmelteryBlock(x, y, z, material, type));
    }
}
