package me.ray.midgard.modules.professions.blacksmith.forge.smeltery;

import org.bukkit.Material;

/**
 * Tipos de blocos que compõem a Smeltery multibloco.
 */
public enum SmelteryBlockType {

    /** Bloco estrutural das paredes/base da smeltery */
    WALL(Material.NETHER_BRICKS, false),

    /** Controlador - bloco principal que ativa a smeltery */
    CONTROLLER(Material.BLAST_FURNACE, true),

    /** Dreno/Faucet - por onde o metal líquido escorre para o casting */
    DRAIN(Material.HOPPER, true),

    /** Tanque de visualização - mostra nível de metal fundido */
    TANK_WINDOW(Material.TINTED_GLASS, false),

    /** Entrada de itens - onde o jogador joga materiais para fundir */
    ITEM_INPUT(Material.DROPPER, true),

    /** Saída de fuel/lava - alimenta a smeltery por baixo */
    FUEL_INPUT(Material.BARREL, true),

    /** Ar interno - espaço vazio dentro da smeltery */
    AIR(Material.AIR, false),

    /** Casting Table - mesa de fundição lateral */
    CASTING_TABLE(Material.SMOOTH_STONE_SLAB, true),

    /** Casting Basin - bacia de fundição lateral */
    CASTING_BASIN(Material.CAULDRON, true);

    private final Material defaultMaterial;
    private final boolean interactive;

    SmelteryBlockType(Material defaultMaterial, boolean interactive) {
        this.defaultMaterial = defaultMaterial;
        this.interactive = interactive;
    }

    public Material getDefaultMaterial() { return defaultMaterial; }
    public boolean isInteractive() { return interactive; }
}
