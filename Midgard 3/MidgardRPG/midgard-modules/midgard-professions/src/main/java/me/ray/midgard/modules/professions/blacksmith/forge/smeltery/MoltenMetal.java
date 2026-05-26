package me.ray.midgard.modules.professions.blacksmith.forge.smeltery;

import me.ray.midgard.modules.professions.ProfessionsModule;
import org.bukkit.Color;
import org.bukkit.Material;

/**
 * Tipos de metais fundidos que podem existir no tanque da Smeltery.
 * Cada metal tem uma temperatura de fusão, cor, e propriedades para forja.
 */
public enum MoltenMetal {

    // Metais base
    IRON("Ferro Fundido", "<white>Ferro Fundido", 1538, Material.IRON_INGOT, Color.fromRGB(200, 200, 200), 1.0, false),
    GOLD("Ouro Fundido", "<gold>Ouro Fundido", 1064, Material.GOLD_INGOT, Color.fromRGB(255, 215, 0), 0.6, false),
    COPPER("Cobre Fundido", "<#E87E04>Cobre Fundido", 1085, Material.COPPER_INGOT, Color.fromRGB(232, 126, 4), 0.8, false),
    NETHERITE_SCRAP("Netherite Fundido", "<dark_red>Netherite Fundido", 2500, Material.NETHERITE_SCRAP, Color.fromRGB(80, 40, 40), 2.0, false),
    EMERALD("Esmeralda Fundida", "<green>Esmeralda Fundida", 1400, Material.EMERALD, Color.fromRGB(0, 200, 50), 0.5, false),
    DIAMOND("Diamante Fundido", "<aqua>Diamante Fundido", 1800, Material.DIAMOND, Color.fromRGB(80, 220, 255), 1.5, false),
    AMETHYST("Ametista Fundida", "<light_purple>Ametista Fundida", 1200, Material.AMETHYST_SHARD, Color.fromRGB(160, 80, 200), 0.7, false),
    QUARTZ("Quartzo Fundido", "<white>Quartzo Fundido", 1100, Material.QUARTZ, Color.fromRGB(230, 220, 210), 0.4, false),
    LAPIS("Lápis Fundido", "<blue>Lápis Fundido", 900, Material.LAPIS_LAZULI, Color.fromRGB(40, 60, 200), 0.3, false),
    REDSTONE("Redstone Fundida", "<red>Redstone Fundida", 800, Material.REDSTONE, Color.fromRGB(200, 0, 0), 0.3, false),

    // Ligas (alloys) - formadas automaticamente
    BRONZE("Bronze Fundido", "<#CD7F32>Bronze Fundido", 950, null, Color.fromRGB(205, 127, 50), 1.2, true),
    STEEL("Aço Fundido", "<gray>Aço Fundido", 1600, null, Color.fromRGB(160, 160, 170), 1.5, true),
    MANYULLYN("Manyullyn Fundido", "<dark_purple>Manyullyn Fundido", 2200, null, Color.fromRGB(120, 0, 180), 2.5, true),
    OBSIDIAN_ALLOY("Obsidiana Forjada", "<dark_gray>Obsidiana Forjada", 2000, null, Color.fromRGB(40, 10, 60), 1.8, true),
    ROSE_GOLD("Ouro Rosa Fundido", "<#F5A0C0>Ouro Rosa Fundido", 1050, null, Color.fromRGB(245, 160, 192), 0.9, true),
    ELECTRUM("Electrum Fundido", "<yellow>Electrum Fundido", 1060, null, Color.fromRGB(255, 250, 140), 1.1, true),
    KNIGHTSLIME("Knightslime Fundido", "<dark_aqua>Knightslime Fundido", 1500, null, Color.fromRGB(200, 80, 200), 1.8, true);

    private final String displayName;
    private final String formattedName; // MiniMessage format
    private final int meltingPoint; // temperatura de fusão em °C
    private final Material sourceItem; // item sólido que produz esse metal (null para ligas)
    private final Color color;
    private final double hardness; // afeta stats das partes
    private final boolean alloy; // se é uma liga formada por combinação

    MoltenMetal(String displayName, String formattedName, int meltingPoint,
                Material sourceItem, Color color, double hardness, boolean alloy) {
        this.displayName = displayName;
        this.formattedName = formattedName;
        this.meltingPoint = meltingPoint;
        this.sourceItem = sourceItem;
        this.color = color;
        this.hardness = hardness;
        this.alloy = alloy;
    }

    public String getDisplayName() {
        var m = ProfessionsModule.getInstance();
        return m != null ? m.getMessage("molten_metal." + name().toLowerCase()) : displayName;
    }
    public String getFormattedName() {
        String prefix = formattedName.substring(0, formattedName.length() - displayName.length());
        return prefix + getDisplayName();
    }
    public int getMeltingPoint() { return meltingPoint; }
    public Material getSourceItem() { return sourceItem; }
    public Color getColor() { return color; }
    public double getHardness() { return hardness; }
    public boolean isAlloy() { return alloy; }

    /**
     * Quantidade de unidades de metal fundido produzidas por item sólido.
     * 1 ingot = 144mb (millibuckets) seguindo o padrão Tinkers
     */
    public int getUnitsPerItem() {
        if (sourceItem == null) {
            return 0;
        }
        return switch (sourceItem) {
            case IRON_INGOT, GOLD_INGOT, COPPER_INGOT -> 144;
            case NETHERITE_SCRAP -> 144;
            case DIAMOND, EMERALD -> 666;
            case AMETHYST_SHARD -> 250;
            case QUARTZ -> 250;
            case LAPIS_LAZULI -> 100;
            case REDSTONE -> 100;
            default -> 144;
        };
    }

    /**
     * Encontra o metal fundido baseado no Material do item sólido.
     */
    public static MoltenMetal fromSourceItem(Material material) {
        for (MoltenMetal metal : values()) {
            if (metal.sourceItem == material) {
                return metal;
            }
        }
        return null;
    }

    /**
     * Tempo de fusão em ticks baseado na quantidade de itens.
     */
    public int getSmeltTimePerItem() {
        return (int) (meltingPoint / 10.0); // escala com temperatura de fusão
    }

    /**
     * Material do bloco usado para representar visualmente.
     */
    public Material getVisualBlock() {
        return switch (this) {
            case IRON -> Material.IRON_BLOCK;
            case GOLD, ROSE_GOLD, ELECTRUM -> Material.GOLD_BLOCK;
            case COPPER, BRONZE -> Material.COPPER_BLOCK;
            case NETHERITE_SCRAP, MANYULLYN -> Material.NETHERITE_BLOCK;
            case EMERALD -> Material.EMERALD_BLOCK;
            case DIAMOND -> Material.DIAMOND_BLOCK;
            case AMETHYST, KNIGHTSLIME -> Material.AMETHYST_BLOCK;
            case QUARTZ -> Material.QUARTZ_BLOCK;
            case LAPIS -> Material.LAPIS_BLOCK;
            case REDSTONE -> Material.REDSTONE_BLOCK;
            case STEEL -> Material.IRON_BLOCK;
            case OBSIDIAN_ALLOY -> Material.OBSIDIAN;
        };
    }
}
