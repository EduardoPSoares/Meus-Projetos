package me.ray.midgard.modules.professions.blacksmith.forge;

/**
 * Represents the tier/level of a forge multiblock structure.
 * Higher tiers allow more advanced recipes and materials.
 */
public enum ForgeTier {

    BASIC(1, "Forja Básica", "<gray>Forja Básica</gray>", 5, 4, 5, 1),
    INTERMEDIATE(2, "Forja Intermediária", "<yellow>Forja Intermediária</yellow>", 7, 5, 7, 20),
    ADVANCED(3, "Forja Avançada", "<green>Forja Avançada</green>", 9, 6, 7, 40),
    MASTER(4, "Forja de Mestre", "<blue>Forja de Mestre</blue>", 9, 7, 9, 70),
    LEGENDARY(5, "Forja Lendária", "<light_purple>Forja Lendária</light_purple>", 11, 8, 11, 95);

    private final int level;
    private final String name;
    private final String displayName;
    private final int width;
    private final int height;
    private final int depth;
    private final int requiredProfessionLevel;

    ForgeTier(int level, String name, String displayName, int width, int height, int depth, int requiredProfessionLevel) {
        this.level = level;
        this.name = name;
        this.displayName = displayName;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.requiredProfessionLevel = requiredProfessionLevel;
    }

    public int getLevel() { return level; }
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getDepth() { return depth; }
    public int getRequiredProfessionLevel() { return requiredProfessionLevel; }

    public static ForgeTier fromLevel(int level) {
        for (ForgeTier tier : values()) {
            if (tier.level == level) { return tier; }
        }
        return null;
    }
}
